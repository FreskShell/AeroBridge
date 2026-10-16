package com.freskshell.aerobridge.portal;

import com.freskshell.aerobridge.AeroBridge;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3dc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rastreia a pose/AABB de cada sublevel a cada tick, por dimensão.
 * Um sublevel que muda de dimensão (AeroPortals) ganha um novo registro, pois as
 * coordenadas anteriores pertencem a outro espaço.
 */
public class SubLevelTracker {

    private static final Map<UUID, TrackedSubLevel> tracked = new HashMap<>();

    public static final class TrackedSubLevel {
        public ServerSubLevel subLevel;
        public final UUID id;
        public final ResourceKey<Level> dimension;
        public Vec3 previousPosition;
        public Quaterniond previousOrientation;
        public Vec3 currentPosition;
        public Quaterniond currentOrientation;
        public long lastUpdateTick;
        public AABB previousAabb;
        public AABB currentAabb;
        /** Cantos da OBB real (ver SubLevelGeometry). */
        public Vec3[] previousCorners;
        public Vec3[] currentCorners;
        /** Lado do plano (+1/-1) de onde o sublevel veio ao entrar no portal. */
        public int entrySide;
        /** Tick em que a transferencia comecou a ser segurada; -1 = nao esta segurando. */
        public long holdSinceTick = -1;
        public CrossingDetector.CrossingState crossingState = CrossingDetector.CrossingState.OUTSIDE;
        public PortalRegistry.PortalRect enteredRect;

        public TrackedSubLevel(ServerSubLevel subLevel, UUID id, ResourceKey<Level> dimension) {
            this.subLevel = subLevel;
            this.id = id;
            this.dimension = dimension;
        }

        public void advanceBounds(AABB newAabb) {
            this.previousAabb = this.currentAabb;
            this.currentAabb = newAabb;
        }

        public void advanceCorners(Vec3[] corners) {
            this.previousCorners = this.currentCorners;
            this.currentCorners = corners;
            advanceBounds(SubLevelGeometry.enclosing(corners));
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

        ResourceKey<Level> dimension = level.dimension();
        long tick = level.getGameTime();
        Set<UUID> seen = new HashSet<>();

        for (ServerSubLevel sub : container.getAllSubLevels()) {
            UUID id = sub.getUniqueId();
            Pose3d pose = sub.logicalPose();
            if (pose == null) continue;
            seen.add(id);

            TrackedSubLevel t = tracked.get(id);
            if (t == null || !t.dimension.equals(dimension)) {
                t = new TrackedSubLevel(sub, id, dimension);
                tracked.put(id, t);
            } else {
                t.subLevel = sub;
            }

            Vector3dc p = pose.position();
            t.advancePose(new Vec3(p.x(), p.y(), p.z()), new Quaterniond(pose.orientation()), tick);

            t.advanceCorners(SubLevelGeometry.worldCorners(sub));
            CrossingDetector.tick(level, t);
        }

        // Remove sublevels desta dimensão que sumiram (destruídos, descarregados ou transferidos).
        Iterator<TrackedSubLevel> it = tracked.values().iterator();
        while (it.hasNext()) {
            TrackedSubLevel t = it.next();
            if (t.dimension.equals(dimension) && !seen.contains(t.id)) {
                PortalRegistry.forget(dimension, t.id);
                it.remove();
            }
        }

        if (tick % 200 == 0 && !tracked.isEmpty()) {
            AeroBridge.LOGGER.debug("[AeroBridge] Tracking {} sublevel(s) total; {} in {}",
                    tracked.size(), seen.size(), dimension.location());
        }
    }

    public static TrackedSubLevel get(UUID id) {
        return tracked.get(id);
    }

    public static Map<UUID, TrackedSubLevel> getAll() {
        return tracked;
    }

    public static void clearDimension(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        tracked.values().removeIf(t -> t.dimension.equals(dimension));
    }

    public static void clearAll() {
        tracked.clear();
    }
}
