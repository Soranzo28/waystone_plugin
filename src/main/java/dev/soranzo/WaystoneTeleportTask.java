package dev.soranzo;

import dev.soranzo.dto.PlayerYamlDTO;
import dev.soranzo.dto.WaystoneYamlDTO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;

public class WaystoneTeleportTask implements Runnable {
    private final WaystoneManager wm;
    private final HashMap<UUID, Long> standingOn = new HashMap<>();

    public WaystoneTeleportTask(WaystoneManager wm) {
        this.wm = wm;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Block blockBelow = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (blockBelow.getType() != Material.LODESTONE) {
                standingOn.remove(player.getUniqueId());
                continue;
            }
            if (!wm.isThisBlockWaystone(blockBelow.getLocation())) continue;
            if (!player.hasPermission("waystones.use")) continue;
            if (wm.isOnCooldown(player.getUniqueId())) continue;

            PlayerYamlDTO pInfo = wm.getDiscoveries().get(player.getUniqueId());
            if (pInfo == null) continue;
            String blockBelowLocationString = wm.locationToString(blockBelow.getLocation());

            String connectionString = pInfo.connections().getOrDefault(blockBelowLocationString, "");
            if (connectionString.isEmpty()) continue;
            Location tpLocation = wm.stringToLocation(connectionString);

            if (!wm.isCrossDimensionAllowed() && !tpLocation.getWorld().equals(player.getWorld())) {
                player.sendActionBar(Component.text("✦ Cross-dimension travel is disabled.")
                        .color(TextColor.color(0xff6b6b)));
                continue;
            }

            if (!standingOn.containsKey(player.getUniqueId())) {
                standingOn.put(player.getUniqueId(), System.currentTimeMillis());
                player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 1.6f);
                blockBelow.getWorld().spawnParticle(Particle.PORTAL,
                        blockBelow.getLocation().clone().add(0.5, 1.0, 0.5), 20, 0.3, 0.3, 0.3, 0.15);
                continue;
            }
            if (System.currentTimeMillis() - standingOn.get(player.getUniqueId()) < wm.getStandingDelay()) continue;
            standingOn.remove(player.getUniqueId());

            tpLocation.add(0.5, 1, 0.5);
            Location originLoc = player.getLocation().clone();

            if (wm.isTeleportCostEnabled()) {
                int cost = wm.getTeleportCost(originLoc, tpLocation);
                if (player.getLevel() < cost) {
                    player.sendActionBar(Component.text("✦ Not enough XP — need " + cost + " level(s).")
                            .color(TextColor.color(0xff6b6b)));
                    standingOn.remove(player.getUniqueId());
                    continue;
                }
                player.setLevel(player.getLevel() - cost);
            }

            originLoc.getWorld().spawnParticle(Particle.PORTAL,
                    originLoc.clone().add(0, 0.5, 0), 60, 0.4, 0.8, 0.4, 0.2);

            player.teleport(tpLocation);
            wm.setCooldown(player.getUniqueId());

            WaystoneYamlDTO destData = wm.getWaystones().get(connectionString);
            String destName = destData != null ? destData.name() : "Waystone";
            Title title = Title.title(
                    Component.text("✦ Waystone ✦").color(TextColor.color(0xffd700)),
                    Component.text(destName).color(TextColor.color(0xa78bfa)),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(300))
            );
            player.showTitle(title);
            player.playSound(tpLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.1f);
            tpLocation.getWorld().spawnParticle(Particle.PORTAL,
                    tpLocation.clone().add(0, 0.5, 0), 80, 0.5, 0.8, 0.5, 0.3);
            tpLocation.getWorld().spawnParticle(Particle.END_ROD,
                    tpLocation.clone().add(0, 1.0, 0), 25, 0.3, 0.6, 0.3, 0.05);
        }
    }
}
