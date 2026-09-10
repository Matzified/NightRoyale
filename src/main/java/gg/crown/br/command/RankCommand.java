package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RankCommand implements CommandExecutor, TabCompleter {

    private final NightRoyalePlugin plugin;

    public RankCommand(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("nightroyale.admin")) {
            sender.sendMessage(Component.text("You do not have permission to modify ranks.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /rank <player> <rank>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        Rank rank = Rank.fromString(args[1]);
        plugin.getRankManager().setRank(target.getUniqueId(), rank);

        sender.sendMessage(Component.text("✔ Set " + target.getName() + "'s rank to " + rank.getDisplayName(), NamedTextColor.GREEN));
        target.sendMessage(Component.text("Your rank has been updated to ", NamedTextColor.YELLOW).append(Component.text(rank.getDisplayName(), rank.getColor())));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return null; // Player names
        if (args.length == 2) {
            return Arrays.stream(Rank.values()).map(Rank::name).toList();
        }
        return Collections.emptyList();
    }
}
