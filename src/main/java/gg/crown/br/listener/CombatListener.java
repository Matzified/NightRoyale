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

        // Put into spectator after death
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            victim.spigot().respawn();
            victim.setGameMode(org.bukkit.GameMode.SPECTATOR);
            victim.teleport(plugin.getMatchManager().getSpectatorLocation());
        }, 2L);

        // Broadcast alive combatants count
        int alive = plugin.getMatchManager().getAliveCount() - 1;
        Bukkit.broadcast(Component.text(victim.getName() + " was eliminated! (" + alive + " players remaining)", NamedTextColor.GRAY));
    }
}
