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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminGUI implements InventoryHolder {

    private static final TextColor BRIGHT = TextColor.color(0xe7dde9);
    private static final TextColor LIGHT  = TextColor.color(0xa889b9);
    private static final TextColor MID    = TextColor.color(0x6e4a75);
    private static final TextColor DIM    = TextColor.color(0x3a2644);
    private static final TextColor NAME   = TextColor.color(0xfce8f3);

    private final Inventory inventory;
    private final int page;
    private final String dimensionFilter;
    private final UUID ownerFilter;

    public AdminGUI(WaystoneManager wm, int page, String dimensionFilter, UUID ownerFilter) {
        this.page = page;
        this.dimensionFilter = dimensionFilter;
        this.ownerFilter = ownerFilter;

        inventory = Bukkit.createInventory(this, 54, buildTitle(dimensionFilter, ownerFilter));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 45; i < 54; i++) inventory.setItem(i, glass);

        inventory.setItem(49, buildFilterItem(dimensionFilter, ownerFilter));

        List<WaystoneYamlDTO> filtered = wm.getWaystones().values().stream()
                .filter(ws -> {
                    if (dimensionFilter != null)
                        return wm.stringToLocation(ws.stringLocation()).getWorld().getName().equals(dimensionFilter);
                    if (ownerFilter != null)
                        return ownerFilter.equals(ws.owner());
                    return true;
                })
                .toList();

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / 45));
        int start = page * 45;
        int end = Math.min(start + 45, filtered.size());

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Component.text("← Previous").color(BRIGHT));
            prevMeta.getPersistentDataContainer().set(WaystoneConstants.WAYSTONE_PAGE_KEY, PersistentDataType.INTEGER, page - 1);
            prev.setItemMeta(prevMeta);
            inventory.setItem(45, prev);
        }

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.displayName(Component.text("Next →").color(BRIGHT));
            nextMeta.getPersistentDataContainer().set(WaystoneConstants.WAYSTONE_PAGE_KEY, PersistentDataType.INTEGER, page + 1);
            next.setItemMeta(nextMeta);
            inventory.setItem(53, next);
        }

        if (filtered.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = empty.getItemMeta();
            emptyMeta.displayName(Component.text("No waystones found.").color(LIGHT));
            empty.setItemMeta(emptyMeta);
            inventory.setItem(22, empty);
            return;
        }

        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, buildWaystoneItem(filtered.get(i), wm));
        }
    }

    private Component buildTitle(String dimensionFilter, UUID ownerFilter) {
        if (dimensionFilter != null)
            return Component.text("Admin ✦ " + formatWorld(dimensionFilter)).color(BRIGHT);
        if (ownerFilter != null) {
            String name = Bukkit.getOfflinePlayer(ownerFilter).getName();
            return Component.text("Admin ✦ " + (name != null ? name : "Unknown")).color(BRIGHT);
        }
        return Component.text("Admin ✦ Waystones").color(BRIGHT);
    }

    private ItemStack buildFilterItem(String dimensionFilter, UUID ownerFilter) {
        ItemStack item;
        String label;

        if (dimensionFilter != null) {
            item = new ItemStack(switch (dimensionFilter) {
                case "world"          -> Material.GRASS_BLOCK;
                case "world_nether"   -> Material.NETHERRACK;
                case "world_the_end"  -> Material.END_STONE;
                default               -> Material.COMPASS;
            });
            label = "Filter: " + formatWorld(dimensionFilter);
        } else if (ownerFilter != null) {
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerFilter));
            item.setItemMeta(skullMeta);
            String name = Bukkit.getOfflinePlayer(ownerFilter).getName();
            label = "Filter: " + (name != null ? name : "Unknown");
        } else {
            item = new ItemStack(Material.COMPASS);
            label = "Filter: All";
        }

        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label).color(LIGHT));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to change filter").color(MID));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(WaystoneConstants.ADMIN_FILTER_KEY, PersistentDataType.STRING, "open");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildWaystoneItem(WaystoneYamlDTO ws, WaystoneManager wm) {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(ws.name()).color(NAME));

        List<Component> lore = new ArrayList<>();
        Location loc = wm.stringToLocation(ws.stringLocation());
        lore.add(Component.text(formatWorld(loc.getWorld().getName()) + "  " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()).color(MID));

        String ownerName = "no owner";
        if (ws.owner() != null) {
            String n = Bukkit.getOfflinePlayer(ws.owner()).getName();
            ownerName = n != null ? n : ws.owner().toString();
        }
        lore.add(Component.text("Owner: " + ownerName).color(MID));
        lore.add(Component.text(ws.active() ? "Active" : "Inactive").color(ws.active() ? LIGHT : DIM));
        if (ws.global()) lore.add(Component.text("✦ Global").color(BRIGHT));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(WaystoneConstants.WAYSTONE_LOCATION_KEY, PersistentDataType.STRING, ws.stringLocation());
        item.setItemMeta(meta);
        return item;
    }

    public static String formatWorld(String worldName) {
        return switch (worldName) {
            case "world"         -> "Overworld";
            case "world_nether"  -> "Nether";
            case "world_the_end" -> "The End";
            default              -> worldName;
        };
    }

    public int getPage() { return page; }
    public String getDimensionFilter() { return dimensionFilter; }
    public UUID getOwnerFilter() { return ownerFilter; }

    @Override
    public Inventory getInventory() { return inventory; }
}
