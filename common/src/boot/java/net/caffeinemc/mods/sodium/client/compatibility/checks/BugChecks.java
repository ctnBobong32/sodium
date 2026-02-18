package net.caffeinemc.mods.sodium.client.compatibility.checks;

/**
 * “检查”用于判断我们当前运行的环境是否合理。通常情况下，失败的检查会导致游戏崩溃并提示用户进行干预。
 */
class BugChecks {
    /**
     * Windows 上某些旧版 Intel Gen7 显卡驱动程序存在缺陷，调用 <pre>glClientWaitSync</pre> 时永远不会返回。
     * 由于死锁发生后无法恢复主线程，尝试解决此问题似乎不可能。将驱动程序更新到版本 15.33.53.5161 可解决该问题，
     * 这是目前推荐的解决方案。
     * <a href="https://github.com/CaffeineMC/sodium/issues/899">GitHub Issue</a>
     */
    public static final boolean ISSUE_899 = configureCheck("issue899", true);

    /**
     * 自 NVIDIA 显卡驱动程序版本 526.47 起，OpenGL 用户模式驱动程序会尝试通过驱动程序中硬编码的逻辑检测 Minecraft，
     * 并启用有缺陷的优化（称为“线程优化”）。这会在帧缓冲初始化（以及某些绘制调用）期间导致崩溃，原因尚不明确，
     * 尤其是在混合图形系统上。此外，由于 NVIDIA 命令提交线程中的气泡问题，性能通常严重下降，尤其是在其他模组查询上下文状态时。
     * <p>
     * Sodium 可以阻止 Minecraft 的检测以及这些不稳定优化的启用，但逻辑非常复杂，且严重依赖于所使用的确切操作系统和图形驱动程序版本。
     * 某些较旧的图形驱动程序版本目前没有可用的解决方法。
     * <a href="https://github.com/CaffeineMC/sodium/issues/1486">GitHub Issue</a>
     */
    public static final boolean ISSUE_1486 = configureCheck("issue1486", true);

    /**
     * 旧版 RivaTuner Statistics Server 会尝试在 Core 配置文件中使用传统的 OpenGL 函数，这根据 OpenGL 实现的不同，
     * 要么导致崩溃，要么产生大量日志垃圾信息。这是因为 RivaTuner 依赖于在应用程序初始化 OpenGL 上下文后立即替换它，
     * 但在使用无错误上下文时操作不当。仅仅配置 RivaTuner 不注入 Minecraft 无法避免此问题，因为它总是首先注入以修改上下文，
     * 然后才禁用自身。
     * <a href="https://github.com/CaffeineMC/sodium/issues/2048">GitHub Issue</a>
     */
    public static final boolean ISSUE_2048 = configureCheck("issue2048", true);

    /**
     * LWJGL 不为其他使用它创建自身 C 绑定的库提供 API 稳定性保证。因此，游戏会在启动早期因破坏性的方法/类型签名更改而崩溃。
     * 我们本不应使用 LWJGL 的这些“内部”类，但完全自己实现所有功能的工作量是巨大的。当 Minecraft 附带包含外部函数与内存 API 的
     * OpenJDK 版本时，我们可以替换对 LWJGL 的依赖并移除此检查。
     * <a href="https://github.com/CaffeineMC/sodium/issues/2561">GitHub Issue</a>
     */
    public static final boolean ISSUE_2561 = configureCheck("issue2561", true);

    /**
     * 华硕 GPU Tweak III 在渲染其游戏内叠加层后未能正确恢复 OpenGL 上下文状态，这经常导致图形损坏、日志文件垃圾信息泛滥和崩溃。
     * 实际上，即使没有安装任何模组，这些问题也可以重现，但由于 Sodium 的渲染管道差异较大，问题似乎更严重。可以通过配置该应用程序
     * 不注入 Minecraft（或 Java 应用程序）来避免此问题。
     * <a href="https://github.com/CaffeineMC/sodium/issues/2637">GitHub Issue</a>
     */
    public static final boolean ISSUE_2637 = configureCheck("issue2637", true);

    private static boolean configureCheck(String name, boolean defaultValue) {
        var propertyValue = System.getProperty(getPropertyKey(name), null);

        if (propertyValue == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(propertyValue);
    }

    private static String getPropertyKey(String name) {
        return "sodium.checks." + name;
    }
}