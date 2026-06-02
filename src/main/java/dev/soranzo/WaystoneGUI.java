package dev.soranzo;

import dev.soranzo.dto.PlayerYamlDTO;
import dev.soranzo.dto.WaystoneYamlDTO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WaystoneGUI implements InventoryHolder {
    private final Inventory inventory;
    private final String originLocation;

    public WaystoneGUI(Player player, WaystoneManager wm, String originLocation, int page) {
        inventory = Bukkit.createInventory(this, 54, Component.text("Waystones of " + player.getName()));

        this.originLocation = originLocation;

        //Static itens for decoration
        ItemStack glassPane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta gMeta = glassPane.getItemMeta();
        gMeta.displayName(Component.text("-").color(NamedTextColor.DARK_AQUA));
        glassPane.setItemMeta(gMeta);

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, glassPane);
            inventory.setItem(53-i, glassPane);
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer("patolheiro"));
        skullMeta.displayName(Component.text("About").color(NamedTextColor.LIGHT_PURPLE));
        skullMeta.lore(List.of(
                Component.text("Made by -> Soranzo").color(NamedTextColor.GRAY),
                Component.text("Github  -> @Soranzo28").color(NamedTextColor.GRAY),
                Component.text("Discord -> soranzo28").color(NamedTextColor.GRAY)
        ));
        skullMeta.getPersistentDataContainer().set(WaystoneConstants.WAYSTONE_AUTHOR_KEY, PersistentDataType.BOOLEAN, true);
        skull.setItemMeta(skullMeta);
        inventory.setItem(49, skull);


        //Start slot to put waystones
        int slot = 9;

        //Player security, if for some reason does not exist, we create a empty one
        PlayerYamlDTO playerData = wm.getDiscoveries().getOrDefault(
                player.getUniqueId(),
                new PlayerYamlDTO(player.getUniqueId(), new HashMap<>(), new ArrayList<>())
        );

        //Calculates how many pages will be needed to show every waystone
        List<String> pDiscoveries = playerData.discoveries();
        int totalPages = (int) Math.ceil((double)pDiscoveries.size() / 36);
        int start = page * 36;
        int end = Math.min(start + 36, pDiscoveries.size());


        //Calculates the next and previous arrows
        if (page > 0) {
            //Next
            ItemStack prev = new ItemStack(Material.COMPASS);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Component.text("← Previous").color(NamedTextColor.YELLOW));
            prevMeta.getPersistentDataContainer().set(WaystoneConstants.WAYSTONE_PAGE_KEY, PersistentDataType.INTEGER, page - 1);
            prev.setItemMeta(prevMeta);
            inventory.setItem(45, prev);
        }

        if (page < totalPages - 1) {
            //Previous
            ItemStack next = new ItemStack(Material.COMPASS);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.displayName(Component.text("Next →").color(NamedTextColor.YELLOW));
            nextMeta.getPersistentDataContainer().set(WaystoneConstants.WAYSTONE_PAGE_KEY, PersistentDataType.INTEGER, page + 1);
            next.setItemMeta(nextMeta);
            inventory.setItem(53, next);
        }

        List<String> pageDiscoveries = pDiscoveries.subList(start, end);

        //Always renders the origin waystone at slot 4, regardless of page
        WaystoneYamlDTO originData = wm.getWaystones().get(originLocation);
        if (originData != null) {
            ItemStack originItem = new ItemStack(Material.LODESTONE);
            ItemMeta originMeta = originItem.getItemMeta();
            Location originLoc = wm.stringToLocation(originLocation);
            List<Component> originLore = new ArrayList<>();
            originLore.add(Component.text(originLoc.getBlockX() + "," + originLoc.getBlockY() + "," + originLoc.getBlockZ()).color(NamedTextColor.GRAY));
            originLore.add(Component.text("You are here!").color(NamedTextColor.YELLOW));
            originMeta.lore(originLore);
            originMeta.displayName(Component.text(originData.name()).color(NamedTextColor.BLUE));
            originItem.setItemMeta(originMeta);
            inventory.setItem(4, originItem);
        }

        //Renders one page at time
        for (String discover : pageDiscoveries) {
            if (originLocation.equals(discover)) continue;

            //Inicialize item and metadata
            ItemStack waystoneItem = new ItemStack(Material.LODESTONE);
            ItemMeta wMeta = waystoneItem.getItemMeta();

            //Gets waystone data
            WaystoneYamlDTO waystoneData = wm.getWaystones().get(discover);
            if (waystoneData == null) continue;
            if (!waystoneData.active()) continue;

            //Get waystone location
            Location waystoneLocation = wm.stringToLocation(waystoneData.stringLocation());

            //Add lore to item
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(
                      waystoneLocation.getBlockX() +
                            "," + waystoneLocation.getBlockY() +
                            ","  + waystoneLocation.getBlockZ())
                    .color(NamedTextColor.GRAY));

            String world = switch (waystoneLocation.getWorld().getName()) {
                case "world" -> "Overworld";
                case "world_nether" -> "Nether";
                case "world_the_end" -> "The End";
                default -> "Unknown";
            };

            lore.add(Component.text(world).color(NamedTextColor.GRAY));
            wMeta.lore(lore);

            //Glows connected waystones
            String currentConnection = playerData.connections().get(originLocation);
            if (discover.equals(currentConnection)) {
                wMeta.setEnchantmentGlintOverride(true);
            }

            //Sets waystone info on PDC
            wMeta.getPersistentDataContainer().set(WaystoneConstants.WAYSTONE_LOCATION_KEY, PersistentDataType.STRING, discover);

            //Sets the display name
            wMeta.displayName(Component.text(waystoneData.name()).color(NamedTextColor.BLUE));

            //Apply changes
            waystoneItem.setItemMeta(wMeta);
            inventory.setItem(slot, waystoneItem);
            slot++;
        }
    }

    public String getOriginLocation(){
        return originLocation;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
