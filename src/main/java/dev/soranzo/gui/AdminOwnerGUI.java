package dev.soranzo.gui;

import dev.soranzo.WaystoneConstants;
import dev.soranzo.WaystoneManager;
import dev.soranzo.dto.WaystoneYamlDTO;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminOwnerGUI implements InventoryHolder {

    private static final TextColor BRIGHT = TextColor.color(0xe7dde9);
    private static final TextColor LIGHT  = TextColor.color(0xa889b9);
    private static final TextColor MID    = TextColor.color(0x6e4a75);

    private final Inventory inventory;
    private final int page;
    private final int adminPage;
    private final String adminDimensionFilter;
    private final UUID adminOwnerFilter;

    public AdminOwnerGUI(WaystoneManager wm, int page, int adminPage, String adminDimensionFilter, UUID adminOwnerFilter) {
        this.page = page;
        this.adminPage = adminPage;
        this.adminDimensionFilter = adminDimensionFilter;
        this.adminOwnerFilter = adminOwnerFilter;

        inventory = Bukkit.createInventory(this, 54, Component.text("Filter by Owner").color(BRIGHT));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 45; i < 54; i++) inventory.setItem(i, glass);

        List<UUID> owners = wm.getWaystones().values().stream()
                .map(WaystoneYamlDTO::owner)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().toList();

        int totalPages = Math.max(1, (int) Math.ceil((double) owners.size() / 45));
        int start = page * 45;
        int end = Math.min(start + 45, owners.size());

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

        // Back button at center of last row
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("← Back").color(LIGHT));
        backMeta.getPersistentDataContainer().set(WaystoneConstants.ADMIN_FILTER_KEY, PersistentDataType.STRING, "back");
        back.setItemMeta(backMeta);
        inventory.setItem(49, back);

        for (int i = start; i < end; i++) {
            UUID ownerUUID = owners.get(i);
            long count = wm.getWaystones().values().stream()
                    .filter(ws -> ownerUUID.equals(ws.owner())).count();
            String name = Bukkit.getOfflinePlayer(ownerUUID).getName();

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerUUID));
            skullMeta.displayName(Component.text(name != null ? name : ownerUUID.toString()).color(BRIGHT));
            skullMeta.lore(List.of(Component.text(count + " waystone(s)").color(MID)));
            skullMeta.getPersistentDataContainer().set(WaystoneConstants.ADMIN_FILTER_KEY, PersistentDataType.STRING, "owner:" + ownerUUID);
            skull.setItemMeta(skullMeta);
            inventory.setItem(i - start, skull);
        }
    }

    public int getPage() { return page; }
    public int getAdminPage() { return adminPage; }
    public String getAdminDimensionFilter() { return adminDimensionFilter; }
    public UUID getAdminOwnerFilter() { return adminOwnerFilter; }

    @Override
    public Inventory getInventory() { return inventory; }
}
