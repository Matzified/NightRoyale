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

    private Location lobbyLocation;
    private Location arenaLocation;
    private Location ghastStartLocation;
    private Location ghastEndLocation;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLocations();

        // Initialize core sub-systems
        this.spectatorManager = new gg.crown.br.spectator.SpectatorManager(this);
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

    private void loadLocations() {
        this.lobbyLocation = getConfig().getLocation("locations.lobby");
        this.arenaLocation = getConfig().getLocation("locations.arena");
        this.ghastStartLocation = getConfig().getLocation("locations.ghast-start");
        this.ghastEndLocation = getConfig().getLocation("locations.ghast-end");
    }

    public Location getLobbyLocation() { return lobbyLocation; }
    public void setLobbyLocation(Location loc) {
        this.lobbyLocation = loc;
        getConfig().set("locations.lobby", loc);
        saveConfig();
    }

    public Location getArenaLocation() { return arenaLocation; }
    public void setArenaLocation(Location loc) {
        this.arenaLocation = loc;
        getConfig().set("locations.arena", loc);
        saveConfig();
    }

    public Location getGhastStartLocation() { return ghastStartLocation; }
    public void setGhastStartLocation(Location loc) {
        this.ghastStartLocation = loc;
        getConfig().set("locations.ghast-start", loc);
        saveConfig();
    }

    public Location getGhastEndLocation() { return ghastEndLocation; }
    public void setGhastEndLocation(Location loc) {
        this.ghastEndLocation = loc;
        getConfig().set("locations.ghast-end", loc);
        saveConfig();
    }

    public void saveStormCenterLocation(Location loc) {
        getConfig().set("locations.storm-center", loc);
        saveConfig();
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
}
