package com.freskshell.aerobridge.portal;

import com.freskshell.aerobridge.AeroBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Detecta travessia de portal por varredura contínua: compara os 8 cantos da OBB real
 * do sublevel no tick anterior vs. atual contra o plano do portal.
 */
public final class CrossingDetector {

    public enum CrossingState { OUTSIDE, CROSSING }

    private CrossingDetector() {}

    public static void tick(ServerLevel level, SubLevelTracker.TrackedSubLevel t) {
        if (t.previousCorners == null || t.currentCorners == null) {
            return;
        }
        long gameTime = level.getGameTime();

        if (t.crossingState == CrossingState.OUTSIDE) {
            List<PortalRegistry.PortalRect> rects =
                    PortalRegistry.getPortals(level, gameTime, t.id, t.currentAabb);
            for (PortalRegistry.PortalRect rect : rects) {
                if (tryEnter(t, rect, gameTime)) {
                    break;
                }
            }
            return;
        }

        // CROSSING: acompanha apenas o portal pelo qual entrou.
        PortalRegistry.PortalRect rect = t.enteredRect;
        if (rect == null) {
            t.crossingState = CrossingState.OUTSIDE;
            return;
        }
        double[] curr = cornerDistances(rect, t.currentCorners);
        double sign = Math.signum(curr[0]);
        boolean allSame = true;
        for (double d : curr) {
            if (Math.signum(d) != sign || Math.abs(d) < 0.01) {
                allSame = false;
                break;
            }
        }
        if (allSame) {
            t.crossingState = CrossingState.OUTSIDE;
            t.enteredRect = null;
            t.entrySide = 0;
            AeroBridge.LOGGER.debug("[AeroBridge] PORTAL TAIL CLEARED sub {} tick {} side={}",
                    t.id, gameTime, sign > 0 ? "+" : "-");
        }
    }

    /** Algum canto da OBB cruzou o plano do portal neste tick, dentro do retângulo? */
    private static boolean tryEnter(SubLevelTracker.TrackedSubLevel t, PortalRegistry.PortalRect rect, long gameTime) {
        double[] prev = cornerDistances(rect, t.previousCorners);
        double[] curr = cornerDistances(rect, t.currentCorners);
        for (int i = 0; i < 8; i++) {
            if (Math.signum(prev[i]) != Math.signum(curr[i]) && (prev[i] != 0 || curr[i] != 0)) {
                double f = prev[i] / (prev[i] - curr[i]);
                Vec3 a = t.previousCorners[i], b = t.currentCorners[i];
                Vec3 hit = a.add(b.subtract(a).scale(f));
                if (insideRect(rect, hit)) {
                    t.crossingState = CrossingState.CROSSING;
                    t.enteredRect = rect;
                    t.entrySide = prev[i] != 0 ? (prev[i] > 0 ? 1 : -1) : (curr[i] > 0 ? -1 : 1);
                    AeroBridge.LOGGER.info("[AeroBridge] PORTAL ENTER sub {} tick {} point ({}, {}, {}) from side {}",
                            t.id, gameTime,
                            String.format("%.2f", hit.x), String.format("%.2f", hit.y), String.format("%.2f", hit.z),
                            t.entrySide > 0 ? "+" : "-");
                    return true;
                }
            }
        }
        return false;
    }

    /** Distância assinada do centro da OBB ao plano do portal (+ = lado da normal). */
    public static double centerDistance(PortalRegistry.PortalRect rect, Vec3[] corners) {
        return SubLevelGeometry.center(corners).subtract(rect.center()).dot(rect.normal());
    }

    /** O centro da OBB já passou para o lado oposto ao de entrada? */
    public static boolean centerCrossed(SubLevelTracker.TrackedSubLevel t) {
        return t.crossingState == CrossingState.CROSSING
                && t.enteredRect != null && t.currentCorners != null && t.entrySide != 0
                && t.entrySide * centerDistance(t.enteredRect, t.currentCorners) < 0;
    }

    /**
     * Fração (0..1) da extensão da OBB, medida ao longo da normal do portal, que já está
     * no lado oposto ao de entrada. Aproximação linear, suficiente para depuração e para o hold.
     */
    public static double crossedFraction(SubLevelTracker.TrackedSubLevel t) {
        if (t.crossingState != CrossingState.CROSSING || t.enteredRect == null
                || t.currentCorners == null || t.entrySide == 0) {
            return 0.0;
        }
        double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
        for (double d : cornerDistances(t.enteredRect, t.currentCorners)) {
            double s = t.entrySide * d;
            lo = Math.min(lo, s);
            hi = Math.max(hi, s);
        }
        if (hi - lo < 1.0e-6 || lo >= 0) return 0.0;
        if (hi <= 0) return 1.0;
        return -lo / (hi - lo);
    }

    public static boolean isSubLevelCrossing(UUID subLevelId) {
        SubLevelTracker.TrackedSubLevel t = SubLevelTracker.get(subLevelId);
        return t != null && t.crossingState == CrossingState.CROSSING;
    }

    private static double[] cornerDistances(PortalRegistry.PortalRect rect, Vec3[] corners) {
        double[] out = new double[corners.length];
        for (int i = 0; i < corners.length; i++) {
            out[i] = corners[i].subtract(rect.center()).dot(rect.normal());
        }
        return out;
    }

    private static boolean insideRect(PortalRegistry.PortalRect rect, Vec3 point) {
        Vec3 d = point.subtract(rect.center());
        return Math.abs(d.dot(rect.u())) <= rect.halfU() + 0.5 && Math.abs(d.dot(rect.v())) <= rect.halfV() + 0.5;
    }
}
