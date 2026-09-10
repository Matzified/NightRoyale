package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.gui.StatsGui;
import gg.crown.br.rank.Rank;
import gg.crown.br.stats.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatsCommand implements CommandExecutor, TabCompleter {

    private final NightRoyalePlugin plugin;

    public StatsCommand(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        OfflinePlayer target;

        if (args.length == 0) {
            if (sender instanceof Player player) {
                target = player;
            } else {
                sender.sendMessage(Component.text("Console usage: /stats <player_name>", NamedTextColor.RED));
                return true;
            }
        } else {
            String name = args[0];
            Player online = Bukkit.getPlayerExact(name);
            if (online != null) {
                target = online;
            } else {
                OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
                target = (cached != null) ? cached : Bukkit.getOfflinePlayer(name);
            }
        }

        String targetName = target.getName() != null ? target.getName() : (args.length > 0 ? args[0] : "Player");
        PlayerStats stats = plugin.getStatsManager().getStats(target.getUniqueId());
        Rank rank = plugin.getRankManager().getRank(target.getUniqueId());

        double winRate = stats.getGames() > 0 ? ((double) stats.getWins() / stats.getGames()) * 100.0 : 0.0;
        int losses = Math.max(0, stats.getGames() - stats.getWins());
        double kd = losses > 0 ? (double) stats.getKills() / losses : stats.getKills();

        Component card = MiniMessage.miniMessage().deserialize(
                "\n" +
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                "       <gradient:#A855F7:#E085FF><b>✦ ɴɪɢʜᴛ ʀᴏʏᴀʟᴇ ᴘʀᴏғɪʟᴇ: " + targetName + " ✦</b></gradient>       \n" +
                "  <gray>Rank:</gray> " + rank.getDisplayName() + "  <dark_gray>•</dark_gray>  <gray>Rating:</gray> <light_purple><b>" + stats.getRating() + " Elo</b></light_purple>  <dark_gray>•</dark_gray>  <gray>Level:</gray> <green><b>Lvl " + stats.getLevel() + "</b></green>\n" +
                "  <gray>Wins:</gray> <gold><b>" + stats.getWins() + "</b></gold> <gray>(" + String.format("%.1f", winRate) + "%)</gray>  <dark_gray>•</dark_gray>  <gray>Kills:</gray> <red><b>" + stats.getKills() + "</b></red>  <dark_gray>•</dark_gray>  <gray>Matches:</gray> <aqua><b>" + stats.getGames() + "</b></aqua>\n" +
                "  <gray>Coins:</gray> <yellow><b>" + stats.getCoins() + " ⛃</b></yellow>  <dark_gray>•</dark_gray>  <gray>XP:</gray> <green><b>" + stats.getXp() + "</b></green>  <dark_gray>•</dark_gray>  <gray>K/D:</gray> <yellow><b>" + String.format("%.2f", kd) + "</b></yellow>\n" +
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n"
        );

        sender.sendMessage(card);

        if (sender instanceof Player player) {
            StatsGui gui = new StatsGui(plugin, target);
            player.openInventory(gui.getInventory());
            player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1.0f, 1.2f);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> known = plugin.getStatsManager().getKnownPlayerNames();
            String prefix = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (String name : known) {
                if (name.toLowerCase().startsWith(prefix)) {
                    matches.add(name);
                }
            }
            return matches;
        }
        return Collections.emptyList();
    }
}
