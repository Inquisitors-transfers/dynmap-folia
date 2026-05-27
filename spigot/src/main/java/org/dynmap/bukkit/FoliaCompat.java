package org.dynmap.bukkit;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.dynmap.Log;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

final class FoliaCompat {
    private final DynmapPlugin plugin;
    private final Set<String> diagnostics = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    FoliaCompat(DynmapPlugin plugin) {
        this.plugin = plugin;
    }

    boolean isFolia() {
        return true;
    }

    void runGlobal(Runnable run, long delayTicks) {
        if ((delayTicks <= 0) && isGlobalOrStartupThread()) {
            run.run();
        }
        else if (delayTicks <= 0) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, run);
        }
        else {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, toConsumer(run), delayTicks);
        }
    }

    void runGlobalRepeating(Runnable run, long initialDelayTicks, long periodTicks) {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, toConsumer(run),
                normalizeDelay(initialDelayTicks), Math.max(1L, periodTicks));
    }

    void runRegion(Location loc, Runnable run, long delayTicks) {
        if ((loc == null) || (loc.getWorld() == null)) {
            runGlobal(run, delayTicks);
        }
        else if ((delayTicks <= 0) && Bukkit.isOwnedByCurrentRegion(loc)) {
            run.run();
        }
        else if (delayTicks <= 0) {
            Bukkit.getRegionScheduler().execute(plugin, loc, run);
        }
        else {
            Bukkit.getRegionScheduler().runDelayed(plugin, loc, toConsumer(run), delayTicks);
        }
    }

    void runEntity(Entity entity, Runnable run, long delayTicks) {
        if (entity == null) {
            runGlobal(run, delayTicks);
        }
        else if ((delayTicks <= 0) && Bukkit.isOwnedByCurrentRegion(entity)) {
            run.run();
        }
        else if (delayTicks <= 0) {
            entity.getScheduler().run(plugin, toConsumer(run), () -> { });
        }
        else {
            entity.getScheduler().runDelayed(plugin, toConsumer(run), () -> { }, delayTicks);
        }
    }

    <T> Future<T> callGlobal(final Callable<T> task) {
        final CompletableFuture<T> future = new CompletableFuture<T>();
        if (isGlobalOrStartupThread()) {
            completeFuture(future, task);
        }
        else {
            Bukkit.getGlobalRegionScheduler().execute(plugin, new Runnable() {
                @Override
                public void run() {
                    completeFuture(future, task);
                }
            });
        }
        return future;
    }

    <T> Future<T> callRegion(World world, int chunkX, int chunkZ, final Callable<T> task) {
        final CompletableFuture<T> future = new CompletableFuture<T>();
        if (world == null) {
            future.complete(null);
        }
        else if (Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ)) {
            completeFuture(future, task);
        }
        else {
            Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, new Runnable() {
                @Override
                public void run() {
                    completeFuture(future, task);
                }
            });
        }
        return future;
    }

    <T> Future<T> callEntity(Entity entity, final Callable<T> task) {
        final CompletableFuture<T> future = new CompletableFuture<T>();
        if (entity == null) {
            future.complete(null);
        }
        else if (Bukkit.isOwnedByCurrentRegion(entity)) {
            completeFuture(future, task);
        }
        else {
            ScheduledTask scheduledTask = entity.getScheduler().run(plugin, new Consumer<ScheduledTask>() {
                @Override
                public void accept(ScheduledTask ignored) {
                    completeFuture(future, task);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    future.complete(null);
                }
            });
            if (scheduledTask == null) {
                future.complete(null);
            }
        }
        return future;
    }

    boolean isServerThread() {
        return isGlobalOrStartupThread();
    }

    void warnIfNotGlobal(String action) {
        if (!isGlobalOrStartupThread()) {
            warnOnce("global:" + action, action + " ran away from the Folia global region thread");
        }
    }

    void warnIfNotRegion(String action, Location loc) {
        if ((loc != null) && (loc.getWorld() != null) && !Bukkit.isOwnedByCurrentRegion(loc)) {
            warnOnce("region:" + action, action + " touched " + formatLocation(loc)
                    + " away from its owning Folia region thread");
        }
    }

    void warnIfNotEntity(String action, Entity entity) {
        if ((entity != null) && !Bukkit.isOwnedByCurrentRegion(entity)) {
            warnOnce("entity:" + action, action + " touched entity " + entity.getUniqueId()
                    + " away from its owning Folia region thread");
        }
    }

    void cancelTasks() {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }

    private static Consumer<ScheduledTask> toConsumer(final Runnable run) {
        return new Consumer<ScheduledTask>() {
            @Override
            public void accept(ScheduledTask ignored) {
                run.run();
            }
        };
    }

    private static long normalizeDelay(long delayTicks) {
        return Math.max(1L, delayTicks);
    }

    private static boolean isGlobalOrStartupThread() {
        return Bukkit.isGlobalTickThread() || Bukkit.isPrimaryThread();
    }

    private void warnOnce(String key, String message) {
        if (diagnostics.add(key)) {
            Log.warning("[Folia diagnostic] " + message);
        }
    }

    private static String formatLocation(Location loc) {
        World world = loc.getWorld();
        String worldName = (world != null) ? world.getName() : "<unknown>";
        return worldName + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static <T> void completeFuture(CompletableFuture<T> future, Callable<T> task) {
        try {
            future.complete(task.call());
        } catch (Throwable x) {
            future.completeExceptionally(x);
        }
    }
}
