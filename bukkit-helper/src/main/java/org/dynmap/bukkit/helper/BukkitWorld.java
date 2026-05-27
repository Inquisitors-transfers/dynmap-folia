package org.dynmap.bukkit.helper;
/**
 * Bukkit specific implementation of DynmapWorld
 */
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.dynmap.DynmapChunk;
import org.dynmap.DynmapLocation;
import org.dynmap.DynmapWorld;
import org.dynmap.utils.MapChunkCache;
import org.dynmap.utils.Polygon;
import org.dynmap.utils.TileFlags;

public class BukkitWorld extends DynmapWorld {
    public interface ThreadSafeAccess {
        <T> T callGlobal(Callable<T> task, T def);
        <T> T callRegion(World world, int chunkX, int chunkZ, Callable<T> task, T def);
    }
    private static ThreadSafeAccess threadSafeAccess;
    private World world;
    private File worldfolder;
    private World.Environment env;
    private boolean skylight;
    private DynmapLocation spawnloc = new DynmapLocation();

    public static void setThreadSafeAccess(ThreadSafeAccess access) {
        threadSafeAccess = access;
    }
    
    public BukkitWorld(World w) {
        this(w.getName(), w.getMaxHeight(), w.getSeaLevel(), w.getEnvironment(),
        	BukkitVersionHelper.helper.getWorldMinY(w));
        setWorldLoaded(w);
        new Permission("dynmap.world." + getName(), "Dynmap access for world " + getName(), PermissionDefault.OP);
    }
    public BukkitWorld(String name, int height, int sealevel, World.Environment env, int miny) {
        super(name, height, sealevel, miny);
        world = null;
        this.env = env;
        skylight = (env == World.Environment.NORMAL);
        new Permission("dynmap.world." + getName(), "Dynmap access for world " + getName(), PermissionDefault.OP);
        // Generate non-default environment lighting table
        switch (env) {
            case NETHER:
                {
                    float f = 0.1F;
                    for (int i = 0; i <= 15; ++i) {
                        float f1 = 1.0F - (float)i / 15.0F;
                        this.setBrightnessTableEntry(i,  (1.0F - f1) / (f1 * 3.0F + 1.0F) * (1.0F - f) + f);
                    }
                }
                break;
            default:
                break;
        }
    }
    /**
     * Set world online
     * @param w - loaded world
     */
    public void setWorldLoaded(World w) {
        world = w;
        worldfolder = world.getWorldFolder();
        env = world.getEnvironment();
        skylight = (env == World.Environment.NORMAL);
    }
    /**
     * Set world unloaded
     */
    @Override
    public void setWorldUnloaded() {
        getSpawnLocation(); /* Remember spawn location before unload */
        world = null;
    }
    /* Test if world is nether */
    @Override
    public boolean isNether() {
        return env == World.Environment.NETHER;
    }
    /* Get world spawn location */
    @Override
    public DynmapLocation getSpawnLocation() {
        if(world != null) {
            return callGlobal(new Callable<DynmapLocation>() {
                @Override
                public DynmapLocation call() throws Exception {
                    Location sloc = world.getSpawnLocation();
                    spawnloc.x = sloc.getBlockX();
                    spawnloc.y = sloc.getBlockY();
                    spawnloc.z = sloc.getBlockZ();
                    spawnloc.world = normalizeWorldName(sloc.getWorld().getName());
                    return spawnloc;
                }
            }, spawnloc);
        }
        return spawnloc;
    }
    /* Get world time */
    @Override
    public long getTime() {
        if(world != null) {
            return callGlobal(new Callable<Long>() {
                @Override
                public Long call() throws Exception {
                    return world.getTime();
                }
            }, -1L);
        }
        else {
            return -1;
        }
    }
    /* World is storming */
    @Override
    public boolean hasStorm() {
        if(world != null) {
            return callGlobal(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return world.hasStorm();
                }
            }, false);
        }
        else {
            return false;
        }
    }
    /* World is thundering */
    @Override
    public boolean isThundering() {
        if(world != null) {
            return callGlobal(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return world.isThundering();
                }
            }, false);
        }
        else {
            return false;
        }
    }
    /* World is loaded */
    @Override
    public boolean isLoaded() {
        return (world != null);
    }
    /* Get light level of block */
    @Override
    public int getLightLevel(int x, int y, int z) {
        if(world != null) {
            if ((y >= minY) && (y < this.worldheight)) {
                final int fx = x, fy = y, fz = z;
                return callRegion(world, x >> 4, z >> 4, new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return Integer.valueOf(world.getBlockAt(fx, fy, fz).getLightLevel());
                    }
                }, 0);
            }
            return 0;
        }
        else {
            return -1;
        }
    }
    /* Get highest Y coord of given location */
    @Override
    public int getHighestBlockYAt(int x, int z) {
        if(world != null) {
            final int fx = x, fz = z;
            return callRegion(world, x >> 4, z >> 4, new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return world.getHighestBlockYAt(fx, fz);
                }
            }, -1);
        }
        else {
            return -1;
        }
    }
    /* Test if sky light level is requestable */
    @Override
    public boolean canGetSkyLightLevel() {
        return skylight && (world != null);
    }
    /* Return sky light level */
    @Override
    public int getSkyLightLevel(int x, int y, int z) {
        if(world != null) {
            if ((y >= minY) && (y < this.worldheight)) {
                final int fx = x, fy = y, fz = z;
                return callRegion(world, x >> 4, z >> 4, new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return Integer.valueOf(world.getBlockAt(fx, fy, fz).getLightFromSky());
                    }
                }, -1);
            }
            else {
                return 15;
            }
        }
        else {
            return -1;
        }
    }
    /**
     * Get world environment ID (lower case - normal, the_end, nether)
     */
    @Override
    public String getEnvironment() {
        return env.name().toLowerCase();
    }
    /**
     * Get map chunk cache for world
     */
    @Override
    public MapChunkCache getChunkCache(List<DynmapChunk> chunks) {
        if(isLoaded()) {
        	return BukkitVersionHelper.helper.getChunkCache(this, chunks);
        }
        else {
            return null;
        }
    }
    
    public World getWorld() {
        return world;
    }
    
    // Return false if unimplemented
    @Override
    public int getChunkMap(TileFlags map) {
        map.clear();
        if (world == null) return -1;
        int cnt = 0;
        if(threadSafeAccess == null) {
            // Mark loaded chunks
            for(Chunk c : world.getLoadedChunks()) {
                map.setFlag(c.getX(), c.getZ(), true);
                cnt++;
            }
        }
        File f = worldfolder;
        if (f == null) return cnt;
        File regiondir = new File(f, "region");
        File[] lst = regiondir.listFiles();
        if(lst != null) {
            byte[] hdr = new byte[4096];
            for(File rf : lst) {
                if(!rf.getName().endsWith(".mca")) {
                    continue;
                }
                String[] parts = rf.getName().split("\\.");
                if((!parts[0].equals("r")) && (parts.length != 4)) continue;
                
                RandomAccessFile rfile = null;
                int x = 0, z = 0;
                try {
                    x = Integer.parseInt(parts[1]);
                    z = Integer.parseInt(parts[2]);
                    rfile = new RandomAccessFile(rf, "r");
                    rfile.read(hdr, 0, hdr.length);
                } catch (IOException iox) {
                    Arrays.fill(hdr,  (byte)0);
                } catch (NumberFormatException nfx) {
                    Arrays.fill(hdr,  (byte)0);
                } finally {
                    if(rfile != null) {
                        try { rfile.close(); } catch (IOException iox) {}
                    }
                }
                for (int i = 0; i < 1024; i++) {
                    int v = hdr[4*i] | hdr[4*i + 1] | hdr[4*i + 2] | hdr[4*i + 3];
                    if (v == 0) continue;
                    int xx = (x << 5) | (i & 0x1F);
                    int zz = (z << 5) | ((i >> 5) & 0x1F);
                    if (!map.setFlag(xx, zz, true)) {
                        cnt++;
                    }
                }
            }
        }
        return cnt;
    }
    private Polygon lastBorder;
    @Override
    public Polygon getWorldBorder() {
        if (world != null) {
            lastBorder = callGlobal(new Callable<Polygon>() {
                @Override
                public Polygon call() throws Exception {
                    return BukkitVersionHelper.helper.getWorldBorder(world);
                }
            }, lastBorder);
        }
        return lastBorder;
    }

    private static <T> T callGlobal(Callable<T> task, T def) {
        if(threadSafeAccess != null) {
            return threadSafeAccess.callGlobal(task, def);
        }
        try {
            T val = task.call();
            return (val != null) ? val : def;
        } catch (Exception x) {
            return def;
        }
    }

    private static <T> T callRegion(World world, int chunkX, int chunkZ, Callable<T> task, T def) {
        if(threadSafeAccess != null) {
            return threadSafeAccess.callRegion(world, chunkX, chunkZ, task, def);
        }
        try {
            T val = task.call();
            return (val != null) ? val : def;
        } catch (Exception x) {
            return def;
        }
    }
}
