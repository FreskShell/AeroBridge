package com.freskshell.aerobridge.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AeroBridgeConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_SWEEP_DETECTION;
    public static final ModConfigSpec.BooleanValue ENABLE_CROSS_DIMENSIONAL_PROXY;
    public static final ModConfigSpec.BooleanValue ENABLE_PORTAL_CLIPPING;
    public static final ModConfigSpec.BooleanValue ENABLE_INSTANT_PLAYER_TELEPORT;

    public static final ModConfigSpec.BooleanValue HOLD_TRANSFER_UNTIL_CENTER_CROSSES;
    public static final ModConfigSpec.IntValue MAX_HOLD_TICKS;

    public static final ModConfigSpec.IntValue PROXY_INTERACTION_RANGE;
    public static final ModConfigSpec.DoubleValue SWEEP_TOLERANCE;

    static {
        BUILDER.push("detection");
        ENABLE_SWEEP_DETECTION = BUILDER
            .comment("Enable swept-AABB portal detection instead of block-based detection")
            .define("enableSweepDetection", true);
        SWEEP_TOLERANCE = BUILDER
            .comment("Tolerance for sweep detection (in blocks)")
            .defineInRange("sweepTolerance", 0.1, 0.01, 1.0);
        BUILDER.pop();

        BUILDER.push("proxy");
        ENABLE_CROSS_DIMENSIONAL_PROXY = BUILDER
            .comment("Enable cross-dimensional interaction proxy")
            .define("enableCrossDimensionalProxy", true);
        PROXY_INTERACTION_RANGE = BUILDER
            .comment("Maximum range for proxy interactions (in blocks)")
            .defineInRange("proxyInteractionRange", 64, 16, 256);
        BUILDER.pop();

        BUILDER.push("rendering");
        ENABLE_PORTAL_CLIPPING = BUILDER
            .comment("Enable portal clipping shader for visual slicing effect")
            .define("enablePortalClipping", true);
        BUILDER.pop();

        BUILDER.push("passengers");
        ENABLE_INSTANT_PLAYER_TELEPORT = BUILDER
            .comment("Enable instant player teleportation (no portal animation)")
            .define("enableInstantPlayerTeleport", true);
        BUILDER.pop();

        BUILDER.push("transfer");
        HOLD_TRANSFER_UNTIL_CENTER_CROSSES = BUILDER
            .comment("EXPERIMENTAL: cancela a transferencia do AeroPortals ate o centro do sublevel passar do plano do portal")
            .define("holdTransferUntilCenterCrosses", false);
        MAX_HOLD_TICKS = BUILDER
            .comment("Seguranca: depois deste tempo (ticks) a transferencia e liberada mesmo sem o centro ter cruzado")
            .defineInRange("maxHoldTicks", 600, 20, 12000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
