package com.freskshell.aerobridge.portal;

import com.freskshell.aerobridge.AeroBridge;
import com.freskshell.aerobridge.config.AeroBridgeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Detector geométrico de travessia de portais usando Swept-AABB (Continuous Collision Detection).
 * Substitui a detecção por blocos do AeroPortals por uma varredura contínua entre poses.
 */
public class SweptAABBDetector {

    /**
     * Detecta se um subnível cruzou um plano de portal entre o tick anterior e o atual.
     *
     * @param sublevelId ID único do subnível
     * @param previousPose A pose do subnível no tick anterior (posição + rotação)
     * @param currentPose A pose atual do subnível
     * @param portalPlane O plano matemático do portal (normal + ponto)
     * @return Optional contendo informações da travessia se detectada
     */
    public static Optional<CrossingDetection> detectCrossing(
            UUID sublevelId,
            SubLevelPose previousPose,
            SubLevelPose currentPose,
            PortalPlane portalPlane) {

        if (!AeroBridgeConfig.ENABLE_SWEEP_DETECTION.get()) {
            return Optional.empty();
        }

        // TODO: Obter AABB real do subnível quando integrarmos com Sable
        // Por enquanto, usa uma AABB de teste
        AABB boundingBox = new AABB(-2, 0, -2, 2, 2, 2);

        // Calcular os vértices da AABB nas duas poses
        List<Vec3> previousVertices = getAABBVertices(boundingBox, previousPose);
        List<Vec3> currentVertices = getAABBVertices(boundingBox, currentPose);

        // Verificar se houve cruzamento do plano
        CrossingResult result = checkPlaneIntersection(
                previousVertices,
                currentVertices,
                portalPlane
        );

        if (result.crossed) {
            AeroBridge.LOGGER.debug(
                    "[AeroBridge] Swept-AABB detected crossing for sublevel {} at {}",
                    sublevelId,
                    result.intersectionPoint
            );

            return Optional.of(new CrossingDetection(
                    sublevelId,
                    result.intersectionPoint,
                    result.intersectionTime,
                    result.penetratedSide
            ));
        }

        return Optional.empty();
    }

    /**
     * Calcula os 8 vértices de uma AABB transformada por uma pose (posição + rotação).
     */
    private static List<Vec3> getAABBVertices(AABB aabb, SubLevelPose pose) {
        List<Vec3> vertices = new ArrayList<>(8);

        double minX = aabb.minX;
        double minY = aabb.minY;
        double minZ = aabb.minZ;
        double maxX = aabb.maxX;
        double maxY = aabb.maxY;
        double maxZ = aabb.maxZ;

        Vec3[] localVertices = {
                new Vec3(minX, minY, minZ),
                new Vec3(maxX, minY, minZ),
                new Vec3(minX, maxY, minZ),
                new Vec3(maxX, maxY, minZ),
                new Vec3(minX, minY, maxZ),
                new Vec3(maxX, minY, maxZ),
                new Vec3(minX, maxY, maxZ),
                new Vec3(maxX, maxY, maxZ)
        };

        Matrix4d transformMatrix = pose.toMatrix();

        for (Vec3 local : localVertices) {
            Vector3d transformed = new Vector3d(local.x, local.y, local.z);
            transformMatrix.transformPosition(transformed);
            vertices.add(new Vec3(transformed.x, transformed.y, transformed.z));
        }

        return vertices;
    }

    private static CrossingResult checkPlaneIntersection(
            List<Vec3> previousVertices,
            List<Vec3> currentVertices,
            PortalPlane plane) {

        int crossedCount = 0;
        Vec3 intersectionPoint = null;
        double intersectionTime = 0.0;
        PortalSide penetratedSide = PortalSide.NONE;

        for (int i = 0; i < previousVertices.size(); i++) {
            Vec3 prev = previousVertices.get(i);
            Vec3 curr = currentVertices.get(i);

            double prevDistance = plane.signedDistanceTo(prev);
            double currDistance = plane.signedDistanceTo(curr);

            if (Math.signum(prevDistance) != Math.signum(currDistance)) {
                crossedCount++;

                double t = prevDistance / (prevDistance - currDistance);
                Vec3 intersection = prev.add(curr.subtract(prev).scale(t));

                if (intersectionPoint == null) {
                    intersectionPoint = intersection;
                    intersectionTime = t;
                    penetratedSide = prevDistance > 0 ? PortalSide.POSITIVE : PortalSide.NEGATIVE;
                }
            }
        }

        boolean crossed = crossedCount > 0;

        return new CrossingResult(crossed, intersectionPoint, intersectionTime, penetratedSide);
    }

    private static class CrossingResult {
        final boolean crossed;
        final Vec3 intersectionPoint;
        final double intersectionTime;
        final PortalSide penetratedSide;

        CrossingResult(boolean crossed, Vec3 intersectionPoint, double intersectionTime, PortalSide penetratedSide) {
            this.crossed = crossed;
            this.intersectionPoint = intersectionPoint;
            this.intersectionTime = intersectionTime;
            this.penetratedSide = penetratedSide;
        }
    }

    public static class CrossingDetection {
        public final UUID sublevelId;
        public final Vec3 intersectionPoint;
        public final double intersectionTime;
        public final PortalSide penetratedSide;

        public CrossingDetection(UUID sublevelId, Vec3 intersectionPoint,
                                 double intersectionTime, PortalSide penetratedSide) {
            this.sublevelId = sublevelId;
            this.intersectionPoint = intersectionPoint;
            this.intersectionTime = intersectionTime;
            this.penetratedSide = penetratedSide;
        }
    }

    public static class SubLevelPose {
        public final Vec3 position;
        public final org.joml.Quaterniond rotation;

        public SubLevelPose(Vec3 position, org.joml.Quaterniond rotation) {
            this.position = position;
            this.rotation = rotation;
        }

        public Matrix4d toMatrix() {
            Matrix4d matrix = new Matrix4d();
            matrix.translation(position.x, position.y, position.z);
            matrix.rotate(rotation);
            return matrix;
        }

        public static SubLevelPose fromPosition(Vec3 position) {
            return new SubLevelPose(position, new org.joml.Quaterniond());
        }
    }

    public static class PortalPlane {
        public final Vec3 normal;
        public final Vec3 point;

        public PortalPlane(Vec3 normal, Vec3 point) {
            this.normal = normal.normalize();
            this.point = point;
        }

        public double signedDistanceTo(Vec3 point) {
            Vec3 diff = point.subtract(this.point);
            return diff.dot(normal);
        }

        public static PortalPlane fromPortalBlock(BlockPos portalPos, net.minecraft.core.Direction facing) {
            Vec3 point = new Vec3(portalPos.getX() + 0.5, portalPos.getY() + 0.5, portalPos.getZ() + 0.5);
            Vec3 normal = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
            return new PortalPlane(normal, point);
        }
    }

    public enum PortalSide {
        NONE,
        POSITIVE,
        NEGATIVE
    }
}
