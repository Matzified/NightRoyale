package gg.crown.br.rank;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RankManager implements Listener {

    private final NightRoyalePlugin plugin;
    private final Map<UUID, Rank> playerRanks = new HashMap<>();
    private final File ranksFile;
    private YamlConfiguration ranksConfig;

    public RankManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
        this.ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        loadRanks();
    }

    public void loadRanks() {
        if (!ranksFile.exists()) {
            try {
                ranksFile.getParentFile().mkdirs();
                ranksFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create ranks.yml: " + e.getMessage());
            }
        }
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        for (String key : ranksConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Rank rank = Rank.fromString(ranksConfig.getString(key));
                playerRanks.put(uuid, rank);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveRanks() {
        if (ranksConfig == null) return;
        for (Map.Entry<UUID, Rank> entry : playerRanks.entrySet()) {
            ranksConfig.set(entry.getKey().toString(), entry.getValue().name());
        }
        try {
            ranksConfig.save(ranksFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save ranks.yml: " + e.getMessage());
        }
    }

    public Rank getRank(Player player) {
        if (player.isOp()) return Rank.OWNER;
        return playerRanks.getOrDefault(player.getUniqueId(), Rank.MEMBER);
    }

    public void setRank(UUID uuid, Rank rank) {
        playerRanks.put(uuid, rank);
        saveRanks();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyDisplay(player);
        }
    }

    public boolean isStaff(Player player) {
        if (player.isOp()) return true;
        return getRank(player).isStaff();
    }

    public void applyDisplay(Player player) {
        Rank rank = getRank(player);
        Component tag = rank.getTagComponent();

        // 1. Tab list display
        player.playerListName(tag.append(Component.text(player.getName(), rank.getColor())));

        // 2. Nametag prefix via Scoreboard Team
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = String.format("%03d_%s", 100 - rank.getWeight(), player.getName());
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        team.prefix(tag);
        team.color(rank.getColor());
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyDisplay(event.getPlayer());
    }
}
