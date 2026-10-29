package com.ludvig.pvpkit.kit;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KitCommand implements CommandExecutor, TabCompleter {

    private final PvPKitPlugin plugin;
    private final KitManager kitManager;

    public KitCommand(PvPKitPlugin plugin) {
        this.plugin = plugin;
        this.kitManager = plugin.getKitManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getKitGui().open(player);
            } else {
                sendKitList(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "list" -> {
                sendKitList(sender);
                return true;
            }
            case "reload" -> {
                if (!requireAdmin(sender)) {
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage(Text.prefix("&aKits and configuration reloaded."));
                return true;
            }
            case "reset" -> {
                handleReset(sender, args);
                return true;
            }
            case "give" -> {
                handleGive(sender, args);
                return true;
            }
            case "preview" -> {
                handlePreview(sender, args);
                return true;
            }
            default -> {
                giveKit(sender, args[0]);
                return true;
            }
        }
    }

    private void giveKit(CommandSender sender, String kitId) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.prefix("&cOnly players can claim kits."));
            return;
        }

        Kit kit = kitManager.getKit(kitId);
        if (kit == null) {
            sender.sendMessage(Text.prefix("&cUnknown kit &f" + kitId + "&c. Use &f/kit list&c."));
            return;
        }

        long remaining = kitManager.getRemainingMillis(player, kit);
        if (remaining > 0L && !player.hasPermission("pvpplugin.kit.bypass")) {
            sender.sendMessage(Text.prefix("&cKit &f" + kit.id() + " &cis on cooldown for &f"
                    + Text.duration(remaining) + "&c."));
            return;
        }

        if (plugin.getConfig().getBoolean("settings.kits.block-in-combat", true)
                && plugin.getCombatManager().isTagged(player)
                && !player.hasPermission("pvpplugin.kit.bypass")) {
            sender.sendMessage(Text.prefix("&cYou cannot claim a kit while in combat."));
            return;
        }

        kitManager.apply(player, kit);
        sender.sendMessage(Text.prefix("&aKit ").append(kit.displayName())
                .append(Text.color(" &aclaimed. &7Cooldown: &f"
                        + Text.duration(kit.cooldownSeconds() * 1000L))));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Text.prefix("&7Usage: &f/kit give <player> <kit>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Text.prefix("&cPlayer &f" + args[1] + " &cis not online."));
            return;
        }

        Kit kit = kitManager.getKit(args[2]);
        if (kit == null) {
            sender.sendMessage(Text.prefix("&cUnknown kit &f" + args[2] + "&c."));
            return;
        }

        kitManager.apply(target, kit);
        kitManager.resetCooldown(target.getUniqueId(), kit.id());
        sender.sendMessage(Text.prefix("&aGave kit ").append(kit.displayName())
                .append(Text.color(" &ato &f" + target.getName() + "&a.")));
        target.sendMessage(Text.prefix("&aYou received kit ").append(kit.displayName())
                .append(Text.color("&a.")));
    }

    private void handlePreview(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.prefix("&cOnly players can preview kits."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Text.prefix("&7Usage: &f/kit preview <kit>"));
            return;
        }

        Kit kit = kitManager.getKit(args[1]);
        if (kit == null) {
            sender.sendMessage(Text.prefix("&cUnknown kit &f" + args[1] + "&c."));
            return;
        }
        plugin.getKitGui().openPreview(player, kit);
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Text.prefix("&7Usage: &f/kit reset <player> [kit]"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Text.prefix("&cPlayer &f" + args[1] + " &cis not online."));
            return;
        }

        if (args.length >= 3) {
            Kit kit = kitManager.getKit(args[2]);
            if (kit == null) {
                sender.sendMessage(Text.prefix("&cUnknown kit &f" + args[2] + "&c."));
                return;
            }
            kitManager.resetCooldown(target.getUniqueId(), kit.id());
            sender.sendMessage(Text.prefix("&aReset cooldown of &f" + kit.id() + " &afor &f" + target.getName() + "&a."));
            return;
        }

        kitManager.resetAllCooldowns(target.getUniqueId());
        sender.sendMessage(Text.prefix("&aReset all kit cooldowns for &f" + target.getName() + "&a."));
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("pvpplugin.kit.admin")) {
            sender.sendMessage(Text.prefix("&cYou don't have permission to do that."));
            return false;
        }
        return true;
    }

    private void sendKitList(CommandSender sender) {
        if (kitManager.getKits().isEmpty()) {
            sender.sendMessage(Text.prefix("&cNo kits are configured."));
            return;
        }

        sender.sendMessage(Text.color("&8&m--------&r &b&lKits &8&m--------"));
        Player player = sender instanceof Player ? (Player) sender : null;

        for (Kit kit : kitManager.getKits()) {
            long remaining = player == null ? 0L : kitManager.getRemainingMillis(player, kit);
            String status = remaining > 0L ? "&c" + Text.duration(remaining) : "&aReady";

            Component line = Text.color("&8» &r").append(kit.displayName())
                    .append(Text.color(" &8(&7" + kit.id() + "&8) &8- " + status + " &8[&bUse&8]"))
                    .clickEvent(ClickEvent.runCommand("/kit " + kit.id()))
                    .hoverEvent(HoverEvent.showText(Text.color("&7Click to claim &f" + kit.id())));

            sender.sendMessage(line);
        }
        sender.sendMessage(Text.color("&8&m--------------------------"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("list");
            suggestions.add("preview");
            if (sender.hasPermission("pvpplugin.kit.admin")) {
                suggestions.add("reload");
                suggestions.add("reset");
                suggestions.add("give");
            }
            suggestions.addAll(kitManager.getKits().stream().map(Kit::id).toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (subCommand.equals("preview")) {
                suggestions.addAll(kitManager.getKits().stream().map(Kit::id).toList());
            } else if (subCommand.equals("reset") || subCommand.equals("give")) {
                suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (subCommand.equals("reset") || subCommand.equals("give")) {
                suggestions.addAll(kitManager.getKits().stream().map(Kit::id).toList());
            }
        }

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .toList();
    }
}
