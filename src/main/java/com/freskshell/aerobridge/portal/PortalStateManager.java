package com.freskshell.aerobridge.portal;

import com.freskshell.aerobridge.AeroBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia o estado dos subníveis durante travessia de portais.
 * Rastreia poses anteriores, estado de travessia e proxies ativos.
 */
public class PortalStateManager {

    private static final Map<UUID, SubLevelState> subLevelStates = new HashMap<>();

    public static void updateSubLevelState(UUID sublevelId, SweptAABBDetector.SubLevelPose currentPose, ServerLevel level) {
        SubLevelState state = subLevelStates.computeIfAbsent(sublevelId, k -> new SubLevelState());

        state.previousPose = state.currentPose;
        state.currentPose = currentPose;
        state.lastUpdateTick = level.getServer().getTickCount();
        state.level = level;

        AeroBridge.LOGGER.debug(
                "[AeroBridge] Updated state for sublevel {} at tick {}",
                sublevelId,
                state.lastUpdateTick
        );
    }

    public static SubLevelState getState(UUID sublevelId) {
        return subLevelStates.get(sublevelId);
    }

    public static void removeState(UUID sublevelId) {
        subLevelStates.remove(sublevelId);
    }

    public static void clearAllStates() {
        subLevelStates.clear();
        AeroBridge.LOGGER.info("[AeroBridge] Cleared all sublevel states");
    }

    public static class SubLevelState {
        public SweptAABBDetector.SubLevelPose previousPose;
        public SweptAABBDetector.SubLevelPose currentPose;
        public long lastUpdateTick;
        public ServerLevel level;
        public boolean isCrossing = false;
        public CrossingInfo crossingInfo;

        public boolean hasValidPoses() {
            return previousPose != null && currentPose != null;
        }
    }

    public static class CrossingInfo {
        public final SweptAABBDetector.PortalPlane portalPlane;
        public final long startTick;
        public boolean completed = false;

        public CrossingInfo(SweptAABBDetector.PortalPlane portalPlane, long startTick) {
            this.portalPlane = portalPlane;
            this.startTick = startTick;
        }
    }
}
