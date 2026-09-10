package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.team.Squad;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final NightRoyalePlugin plugin;

    public TeamCommand(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can manage squads.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("--- Squad Commands ---", NamedTextColor.AQUA));
            player.sendMessage(Component.text("/team invite <player> — Invite player to your squad", NamedTextColor.GRAY));
            player.sendMessage(Component.text("/team accept <player> — Accept pending squad invite", NamedTextColor.GRAY));
            player.sendMessage(Component.text("/team leave — Leave current squad", NamedTextColor.GRAY));
            player.sendMessage(Component.text("/team list — View squad members", NamedTextColor.GRAY));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /team invite <player>", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                plugin.getTeamManager().invite(player, target);
                return true;
            }
            case "accept" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /team accept <player>", NamedTextColor.RED));
                    return true;
                }
                Player leader = Bukkit.getPlayer(args[1]);
                if (leader == null || !leader.isOnline()) {
                    player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                plugin.getTeamManager().accept(player, leader);
                return true;
            }
            case "leave" -> {
                plugin.getTeamManager().leave(player);
                return true;
            }
            case "list" -> {
                Squad squad = plugin.getTeamManager().getSquad(player);
                if (squad == null) {
                    player.sendMessage(Component.text("You are not currently in a squad.", NamedTextColor.YELLOW));
                    return true;
                }
                player.sendMessage(Component.text("--- Your Squad Members (" + squad.size() + "/" + plugin.getTeamManager().getMaxTeamSize() + ") ---", NamedTextColor.AQUA));
                for (UUID member : squad.getMembers()) {
                    Player p = Bukkit.getPlayer(member);
                    String name = p != null ? p.getName() : "Offline";
                    player.sendMessage(Component.text("• " + name, member.equals(squad.getLeader()) ? NamedTextColor.GOLD : NamedTextColor.WHITE));
                }
                return true;
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("invite", "accept", "leave", "list");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("accept"))) {
            return null; // Player names
        }
        return Collections.emptyList();
    }
}
