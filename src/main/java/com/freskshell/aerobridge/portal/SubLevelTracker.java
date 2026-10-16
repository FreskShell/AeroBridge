package com.freskshell.aerobridge.portal;

import com.freskshell.aerobridge.AeroBridge;
import com.breakinblocks.aeroportals.util.AabbUtil;
import net.minecraft.world.phys.AABB;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3dc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SubLevelTracker {

    private static final Map<UUID, TrackedSubLevel> tracked = new HashMap<>();

    public static final class TrackedSubLevel {
        public final ServerSubLevel subLevel;
        public final UUID id;
        public Vec3 previousPosition;
        public Quaterniond previousOrientation;
        public Vec3 currentPosition;
        public Quaterniond currentOrientation;
        public long lastUpdateTick;
        public AABB previousAabb;
        public AABB currentAabb;
        public CrossingDetector.CrossingState crossingState = CrossingDetector.CrossingState.OUTSIDE;
        public PortalRegistry.PortalRect enteredRect;

        public void advanceBounds(AABB newAabb) {
            this.previousAabb = this.currentAabb;
            this.currentAabb = newAabb;
        }

        public TrackedSubLevel(ServerSubLevel subLevel, UUID id) {
            this.subLevel = subLevel;
            this.id = id;
        }

        public void advancePose(Vec3 newPos, Quaterniond newOri, long tick) {
            this.previousPosition = this.currentPosition;
            this.previousOrientation = this.currentOrientation;
            this.currentPosition = newPos;
            this.currentOrientation = newOri;
            this.lastUpdateTick = tick;
        }
    }

    public static void tick(ServerLevel level) {
        SubLevelContainer rawContainer = ((SubLevelContainerHolder) level).sable$getPlotContainer();
        if (!(rawContainer instanceof ServerSubLevelContainer container)) {
            return;
        }

        List<ServerSubLevel> subs = container.getAllSubLevels();

        // NÃO remove subníveis do mapa - eles podem estar em outras dimensões
        // tracked.keySet().removeIf(id -> subs.stream().noneMatch(s -> s.getUniqueId().equals(id)));

        long tick = level.getGameTime();

        for (ServerSubLevel sub : subs) {
            UUID id = sub.getUniqueId();
            Pose3d pose = sub.logicalPose();
            if (pose == null) continue;

            Vector3dc posVec = pose.position();
            Vec3 pos = new Vec3(posVec.x(), posVec.y(), posVec.z());
            Quaterniond ori = new Quaterniond(pose.orientation());

            TrackedSubLevel trackedSub = tracked.computeIfAbsent(id, k -> new TrackedSubLevel(sub, id));
            trackedSub.advancePose(pos, ori, tick);

            AabbUtil.ensureBoundsCurrent(sub);
            AABB worldAabb = AabbUtil.worldAabb(sub);
            trackedSub.advanceBounds(worldAabb);
            CrossingDetector.tick(level, trackedSub);
        }

        if (tick % 20 == 0 && !tracked.isEmpty()) {
            AeroBridge.LOGGER.info("[AeroBridge] Tracking {} sublevel(s) in {}:",
                    tracked.size(), level.dimension().location());
            for (TrackedSubLevel t : tracked.values()) {
                AeroBridge.LOGGER.info("[AeroBridge]   sub {} -> pos = {} | ori = (w={}, x={}, y={}, z={})",
                        t.id,
                        t.currentPosition,
                        t.currentOrientation.w,
                        t.currentOrientation.x,
                        t.currentOrientation.y,
                        t.currentOrientation.z);
            }
        }
    }

    public static TrackedSubLevel get(UUID id) {
        return tracked.get(id);
    }

    public static Map<UUID, TrackedSubLevel> getAll() {
        return tracked;
    }
}
