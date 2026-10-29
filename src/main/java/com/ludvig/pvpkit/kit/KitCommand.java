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
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sendKitList(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("pvpplugin.admin")) {
                sender.sendMessage(Text.prefix("&cYou don't have permission to do that."));
                return true;
            }
            plugin.reloadAll();
            sender.sendMessage(Text.prefix("&aKits and configuration reloaded."));
            return true;
        }

        if (subCommand.equals("reset")) {
            handleReset(sender, args);
            return true;
        }

        giveKit(sender, args[0]);
        return true;
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

        kitManager.apply(player, kit);
        sender.sendMessage(Text.prefix("&aKit ").append(kit.displayName())
                .append(Text.color(" &aclaimed. &7Cooldown: &f" + Text.duration(kit.cooldownSeconds() * 1000L))));
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pvpplugin.kit.admin")) {
            sender.sendMessage(Text.prefix("&cYou don't have permission to do that."));
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
            suggestions.add("reload");
            if (sender.hasPermission("pvpplugin.kit.admin")) {
                suggestions.add("reset");
            }
            suggestions.addAll(kitManager.getKits().stream().map(Kit::id).toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("reset")) {
            suggestions.addAll(kitManager.getKits().stream().map(Kit::id).toList());
        }

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .toList();
    }
}
