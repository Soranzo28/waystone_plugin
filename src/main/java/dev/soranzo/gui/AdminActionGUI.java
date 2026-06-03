package dev.soranzo.gui;

import dev.soranzo.WaystoneConstants;
import dev.soranzo.WaystoneManager;
import dev.soranzo.dto.WaystoneYamlDTO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminActionGUI implements InventoryHolder {

    private static final TextColor BRIGHT = TextColor.color(0xe7dde9);
    private static final TextColor LIGHT  = TextColor.color(0xa889b9);
    private static final TextColor MID    = TextColor.color(0x6e4a75);
    private static final TextColor DIM    = TextColor.color(0x3a2644);
    private static final TextColor NAME   = TextColor.color(0xfce8f3);
    private static final TextColor RED    = TextColor.color(0xff6b6b);

    private final Inventory inventory;
    private final String waystoneLocation;
    private final int adminPage;
    private final String adminDimensionFilter;
    private final UUID adminOwnerFilter;

    public AdminActionGUI(WaystoneYamlDTO ws, WaystoneManager wm, int adminPage, String adminDimensionFilter, UUID adminOwnerFilter) {
        this.waystoneLocation = ws.stringLocation();
        this.adminPage = adminPage;
        this.adminDimensionFilter = adminDimensionFilter;
        this.adminOwnerFilter = adminOwnerFilter;

        inventory = Bukkit.createInventory(this, 27, Component.text(ws.name()).color(NAME));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);

        // Waystone display at top center
        inventory.setItem(4, buildInfoItem(ws, wm));

        // Action buttons — row 2
        inventory.setItem(10, buildActionItem(Material.ENDER_PEARL, "Teleport", "tp",     ws.stringLocation(), BRIGHT));
        inventory.setItem(12, buildActionItem(Material.BOOK,        "Info",     "info",   ws.stringLocation(), BRIGHT));
        inventory.setItem(14, buildActionItem(Material.BARRIER,     "Delete",   "delete", ws.stringLocation(), RED));
        inventory.setItem(16, buildGlobalItem(ws));
    }

    private ItemStack buildInfoItem(WaystoneYamlDTO ws, WaystoneManager wm) {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(ws.name()).color(NAME));

        List<Component> lore = new ArrayList<>();
        Location loc = wm.stringToLocation(ws.stringLocation());
        String world = AdminGUI.formatWorld(loc.getWorld().getName());
        lore.add(Component.text(world + "  " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()).color(MID));

        String ownerName = "no owner";
        if (ws.owner() != null) {
            String n = Bukkit.getOfflinePlayer(ws.owner()).getName();
            ownerName = n != null ? n : ws.owner().toString();
        }
        lore.add(Component.text("Owner: " + ownerName).color(MID));
        lore.add(Component.text(ws.active() ? "Active" : "Inactive").color(ws.active() ? LIGHT : DIM));
        if (ws.global()) lore.add(Component.text("✦ Global").color(BRIGHT));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildActionItem(Material material, String label, String action, String locString, TextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label).color(color));
        meta.getPersistentDataContainer().set(WaystoneConstants.ADMIN_ACTION_KEY, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(WaystoneConstants.WAYSTONE_LOCATION_KEY, PersistentDataType.STRING, locString);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildGlobalItem(WaystoneYamlDTO ws) {
        boolean isGlobal = ws.global();
        ItemStack item = new ItemStack(isGlobal ? Material.NETHER_STAR : Material.GUNPOWDER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(isGlobal ? "✦ Remove Global" : "Set as Global").color(isGlobal ? LIGHT : MID));
        meta.getPersistentDataContainer().set(WaystoneConstants.ADMIN_ACTION_KEY, PersistentDataType.STRING, "global");
        meta.getPersistentDataContainer().set(WaystoneConstants.WAYSTONE_LOCATION_KEY, PersistentDataType.STRING, ws.stringLocation());
        item.setItemMeta(meta);
        return item;
    }

    public String getWaystoneLocation() { return waystoneLocation; }
    public int getAdminPage() { return adminPage; }
    public String getAdminDimensionFilter() { return adminDimensionFilter; }
    public UUID getAdminOwnerFilter() { return adminOwnerFilter; }

    @Override
    public Inventory getInventory() { return inventory; }
}
