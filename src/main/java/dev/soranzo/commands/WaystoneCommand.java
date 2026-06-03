package dev.soranzo.commands;

import dev.soranzo.*;
import dev.soranzo.dto.WaystoneYamlDTO;
import dev.soranzo.gui.AdminGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class WaystoneCommand implements CommandExecutor, TabCompleter {
    private final WaystoneManager wm;

    private static final TextColor BRIGHT = TextColor.color(0xe7dde9); // headers, confirmations
    private static final TextColor LIGHT  = TextColor.color(0xa889b9); // labels, hints
    private static final TextColor MID    = TextColor.color(0x6e4a75); // secondary info
    private static final TextColor DIM    = TextColor.color(0x553366); // inactive, dimmed
    private static final TextColor NAME   = TextColor.color(0x6e4a75); // waystone names
    private static final TextColor RED    = TextColor.color(0xff6b6b); // errors

    public WaystoneCommand(WaystoneManager wm) {
        this.wm = wm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("waystones.admin")) {
            sender.sendMessage(Component.text("✦ You don't have permission to use this command.").color(RED));
            return true;
        }

        if (args.length == 0) return false;

        switch (args[0].toLowerCase()) {
            case "list"   -> handleList(sender);
            case "delete" -> handleDelete(sender, args);
            case "reload" -> handleReload(sender);
            case "global" -> handleGlobal(sender, args);
            case "admin"  -> handleAdmin(sender);
            case "give"   -> handleGive(sender, args);
            default       -> { return false; }
        }
        return true;
    }

    private void handleList(CommandSender sender) {
        HashMap<String, WaystoneYamlDTO> waystones = wm.getWaystones();
        if (waystones.isEmpty()) {
            sender.sendMessage(Component.text("✦ No waystones registered.").color(LIGHT));
            return;
        }

        if (!(sender instanceof Player player)) {
            // Console fallback
            sender.sendMessage(Component.text("✦ Waystones (" + waystones.size() + "):").color(BRIGHT).decorate(TextDecoration.BOLD));
            for (WaystoneYamlDTO ws : waystones.values()) {
                Location loc = wm.stringToLocation(ws.stringLocation());
                String ownerName = "no owner";
                if (ws.owner() != null) {
                    String name = Bukkit.getOfflinePlayer(ws.owner()).getName();
                    ownerName = name != null ? name : ws.owner().toString();
                }
                sender.sendMessage(Component.text("  - ").color(BRIGHT)
                        .append(Component.text(ws.name()).color(NAME).decorate(TextDecoration.BOLD))
                        .append(Component.text(" [" + (ws.active() ? "active" : "inactive") + "] ").color(ws.active() ? LIGHT : DIM))
                        .append(Component.text("@ " + AdminGUI.formatWorld(loc.getWorld().getName()) + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()).color(BRIGHT))
                        .append(Component.text(" (" + ownerName + ")").color(BRIGHT)));
            }
            return;
        }

        player.openBook(buildListBook(waystones));
    }

    private ItemStack buildListBook(HashMap<String, WaystoneYamlDTO> waystones) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) book.getItemMeta();
        bookMeta.title(Component.text("Waystones"));
        bookMeta.author(Component.text("Waystones"));

        List<Component> pages = new ArrayList<>();
        List<WaystoneYamlDTO> list = new ArrayList<>(waystones.values());

        int perPage = 3;
        for (int i = 0; i < list.size(); i += perPage) {
            Component page = Component.empty();
            for (int j = i; j < Math.min(i + perPage, list.size()); j++) {
                WaystoneYamlDTO ws = list.get(j);
                Location loc = wm.stringToLocation(ws.stringLocation());
                String world = AdminGUI.formatWorld(loc.getWorld().getName());
                String ownerName = "None";
                if (ws.owner() != null) {
                    String n = Bukkit.getOfflinePlayer(ws.owner()).getName();
                    ownerName = n != null ? n : ws.owner().toString();
                }
                page = page
                        .append(Component.text(ws.name()).color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
                        .append(Component.newline())
                        .append(Component.text(world + "  " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()).color(NamedTextColor.GRAY))
                        .append(Component.newline())
                        .append(Component.text("Owner: ").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
                        .append(Component.text(ownerName).color(NamedTextColor.GRAY))
                        .append(Component.newline())
                        .append(Component.text(ws.active() ? "Active" : "Inactive").color(ws.active() ? NamedTextColor.YELLOW : NamedTextColor.GRAY))
                        .append(ws.global() ? Component.text("  Global").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD) : Component.empty())
                        .append(Component.newline())
                        .append(Component.text("- - - - - - - -").color(NamedTextColor.GRAY))
                        .append(Component.newline());
            }
            pages.add(page);
        }

        bookMeta.pages(pages);
        book.setItemMeta(bookMeta);
        return book;
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("✦ Usage: /waystone delete <name> [index]").color(RED));
            return;
        }

        // Last arg is index if it's a number, otherwise the whole thing is the name
        Integer index = null;
        String name;
        String lastArg = args[args.length - 1];
        if (args.length > 2 && lastArg.matches("\\d+")) {
            index = Integer.parseInt(lastArg) - 1;
            name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length - 1));
        } else {
            name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        }

        List<WaystoneYamlDTO> matches = wm.getWaystonesByName(name);

        if (matches.isEmpty()) {
            sender.sendMessage(Component.text("✦ No waystone found with name \"" + name + "\".").color(RED));
            return;
        }

        if (matches.size() > 1 && index == null) {
            sender.sendMessage(Component.text("✦ Multiple waystones named \"" + name + "\":").color(LIGHT));
            for (int i = 0; i < matches.size(); i++) {
                Location loc = wm.stringToLocation(matches.get(i).stringLocation());
                String ownerName = "no owner";
                if (matches.get(i).owner() != null) {
                    String n = Bukkit.getOfflinePlayer(matches.get(i).owner()).getName();
                    ownerName = n != null ? n : matches.get(i).owner().toString();
                }
                sender.sendMessage(
                    Component.text("  [" + (i + 1) + "] ").color(LIGHT)
                        .append(Component.text(loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()).color(BRIGHT))
                        .append(Component.text(" (" + ownerName + ")").color(BRIGHT))
                );
            }
            sender.sendMessage(Component.text("✦ Use /waystone delete " + name + " <index> to specify which one.").color(LIGHT));
            return;
        }

        if (index != null && (index < 0 || index >= matches.size())) {
            sender.sendMessage(Component.text("✦ Invalid index. Use a number between 1 and " + matches.size() + ".").color(RED));
            return;
        }

        WaystoneYamlDTO target = index != null ? matches.get(index) : matches.get(0);
        Location loc = wm.stringToLocation(target.stringLocation());
        wm.unregisterWaystone(loc);
        loc.getBlock().setType(Material.AIR);

        sender.sendMessage(
            Component.text("✦ Deleted waystone \"").color(BRIGHT)
                .append(Component.text(name).color(NAME))
                .append(Component.text("\" at ").color(BRIGHT))
                .append(Component.text(loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()).color(BRIGHT))
                .append(Component.text(".").color(BRIGHT))
        );
    }

    private void handleGlobal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("✦ This command requires a player.").color(RED));
            return;
        }
        if (args.length < 2 || (!args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("remove"))) {
            sender.sendMessage(Component.text("✦ Usage: /waystone global <add|remove>").color(RED));
            return;
        }

        Block target = player.getTargetBlockExact(10);
        if (target == null || target.getType() != Material.LODESTONE) {
            player.sendMessage(Component.text("✦ Look at a waystone block (max 10 blocks).").color(RED));
            return;
        }
        if (!wm.isThisBlockWaystone(target.getLocation())) {
            player.sendMessage(Component.text("✦ That block is not a registered waystone.").color(RED));
            return;
        }

        boolean adding = args[1].equalsIgnoreCase("add");

        if (adding) {
            WaystoneYamlDTO ws = wm.getWaystones().get(wm.locationToString(target.getLocation()));
            if (!ws.active()) {
                player.sendMessage(Component.text("✦ This waystone has no name yet — place a sign to name it first.").color(RED));
                return;
            }
        }

        wm.setWaystoneGlobal(target.getLocation(), adding);

        String wsName = wm.getWaystones().get(wm.locationToString(target.getLocation())).name();
        if (adding) {
            sender.sendMessage(Component.text("✦ ").color(BRIGHT)
                .append(Component.text(wsName).color(NAME))
                .append(Component.text(" is now a global waystone.").color(BRIGHT)));
        } else {
            sender.sendMessage(Component.text("✦ ").color(BRIGHT)
                .append(Component.text(wsName).color(NAME))
                .append(Component.text(" is no longer global.").color(BRIGHT)));
        }
    }

    private void handleAdmin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("✦ This command requires a player.").color(RED));
            return;
        }
        player.openInventory(new AdminGUI(wm, 0, null, null).getInventory());
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("✦ Usage: /waystone give <player>").color(RED));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("✦ Player \"" + args[1] + "\" not found.").color(RED));
            return;
        }
        target.getInventory().addItem(WaystoneRecipe.getWaystoneItem(Waystone.getThisPlugin()));
        sender.sendMessage(Component.text("✦ Gave a waystone to ").color(BRIGHT)
                .append(Component.text(target.getName()).color(NAME).decorate(TextDecoration.BOLD))
                .append(Component.text(".").color(BRIGHT)));
        target.sendActionBar(Component.text("✦ You received a waystone.").color(TextColor.color(0xa889b9)));
    }

    private void handleReload(CommandSender sender) {
        wm.reload();
        sender.sendMessage(Component.text("✦ Waystone data reloaded.").color(BRIGHT));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("waystones.admin")) return List.of();

        if (args.length == 1) {
            return List.of("list", "delete", "reload", "global", "admin", "give").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("global")) {
            return List.of("add", "remove").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("delete")) {
            return wm.getWaystones().values().stream()
                    .map(WaystoneYamlDTO::name)
                    .distinct()
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
