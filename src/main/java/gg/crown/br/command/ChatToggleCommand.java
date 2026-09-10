package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ChatToggleCommand implements CommandExecutor, TabCompleter {

    private final NightRoyalePlugin plugin;

    public ChatToggleCommand(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("nightroyale.staff")) {
            sender.sendMessage(Component.text("You do not have permission to toggle chat.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /chat <enable|disable>", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on")) {
            plugin.getModerationManager().setChatEnabled(true);
            return true;
        } else if (args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off")) {
            plugin.getModerationManager().setChatEnabled(false);
            return true;
        }

        sender.sendMessage(Component.text("Usage: /chat <enable|disable>", NamedTextColor.RED));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("enable", "disable");
        }
        return Collections.emptyList();
    }
}
