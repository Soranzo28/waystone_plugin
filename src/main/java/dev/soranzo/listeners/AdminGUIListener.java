package dev.soranzo.listeners;

import dev.soranzo.*;
import dev.soranzo.dto.WaystoneYamlDTO;
import dev.soranzo.gui.AdminActionGUI;
import dev.soranzo.gui.AdminFilterGUI;
import dev.soranzo.gui.AdminGUI;
import dev.soranzo.gui.AdminOwnerGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class AdminGUIListener implements Listener {

    private static final TextColor BRIGHT = TextColor.color(0xe7dde9);
    private static final TextColor LIGHT  = TextColor.color(0xa889b9);
    private static final TextColor MID    = TextColor.color(0x6e4a75);
    private static final TextColor NAME   = TextColor.color(0xfce8f3);
    private static final TextColor RED    = TextColor.color(0xff6b6b);

    private final WaystoneManager wm;
    private final WaystoneTeleportTask teleportTask;

    public AdminGUIListener(WaystoneManager wm, WaystoneTeleportTask teleportTask) {
        this.wm = wm;
        this.teleportTask = teleportTask;
    }

    private void playNavSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getInventory().getHolder() instanceof AdminGUI adminGUI) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            ItemMeta meta = event.getCurrentItem().getItemMeta();
            if (meta == null) return;
            handleAdminGUIClick(player, adminGUI, meta);

        } else if (event.getInventory().getHolder() instanceof AdminFilterGUI filterGUI) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            ItemMeta meta = event.getCurrentItem().getItemMeta();
            if (meta == null) return;
            handleFilterGUIClick(player, filterGUI, meta);

        } else if (event.getInventory().getHolder() instanceof AdminOwnerGUI ownerGUI) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            ItemMeta meta = event.getCurrentItem().getItemMeta();
            if (meta == null) return;
            handleOwnerGUIClick(player, ownerGUI, meta);

        } else if (event.getInventory().getHolder() instanceof AdminActionGUI actionGUI) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            ItemMeta meta = event.getCurrentItem().getItemMeta();
            if (meta == null) return;
            handleActionGUIClick(player, actionGUI, meta);
        }
    }

    private void handleAdminGUIClick(Player player, AdminGUI gui, ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();

        if (pdc.has(WaystoneConstants.WAYSTONE_LOCATION_KEY, PersistentDataType.STRING)) {
            String locString = pdc.get(WaystoneConstants.WAYSTONE_LOCATION_KEY, PersistentDataType.STRING);
            WaystoneYamlDTO ws = wm.getWaystones().get(locString);
            if (ws == null) return;
            playNavSound(player);
            player.openInventory(new AdminActionGUI(ws, wm, gui.getPage(), gui.getDimensionFilter(), gui.getOwnerFilter()).getInventory());

        } else if (pdc.has(WaystoneConstants.ADMIN_FILTER_KEY, PersistentDataType.STRING)) {
            playNavSound(player);
            player.openInventory(new AdminFilterGUI(wm, gui.getPage(), gui.getDimensionFilter(), gui.getOwnerFilter()).getInventory());

        } else if (pdc.has(WaystoneConstants.WAYSTONE_PAGE_KEY, PersistentDataType.INTEGER)) {
            int nextPage = pdc.get(WaystoneConstants.WAYSTONE_PAGE_KEY, PersistentDataType.INTEGER);
            playNavSound(player);
            player.openInventory(new AdminGUI(wm, nextPage, gui.getDimensionFilter(), gui.getOwnerFilter()).getInventory());
        }
    }

    private void handleFilterGUIClick(Player player, AdminFilterGUI gui, ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();
        if (!pdc.has(WaystoneConstants.ADMIN_FILTER_KEY, PersistentDataType.STRING)) return;

        String filterValue = pdc.get(WaystoneConstants.ADMIN_FILTER_KEY, PersistentDataType.STRING);
        playNavSound(player);

        if ("all".equals(filterValue)) {
            player.openInventory(new AdminGUI(wm, 0, null, null).getInventory());
        } else if (filterValue.startsWith("dim:")) {
            player.openInventory(new AdminGUI(wm, 0, filterValue.substring(4), null).getInventory());
        } else if ("owner_list".equals(filterValue)) {
            player.openInventory(new AdminOwnerGUI(wm, 0, gui.getAdminPage(), gui.getAdminDimensionFilter(), gui.getAdminOwnerFilter()).getInventory());
        }
    }

    private void handleOwnerGUIClick(Player player, AdminOwnerGUI gui, ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();

        if (pdc.has(WaystoneConstants.ADMIN_FILTER_KEY, PersistentDataType.STRING)) {
            String filterValue = pdc.get(WaystoneConstants.ADMIN_FILTER_KEY, PersistentDataType.STRING);
            playNavSound(player);

            if (filterValue.startsWith("owner:")) {
                UUID ownerUUID = UUID.fromString(filterValue.substring(6));
                player.openInventory(new AdminGUI(wm, 0, null, ownerUUID).getInventory());
            } else if ("back".equals(filterValue)) {
                player.openInventory(new AdminFilterGUI(wm, gui.getAdminPage(), gui.getAdminDimensionFilter(), gui.getAdminOwnerFilter()).getInventory());
            }
        } else if (pdc.has(WaystoneConstants.WAYSTONE_PAGE_KEY, PersistentDataType.INTEGER)) {
            int nextPage = pdc.get(WaystoneConstants.WAYSTONE_PAGE_KEY, PersistentDataType.INTEGER);
            playNavSound(player);
            player.openInventory(new AdminOwnerGUI(wm, nextPage, gui.getAdminPage(), gui.getAdminDimensionFilter(), gui.getAdminOwnerFilter()).getInventory());
        }
    }

    private void handleActionGUIClick(Player player, AdminActionGUI gui, ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();
        if (!pdc.has(WaystoneConstants.ADMIN_ACTION_KEY, PersistentDataType.STRING)) return;
        if (!pdc.has(WaystoneConstants.WAYSTONE_LOCATION_KEY, PersistentDataType.STRING)) return;

        String action = pdc.get(WaystoneConstants.ADMIN_ACTION_KEY, PersistentDataType.STRING);
        String locString = pdc.get(WaystoneConstants.WAYSTONE_LOCATION_KEY, PersistentDataType.STRING);
        Location location = wm.stringToLocation(locString);
        WaystoneYamlDTO ws = wm.getWaystones().get(locString);
        if (ws == null) return;

        switch (action) {
            case "tp" -> {
                player.closeInventory();
                Location tpLoc = location.clone().add(0.5, 1, 0.5);
                player.teleport(tpLoc);
                teleportTask.addBlockedUntilLeave(player.getUniqueId());
                player.sendActionBar(Component.text("✦ Teleported to ").color(LIGHT)
                        .append(Component.text(ws.name()).color(NAME)));
                player.playSound(tpLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.1f);
            }
            case "info" -> {
                player.closeInventory();
                player.openBook(buildInfoBook(ws));
            }
            case "delete" -> {
                wm.unregisterWaystone(location);
                location.getBlock().setType(Material.AIR);
                player.closeInventory();
                player.sendActionBar(Component.text("✦ Deleted ").color(LIGHT)
                        .append(Component.text(ws.name()).color(NAME)));
                player.playSound(player.getLocation(), Sound.BLOCK_LODESTONE_BREAK, 1.0f, 0.8f);
            }
            case "global" -> {
                boolean newGlobal = !ws.global();
                if (newGlobal && !ws.active()) {
                    player.sendActionBar(Component.text("✦ This waystone has no name yet.").color(RED));
                    return;
                }
                wm.setWaystoneGlobal(location, newGlobal);
                playNavSound(player);
                WaystoneYamlDTO updated = wm.getWaystones().get(locString);
                if (updated != null)
                    player.openInventory(new AdminActionGUI(updated, wm, gui.getAdminPage(), gui.getAdminDimensionFilter(), gui.getAdminOwnerFilter()).getInventory());
            }
        }
    }

    private ItemStack buildInfoBook(WaystoneYamlDTO ws) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) book.getItemMeta();
        bookMeta.title(Component.text("Waystone Info"));
        bookMeta.author(Component.text("Waystones"));

        Location loc = wm.stringToLocation(ws.stringLocation());
        String world = AdminGUI.formatWorld(loc.getWorld().getName());

        String ownerName = "None";
        if (ws.owner() != null) {
            String n = Bukkit.getOfflinePlayer(ws.owner()).getName();
            ownerName = n != null ? n : ws.owner().toString();
        }

        Component page = Component.text(ws.name()).color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("- - - - - - - -").color(NamedTextColor.GRAY))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Location").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text(world).color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text(loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()).color(NamedTextColor.GRAY))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Owner").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text(ownerName).color(NamedTextColor.GRAY))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Status").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text(ws.active() ? "Active" : "Inactive").color(ws.active() ? NamedTextColor.YELLOW : NamedTextColor.GRAY))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Global: ").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
                .append(Component.text(ws.global() ? "Yes" : "No").color(ws.global() ? NamedTextColor.YELLOW : NamedTextColor.GRAY));

        bookMeta.pages(page);
        book.setItemMeta(bookMeta);
        return book;
    }
}
