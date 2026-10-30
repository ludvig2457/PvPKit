package com.ludvig.pvpkit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class SchedulerCompat {

    private static final boolean FOLIA = detectFolia();

    private SchedulerCompat() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static Object runTimer(Plugin plugin, Runnable action, long delay, long period) {
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskTimer(plugin, action, delay, period);
        }
        try {
            Object scheduler = globalScheduler();
            Method method = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
            return method.invoke(scheduler, plugin, (Consumer<Object>) task -> action.run(), delay, period);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not schedule Folia task", exception);
        }
    }

    public static Object repeatForPlayers(Plugin plugin, long delay, long period, Consumer<Player> action) {
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    action.accept(player);
                }
            }, delay, period);
        }
        try {
            Object scheduler = globalScheduler();
            Method method = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
            return method.invoke(scheduler, plugin, (Consumer<Object>) task -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    runForPlayer(plugin, player, () -> action.accept(player));
                }
            }, delay, period);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not schedule Folia player task", exception);
        }
    }

    public static void runForPlayer(Plugin plugin, Player player, Runnable action) {
        runForEntity(plugin, player, action);
    }

    public static void runForEntity(Plugin plugin, Entity entity, Runnable action) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTask(plugin, action);
            return;
        }
        try {
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
            Method method = scheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class);
            method.invoke(scheduler, plugin, (Consumer<Object>) task -> action.run(), (Runnable) () -> {
            });
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not schedule Folia entity task", exception);
        }
    }

    public static void runAsync(Plugin plugin, Runnable action) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, action);
            return;
        }
        try {
            Object scheduler = Bukkit.getServer().getClass().getMethod("getAsyncScheduler").invoke(Bukkit.getServer());
            Method method = scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
            method.invoke(scheduler, plugin, (Consumer<Object>) task -> action.run());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not schedule Folia async task", exception);
        }
    }

    public static void teleport(Player player, Location location) {
        if (!FOLIA) {
            player.teleport(location);
            return;
        }
        try {
            Method method = player.getClass().getMethod("teleportAsync", Location.class);
            method.invoke(player, location);
        } catch (ReflectiveOperationException exception) {
            player.teleport(location);
        }
    }

    public static void cancel(Object task) {
        if (task == null) {
            return;
        }
        try {
            task.getClass().getMethod("cancel").invoke(task);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Object globalScheduler() throws ReflectiveOperationException {
        return Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler").invoke(Bukkit.getServer());
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
