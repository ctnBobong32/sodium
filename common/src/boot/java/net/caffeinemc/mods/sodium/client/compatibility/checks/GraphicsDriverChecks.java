package net.caffeinemc.mods.sodium.client.compatibility.checks;

import net.caffeinemc.mods.sodium.client.compatibility.environment.GlContextInfo;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterVendor;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.intel.IntelWorkarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaDriverVersion;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.caffeinemc.mods.sodium.client.platform.NativeWindowHandle;
import net.caffeinemc.mods.sodium.client.platform.PlatformHelper;

/**
 * 图形驱动程序检查类，在 OpenGL 上下文初始化后执行特定显卡驱动的兼容性验证。
 */
class GraphicsDriverChecks {
    /**
     * 在上下文初始化后执行驱动程序检查。
     * 如果检测到不兼容的 Intel 或 NVIDIA 驱动程序，将显示错误消息并关闭游戏。
     *
     * @param window  本地窗口句柄，用于显示消息框
     * @param context OpenGL 上下文信息
     */
    static void postContextInit(NativeWindowHandle window, GlContextInfo context) {
        var vendor = GraphicsAdapterVendor.fromContext(context);

        if (vendor == GraphicsAdapterVendor.UNKNOWN) {
            return;
        }

        // Intel 驱动程序兼容性检查（问题 #899）
        if (vendor == GraphicsAdapterVendor.INTEL && BugChecks.ISSUE_899) {
            var installedVersion = IntelWorkarounds.findIntelDriverMatchingBug899();

            if (installedVersion != null) {
                var installedVersionString = installedVersion.toString();

                PlatformHelper.showCriticalErrorAndClose(window,
                        "Sodium 渲染器 - 不支持的驱动程序",
                        """
                                游戏启动失败，因为当前安装的 Intel 显卡驱动程序不兼容。
                                
                                已安装版本：###CURRENT_DRIVER###
                                所需版本：10.18.10.5161 或更高版本
                                
                                请单击“帮助”按钮以阅读有关如何解决此问题的更多信息。"""
                                .replace("###CURRENT_DRIVER###", installedVersionString),
                        "https://link.caffeinemc.net/help/sodium/graphics-driver/windows/intel/gh-899");
            }
        }

        // NVIDIA 驱动程序兼容性检查（问题 #1486）
        if (vendor == GraphicsAdapterVendor.NVIDIA && BugChecks.ISSUE_1486) {
            var installedVersion = NvidiaWorkarounds.findNvidiaDriverMatchingBug1486();

            if (installedVersion != null) {
                var installedVersionString = NvidiaDriverVersion.parse(installedVersion)
                        .toString();

                PlatformHelper.showCriticalErrorAndClose(window,
                        "Sodium 渲染器 - 不支持的驱动程序",
                        """
                                游戏启动失败，因为当前安装的 NVIDIA 显卡驱动程序不兼容。
                                
                                已安装版本：###CURRENT_DRIVER###
                                所需版本：536.23 或更高版本
                                
                                请单击“帮助”按钮以阅读有关如何解决此问题的更多信息。"""
                                .replace("###CURRENT_DRIVER###", installedVersionString),
                        "https://link.caffeinemc.net/help/sodium/graphics-driver/windows/nvidia/gh-1486");
            }
        }
    }
}