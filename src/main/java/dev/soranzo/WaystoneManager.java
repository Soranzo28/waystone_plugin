package dev.soranzo;

import dev.soranzo.dto.PlayerYamlDTO;
import dev.soranzo.dto.WaystoneYamlDTO;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class WaystoneManager {
    private final Waystone plugin;
    private FileConfiguration data;
    private File file;

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private final HashMap<String, WaystoneYamlDTO> waystonesInfo = new HashMap<>();
    private final HashMap<UUID, PlayerYamlDTO> playerInfo = new HashMap<>();
    private final HashSet<String> globalWaystones = new HashSet<>();

    public WaystoneManager(Waystone plugin) {
       this.plugin = plugin;
       File file = new File(plugin.getDataFolder(), "waystone.yml");

       try {
           if (!file.exists()) {
               file.getParentFile().mkdirs();
               file.createNewFile();
           }
           data = YamlConfiguration.loadConfiguration(file);
           this.file = file;
           populateWaystones();
           populateDiscoveries();
       } catch (java.io.IOException e) {
           e.printStackTrace();
       }
    }

    public HashMap<String, WaystoneYamlDTO> getWaystones() { return waystonesInfo; }
    public HashMap<UUID, PlayerYamlDTO> getDiscoveries() { return playerInfo; }

    public List<WaystoneYamlDTO> getWaystonesByName(String name) {
        return waystonesInfo.values().stream()
                .filter(w -> w.name().equalsIgnoreCase(name))
                .collect(java.util.stream.Collectors.toList());
    }

    public void reload() {
        plugin.reloadConfig();
        data = YamlConfiguration.loadConfiguration(file);
        waystonesInfo.clear();
        playerInfo.clear();
        globalWaystones.clear();
        populateWaystones();
        populateDiscoveries();
    }


    //Helper methods
    private void populateWaystones() {
        if (!data.contains("waystones")) return;

        for (String key : data.getConfigurationSection("waystones").getKeys(false)){
            String name = data.getString("waystones." + key + ".name");
            boolean active = data.getBoolean("waystones." + key + ".active");
            String ownerStr = data.getString("waystones." + key + ".owner");
            UUID owner = ownerStr != null ? UUID.fromString(ownerStr) : null;
            boolean global = data.getBoolean("waystones." + key + ".global", false);
            if (global) globalWaystones.add(key);

            waystonesInfo.put(key, new WaystoneYamlDTO(key, name, active, owner, global));
        }
    }

    //Not the ideal method, it loads every player data into memory even if the player is offline
    private void populateDiscoveries() {
        if (!data.contains("players")) return;

        for (String uuidString : data.getConfigurationSection("players").getKeys(false)) {
            UUID playerUUID = UUID.fromString(uuidString);
            List<String> pDiscoveries = data.getStringList("players." + uuidString + ".discoveries");
            HashMap<String, String> pConnections = new HashMap<>();
            if (data.contains("players." + uuidString + ".connections")) {
                for (Map.Entry<String, Object> entry : data.getConfigurationSection("players." + uuidString + ".connections").getValues(false).entrySet()) {
                    pConnections.put(entry.getKey(), (String) entry.getValue());
                }
            }
            playerInfo.put(playerUUID, new PlayerYamlDTO(playerUUID, pConnections, pDiscoveries));
        }
    }

    public String locationToString(Location location) {
        return  location.getWorld().getName() +
                "," +
                location.getBlockX() +
                "," +
                location.getBlockY() +
                "," +
                location.getBlockZ();
    }

    public Location stringToLocation(String locationString){
        String[] parts = locationString.split(",");
        World world = Bukkit.getWorld(parts[0]);
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        return new Location(world, x,y,z);
    }

    private void saveDataFile() {
        try{
            data.save(file);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private boolean discoveryAlreadyDiscovered(Location location, PlayerYamlDTO pInfo) {
        return pInfo.discoveries().contains(locationToString(location));
    }

    public boolean getWaystoneStatus(Location location) {
        WaystoneYamlDTO waystone = waystonesInfo.get(locationToString(location));
        if (waystone == null) return false;
        return waystone.active();
    }

    public boolean isThisBlockWaystone(Location location) {
        return waystonesInfo.containsKey(locationToString(location));
    }

    public long getStandingDelay() {
        return plugin.getConfig().getLong("standing-delay", 10) * 50L;
    }

    public long getCooldownDelay() {
        return plugin.getConfig().getLong("teleport-cooldown", 3) * 1000L;
    }

    public boolean isCrossDimensionAllowed() {
        return plugin.getConfig().getBoolean("cross-dimension", false);
    }

    public boolean isTeleportCostEnabled() {
        return plugin.getConfig().getBoolean("teleport-cost-enabled", true);
    }

    public int getTeleportCostXp(Location from, Location to) {
        int scale = plugin.getConfig().getInt("teleport-cost-scale", 20);
        if (!from.getWorld().equals(to.getWorld())) {
            boolean involvesEnd = from.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END
                    || to.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END;
            int fee = involvesEnd
                    ? plugin.getConfig().getInt("end-dimension-fee", 160)
                    : plugin.getConfig().getInt("cross-dimension-fee", 55);
            double dx = from.getX() - to.getX();
            double dz = from.getZ() - to.getZ();
            return calculateCostXp(Math.sqrt(dx * dx + dz * dz), scale) + fee;
        }
        return calculateCostXp(from.distance(to), scale);
    }

    public static double xpToLevel(int xp) {
        if (xp <= 352)  return -3 + Math.sqrt(9 + xp);
        if (xp <= 1507) return (40.5 + Math.sqrt(10.0 * xp - 1959.75)) / 5;
        return (162.5 + Math.sqrt(18.0 * xp - 13553.75)) / 9;
    }

    static int calculateCostXp(double distance, int scale) {
        return (int) (distance / scale);
    }

    public boolean isOnCooldown(UUID uuid) {
        Long time = cooldowns.get(uuid);
        if (time == null) return false;
        if (System.currentTimeMillis() - time >= getCooldownDelay()) {
            cooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    public void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }


    //Yaml and in memory data handlers
    public void registerNewWaystone(Location location, String name, UUID playerUUID) {
        plugin.getLogger().info("registerNewWaystone called for " + locationToString(location));
        if (data == null) {
            plugin.getLogger().severe("data is null!");
            return;
        }
        //Creates new waystone entry
        data.set("waystones." + locationToString(location) + ".name", name);
        data.set("waystones." + locationToString(location) + ".active", false);
        data.set("waystones." + locationToString(location) + ".owner", playerUUID.toString());
        saveDataFile();

        waystonesInfo.put(locationToString(location), new WaystoneYamlDTO(locationToString(location), name, false, playerUUID, false));
    }

    public void unregisterWaystone(Location location) {
        data.set("waystones." + locationToString(location), null);

        waystonesInfo.remove(locationToString(location));
        globalWaystones.remove(locationToString(location));

        for (PlayerYamlDTO player : playerInfo.values()) {

            String stringLocation = locationToString(location);

            List<String> pDiscoveries = new ArrayList<>(player.discoveries());
            pDiscoveries.remove(stringLocation);

            HashMap<String, String> pConnections = new HashMap<>(player.connections());
            pConnections.remove(stringLocation);
            pConnections.values().removeIf(dest -> dest.equals(stringLocation));

            playerInfo.put(player.playerUUID(), new PlayerYamlDTO(player.playerUUID(), pConnections, pDiscoveries));
            data.set("players." + player.playerUUID() + ".discoveries", pDiscoveries);
            data.set("players." + player.playerUUID() + ".connections", pConnections.isEmpty() ? null : pConnections);

        }
        saveDataFile();
    }

    public boolean registerNewDiscover(Location location, UUID playerUUID) {
        PlayerYamlDTO old = playerInfo.getOrDefault(playerUUID, new PlayerYamlDTO(playerUUID, new HashMap<>(), new ArrayList<>()));

        if (discoveryAlreadyDiscovered(location, old)) return false;

        List<String> pDiscoveries =  new ArrayList<>(old.discoveries());
        pDiscoveries.add(locationToString(location));
        data.set("players." + playerUUID.toString() + ".discoveries", pDiscoveries);
        saveDataFile();

        playerInfo.put(old.playerUUID(), new PlayerYamlDTO(playerUUID, old.connections(), pDiscoveries));
        return true;
    }

    public void setConnection(UUID playerUUID, String originString, String destinyString) {
        data.set("players." + playerUUID.toString() + ".connections." + originString, destinyString);

        saveDataFile();

        PlayerYamlDTO old = playerInfo.getOrDefault(playerUUID, new PlayerYamlDTO(playerUUID, new HashMap<>(), new ArrayList<>()));
        HashMap<String, String> pConnections = new HashMap<>(old.connections());
        pConnections.put(originString, destinyString);

        playerInfo.put(playerUUID, new PlayerYamlDTO(
                playerUUID,
                pConnections,
                old.discoveries()
        ));
    }

    public void setWaystoneActive(Location location) {
        data.set("waystones." + locationToString(location) + ".active", true);
        saveDataFile();

        WaystoneYamlDTO old = waystonesInfo.get(locationToString(location));
        if (old == null) return;
        waystonesInfo.put(locationToString(location), new WaystoneYamlDTO(locationToString(location), old.name(), true, old.owner(), old.global()));
    }

    public void setWaystoneInactive(Location location) {
        data.set("waystones." + locationToString(location) + ".active", false);
        saveDataFile();

        WaystoneYamlDTO old = waystonesInfo.get(locationToString(location));
        if (old == null) return;
        waystonesInfo.put(locationToString(location), new WaystoneYamlDTO(locationToString(location), old.name(), false, old.owner(), old.global()));
    }

    public void setWaystoneGlobal(Location location, boolean global) {
        String key = locationToString(location);
        data.set("waystones." + key + ".global", global);
        saveDataFile();
        if (global) {
            globalWaystones.add(key);
            for (UUID uuid : playerInfo.keySet()) {
                registerNewDiscover(location, uuid);
            }
        } else {
            globalWaystones.remove(key);
        }
        WaystoneYamlDTO old = waystonesInfo.get(key);
        if (old == null) return;
        waystonesInfo.put(key, new WaystoneYamlDTO(key, old.name(), old.active(), old.owner(), global));
    }

    public void ensureGlobalsDiscovered(UUID playerUUID) {
        for (String key : globalWaystones) {
            WaystoneYamlDTO ws = waystonesInfo.get(key);
            if (ws != null && ws.active()) {
                registerNewDiscover(stringToLocation(key), playerUUID);
            }
        }
    }

    public void setWaystoneName(Location location, String name) {
        data.set("waystones." + locationToString(location) + ".name", name);
        saveDataFile();
        WaystoneYamlDTO old = waystonesInfo.get(locationToString(location));
        if (old == null) return;
        waystonesInfo.put(locationToString(location), new WaystoneYamlDTO(locationToString(location), name, old.active(), old.owner(), old.global()));
    }
}
