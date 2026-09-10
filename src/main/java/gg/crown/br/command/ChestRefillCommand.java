package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ChestRefillCommand implements CommandExecutor {

    private final NightRoyalePlugin plugin;

    public ChestRefillCommand(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("nightroyale.admin")) {
            sender.sendMessage(Component.text("You do not have permission to refill chests.", NamedTextColor.RED));
            return true;
        }

        plugin.getChestManager().refillAllChests(
                plugin.getMatchManager().getCurrentMode(),
                plugin.getScenarioManager().getActiveScenarios()
        );
        sender.sendMessage(Component.text("✔ All chests refilled successfully.", NamedTextColor.GREEN));
        return true;
    }
}
