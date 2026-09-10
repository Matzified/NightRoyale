package gg.crown.br.state;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.mode.GameMode;
import gg.crown.br.mode.Scenario;
import gg.crown.br.stats.MatchRecorder;
import gg.crown.br.stats.PlayerStats;
import gg.crown.br.team.Squad;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Duration;
import java.util.*;

public class MatchManager {

    private final NightRoyalePlugin plugin;

    private MatchState state = MatchState.IDLE;
    private GameMode currentMode = GameMode.SMP;
    private final Set<Scenario> currentScenarios = new HashSet<>();

    private long countdownSeconds = 300;
    private long recruitmentSeconds = 60;
    private long graceSeconds = 60;
    private int stormMinutes = 15;
    private String hostName = "System";

    private long matchStartTimestamp;
    private boolean gracePeriod = true;

    private final Set<UUID> matchRoster = new HashSet<>();
    private final Map<UUID, Integer> matchKills = new HashMap<>();

    private BukkitTask mainPulseTask;
    private BukkitTask resetTask;

    public MatchManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
        loadMatchYaml();
        startPulseTask();
    }

    public MatchState getState() { return state; }
    public GameMode getCurrentMode() { return currentMode; }
    public boolean isGracePeriod() { return gracePeriod; }
    public long getCountdownRemainingSeconds() { return countdownSeconds; }
    public long getRecruitmentRemainingSeconds() { return recruitmentSeconds; }
    public long getGraceRemainingSeconds() { return graceSeconds; }
    public int getMatchKills(UUID uuid) { return matchKills.getOrDefault(uuid, 0); }
    public void recordKill(UUID uuid) { matchKills.put(uuid, matchKills.getOrDefault(uuid, 0) + 1); }

    public long getMatchElapsedSeconds() {
        if (state != MatchState.ACTIVE && state != MatchState.ENDING) return 0;
        return (System.currentTimeMillis() - matchStartTimestamp) / 1000L;
    }

    public boolean hasScenario(Scenario s) {
        return currentScenarios.contains(s);
    }

    public boolean isMatchWorld(World world) {
        return plugin.getArenaWorldManager().isMatchWorld(world);
    }

    public Location getSpectatorLocation() {
        World matchWorld = plugin.getArenaWorldManager().getActiveMatchWorld();
        if (matchWorld != null) {
            Location stormCenter = plugin.getStormManager().getStormCenter();
            return stormCenter != null ? stormCenter.clone().add(0, 50, 0) : matchWorld.getSpawnLocation().add(0, 50, 0);
        }
        return plugin.getLobbyLocation();
    }

    public int getAliveCount() {
        int count = 0;
        for (UUID uuid : matchRoster) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getGameMode() == org.bukkit.GameMode.SURVIVAL && !p.isDead()) {
                count++;
            }
        }
        return count;
    }

    public List<Player> getAlivePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : matchRoster) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getGameMode() == org.bukkit.GameMode.SURVIVAL && !p.isDead()) {
                list.add(p);
            }
        }
        return list;
    }

    // --- Main State Machine Pulse (1 second ticker) ---
    private void startPulseTask() {
        if (mainPulseTask != null) mainPulseTask.cancel();
        mainPulseTask = Bukkit.getScheduler().runTaskTimer(plugin, this::pulse, 20L, 20L);
    }

    private void pulse() {
        plugin.getScoreboardManager().updateAll();

        switch (state) {
            case COUNTDOWN -> tickCountdown();
            case RECRUITMENT -> tickRecruitment();
            case ACTIVE -> tickActive();
            default -> {}
        }
    }

    // --- 1. COUNTDOWN TICK ---
    private void tickCountdown() {
        // Discord 5-minute ping
        if (countdownSeconds == 300) {
            plugin.getDiscordNotifier().sendFiveMinuteWarning(
                    Bukkit.getOnlinePlayers().size(),
                    plugin.getLoginGateManager().getPurgeCap(),
                    currentMode.getDisplayName()
            );
        }

        // On-screen title only in the final 5 minutes
        if (countdownSeconds <= 300 && (countdownSeconds % 60 == 0 || countdownSeconds <= 10)) {
            Title title = Title.title(
                    Component.text("PURGE IN " + countdownSeconds + "s", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                    Component.text("Get ready for battle!", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(1), Duration.ofMillis(200))
            );
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(title);
            }
        }

        // Rising bell sound during final 10 seconds
        if (countdownSeconds <= 10 && countdownSeconds > 0) {
            float pitch = 0.5f + ((10 - (float) countdownSeconds) * 0.1f);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, pitch);
            }
        }

        countdownSeconds--;

        if (countdownSeconds <= 0) {
            if (Bukkit.getOnlinePlayers().size() < 2) {
                Bukkit.broadcast(Component.text("✖ Match cancelled: Not enough players to start.", NamedTextColor.RED));
                transitionTo(MatchState.IDLE);
                return;
            }

            if (plugin.getTeamManager().getMaxTeamSize() > 1) {
                transitionTo(MatchState.RECRUITMENT);
            } else {
                transitionTo(MatchState.ACTIVE);
            }
        }
    }

    // --- 2. RECRUITMENT TICK ---
    private void tickRecruitment() {
        if (recruitmentSeconds <= 5 || recruitmentSeconds % 15 == 0) {
            Bukkit.broadcast(Component.text("⏳ Squad formation ends in " + recruitmentSeconds + "s! Use /team", NamedTextColor.AQUA));
        }

        recruitmentSeconds--;
        if (recruitmentSeconds <= 0) {
            List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
            plugin.getTeamManager().autoFillUnpaired(online);
            transitionTo(MatchState.ACTIVE);
        }
    }

    // --- 3. ACTIVE TICK ---
    private void tickActive() {
        // Grace period countdown
        if (gracePeriod) {
            graceSeconds--;
            if (graceSeconds <= 0) {
                endGracePeriod();
            }
        } else {
            // Win condition check every second
            checkWinCondition();
        }
    }

    private void endGracePeriod() {
        gracePeriod = false;

        // Title and sound for everyone
        Title title = Title.title(
                Component.text("CHESTS REFILLED", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD),
                Component.text("Grace has ended — PvP is now ENABLED!", NamedTextColor.RED),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }

        // Refill chests
        plugin.getChestManager().refillAllChests(currentMode, currentScenarios);

        // Start storm shrinking
        plugin.getStormManager().startClosing(stormMinutes);

        // Discord webhook ping
        List<String> scenNames = currentScenarios.stream().map(Scenario::getDisplayName).toList();
        plugin.getDiscordNotifier().sendPurgeBegun(
                Bukkit.getOnlinePlayers().size(),
                matchRoster.size(),
                currentMode.getDisplayName(),
                scenNames
        );

        // Start hazards
        plugin.getHazardManager().start();
    }

    private void checkWinCondition() {
        int alive = getAliveCount();

        if (alive == 0) {
            // No winner
            handleMatchEnd(Collections.emptyList());
            return;
        }

        if (plugin.getTeamManager().getMaxTeamSize() > 1) {
            // Squad mode: check how many squads remain alive
            Set<Squad> activeSquads = new HashSet<>();
            for (Player p : getAlivePlayers()) {
                Squad s = plugin.getTeamManager().getSquad(p);
                if (s != null) activeSquads.add(s);
            }

            if (activeSquads.size() == 1) {
                Squad winningSquad = activeSquads.iterator().next();
                List<Player> winners = new ArrayList<>();
                for (UUID member : winningSquad.getMembers()) {
                    Player p = Bukkit.getPlayer(member);
                    if (p != null) winners.add(p);
                }
                handleMatchEnd(winners);
            }
        } else {
            // Solo mode: 1 player left
            if (alive == 1) {
                handleMatchEnd(getAlivePlayers());
            }
        }
    }

    private void handleMatchEnd(List<Player> winners) {
        transitionTo(MatchState.ENDING);

        long duration = getMatchElapsedSeconds();
        List<String> winnerNames = winners.stream().map(Player::getName).toList();

        // Determine Team MVP and Game MVP
        String teamMvp = null;
        if (winners.size() > 1) {
            Player topTeam = winners.stream()
                    .max(Comparator.comparingInt(p -> getMatchKills(p.getUniqueId())))
                    .orElse(null);
            if (topTeam != null) teamMvp = topTeam.getName();
        }

        Map.Entry<UUID, Integer> topKiller = matchKills.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        String gameMvp = null;
        int gameMvpKills = 0;
        if (topKiller != null && topKiller.getValue() > 0) {
            Player p = Bukkit.getPlayer(topKiller.getKey());
            gameMvp = p != null ? p.getName() : "Unknown";
            gameMvpKills = topKiller.getValue();
        }

        // 1. In-game announcements and rewards
        if (!winners.isEmpty()) {
            Title winTitle = Title.title(
                    Component.text("VICTORY ROYALE", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                    Component.text(String.join(", ", winnerNames) + " won the match!", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(5), Duration.ofSeconds(1))
            );

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(winTitle);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }

            int winCoins = plugin.getConfig().getInt("rewards.coins-per-win", 250);
            int winXp = plugin.getConfig().getInt("rewards.xp-per-win", 500);

            for (Player w : winners) {
                PlayerStats stats = plugin.getStatsManager().getStats(w.getUniqueId());
                stats.addWin();
                stats.addCoins(winCoins);
                stats.addXp(winXp);
                w.sendMessage(Component.text("🏆 + " + winCoins + " Coins, + " + winXp + " XP (Victory!)", NamedTextColor.GOLD));
            }
        } else {
            Bukkit.broadcast(Component.text("✖ Match ended in a draw — zero combatants survived.", NamedTextColor.RED));
        }

        // 2. Discord Game Over Webhook
        plugin.getDiscordNotifier().sendGameOver(winnerNames, teamMvp, gameMvp, gameMvpKills, duration, currentMode.getDisplayName());

        // 3. Save to matches.json
        List<MatchRecorder.BoardEntry> board = new ArrayList<>();
        for (UUID uuid : matchRoster) {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : "Player";
            int kills = getMatchKills(uuid);
            int placement = winners.stream().anyMatch(w -> w.getUniqueId().equals(uuid)) ? 1 : 2;
            int rating = plugin.getStatsManager().getStats(uuid).getRating();
            board.add(new MatchRecorder.BoardEntry(name, uuid.toString(), kills, placement, rating));
        }

        int gameNumber = plugin.getStatsManager().getGamesRun() + 1;
        plugin.getStatsManager().setGamesRun(gameNumber);

        String primaryWinner = winnerNames.isEmpty() ? null : String.join(", ", winnerNames);
        MatchRecorder.MatchEntry entry = new MatchRecorder.MatchEntry(
                gameNumber,
                System.currentTimeMillis(),
                matchRoster.size(),
                primaryWinner,
                board
        );
        plugin.getMatchRecorder().recordMatch(entry);

        // Schedule RESET phase: 60s with winner, 8s without
        long delaySeconds = winners.isEmpty() ? 8L : 60L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> transitionTo(MatchState.RESET), 20L * delaySeconds);
    }

    // --- State Transitions ---
    public void transitionTo(MatchState newState) {
        this.state = newState;

        switch (newState) {
            case IDLE -> {
                matchRoster.clear();
                matchKills.clear();
                plugin.getStormManager().stop();
                plugin.getGoldCrateManager().cancel();
                plugin.getHazardManager().stop();
                plugin.getDeploymentManager().clearAll();
                plugin.getScenarioManager().clear();
                plugin.getTeamManager().clearAll();
                plugin.getChestManager().clearAll();
            }
            case COUNTDOWN -> {
                plugin.getLoginGateManager().setOpen(true);
            }
            case ACTIVE -> {
                matchStartTimestamp = System.currentTimeMillis();
                gracePeriod = true;
                graceSeconds = plugin.getConfig().getLong("match.grace-seconds", 60);

                // Create fresh match world
                World matchWorld = plugin.getArenaWorldManager().createMatchWorld();

                // Setup Storm
                Location stormCenter = plugin.getStormManager().getStormCenter();
                plugin.getStormManager().setupStorm(matchWorld, stormCenter, 550.0, 20.0);

                // Arm Gold Crate if scenario is on
                if (hasScenario(Scenario.CRATE)) {
                    plugin.getGoldCrateManager().armCrateTimer(stormCenter);
                }

                // Clear debris
                plugin.getFoliageGuardian().clearItems(matchWorld);

                // Scatter arena loot chests
                plugin.getChestManager().scatter(matchWorld, currentMode, currentScenarios);

                // Populate match roster & equip kits
                matchRoster.clear();
                matchKills.clear();
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

                for (Player p : players) {
                    p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    p.setHealth(20.0);
                    p.setFoodLevel(20);
                    matchRoster.add(p.getUniqueId());
                    plugin.getKitManager().equipKit(p, currentMode);
                    plugin.getStatsManager().getStats(p.getUniqueId()).addGame();
                }

                // Deploy via Happy Ghast bus
                Location ghastStart = plugin.getGhastStartLocation();
                Location ghastEnd = plugin.getGhastEndLocation();
                plugin.getDeploymentManager().deployPlayers(players, ghastStart, ghastEnd);
            }
            case RESET -> {
                Bukkit.broadcast(Component.text("Match complete. Resetting in 15 seconds...", NamedTextColor.YELLOW));
                plugin.getLoginGateManager().setOpen(false);

                if (resetTask != null) resetTask.cancel();
                resetTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Location lobby = plugin.getLobbyLocation();

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) continue;

                        p.getInventory().clear();
                        p.getInventory().setArmorContents(null);
                        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
                        p.setFireTicks(0);
                        p.setGliding(false);
                        p.setFallDistance(0.0f);
                        p.setHealth(20.0);
                        p.setFoodLevel(20);
                        p.setGameMode(org.bukkit.GameMode.ADVENTURE);

                        if (lobby != null) {
                            p.teleport(lobby);
                        }
                    }

                    plugin.getArenaWorldManager().discardMatchWorld();
                    transitionTo(MatchState.IDLE);
                }, 20L * 15);
            }
        }
    }

    // --- Bot Command Handshake ---
    public void configureAndStartMatch(Map<String, String> params) {
        if (params.containsKey("mode")) {
            this.currentMode = GameMode.fromString(params.get("mode"));
        }
        if (params.containsKey("teamsize")) {
            String ts = params.get("teamsize").toUpperCase();
            int size = switch (ts) {
                case "DUOS", "2" -> 2;
                case "TRIOS", "3" -> 3;
                default -> 1;
            };
            plugin.getTeamManager().setMaxTeamSize(size);
        }
        if (params.containsKey("storm")) {
            try { this.stormMinutes = Integer.parseInt(params.get("storm")); } catch (NumberFormatException ignored) {}
        }
        if (params.containsKey("cap")) {
            try { plugin.getLoginGateManager().setPurgeCap(Integer.parseInt(params.get("cap"))); } catch (NumberFormatException ignored) {}
        }
        if (params.containsKey("host")) {
            this.hostName = params.get("host");
        }
        if (params.containsKey("grace")) {
            try { this.graceSeconds = Long.parseLong(params.get("grace")); } catch (NumberFormatException ignored) {}
        }
        if (params.containsKey("countdown")) {
            try { this.countdownSeconds = Long.parseLong(params.get("countdown")); } catch (NumberFormatException ignored) {}
        }
        if (params.containsKey("scenarios")) {
            currentScenarios.clear();
            String raw = params.get("scenarios");
            if (!raw.equalsIgnoreCase("none")) {
                for (String part : raw.split(",")) {
                    Scenario s = Scenario.fromKey(part);
                    if (s != null) currentScenarios.add(s);
                }
            }
            plugin.getScenarioManager().setScenarios(currentScenarios, currentMode);
        }

        if (Boolean.parseBoolean(params.getOrDefault("begin", "false"))) {
            transitionTo(MatchState.COUNTDOWN);
        }
    }

    public void loadMatchYaml() {
        File file = new File(plugin.getDataFolder(), "match.yml");
        if (!file.exists()) {
            plugin.saveResource("match.yml", false);
        }
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains("mode")) {
            this.currentMode = GameMode.fromString(config.getString("mode"));
        }
        if (config.contains("team-size")) {
            String ts = config.getString("team-size", "SOLOS").toUpperCase();
            int size = switch (ts) {
                case "DUOS", "2" -> 2;
                case "TRIOS", "3" -> 3;
                default -> 1;
            };
            plugin.getTeamManager().setMaxTeamSize(size);
        }
        if (config.contains("storm-minutes")) {
            this.stormMinutes = config.getInt("storm-minutes", 20);
        }
        if (config.contains("purge-cap")) {
            plugin.getLoginGateManager().setPurgeCap(config.getInt("purge-cap", 60));
        }
        if (config.contains("hosted-by")) {
            this.hostName = config.getString("hosted-by", "System");
        }
        if (config.contains("grace-seconds")) {
            this.graceSeconds = config.getLong("grace-seconds", 60);
        }
        if (config.contains("countdown-seconds")) {
            this.countdownSeconds = config.getLong("countdown-seconds", 300);
        }
        if (config.contains("scenarios")) {
            currentScenarios.clear();
            List<String> rawList = config.getStringList("scenarios");
            for (String raw : rawList) {
                Scenario s = Scenario.fromKey(raw);
                if (s != null) currentScenarios.add(s);
            }
            plugin.getScenarioManager().setScenarios(currentScenarios, currentMode);
        }
    }

    public void stop() {
        if (mainPulseTask != null) {
            mainPulseTask.cancel();
            mainPulseTask = null;
        }
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
    }
}
