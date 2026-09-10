package gg.crown.br.hud;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.state.MatchState;
import gg.crown.br.stats.PlayerStats;
import gg.crown.br.team.Squad;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager {

    private final NightRoyalePlugin plugin;
    private final Map<UUID, FastBoard> boards = new HashMap<>();

    public ScoreboardManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public void updatePlayer(Player player) {
        FastBoard board = boards.computeIfAbsent(player.getUniqueId(), id -> new FastBoard(player));

        MatchState state = plugin.getMatchManager().getState();
        String region = plugin.getConfig().getString("scoreboard.region", "EU");
        String discordLink = plugin.getConfig().getString("scoreboard.discord-link", "discord.gg/nightroyale");

        board.updateTitle(Component.text("NIGHT ROYALE", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).font(Key.key("nightroyale:main")));

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());

        if (state == MatchState.ACTIVE || state == MatchState.ENDING) {
            // Match HUD
            int alive = plugin.getMatchManager().getAliveCount();
            int kills = plugin.getMatchManager().getMatchKills(player.getUniqueId());
            long matchSeconds = plugin.getMatchManager().getMatchElapsedSeconds();

            lines.add(Component.text("Time: ", NamedTextColor.GRAY).append(Component.text(formatTime(matchSeconds), NamedTextColor.WHITE)));
            lines.add(Component.text("Alive: ", NamedTextColor.GRAY).append(Component.text(alive, NamedTextColor.GREEN)));
            lines.add(Component.text("Kills: ", NamedTextColor.GRAY).append(Component.text(kills, NamedTextColor.RED)));

            Squad squad = plugin.getTeamManager().getSquad(player);
            String squadText = (squad != null) ? squad.size() + "/" + plugin.getTeamManager().getMaxTeamSize() : "1/1";
            lines.add(Component.text("Squad: ", NamedTextColor.GRAY).append(Component.text(squadText, NamedTextColor.AQUA)));

            if (plugin.getMatchManager().isGracePeriod()) {
                long graceLeft = plugin.getMatchManager().getGraceRemainingSeconds();
                lines.add(Component.text("Grace: ", NamedTextColor.GRAY).append(Component.text(graceLeft + "s", NamedTextColor.YELLOW)));
            }

            lines.add(Component.empty());
            lines.add(Component.text("Region: ", NamedTextColor.DARK_GRAY).append(Component.text(region + " (" + player.getPing() + "ms)", NamedTextColor.GRAY)));
            lines.add(Component.text(discordLink, NamedTextColor.DARK_PURPLE));

        } else {
            // Lobby / Pre-game HUD
            PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());

            if (state == MatchState.COUNTDOWN) {
                long sec = plugin.getMatchManager().getCountdownRemainingSeconds();
                lines.add(Component.text("Purge in: ", NamedTextColor.GOLD).append(Component.text(sec + "s", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)));
            } else if (state == MatchState.RECRUITMENT) {
                long sec = plugin.getMatchManager().getRecruitmentRemainingSeconds();
                lines.add(Component.text("Recruiting: ", NamedTextColor.AQUA).append(Component.text(sec + "s", NamedTextColor.WHITE)));
            } else {
                lines.add(Component.text("Status: ", NamedTextColor.GRAY).append(Component.text("Waiting for game", NamedTextColor.YELLOW)));
            }

            lines.add(Component.empty());
            lines.add(Component.text("Level: ", NamedTextColor.GRAY).append(Component.text(stats.getLevel(), NamedTextColor.GREEN)));
            lines.add(Component.text("Rating: ", NamedTextColor.GRAY).append(Component.text(stats.getRating(), NamedTextColor.YELLOW)));
            lines.add(Component.text("Wins: ", NamedTextColor.GRAY).append(Component.text(stats.getWins(), NamedTextColor.AQUA)));
            lines.add(Component.text("Kills: ", NamedTextColor.GRAY).append(Component.text(stats.getKills(), NamedTextColor.RED)));
            lines.add(Component.text("Coins: ", NamedTextColor.GRAY).append(Component.text(stats.getCoins(), NamedTextColor.GOLD)));

            lines.add(Component.empty());
            lines.add(Component.text(discordLink, NamedTextColor.DARK_PURPLE));
        }

        board.updateLines(lines);
    }

    public void remove(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    private String formatTime(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    // Lightweight clean wrapper around Bukkit scoreboard objective
    public static class FastBoard {
        private final Player player;
        private final Scoreboard scoreboard;
        private final Objective objective;
        private final List<String> currentEntries = new ArrayList<>();

        public FastBoard(Player player) {
            this.player = player;
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            this.objective = scoreboard.registerNewObjective("nr_hud", Criteria.DUMMY, Component.empty());
            this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            player.setScoreboard(scoreboard);
        }

        public void updateTitle(Component title) {
            objective.displayName(title);
        }

        public void updateLines(List<Component> lines) {
            // Clear old entries
            for (String entry : currentEntries) {
                scoreboard.resetScores(entry);
            }
            currentEntries.clear();

            int score = lines.size();
            for (int i = 0; i < lines.size(); i++) {
                Component line = lines.get(i);
                // Create unique fake entry name using invisible color codes
                String entry = "§" + (char) ('a' + i) + "§r";
                Team team = scoreboard.getTeam("line_" + i);
                if (team == null) {
                    team = scoreboard.registerNewTeam("line_" + i);
                    team.addEntry(entry);
                }
                team.prefix(line);
                objective.getScore(entry).setScore(score - i);
                currentEntries.add(entry);
            }
        }

        public void delete() {
            objective.unregister();
            for (Team team : scoreboard.getTeams()) {
                team.unregister();
            }
        }
    }
}
