package org.dynmap.bukkit.helper.folia26;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.dynmap.DynmapChunk;
import org.dynmap.bukkit.helper.BukkitMaterial;
import org.dynmap.bukkit.helper.BukkitVersionHelper;
import org.dynmap.bukkit.helper.BukkitWorld;
import org.dynmap.renderer.DynmapBlockState;
import org.dynmap.utils.MapChunkCache;
import org.dynmap.utils.Polygon;

import java.util.List;

public class BukkitVersionHelperFolia26 extends BukkitVersionHelper {
    private static final Map<String, DynmapBlockState> blockDataCache = new ConcurrentHashMap<String, DynmapBlockState>();
    private final HashMap<String, DynmapBlockState> lastBlockState = new HashMap<String, DynmapBlockState>();
    private String[] blockNames;
    private Object[] biomeList;
    private String[] biomeNames;

    @Override
    public boolean isUnsafeAsync() {
        return true;
    }

    @Override
    public Object[] getBiomeBaseList() {
        if(biomeList == null) {
            biomeList = Biome.values();
        }
        return biomeList;
    }

    @Override
    public float getBiomeBaseTemperature(Object bb) {
        return 0.5F;
    }

    @Override
    public float getBiomeBaseHumidity(Object bb) {
        return 0.5F;
    }

