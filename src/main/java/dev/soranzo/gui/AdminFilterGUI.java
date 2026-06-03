package dev.soranzo.gui;

import dev.soranzo.WaystoneConstants;
import dev.soranzo.WaystoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class AdminFilterGUI implements InventoryHolder {

    private static final TextColor BRIGHT = TextColor.color(0xe7dde9);
    private static final TextColor LIGHT  = TextColor.color(0xa889b9);
    private static final TextColor MID    = TextColor.color(0x6e4a75);

    private final Inventory inventory;
    private final int adminPage;
    private final String adminDimensionFilter;
    private final UUID adminOwnerFilter;

    public AdminFilterGUI(WaystoneManager wm, int adminPage, String adminDimensionFilter, UUID adminOwnerFilter) {
        this.adminPage = adminPage;
        this.adminDimensionFilter = adminDimensionFilter;
        this.adminOwnerFilter = adminOwnerFilter;

        inventory = Bukkit.createInventory(this, 27, Component.text("Filter Waystones").color(BRIGHT));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);

        // Row 1 — dimension filters alternating with glass
        // slots: 0=glass 1=All 2=glass 3=Overworld 4=glass 5=Nether 6=glass 7=End 8=glass
        inventory.setItem(1, buildDimItem(Material.COMPASS,    "All",      "all"));
        inventory.setItem(3, buildDimItem(Material.GRASS_BLOCK,"Overworld","dim:world"));
        inventory.setItem(5, buildDimItem(Material.NETHERRACK, "Nether",   "dim:world_nether"));
        inventory.setItem(7, buildDimItem(Material.END_STONE,  "The End",  "dim:world_the_end"));

        // Row 2 center (slot 13) — owner list button
        String ownerBtnName = wm.getWaystones().values().stream().anyMatch(ws -> ws.owner() != null)
                ? "Filter by Owner" : "No owners registered";
        ItemStack ownerBtn = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta ownerMeta = (SkullMeta) ownerBtn.getItemMeta();
        ownerMeta.displayName(Component.text(ownerBtnName).color(BRIGHT));
        ownerMeta.lore(List.of(Component.text("Click to browse by owner").color(MID)));
        ownerMeta.getPersistentDataContainer().set(WaystoneConstants.ADMIN_FILTER_KEY, PersistentDataType.STRING, "owner_list");
        ownerBtn.setItemMeta(ownerMeta);
        inventory.setItem(13, ownerBtn);
    }

    private ItemStack buildDimItem(Material material, String label, String filterValue) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label).color(BRIGHT));
        meta.getPersistentDataContainer().set(WaystoneConstants.ADMIN_FILTER_KEY, PersistentDataType.STRING, filterValue);
        item.setItemMeta(meta);
        return item;
    }

    public int getAdminPage() { return adminPage; }
    public String getAdminDimensionFilter() { return adminDimensionFilter; }
    public UUID getAdminOwnerFilter() { return adminOwnerFilter; }

    @Override
    public Inventory getInventory() { return inventory; }
}
