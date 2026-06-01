package dev.soranzo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class WaystoneRecipe {

    public static ItemStack getWaystoneItem(Waystone pl) {
        ItemStack result = new ItemStack(Material.LODESTONE);
        ItemMeta meta = result.getItemMeta();

        meta.displayName(Component.text("Waystone").color(NamedTextColor.AQUA));

        meta.getPersistentDataContainer().set(
                WaystoneConstants.IS_WAYSTONE_KEY,
                PersistentDataType.BOOLEAN,
                true
        );

        meta.setEnchantmentGlintOverride(true);
        result.setItemMeta(meta);

        return result;
    }

    public static void register(Waystone pl) {

        NamespacedKey key = WaystoneConstants.IS_WAYSTONE_KEY;
        ItemStack result = getWaystoneItem(pl);
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        recipe.shape(
                " E ",
                " L ",
                " A "
        );

        recipe.setIngredient('E', Material.ENDER_PEARL);
        recipe.setIngredient('L', Material.LODESTONE);
        recipe.setIngredient('A', Material.AMETHYST_SHARD);

        Bukkit.addRecipe(recipe);
    }

}
