package dev.soranzo;

import dev.soranzo.listeners.WaystoneRegisterListener;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class Waystone extends JavaPlugin {

    private final WaystoneManager wm = new WaystoneManager(this);
    private static Waystone instance;

    @Override
    public void onEnable() {
        instance = this;
        WaystoneRecipe.register(this);
        getServer().getPluginManager().registerEvents(new WaystoneRegisterListener(this), this);
        Bukkit.getScheduler().runTaskTimer(this, new WaystoneTeleportTask(wm), 0L, 10L);
    }

    @Override
    public void onDisable() {}

    public WaystoneManager getWaystoneManager() {
        return wm;
    }

    public static Waystone getThisPlugin(){
        return instance;
    }


}

