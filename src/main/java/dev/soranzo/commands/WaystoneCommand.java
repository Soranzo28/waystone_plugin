package dev.soranzo.commands;

import dev.soranzo.WaystoneManager;
import dev.soranzo.dto.WaystoneYamlDTO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class WaystoneCommand implements CommandExecutor, TabCompleter {
    private final WaystoneManager wm;

    private static final TextColor ACCENT = TextColor.color(0x818cf8);
    private static final TextColor GOLD   = TextColor.color(0xffd700);
    private static final TextColor RED    = TextColor.color(0xff6b6b);
    private static final TextColor GRAY   = TextColor.color(0xa0aec0);

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
            default       -> { return false; }
        }
        return true;
    }

    private void handleList(CommandSender sender) {
        HashMap<String, WaystoneYamlDTO> waystones = wm.getWaystones();
        if (waystones.isEmpty()) {
            sender.sendMessage(Component.text("✦ No waystones registered.").color(ACCENT));
            return;
        }

        sender.sendMessage(Component.text("✦ Waystones (" + waystones.size() + "):").color(ACCENT).decorate(TextDecoration.BOLD));
        for (WaystoneYamlDTO ws : waystones.values()) {
            Location loc = wm.stringToLocation(ws.stringLocation());
            String coords = loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            String status = ws.active() ? "active" : "inactive";
            TextColor statusColor = ws.active() ? TextColor.color(0x4ade80) : RED;

            String ownerName = "no owner";
            if (ws.owner() != null) {
                String name = Bukkit.getOfflinePlayer(ws.owner()).getName();
                ownerName = name != null ? name : ws.owner().toString();
            }

            sender.sendMessage(
                Component.text("  - ").color(GRAY)
                    .append(Component.text(ws.name()).color(GOLD).decorate(TextDecoration.BOLD))
                    .append(Component.text(" [").color(GRAY))
                    .append(Component.text(status).color(statusColor))
                    .append(Component.text("] ").color(GRAY))
                    .append(Component.text("@ " + coords).color(GRAY))
                    .append(Component.text(" (" + ownerName + ")").color(GRAY))
            );
        }
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
            sender.sendMessage(Component.text("✦ Multiple waystones named \"" + name + "\":").color(ACCENT));
            for (int i = 0; i < matches.size(); i++) {
                Location loc = wm.stringToLocation(matches.get(i).stringLocation());
                String ownerName = "no owner";
                if (matches.get(i).owner() != null) {
                    String n = Bukkit.getOfflinePlayer(matches.get(i).owner()).getName();
                    ownerName = n != null ? n : matches.get(i).owner().toString();
                }
                sender.sendMessage(
                    Component.text("  [" + (i + 1) + "] ").color(GOLD)
                        .append(Component.text(loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()).color(GRAY))
                        .append(Component.text(" (" + ownerName + ")").color(GRAY))
                );
            }
            sender.sendMessage(Component.text("✦ Use /waystone delete " + name + " <index> to specify which one.").color(ACCENT));
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
            Component.text("✦ Deleted waystone \"").color(ACCENT)
                .append(Component.text(name).color(GOLD))
                .append(Component.text("\" at ").color(ACCENT))
                .append(Component.text(loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()).color(GRAY))
                .append(Component.text(".").color(ACCENT))
        );
    }

    private void handleReload(CommandSender sender) {
        wm.reload();
        sender.sendMessage(Component.text("✦ Waystone data reloaded.").color(ACCENT));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("waystones.admin")) return List.of();

        if (args.length == 1) {
            return List.of("list", "delete", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("delete")) {
            return wm.getWaystones().values().stream()
                    .map(WaystoneYamlDTO::name)
                    .distinct()
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
