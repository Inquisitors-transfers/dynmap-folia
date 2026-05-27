package org.dynmap.bukkit.helper.folia26;

import org.bukkit.ChunkSnapshot;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.dynmap.bukkit.helper.AbstractMapChunkCache;
import org.dynmap.renderer.DynmapBlockState;

public class Folia26MapChunkCache extends AbstractMapChunkCache {
    public static class WrappedSnapshot implements Snapshot {
        private final ChunkSnapshot snapshot;

        public WrappedSnapshot(ChunkSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public DynmapBlockState getBlockType(int x, int y, int z) {
            try {
                BlockData data = snapshot.getBlockData(x & 0xF, y, z & 0xF);
                return BukkitVersionHelperFolia26.getBlockState(data);
            } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException xcx) {
                return DynmapBlockState.AIR;
            }
        }

        @Override
        public int getBlockSkyLight(int x, int y, int z) {
            return snapshot.getBlockSkyLight(x & 0xF, y, z & 0xF);
        }

        @Override
        public int getBlockEmittedLight(int x, int y, int z) {
            return snapshot.getBlockEmittedLight(x & 0xF, y, z & 0xF);
        }

        @Override
        public int getHighestBlockYAt(int x, int z) {
            return snapshot.getHighestBlockYAt(x & 0xF, z & 0xF);
        }

        @Override
        public Biome getBiome(int x, int z) {
            return snapshot.getBiome(x & 0xF, z & 0xF);
        }

        @Override
        public boolean isSectionEmpty(int sy) {
            return false;
        }

        @Override
        public Object[] getBiomeBaseFromSnapshot() {
            return null;
        }
    }

    @Override
    public Snapshot wrapChunkSnapshot(ChunkSnapshot css) {
        return new WrappedSnapshot(css);
    }
}
