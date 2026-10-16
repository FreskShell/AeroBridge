package com.freskshell.aerobridge.portal;

import com.freskshell.aerobridge.AeroBridge;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Detecta travessia de portal por varredura contínua (Swept-AABB):
 * compara os 8 cantos da AABB no tick anterior vs. atual contra o plano do portal.
 */
public final class CrossingDetector {

    public enum CrossingState { OUTSIDE, CROSSING }

    private CrossingDetector() {}

    public static void tick(ServerLevel level, SubLevelTracker.TrackedSubLevel t) {
        // LOG TEMPORÁRIO: confirmar que está sendo chamado
        if (level.getGameTime() % 20 == 0) {
            AeroBridge.LOGGER.info("[AeroBridge] CrossingDetector.tick called for sub {}, prevAabb={}, currAabb={}",
                t.id, t.previousAabb != null, t.currentAabb != null);
        }

        if (t.previousAabb == null || t.currentAabb == null) {
            AeroBridge.LOGGER.info("[AeroBridge] CrossingDetector.tick: previousAabb or currentAabb is null for sub {}", t.id);
            return;
        }

        List<PortalRegistry.PortalRect> rects =
                PortalRegistry.getPortals(level, level.getGameTime(), List.of(t.currentAabb));

        if (rects.isEmpty()) {
            AeroBridge.LOGGER.info("[AeroBridge] CrossingDetector.tick: no portals found for sub {}", t.id);
            return;
        }

        AeroBridge.LOGGER.info("[AeroBridge] CrossingDetector.tick: checking {} portals for sub {}", rects.size(), t.id);

        for (PortalRegistry.PortalRect rect : rects) {
            double[] prev = cornerDistances(rect, t.previousAabb);
            double[] curr = cornerDistances(rect, t.currentAabb);

            AeroBridge.LOGGER.info("[AeroBridge] Sub {} distances: prev={}, curr={}", t.id,
                Arrays.toString(prev), Arrays.toString(curr));

            if (t.crossingState == CrossingState.OUTSIDE) {
                // Algum canto cruzou o plano neste tick?
                for (int i = 0; i < 8; i++) {
                    if (Math.signum(prev[i]) != Math.signum(curr[i]) && (prev[i] != 0 || curr[i] != 0)) {
                        double f = prev[i] / (prev[i] - curr[i]);
                        Vec3 a = corner(t.previousAabb, i), b = corner(t.currentAabb, i);
                        Vec3 hit = a.add(b.subtract(a).scale(f));
                        if (insideRect(rect, hit)) {
                            t.crossingState = CrossingState.CROSSING;
                            t.enteredRect = rect;
                            AeroBridge.LOGGER.info("[AeroBridge] PORTAL ENTER sub {} tick {} point ({}, {}, {}) side={}",
                                    t.id, level.getGameTime(),
                                    String.format("%.2f", hit.x), String.format("%.2f", hit.y), String.format("%.2f", hit.z),
                                    prev[i] > 0 ? "+" : "-");
                            break;
                        }
                    }
                }
            } else if (t.crossingState == CrossingState.CROSSING) {
                // Todos os 8 cantos atuais do mesmo lado (e oposto ao lado de entrada)?
                double sign = Math.signum(curr[0]);
                boolean allSame = true;
                for (double d : curr) if (Math.signum(d) != sign || Math.abs(d) < 0.01) { allSame = false; break; }
                if (allSame) {
                    t.crossingState = CrossingState.OUTSIDE;
                    AeroBridge.LOGGER.info("[AeroBridge] PORTAL TAIL CLEARED sub {} tick {} side={}",
                            t.id, level.getGameTime(), sign > 0 ? "+" : "-");
                    t.enteredRect = null;
                }
            }
        }
    }

    /**
     * Verifica se um subnível específico está atualmente atravessando um portal.
     * Usado pelo Mixin para decidir se bloqueia a transferência do AeroPortals.
     * (ESTE MÉTODO FICA DENTRO DA CLASSE, entre o tick e os métodos privados)
     */
    public static boolean isSubLevelCrossing(UUID subLevelId) {
        SubLevelTracker.TrackedSubLevel t = SubLevelTracker.get(subLevelId);
        return t != null && t.crossingState == CrossingState.CROSSING;
    }

    private static double[] cornerDistances(PortalRegistry.PortalRect rect, AABB box) {
        double[] out = new double[8];
        for (int i = 0; i < 8; i++) {
            Vec3 c = corner(box, i);
            out[i] = c.subtract(rect.center()).dot(rect.normal());
        }
        return out;
    }

    private static boolean insideRect(PortalRegistry.PortalRect rect, Vec3 point) {
        Vec3 d = point.subtract(rect.center());
        return Math.abs(d.dot(rect.u())) <= rect.halfU() + 0.5 && Math.abs(d.dot(rect.v())) <= rect.halfV() + 0.5;
    }

    private static Vec3 corner(AABB b, int i) {
        return new Vec3(
                (i & 1) == 0 ? b.minX : b.maxX,
                (i & 2) == 0 ? b.minY : b.maxY,
                (i & 4) == 0 ? b.minZ : b.maxZ);
    }
}
