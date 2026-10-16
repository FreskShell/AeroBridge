package com.freskshell.aerobridge;

import com.breakinblocks.aeroportals.util.AabbUtil;
import com.freskshell.aerobridge.portal.CrossingDetector;
import com.freskshell.aerobridge.portal.SubLevelGeometry;
import com.freskshell.aerobridge.portal.SubLevelTracker;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** /aerobridge status: lista os sublevels rastreados e o estado de travessia. */
public final class AeroBridgeCommands {

    private AeroBridgeCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("aerobridge")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource()))));
    }

    private static int status(CommandSourceStack src) {
        List<SubLevelTracker.TrackedSubLevel> all = new ArrayList<>(SubLevelTracker.getAll().values());
        if (all.isEmpty()) {
            src.sendSuccess(() -> Component.literal("[AeroBridge] Nenhum sublevel rastreado."), false);
            return 0;
        }
        for (SubLevelTracker.TrackedSubLevel t : all) {
            final String line = String.format("[AeroBridge] %s @ %s | %s | cruzado=%.0f%% | centroCruzou=%s | obbVsSable=%s",
                    t.id.toString().substring(0, 8), t.dimension.location(), t.crossingState,
                    CrossingDetector.crossedFraction(t) * 100.0, CrossingDetector.centerCrossed(t),
                    obbDelta(t));
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return all.size();
    }

    /** Maior diferença (em blocos) entre a AABB que envolve a OBB e a AABB mundial do Sable. */
    private static String obbDelta(SubLevelTracker.TrackedSubLevel t) {
        try {
            AABB mine = SubLevelGeometry.enclosing(t.currentCorners);
            AABB sable = AabbUtil.worldAabb(t.subLevel);
            double d = Math.max(
                    Math.max(Math.abs(mine.minX - sable.minX), Math.abs(mine.maxX - sable.maxX)),
                    Math.max(Math.max(Math.abs(mine.minY - sable.minY), Math.abs(mine.maxY - sable.maxY)),
                            Math.max(Math.abs(mine.minZ - sable.minZ), Math.abs(mine.maxZ - sable.maxZ))));
            return String.format("%.2f", d);
        } catch (Exception e) {
            return "n/d";
        }
    }
}
