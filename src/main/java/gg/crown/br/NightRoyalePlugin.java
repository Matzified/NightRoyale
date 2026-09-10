package gg.crown.br;

import gg.crown.br.command.*;
import gg.crown.br.crate.GoldCrateManager;
import gg.crown.br.deploy.DeploymentManager;
import gg.crown.br.discord.DiscordNotifier;
import gg.crown.br.hazard.HazardManager;
import gg.crown.br.hud.ScoreboardManager;
import gg.crown.br.listener.CombatListener;
import gg.crown.br.listener.LobbyListener;
import gg.crown.br.loot.ArmorUpgradeManager;
import gg.crown.br.loot.ChestManager;
import gg.crown.br.loot.EnchantBookHandler;
import gg.crown.br.loot.LootConsolidationListener;
import gg.crown.br.mode.KitManager;
import gg.crown.br.mode.ScenarioManager;
import gg.crown.br.moderation.ModerationManager;
import gg.crown.br.motd.MotdManager;
import gg.crown.br.rank.RankManager;
import gg.crown.br.spectator.SpectatorManager;
import gg.crown.br.state.LoginGateManager;
import gg.crown.br.state.MatchManager;
import gg.crown.br.stats.MatchRecorder;
import gg.crown.br.stats.StatsManager;
import gg.crown.br.storm.StormManager;
import gg.crown.br.team.TeamManager;
import gg.crown.br.world.AdvancementBlocker;
import gg.crown.br.world.ArenaWorldManager;
import gg.crown.br.world.FoliageGuardian;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class NightRoyalePlugin extends JavaPlugin {

    private LoginGateManager loginGateManager;
    private MatchManager matchManager;
    private KitManager kitManager;
    private ScenarioManager scenarioManager;
    private ChestManager chestManager;
    private ArmorUpgradeManager armorUpgradeManager;
    private TeamManager teamManager;
    private DeploymentManager deploymentManager;
    private GoldCrateManager goldCrateManager;
    private StormManager stormManager;
    private HazardManager hazardManager;
    private RankManager rankManager;
    private ModerationManager moderationManager;
    private StatsManager statsManager;
    private MatchRecorder matchRecorder;
    private DiscordNotifier discordNotifier;
    private ScoreboardManager scoreboardManager;
    private ArenaWorldManager arenaWorldManager;
    private FoliageGuardian foliageGuardian;
    private SpectatorManager spectatorManager;
    private MotdManager motdManager;

    private Location lobbyLocation;
    private Location arenaLocation;
    private Location ghastStartLocation;
    private Location ghastEndLocation;
    private Location arenaPos1;
    private Location arenaPos2;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("arenas.yml", false);
        saveResource("match.yml", false);
        saveResource("ranks.yml", false);
        saveResource("stats.yml", false);
        saveResource("matches.json", false);
        loadLocations();

        // Initialize core sub-systems
        this.spectatorManager = new gg.crown.br.spectator.SpectatorManager(this);
        this.motdManager = new MotdManager(this);
        this.loginGateManager = new LoginGateManager(this);
        this.kitManager = new KitManager(this);
        this.scenarioManager = new ScenarioManager(this);
        this.chestManager = new ChestManager(this);
        this.armorUpgradeManager = new ArmorUpgradeManager(this);
        this.teamManager = new TeamManager(this);
        this.deploymentManager = new DeploymentManager(this);
        this.goldCrateManager = new GoldCrateManager(this);
        this.stormManager = new StormManager(this);
        this.hazardManager = new HazardManager(this);
        this.rankManager = new RankManager(this);
        this.moderationManager = new ModerationManager(this);
        this.statsManager = new StatsManager(this);
        this.matchRecorder = new MatchRecorder(this);
        this.discordNotifier = new DiscordNotifier(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.arenaWorldManager = new ArenaWorldManager(this);
        this.foliageGuardian = new FoliageGuardian(this);

        this.matchManager = new MatchManager(this);

        // Register event listeners
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(loginGateManager, this);
        pm.registerEvents(kitManager, this);
        pm.registerEvents(scenarioManager, this);
        pm.registerEvents(armorUpgradeManager, this);
        pm.registerEvents(new LootConsolidationListener(this), this);
        pm.registerEvents(new EnchantBookHandler(), this);
        pm.registerEvents(teamManager, this);
        pm.registerEvents(deploymentManager, this);
        pm.registerEvents(goldCrateManager, this);
        pm.registerEvents(hazardManager, this);
        pm.registerEvents(rankManager, this);
        pm.registerEvents(moderationManager, this);
        pm.registerEvents(statsManager, this);
        pm.registerEvents(new AdvancementBlocker(), this);
        pm.registerEvents(foliageGuardian, this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new LobbyListener(this), this);
        pm.registerEvents(spectatorManager, this);
        pm.registerEvents(motdManager, this);
        pm.registerEvents(scoreboardManager, this);

        // Register commands
        var nrCmd = new NightRoyaleCommand(this);
        getCommand("nightroyale").setExecutor(nrCmd);
        getCommand("nightroyale").setTabCompleter(nrCmd);

        var teamCmd = new TeamCommand(this);
        getCommand("team").setExecutor(teamCmd);
        getCommand("team").setTabCompleter(teamCmd);

        getCommand("chest").setExecutor(new ChestRefillCommand(this));
        getCommand("revive").setExecutor(new ReviveCommand(this));

        var rankCmd = new RankCommand(this);
        getCommand("rank").setExecutor(rankCmd);
        getCommand("rank").setTabCompleter(rankCmd);

        var chatCmd = new ChatToggleCommand(this);
        getCommand("chat").setExecutor(chatCmd);
        getCommand("chat").setTabCompleter(chatCmd);

        var modCmd = new ModerationCommands(this);
        getCommand("report").setExecutor(modCmd);
        getCommand("mute").setExecutor(modCmd);
        getCommand("unmute").setExecutor(modCmd);

        getCommand("discord").setExecutor(new DiscordLinkCommand(this));
        getCommand("elytra").setExecutor(new ElytraToyCommand(this));

        getLogger().info("Night Royale v" + getDescription().getVersion() + " initialized by " + getDescription().getAuthors());
    }

    @Override
    public void onDisable() {
        if (matchManager != null) matchManager.stop();
        if (scoreboardManager != null) scoreboardManager.clearAll();
        if (statsManager != null) statsManager.saveAll();
        if (rankManager != null) rankManager.saveRanks();
        if (chestManager != null) chestManager.clearAll();
        if (spectatorManager != null) spectatorManager.clearAll();
        if (deploymentManager != null) deploymentManager.clearAll();
        if (goldCrateManager != null) goldCrateManager.cancel();
        if (stormManager != null) stormManager.stop();
        if (hazardManager != null) hazardManager.stop();
        if (arenaWorldManager != null) arenaWorldManager.discardMatchWorld();

        getLogger().info("Night Royale disabled safely.");
    }

    public void loadLocations() {
        this.lobbyLocation = readLocation("lobby");
        this.arenaLocation = readLocation("arena");
        this.ghastStartLocation = readLocation("ghast-start");
        this.ghastEndLocation = readLocation("ghast-end");
        this.arenaPos1 = readLocation("arena-pos1");
        this.arenaPos2 = readLocation("arena-pos2");

        Location stormCenter = readLocation("storm-center");
        if (stormCenter != null && stormManager != null) {
            stormManager.setStormCenter(stormCenter);
        }
    }

    private Location readLocation(String key) {
        Location loc = gg.crown.br.util.LocationUtil.read(this, key);
        if (loc != null) return loc;
        return gg.crown.br.util.LocationUtil.read(this, "locations." + key);
    }

    public Location getLobbyLocation() { return lobbyLocation; }
    public void setLobbyLocation(Location loc) {
        this.lobbyLocation = loc;
        gg.crown.br.util.LocationUtil.write(this, "lobby", loc);
    }

    public Location getArenaLocation() { return arenaLocation; }
    public void setArenaLocation(Location loc) {
        this.arenaLocation = loc;
        gg.crown.br.util.LocationUtil.write(this, "arena", loc);
    }

    public Location getGhastStartLocation() { return ghastStartLocation; }
    public void setGhastStartLocation(Location loc) {
        this.ghastStartLocation = loc;
        gg.crown.br.util.LocationUtil.write(this, "ghast-start", loc);
    }

    public Location getGhastEndLocation() { return ghastEndLocation; }
    public void setGhastEndLocation(Location loc) {
        this.ghastEndLocation = loc;
        gg.crown.br.util.LocationUtil.write(this, "ghast-end", loc);
    }

    public Location getArenaPos1() { return arenaPos1; }
    public void setArenaPos1(Location loc) {
        this.arenaPos1 = loc;
        gg.crown.br.util.LocationUtil.write(this, "arena-pos1", loc);
    }

    public Location getArenaPos2() { return arenaPos2; }
    public void setArenaPos2(Location loc) {
        this.arenaPos2 = loc;
        gg.crown.br.util.LocationUtil.write(this, "arena-pos2", loc);
    }

    public void saveStormCenterLocation(Location loc) {
        gg.crown.br.util.LocationUtil.write(this, "storm-center", loc);
    }

    // Getters for all manager components
    public LoginGateManager getLoginGateManager() { return loginGateManager; }
    public MatchManager getMatchManager() { return matchManager; }
    public KitManager getKitManager() { return kitManager; }
    public ScenarioManager getScenarioManager() { return scenarioManager; }
    public ChestManager getChestManager() { return chestManager; }
    public ArmorUpgradeManager getArmorUpgradeManager() { return armorUpgradeManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public DeploymentManager getDeploymentManager() { return deploymentManager; }
    public GoldCrateManager getGoldCrateManager() { return goldCrateManager; }
    public StormManager getStormManager() { return stormManager; }
    public HazardManager getHazardManager() { return hazardManager; }
    public RankManager getRankManager() { return rankManager; }
    public ModerationManager getModerationManager() { return moderationManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public MatchRecorder getMatchRecorder() { return matchRecorder; }
    public DiscordNotifier getDiscordNotifier() { return discordNotifier; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public ArenaWorldManager getArenaWorldManager() { return arenaWorldManager; }
    public FoliageGuardian getFoliageGuardian() { return foliageGuardian; }
    public SpectatorManager getSpectatorManager() { return spectatorManager; }
    public MotdManager getMotdManager() { return motdManager; }
}
