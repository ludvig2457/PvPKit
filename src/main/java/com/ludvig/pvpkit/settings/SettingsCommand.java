package com.ludvig.pvpkit.settings;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SettingsCommand implements CommandExecutor {

    private final SettingsGui settingsGui;

    public SettingsCommand(PvPKitPlugin plugin) {
        this.settingsGui = plugin.getSettingsGui();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.prefix("&cOnly players can open the settings menu."));
            return true;
        }
        settingsGui.open(player);
        return true;
    }
}
