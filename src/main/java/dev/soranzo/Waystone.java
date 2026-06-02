package dev.soranzo;

import dev.soranzo.commands.WaystoneCommand;
import dev.soranzo.listeners.WaystoneRegisterListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Waystone extends JavaPlugin {

    private final WaystoneManager wm = new WaystoneManager(this);
    private static Waystone instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        WaystoneRecipe.register(this);
        getServer().getPluginManager().registerEvents(new WaystoneRegisterListener(this), this);
        Bukkit.getScheduler().runTaskTimer(this, new WaystoneTeleportTask(wm), 0L, 10L);
        WaystoneCommand waystoneCommand = new WaystoneCommand(wm);
        getCommand("waystone").setExecutor(waystoneCommand);
        getCommand("waystone").setTabCompleter(waystoneCommand);
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

