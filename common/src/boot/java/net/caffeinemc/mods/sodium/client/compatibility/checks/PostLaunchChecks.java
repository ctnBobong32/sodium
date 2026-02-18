package net.caffeinemc.mods.sodium.client.compatibility.checks;

import net.caffeinemc.mods.sodium.client.compatibility.environment.GlContextInfo;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.caffeinemc.mods.sodium.client.platform.NativeWindowHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在 OpenGL 上下文初始化后执行驱动检查和兼容性处理。
 * 已禁用 PojavLauncher 检测，避免在 Android 环境下崩溃。
 */
public class PostLaunchChecks {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-PostlaunchChecks");

    public static void onContextInitialized(NativeWindowHandle window, GlContextInfo context) {
        GraphicsDriverChecks.postContextInit(window, context);
        NvidiaWorkarounds.applyContextChanges(context);
    }

    // 检测逻辑保留，仅用于日志记录（可选）
    private static boolean isUsingPojavLauncher() {
        if (System.getenv("POJAV_RENDERER") != null) {
            LOGGER.warn("检测到环境变量 POJAV_RENDERER，当前运行环境为 Android");
            return true;
        }

        var librarySearchPaths = System.getProperty("java.library.path", null);
        if (librarySearchPaths != null) {
            for (var path : librarySearchPaths.split(":")) {
                if (isKnownAndroidPathFragment(path)) {
                    LOGGER.warn("发现库路径位于 Android 文件系统: {}", path);
                    return true;
                }
            }
        }

        var workingDirectory = System.getProperty("user.home", null);
        if (workingDirectory != null && isKnownAndroidPathFragment(workingDirectory)) {
            LOGGER.warn("工作目录位于 Android 文件系统: {}", workingDirectory);
        }

        return false;
    }

    private static boolean isKnownAndroidPathFragment(String path) {
        return path.matches("/data/user/[0-9]+/net\\.kdt\\.pojavlaunch");
    }
}