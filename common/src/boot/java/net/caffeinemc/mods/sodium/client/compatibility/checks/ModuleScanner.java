package net.caffeinemc.mods.sodium.client.compatibility.checks;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32Util;
import net.caffeinemc.mods.sodium.client.platform.MessageBox;
import net.caffeinemc.mods.sodium.client.platform.NativeWindowHandle;
import net.caffeinemc.mods.sodium.client.platform.windows.WindowsFileVersion;
import net.caffeinemc.mods.sodium.client.platform.windows.api.Kernel32;
import net.caffeinemc.mods.sodium.client.platform.windows.api.version.Version;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用于判断当前进程是否被注入或以其他方式修改的实用类。
 * 通常应在 OpenGL 上下文创建后访问，因为大多数第三方软件会等到 OpenGL ICD 初始化后才进行注入。
 */
public class ModuleScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-Win32ModuleChecks");

    private static final String[] RTSS_HOOKS_MODULE_NAMES = {
            "RTSSHooks64.dll",
            "RTSSHooks.dll"
    };

    private static final String[] ASUS_GPU_TWEAK_MODULE_NAMES = {
            "GTIII-OSD64-GL.dll",   "GTIII-OSD-GL.dll",
            "GTIII-OSD64-VK.dll",   "GTIII-OSD-VK.dll",
            "GTIII-OSD64.dll",      "GTIII-OSD.dll"
    };

    public static void checkModules(NativeWindowHandle window) {
        List<String> modules;

        try {
            modules = listModules();
        } catch (Throwable t) {
            LOGGER.warn("扫描当前加载的模块失败", t);
            return;
        }

        if (modules.isEmpty()) {
            return;
        }

        // RivaTuner 会挂钩 wglCreateContext() 函数以进行自我注入，即使进程在设置中被加入黑名单也会注入。
        // 阻止它注入的唯一方法是完全关闭服务器进程。
        if (BugChecks.ISSUE_2048 && isModuleLoaded(modules, RTSS_HOOKS_MODULE_NAMES)) {
            checkRTSSModules(window);
        }

        // ASUS GPU Tweak III 会挂钩 SwapBuffers() 函数以进行自我注入，即使禁用了屏幕显示（OSD）也会注入。
        // 阻止它挂钩游戏的唯一方法是将 Java 进程添加到黑名单，或完全卸载该应用程序。
        if (BugChecks.ISSUE_2637 && isModuleLoaded(modules, ASUS_GPU_TWEAK_MODULE_NAMES)) {
            checkASUSGpuTweakIII(window);
        }
    }

    private static List<String> listModules() {
        if (!Platform.isWindows()) {
            return List.of();
        }

        var pid = com.sun.jna.platform.win32.Kernel32.INSTANCE.GetCurrentProcessId();
        var modules = new ArrayList<String>();

        for (var module : Kernel32Util.getModules(pid)) {
            modules.add(module.szModule());
        }

        return Collections.unmodifiableList(modules);
    }

    private static void checkRTSSModules(NativeWindowHandle window) {
        LOGGER.warn("RivaTuner Statistics Server (RTSS) 已注入到进程中！正在尝试应用兼容性解决方法...");

        @Nullable WindowsFileVersion version = null;

        try {
            version = findRTSSModuleVersion();
        } catch (Throwable t) {
            LOGGER.warn("读取文件版本时抛出异常", t);
        }

        if (version == null) {
            LOGGER.warn("无法确定 RivaTuner Statistics Server 的版本");
        } else {
            LOGGER.info("检测到 RivaTuner Statistics Server 版本：{}", version);
        }

        if (version == null || !isRTSSCompatible(version)) {
            MessageBox.showMessageBox(window, MessageBox.IconType.ERROR, "Sodium 渲染器",
                    """
                            您似乎正在使用与 Sodium 不兼容的旧版 RivaTuner Statistics Server (RTSS)。
                            
                            您必须更新到较新版本（7.3.4 及更高版本）或关闭 RivaTuner Statistics Server 应用程序。

                            有关如何解决此问题的更多信息，请单击“帮助”按钮。""",
                    "https://link.caffeinemc.net/help/sodium/incompatible-software/rivatuner-statistics-server/gh-2048");

            throw new RuntimeException("安装的 RivaTuner Statistics Server (RTSS) 版本与 Sodium 不兼容，" +
                    "详情请参阅：https://link.caffeinemc.net/help/sodium/incompatible-software/rivatuner-statistics-server/gh-2048");
        }
    }

    private static boolean isRTSSCompatible(WindowsFileVersion version) {
        int x = version.x();
        int y = version.y();
        int z = version.z();

        // 版本 >= 7.3.4
        return x > 7 || (x == 7 && y > 3) || (x == 7 && y == 3 && z >= 4);
    }

    private static void checkASUSGpuTweakIII(NativeWindowHandle window) {
        MessageBox.showMessageBox(window, MessageBox.IconType.ERROR, "Sodium 渲染器",
                """
                        ASUS GPU Tweak III 与 Minecraft 不兼容，在 Minecraft 中使用时会导致严重的性能问题和图形损坏。
                        
                        您必须执行以下操作之一才能继续：
                        
                        a) 打开 ASUS GPU Tweak III 的设置，启用黑名单选项，单击“从文件浏览...”，然后选择 Minecraft 使用的 Java 运行时（javaw.exe）。
                        
                        b) 完全卸载 ASUS GPU Tweak III 应用程序。
                        
                        有关如何解决此问题的更多信息，请单击“帮助”按钮。""",
                "https://link.caffeinemc.net/help/sodium/incompatible-software/asus-gtiii/gh-2637");

        throw new RuntimeException("ASUS GPU Tweak III 与 Minecraft 不兼容，" +
                "详情请参阅：https://link.caffeinemc.net/help/sodium/incompatible-software/asus-gtiii/gh-2637");
    }

    private static @Nullable WindowsFileVersion findRTSSModuleVersion() {
        long module;

        try {
            module = Kernel32.getModuleHandleByNames(RTSS_HOOKS_MODULE_NAMES);
        } catch (Throwable t) {
            LOGGER.warn("定位模块失败", t);
            return null;
        }

        String moduleFileName;

        try {
            moduleFileName = Kernel32.getModuleFileName(module);
        } catch (Throwable t) {
            LOGGER.warn("获取模块路径失败", t);
            return null;
        }

        var modulePath = Path.of(moduleFileName);
        var moduleDirectory = modulePath.getParent();

        LOGGER.info("正在搜索目录：{}", moduleDirectory);

        var executablePath = moduleDirectory.resolve("RTSS.exe");

        if (!Files.exists(executablePath)) {
            LOGGER.warn("找不到可执行文件：{}", executablePath);
            return null;
        }

        LOGGER.info("正在解析文件：{}", executablePath);

        var version = Version.getModuleFileVersion(executablePath.toAbsolutePath().toString());

        if (version == null) {
            LOGGER.warn("找不到版本结构");
            return null;
        }

        var fileVersion = version.queryFixedFileInfo();

        if (fileVersion == null) {
            LOGGER.warn("无法查询文件版本");
            return null;
        }

        return WindowsFileVersion.fromFileVersion(fileVersion);
    }

    private static boolean isModuleLoaded(List<String> modules, String[] names) {
        for (var name : names) {
            for (var module : modules) {
                if (module.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }

        return false;
    }
}