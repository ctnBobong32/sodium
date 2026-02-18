package net.caffeinemc.mods.sodium.client.compatibility.checks;

import net.caffeinemc.mods.sodium.client.platform.PlatformHelper;
import org.lwjgl.Version;

/**
 * 在游戏创建 OpenGL 上下文之前执行 OpenGL 驱动程序验证。
 * 该检查在游戏启动的最早阶段运行，并使用自定义硬件探测器查找有问题的驱动程序。
 */
public class PreLaunchChecks {
    // 这些版本常量在编译时内联。
    private static final String REQUIRED_LWJGL_VERSION =
            Version.VERSION_MAJOR + "." + Version.VERSION_MINOR + "." + Version.VERSION_REVISION;

    public static void checkEnvironment() {
        if (BugChecks.ISSUE_2561) {
            checkLwjglRuntimeVersion();
        }
    }

    private static void checkLwjglRuntimeVersion() {
        if (isUsingKnownCompatibleLwjglVersion()) {
            return;
        }

        String advice;

        if (isUsingPrismLauncher()) {
            advice = """
                    看起来您正在使用 Prism Launcher 启动游戏。您可以通过打开实例设置并导航到侧边栏中的“版本”部分来解决此问题。""";
        } else {
            advice = """
                    您必须在启动器中更改 LWJGL 版本才能继续。这通常由启动器中配置文件或实例的设置控制。""";
        }

        String message = """
                        游戏启动失败，因为当前激活的 LWJGL 版本不兼容。
                        
                        已安装版本：###CURRENT_VERSION###
                        所需版本：###REQUIRED_VERSION###
                        
                        ###ADVICE_STRING###"""
                .replace("###CURRENT_VERSION###", Version.getVersion())
                .replace("###REQUIRED_VERSION###", REQUIRED_LWJGL_VERSION)
                .replace("###ADVICE_STRING###", advice);

        PlatformHelper.showCriticalErrorAndClose(null, "Sodium 渲染器 - 不支持的 LWJGL 版本", message,
                "https://link.caffeinemc.net/help/sodium/runtime-issue/lwjgl3/gh-2561");
    }

    private static boolean isUsingKnownCompatibleLwjglVersion() {
        return Version.getVersion()
                .startsWith(REQUIRED_LWJGL_VERSION);
    }

    private static boolean isUsingPrismLauncher() {
        return getLauncherBrand()
                .equalsIgnoreCase("PrismLauncher");
    }

    private static String getLauncherBrand() {
        return System.getProperty("minecraft.launcher.brand", "unknown");
    }
}