package dev.soranzo;

import org.bukkit.NamespacedKey;

public class WaystoneConstants {
    private static final Waystone instance = Waystone.getThisPlugin();
    public static final NamespacedKey WAYSTONE_LOCATION_KEY = new NamespacedKey(instance, "waystone_location");
    public static final NamespacedKey IS_WAYSTONE_KEY = new NamespacedKey(instance, "is_waystone");
    public static final NamespacedKey WAYSTONE_PAGE_KEY = new NamespacedKey(instance, "waystone_page");
    public static final NamespacedKey WAYSTONE_AUTHOR_KEY = new NamespacedKey(instance, "waystone_author");
}
