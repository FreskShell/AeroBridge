package com.freskshell.aerobridge.portal;

import com.freskshell.aerobridge.AeroBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class PortalRegistry {

    public record PortalRect(Vec3 center, Vec3 normal, Vec3 u, Vec3 v, double halfU, double halfV) {}

    private record Entry(long tick, List<PortalRect> rects) {}

    /** dimensão -> (dono do foco, normalmente o UUID do sublevel) -> portais próximos. */
    private static final Map<ResourceKey<Level>, Map<UUID, Entry>> cache = new HashMap<>();
    private static final long SCAN_INTERVAL_TICKS = 10;
    private static final double SCAN_MARGIN = 12.0;

    private PortalRegistry() {}

    /** Portais próximos de {@code focus}, com cache por dono (antes o cache era por nível e outro sublevel sobrescrevia). */
    public static List<PortalRect> getPortals(ServerLevel level, long tick, UUID owner, AABB focus) {
        Map<UUID, Entry> perLevel = cache.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Entry e = perLevel.get(owner);
        if (e == null || tick - e.tick() >= SCAN_INTERVAL_TICKS) {
            e = new Entry(tick, scan(level, focus.inflate(SCAN_MARGIN)));
            perLevel.put(owner, e);
        }
        return e.rects();
    }

    /** Portais já conhecidos para este dono, sem disparar nova varredura. */
    public static List<PortalRect> peek(ResourceKey<Level> dimension, UUID owner) {
        Map<UUID, Entry> perLevel = cache.get(dimension);
        Entry e = perLevel == null ? null : perLevel.get(owner);
        return e == null ? List.of() : e.rects();
    }

    public static void forget(ResourceKey<Level> dimension, UUID owner) {
        Map<UUID, Entry> perLevel = cache.get(dimension);
        if (perLevel != null) perLevel.remove(owner);
    }

    public static void invalidate(ServerLevel level) {
        cache.remove(level.dimension());
    }

    public static void clearAll() {
        cache.clear();
    }

    private static List<PortalRect> scan(ServerLevel level, AABB region) {
        Set<BlockPos> portalBlocks = new HashSet<>();
        int minX = (int) Math.floor(region.minX), maxX = (int) Math.ceil(region.maxX);
        int minY = (int) Math.floor(region.minY), maxY = (int) Math.ceil(region.maxY);
        int minZ = (int) Math.floor(region.minZ), maxZ = (int) Math.ceil(region.maxZ);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (!level.hasChunkAt(cursor)) continue; // nunca forçar carregamento de chunk
                    if (level.getBlockState(cursor).is(Blocks.NETHER_PORTAL)) {
                        portalBlocks.add(cursor.immutable());
                    }
                }

        AeroBridge.LOGGER.debug("[AeroBridge] PortalRegistry.scan found {} portal blocks in region {}",
            portalBlocks.size(), region);

        List<PortalRect> rects = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos seed : portalBlocks) {
            if (!visited.add(seed)) continue;
            Deque<BlockPos> queue = new ArrayDeque<>();
            queue.add(seed);
            int gMinX = seed.getX(), gMaxX = seed.getX();
            int gMinY = seed.getY(), gMaxY = seed.getY();
            int gMinZ = seed.getZ(), gMaxZ = seed.getZ();
            Direction.Axis axis = level.getBlockState(seed).getValue(BlockStateProperties.HORIZONTAL_AXIS);

            while (!queue.isEmpty()) {
                BlockPos p = queue.poll();
                gMinX = Math.min(gMinX, p.getX()); gMaxX = Math.max(gMaxX, p.getX());
                gMinY = Math.min(gMinY, p.getY()); gMaxY = Math.max(gMaxY, p.getY());
                gMinZ = Math.min(gMinZ, p.getZ()); gMaxZ = Math.max(gMaxZ, p.getZ());
                for (BlockPos n : new BlockPos[]{p.above(), p.below(), p.north(), p.south(), p.east(), p.west()}) {
                    if (portalBlocks.contains(n) && visited.add(n)) queue.add(n);
                }
            }

            if (axis == Direction.Axis.X) {
                PortalRect rect = new PortalRect(
                        new Vec3((gMinX + gMaxX + 1) / 2.0, (gMinY + gMaxY + 1) / 2.0, gMinZ + 0.5),
                        new Vec3(0, 0, 1), new Vec3(1, 0, 0), new Vec3(0, 1, 0),
                        (gMaxX - gMinX + 1) / 2.0, (gMaxY - gMinY + 1) / 2.0);
                rects.add(rect);
                AeroBridge.LOGGER.debug("[AeroBridge] PortalRect X-axis: center={}, normal={}, halfU={}, halfV={}",
                    rect.center(), rect.normal(), rect.halfU(), rect.halfV());
            } else {
                PortalRect rect = new PortalRect(
                        new Vec3(gMinX + 0.5, (gMinY + gMaxY + 1) / 2.0, (gMinZ + gMaxZ + 1) / 2.0),
                        new Vec3(1, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 1, 0),
                        (gMaxZ - gMinZ + 1) / 2.0, (gMaxY - gMinY + 1) / 2.0);
                rects.add(rect);
                AeroBridge.LOGGER.debug("[AeroBridge] PortalRect Z-axis: center={}, normal={}, halfU={}, halfV={}",
                    rect.center(), rect.normal(), rect.halfU(), rect.halfV());
            }
        }
        return rects;
    }
}
