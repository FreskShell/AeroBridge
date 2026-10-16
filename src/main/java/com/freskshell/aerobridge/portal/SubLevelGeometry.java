package com.freskshell.aerobridge.portal;

import com.breakinblocks.aeroportals.util.AabbUtil;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Geometria real (OBB) de um sublevel: a caixa local do plot transformada pela pose lógica.
 * A AABB alinhada ao mundo infla muito quando o sublevel está girado; a OBB não.
 */
public final class SubLevelGeometry {

    private SubLevelGeometry() {}

    /** 8 cantos da OBB no mundo. Índice: bit0 = x(min/max), bit1 = y, bit2 = z. */
    public static Vec3[] worldCorners(ServerSubLevel sub) {
        AabbUtil.ensureBoundsCurrent(sub);
        AABB local = AabbUtil.plotAabb(sub);
        Pose3d pose = sub.logicalPose();
        Vec3[] out = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            Vec3 c = new Vec3(
                    (i & 1) == 0 ? local.minX : local.maxX,
                    (i & 2) == 0 ? local.minY : local.maxY,
                    (i & 4) == 0 ? local.minZ : local.maxZ);
            out[i] = pose.transformPosition(c);
        }
        return out;
    }

    public static AABB enclosing(Vec3[] corners) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 c : corners) {
            minX = Math.min(minX, c.x); minY = Math.min(minY, c.y); minZ = Math.min(minZ, c.z);
            maxX = Math.max(maxX, c.x); maxY = Math.max(maxY, c.y); maxZ = Math.max(maxZ, c.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static Vec3 center(Vec3[] corners) {
        double x = 0, y = 0, z = 0;
        for (Vec3 c : corners) { x += c.x; y += c.y; z += c.z; }
        return new Vec3(x / corners.length, y / corners.length, z / corners.length);
    }
}
