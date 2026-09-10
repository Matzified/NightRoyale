package gg.crown.br.stats;

import gg.crown.br.NightRoyalePlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager implements Listener {

    private final NightRoyalePlugin plugin;
    private final File statsFile;
    private YamlConfiguration statsConfig;
    private final Map<UUID, PlayerStats> cachedStats = new HashMap<>();

    public StatsManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!statsFile.exists()) {
            try {
                statsFile.getParentFile().mkdirs();
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create stats.yml: " + e.getMessage());
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    public PlayerStats getStats(UUID uuid) {
        return cachedStats.computeIfAbsent(uuid, id -> {
            String path = id.toString();
            if (statsConfig.contains(path)) {
                int kills = statsConfig.getInt(path + ".kills", 0);
                int wins = statsConfig.getInt(path + ".wins", 0);
                int games = statsConfig.getInt(path + ".games", 0);
                int coins = statsConfig.getInt(path + ".coins", 0);
                int xp = statsConfig.getInt(path + ".xp", 0);
                return new PlayerStats(kills, wins, games, coins, xp);
            }
            return new PlayerStats();
        });
    }

    public void savePlayerStats(UUID uuid) {
        PlayerStats stats = cachedStats.get(uuid);
        if (stats == null || statsConfig == null) return;

        String path = uuid.toString();
        statsConfig.set(path + ".kills", stats.getKills());
        statsConfig.set(path + ".wins", stats.getWins());
        statsConfig.set(path + ".games", stats.getGames());
        statsConfig.set(path + ".coins", stats.getCoins());
        statsConfig.set(path + ".xp", stats.getXp());

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save stats.yml: " + e.getMessage());
        }
    }

    public void saveAll() {
        if (statsConfig == null) return;
        for (Map.Entry<UUID, PlayerStats> entry : cachedStats.entrySet()) {
            String path = entry.getKey().toString();
            PlayerStats stats = entry.getValue();
            statsConfig.set(path + ".kills", stats.getKills());
            statsConfig.set(path + ".wins", stats.getWins());
            statsConfig.set(path + ".games", stats.getGames());
            statsConfig.set(path + ".coins", stats.getCoins());
            statsConfig.set(path + ".xp", stats.getXp());
        }
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save stats.yml: " + e.getMessage());
        }
    }

    public int getGamesRun() {
        return statsConfig != null ? statsConfig.getInt("server.games-run", 0) : 0;
    }

    public void setGamesRun(int count) {
        if (statsConfig != null) {
            statsConfig.set("server.games-run", count);
            try {
                statsConfig.save(statsFile);
            } catch (IOException ignored) {}
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        savePlayerStats(event.getPlayer().getUniqueId());
    }
}
