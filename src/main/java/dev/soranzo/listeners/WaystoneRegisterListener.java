package dev.soranzo.listeners;

import dev.soranzo.*;
import dev.soranzo.dto.WaystoneYamlDTO;
import dev.soranzo.gui.WaystoneGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;
import java.util.UUID;

public class WaystoneRegisterListener implements Listener {
    private final Waystone pl;
    private final WaystoneManager wm;

    public WaystoneRegisterListener(Waystone pl) {
        this.pl = pl;
        wm = pl.getWaystoneManager();
    }

    // Handles waystone events as a block
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        boolean isWaystone = meta.getPersistentDataContainer().has(WaystoneConstants.IS_WAYSTONE_KEY, PersistentDataType.BOOLEAN);

        if (!isWaystone) return;
        if (!event.getPlayer().hasPermission("waystones.place")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendActionBar(Component.text("✦ You don't have permission to place waystones.")
                    .color(TextColor.color(0xff6b6b)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        wm.registerNewWaystone(event.getBlock().getLocation(), "nameless", event.getPlayer().getUniqueId());

        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        player.playSound(player.getLocation(), Sound.BLOCK_LODESTONE_PLACE, 1.0f, 0.6f);
        loc.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(0.5, 1.0, 0.5), 25, 0.4, 0.4, 0.4, 0.2);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (wm.isThisBlockWaystone(event.getBlock().getLocation())) {
            Player player = event.getPlayer();
            Location loc = event.getBlock().getLocation();

            WaystoneYamlDTO waystone = wm.getWaystones().get(wm.locationToString(loc));
            UUID owner = waystone != null ? waystone.owner() : null;
            boolean isOwner = player.getUniqueId().equals(owner);
            if (!isOwner && !player.hasPermission("waystones.break")) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("✦ You are not the owner of this waystone.")
                        .color(TextColor.color(0xff6b6b)));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            player.playSound(player.getLocation(), Sound.BLOCK_LODESTONE_BREAK, 1.0f, 0.8f);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0.5, 0.5, 0.5), 30, 0.3, 0.3, 0.3, 0.05);
            loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3, 0.0);

            wm.unregisterWaystone(event.getBlock().getLocation());
        } else if (event.getBlock().getBlockData() instanceof WallSign wallSign) {
            Block signBlock = event.getBlock();
            Block attached = signBlock.getRelative(wallSign.getFacing().getOppositeFace());

            if (attached.getType() != Material.LODESTONE) return;
            if (!wm.isThisBlockWaystone(attached.getLocation())) return;

            Player player = event.getPlayer();
            WaystoneYamlDTO attachedWaystone = wm.getWaystones().get(wm.locationToString(attached.getLocation()));
            UUID attachedOwner = attachedWaystone != null ? attachedWaystone.owner() : null;
            if (!player.getUniqueId().equals(attachedOwner) && !player.hasPermission("waystones.break")) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("✦ You are not the owner of this waystone.")
                        .color(TextColor.color(0xff6b6b)));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            attached.getLocation().getWorld().spawnParticle(Particle.SMOKE,
                    attached.getLocation().clone().add(0.5, 1.5, 0.5), 20, 0.2, 0.3, 0.2, 0.02);

            wm.setWaystoneInactive(attached.getLocation());
        }
    }

    // Handles waystone explosions
    private void handleExplosion(List<Block> blocks) {
        blocks.removeIf(block -> {
            if (block.getType() == Material.LODESTONE && wm.isThisBlockWaystone(block.getLocation())) {
                Location loc = block.getLocation().clone();
                loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0.5, 0.5, 0.5), 40, 0.5, 0.5, 0.5, 0.05);
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0.5, 0.5, 0.5), 30, 0.4, 0.4, 0.4, 0.1);
                loc.getWorld().playSound(loc, Sound.BLOCK_LODESTONE_BREAK, 1.0f, 0.8f);

                block.getWorld().dropItemNaturally(block.getLocation(), WaystoneRecipe.getWaystoneItem(pl));
                wm.unregisterWaystone(block.getLocation());
                block.setType(Material.AIR);
                return true;
            }
            return false;
        });
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    // Handles waystone naming
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Block signBlock = event.getBlock();
        if (!(signBlock.getBlockData() instanceof WallSign wallSign)) return;

        Block attached = signBlock.getRelative(wallSign.getFacing().getOppositeFace());

        if (attached.getType() != Material.LODESTONE) return;
        if (!wm.isThisBlockWaystone(attached.getLocation())) return;

        String name = null;
        for (Component line : event.lines()) {
            String text = PlainTextComponentSerializer.plainText().serialize(line);
            if (!text.isBlank()) {
                name = text;
                break;
            }
        }

        WaystoneYamlDTO existing = wm.getWaystones().get(wm.locationToString(attached.getLocation()));
        String oldName = existing.name();

        UUID owner = existing.owner();
        Player player = event.getPlayer();
        boolean isOwner = player.getUniqueId().equals(owner);
        if (!isOwner && !player.hasPermission("waystones.break")) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("✦ You are not the owner of this waystone.")
                    .color(TextColor.color(0xff6b6b)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (name == null) return;
        String trimmed_name = name.trim();
        wm.setWaystoneName(attached.getLocation(), trimmed_name);
        wm.setWaystoneActive(attached.getLocation());

        Location attachedCenter = attached.getLocation().clone().add(0.5, 0.5, 0.5);
        attached.getLocation().getWorld().spawnParticle(Particle.CLOUD, attachedCenter, 30, 0.3, 0.4, 0.3, 0.05);
        if ("nameless".equals(oldName)) {
            player.sendActionBar(Component.text("✦ Waystone named: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(name)
                            .color(NamedTextColor.LIGHT_PURPLE)));
        } else {
            player.sendActionBar(Component.text("✦ Waystone renamed: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(oldName)
                            .color(NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(" → ").color(NamedTextColor.GRAY))
                    .append(Component.text(name)
                            .color(NamedTextColor.LIGHT_PURPLE)));
        }
    }

    // Handles waystone interactions
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
        if (Tag.SIGNS.isTagged(itemInHand.getType())) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.LODESTONE) return;
        if (!wm.isThisBlockWaystone(block.getLocation())) return;

        if (!event.getPlayer().hasPermission("waystones.use")) {
            Player player = event.getPlayer();
            player.sendActionBar(Component.text("✦ You don't have permission to use waystones.")
                    .color(TextColor.color(0xff6b6b)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (!wm.getWaystoneStatus(block.getLocation())) {
            Player player = event.getPlayer();
            player.sendActionBar(Component.text("✦ This waystone has not been named yet")
                    .color(NamedTextColor.GRAY));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (wm.registerNewDiscover(block.getLocation(), event.getPlayer().getUniqueId())) {
            Player player = event.getPlayer();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.0f);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.5f);
            block.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    block.getLocation().clone().add(0.5, 1.0, 0.5), 70, 0.4, 0.8, 0.4, 0.3);
            return;
        }

        Player player = event.getPlayer();
        String wsName = wm.getWaystones().get(wm.locationToString(block.getLocation())).name();
        player.sendActionBar(Component.text("✦ " + wsName + " — Choose your destination")
                .color(NamedTextColor.GRAY));
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.08f, 2.0f);
        block.getWorld().spawnParticle(Particle.END_ROD,
                block.getLocation().clone().add(0.5, 1.2, 0.5), 12, 0.3, 0.4, 0.3, 0.05);

        String originLocation = wm.locationToString(block.getLocation());
        if (!wm.getDiscoveries().containsKey(player.getUniqueId())) {
            wm.ensureGlobalsDiscovered(player.getUniqueId());
        }
        player.openInventory(new WaystoneGUI(player, wm, originLocation, 0).getInventory());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof WaystoneGUI)) return;
        ((Player) event.getPlayer()).stopSound(Sound.BLOCK_PORTAL_AMBIENT);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WaystoneGUI gui)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;


        boolean isWaystone = false;
        boolean isArrow = false;
        boolean isAuthor = false;

        ItemMeta meta = clicked.getItemMeta();

        if (meta.getPersistentDataContainer().has(WaystoneConstants.WAYSTONE_LOCATION_KEY, PersistentDataType.STRING)) {
            isWaystone = true;
        } else if (meta.getPersistentDataContainer().has(WaystoneConstants.WAYSTONE_PAGE_KEY, PersistentDataType.INTEGER)) {
            isArrow = true;
        } else if (meta.getPersistentDataContainer().has(WaystoneConstants.WAYSTONE_AUTHOR_KEY, PersistentDataType.BOOLEAN)) {
            isAuthor = true;
        }

        if (isWaystone) {

            String locationString = meta.getPersistentDataContainer().get(WaystoneConstants.WAYSTONE_LOCATION_KEY, PersistentDataType.STRING);
            WaystoneYamlDTO waystoneData = wm.getWaystones().get(locationString);
            if (waystoneData == null) return;

            String destinyLocationString = waystoneData.stringLocation();

            String originLocationString = gui.getOriginLocation();
            wm.setConnection(event.getWhoClicked().getUniqueId(), originLocationString, destinyLocationString);

            Player p = (Player) event.getWhoClicked();
            WaystoneYamlDTO originData = wm.getWaystones().get(originLocationString);
            String originName = originData != null ? originData.name() : "?";
            String destName = waystoneData.name();
            p.sendActionBar(Component.text("✦ Destination set: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(originName)
                            .color(NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(" → ").color(NamedTextColor.GRAY))
                    .append(Component.text(destName)
                            .color(NamedTextColor.LIGHT_PURPLE)));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
            p.closeInventory();
        }
        else if (isAuthor) {
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            TextColor purple = TextColor.color(0x818cf8);
            TextColor gold   = TextColor.color(0xffd700);
            TextColor white  = TextColor.color(0xffffff);
            TextColor gray   = TextColor.color(0xa0aec0);
            String line = "=================";
            p.sendMessage(Component.text("✦ " + line + " AUTHOR " + line + " ✦").color(purple));
            p.sendMessage(
                Component.text("  Name    ").color(gray)
                    .append(Component.text("Soranzo").color(gold))
            );
            p.sendMessage(
                Component.text("  Github   ").color(gray)
                    .append(Component.text("github.com/Soranzo28").color(white)
                        .clickEvent(ClickEvent.openUrl("https://github.com/Soranzo28"))
                        .decorate(TextDecoration.UNDERLINED))
            );
            p.sendMessage(
                Component.text("  Discord  ").color(gray)
                    .append(Component.text("soranzo28").color(white))
            );
            p.sendMessage(Component.text("✦ " + line + "========" + line + " ✦").color(purple));
        }
        else if (isArrow) {
            Player p = (Player) event.getWhoClicked();
            int nextPage = meta.getPersistentDataContainer().get(WaystoneConstants.WAYSTONE_PAGE_KEY, PersistentDataType.INTEGER);
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1.2f);
            p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.08f, 2.0f);
            p.openInventory(new WaystoneGUI(p, wm, gui.getOriginLocation(), nextPage).getInventory());
        }
    }
}
