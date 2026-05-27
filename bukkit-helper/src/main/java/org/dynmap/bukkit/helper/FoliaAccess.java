package org.dynmap.bukkit.helper;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public interface FoliaAccess {
    boolean isFolia();

    void runGlobal(Runnable run, long delayTicks);

    void runGlobalRepeating(Runnable run, long initialDelayTicks, long periodTicks);

    void runRegion(Location loc, Runnable run, long delayTicks);

    void runEntity(Entity entity, Runnable run, long delayTicks);

    <T> Future<T> callGlobal(Callable<T> task);

    <T> Future<T> callRegion(World world, int chunkX, int chunkZ, Callable<T> task);

    <T> Future<T> callEntity(Entity entity, Callable<T> task);

    boolean isOwnedByCurrentRegion(World world, int chunkX, int chunkZ);

    boolean isServerThread();

    void warnIfNotGlobal(String action);

    void warnIfNotRegion(String action, Location loc);

    void warnIfNotEntity(String action, Entity entity);

    void cancelTasks();
}
