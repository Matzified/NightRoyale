package gg.crown.br.listener;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.mode.GameMode;
import gg.crown.br.state.MatchState;
import gg.crown.br.stats.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.time.Duration;

public class CombatListener implements Listener {

    private final NightRoyalePlugin plugin;

    public CombatListener(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageDuringGrace(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        // Cancel damage during Grace Period or outside ACTIVE state
        if (plugin.getMatchManager().getState() != MatchState.ACTIVE || plugin.getMatchManager().isGracePeriod()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (plugin.getMatchManager().getState() != MatchState.ACTIVE) return;

        Player killer = victim.getKiller();

        // 1. Reward Killer
        if (killer != null && !killer.equals(victim)) {
            plugin.getMatchManager().recordKill(killer.getUniqueId());

            PlayerStats killerStats = plugin.getStatsManager().getStats(killer.getUniqueId());
            killerStats.addKill();
            int coinsPerKill = plugin.getConfig().getInt("rewards.coins-per-kill", 25);
            int xpPerKill = plugin.getConfig().getInt("rewards.xp-per-kill", 50);
            killerStats.addCoins(coinsPerKill);
            killerStats.addXp(xpPerKill);

            killer.sendMessage(Component.text("+ " + coinsPerKill + " Coins, + " + xpPerKill + " XP (Kill)", NamedTextColor.GOLD));

            // Earning a kill triggers an armor upgrade!
            plugin.getArmorUpgradeManager().triggerUpgrade(killer);
        }

        // 2. UHC Golden Head drop
        if (plugin.getMatchManager().getCurrentMode() == GameMode.UHC) {
            victim.getWorld().dropItemNaturally(victim.getLocation(), plugin.getChestManager().createGoldenHead());
        }

        // 3. Victim elimination title & spectator transition
        Title eliminationTitle = Title.title(
                Component.text("ELIMINATED", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.text(killer != null ? "Killed by " + killer.getName() : "You died.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        victim.showTitle(eliminationTitle);
        victim.playSound(victim.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.5f);

        // Put into spectator after death with custom tools, choice GUI, and interactive chat prompt
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!victim.isOnline()) return;
            victim.spigot().respawn();
            Location specLoc = plugin.getMatchManager().getSpectatorLocation();
            if (specLoc != null) victim.teleport(specLoc);
            plugin.getSpectatorManager().makeSpectator(victim);

            // Interactive clickable chat options
            Component spectateBtn = Component.text("[ 👁 SPECTATE ]", NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Click to stay in the arena and watch the match!", NamedTextColor.GREEN)))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/nr spectate"));

            Component lobbyBtn = Component.text("[ 🚪 RETURN TO LOBBY ]", NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Click to leave the arena and wait in the lobby for the next game!", NamedTextColor.RED)))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/lobby"));

            victim.sendMessage(Component.empty());
            victim.sendMessage(Component.text("---------------------------------------------", NamedTextColor.DARK_PURPLE));
            victim.sendMessage(Component.text("☠ You were eliminated! What would you like to do?", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            victim.sendMessage(Component.text("  ").append(spectateBtn).append(Component.text("   •   ", NamedTextColor.DARK_GRAY)).append(lobbyBtn));
            victim.sendMessage(Component.text("---------------------------------------------", NamedTextColor.DARK_PURPLE));
            victim.sendMessage(Component.empty());

            // Open post-death choice GUI
            plugin.getSpectatorManager().openDeathChoiceGUI(victim);
        }, 4L);

        // Broadcast alive combatants count
        int remaining = 0;
        for (Player p : plugin.getMatchManager().getAlivePlayers()) {
            if (!p.getUniqueId().equals(victim.getUniqueId())) {
                remaining++;
            }
        }
        Bukkit.broadcast(Component.text(victim.getName() + " was eliminated! (" + remaining + " players remaining)", NamedTextColor.GRAY));
    }

    @EventHandler(ignoreCancelled = true)
    public void onWindReset(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (plugin.getMatchManager().getState() != MatchState.ACTIVE) return;
        if (!plugin.getMatchManager().hasScenario(gg.crown.br.mode.Scenario.WIND_RESET)) return;

        if (!(event.getEntity() instanceof org.bukkit.entity.WindCharge wc)) return;
        if (!(wc.getShooter() instanceof Player player)) return;

        org.bukkit.util.Vector v = player.getVelocity();
        boolean falling = v.getY() <= -0.3;
        boolean aimingDown = player.getLocation().getPitch() >= 60.0f;
        if (!falling || !aimingDown) return;

        player.setVelocity(new org.bukkit.util.Vector(v.getX(), 0.0, v.getZ()));
        player.setFallDistance(0.0f);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
        if (event.getEntityType() == org.bukkit.entity.EntityType.ENDERMITE) {
            event.setCancelled(true);
        }
    }
}