    @Override
    public String getBiomeBaseIDString(Object bb) {
        return ((Biome)bb).name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getBiomeBaseResourceLocsation(Object bb) {
        return "minecraft:" + getBiomeBaseIDString(bb);
    }

    @Override
    public int getBiomeBaseID(Object bb) {
        return (bb instanceof Biome) ? ((Biome)bb).ordinal() : 0;
    }

    @Override
    public Object getUnloadQueue(World world) {
        return null;
    }

    @Override
    public boolean isInUnloadQueue(Object unloadqueue, int x, int z) {
        return false;
    }

    @Override
    public Object[] getBiomeBaseFromSnapshot(ChunkSnapshot css) {
        return null;
    }

    @Override
    public long getInhabitedTicks(Chunk c) {
        return 0;
    }

    @Override
    public Map<?, ?> getTileEntitiesForChunk(Chunk c) {
        return Collections.emptyMap();
    }

    @Override
    public int getTileEntityX(Object te) {
        return 0;
    }

    @Override
    public int getTileEntityY(Object te) {
        return 0;
    }

    @Override
    public int getTileEntityZ(Object te) {
        return 0;
    }

    @Override
    public Object readTileEntityNBT(Object te, World world) {
        return null;
    }

    @Override
    public Object getFieldValue(Object nbt, String field) {
        return null;
    }

    @Override
    public void unloadChunkNoSave(World w, Chunk c, int cx, int cz) {
        w.unloadChunkRequest(cx, cz);
    }

    @Override
    public String[] getBlockNames() {
        if(blockNames == null) {
            Material[] mats = Material.values();
            blockNames = new String[mats.length];
            for(Material mat : mats) {
                if(mat.isBlock()) {
                    blockNames[mat.ordinal()] = mat.getKey().toString();
                }
            }
        }
        return blockNames;
    }

    @Override
    public String[] getBiomeNames() {
        if(biomeNames == null) {
            Biome[] biomes = Biome.values();
            biomeNames = new String[biomes.length];
            for(Biome biome : biomes) {
                biomeNames[biome.ordinal()] = getBiomeBaseResourceLocsation(biome);
            }
        }
        return biomeNames;
    }

    @Override
    public Player[] getOnlinePlayers() {
        Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
        return players.toArray(new Player[0]);
    }

    @Override
    public double getHealth(Player p) {
        return p.getHealth();
    }

    @Override
    public BukkitMaterial[] getMaterialList() {
        return new BukkitMaterial[Material.values().length];
    }

    @Override
    public void initializeBlockStates() {
        getBlockNames();
        for(Material mat : Material.values()) {
            if(!mat.isBlock()) {
                continue;
            }
            try {
                registerInitialBlockDataStates(mat);
            } catch (IllegalArgumentException x) {
            }
        }
        stateByID = new DynmapBlockState[0];
    }

    @Override
    public MapChunkCache getChunkCache(BukkitWorld dw, List<DynmapChunk> chunks) {
        Folia26MapChunkCache c = new Folia26MapChunkCache();
        c.setChunks(dw, chunks);
        return c;
    }

    @Override
    public String getStateStringByCombinedId(int blkid, int meta) {
        return "";
    }

    @Override
    public Polygon getWorldBorder(World world) {
        Polygon p = null;
        WorldBorder wb = world.getWorldBorder();
        if(wb != null) {
            Location c = wb.getCenter();
            double size = wb.getSize();
            if((size > 1) && (size < 1E7)) {
                size = size / 2;
                p = new Polygon();
                p.addVertex(c.getX() - size, c.getZ() - size);
                p.addVertex(c.getX() + size, c.getZ() - size);
                p.addVertex(c.getX() + size, c.getZ() + size);
                p.addVertex(c.getX() - size, c.getZ() + size);
            }
        }
        return p;
    }

    @Override
    public int getWorldMinY(World w) {
        return w.getMinHeight();
    }

    @Override
    public String getSkinURL(Player player) {
        return null;
    }

    static DynmapBlockState getBlockState(BlockData data) {
        String key = data.getAsString(false);
        DynmapBlockState state = blockDataCache.get(key);
        if(state != null) {
            return state;
        }
        return registerBlockData(data, key);
    }

    private static DynmapBlockState registerBlockData(BlockData data, String full) {
        String name = full;
        String states = "";
        int idx = full.indexOf('[');
        if(idx >= 0) {
            name = full.substring(0, idx);
            int end = full.lastIndexOf(']');
            if(end > idx) {
                states = full.substring(idx + 1, end);
            }
        }
        DynmapBlockState state = DynmapBlockState.getStateByNameAndState(name, states);
        if(state == DynmapBlockState.AIR && !DynmapBlockState.AIR_BLOCK.equals(name)) {
            state = DynmapBlockState.getBaseStateByName(name);
        }
        blockDataCache.put(full, state);
        return state;
    }

    private void registerInitialBlockDataStates(Material mat) {
        BlockType blockType = mat.asBlockType();
        if(blockType == null) {
            registerInitialBlockData(mat.createBlockData());
            return;
        }
        Collection<? extends BlockData> states = blockType.createBlockDataStates();
        if(states == null || states.isEmpty()) {
            registerInitialBlockData(blockType.createBlockData());
            return;
        }
        for(BlockData data : states) {
            registerInitialBlockData(data);
        }
    }

    private void registerInitialBlockData(BlockData data) {
        String full = data.getAsString(false);
        String name = full;
        String states = "";
        int idx = full.indexOf('[');
        if(idx >= 0) {
            name = full.substring(0, idx);
            int end = full.lastIndexOf(']');
            if(end > idx) {
                states = full.substring(idx + 1, end);
            }
        }
        DynmapBlockState base = lastBlockState.get(name);
        int stateidx = (base == null) ? 0 : base.getStateCount();
        DynmapBlockState.Builder builder = new DynmapBlockState.Builder()
                .setBaseState(base)
                .setStateIndex(stateidx)
                .setBlockName(name)
                .setStateName(states)
                .setMaterial(data.getMaterial().name());
        if(data.getMaterial().isAir()) {
            builder.setAir();
        }
        if(data.getMaterial().isSolid()) {
            builder.setSolid();
        }
        if(data.getMaterial().name().endsWith("_LOG") || data.getMaterial().name().endsWith("_STEM")) {
            builder.setLog();
        }
        if(data.getMaterial().name().endsWith("_LEAVES")) {
            builder.setLeaves();
        }
        if(states.contains("waterlogged=true")) {
            builder.setWaterlogged();
        }
        DynmapBlockState state = builder.build();
        if(base == null) {
            lastBlockState.put(name, state);
        }
        blockDataCache.put(full, state);
    }
}
