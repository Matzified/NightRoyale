package gg.crown.br.mode;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class ScenarioManager implements Listener {

    private final NightRoyalePlugin plugin;
    private final Set<Scenario> activeScenarios = new HashSet<>();
    private Objective healthObjective;

    public ScenarioManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public Set<Scenario> getActiveScenarios() {
        return activeScenarios;
    }

    public boolean hasScenario(Scenario scenario) {
        return activeScenarios.contains(scenario);
    }

    public void setScenarios(Set<Scenario> scenarios, GameMode mode) {
        activeScenarios.clear();
        for (Scenario s : scenarios) {
            if (s.isMaceOnly() && mode != GameMode.MACE) {
                continue; // Mode-scoped restriction
            }
            activeScenarios.add(s);
        }

        applyScenarios();
    }

    public void applyScenarios() {
        // 1. OLD_COMBAT handling
        boolean oldCombat = activeScenarios.contains(Scenario.OLD_COMBAT);
        for (Player p : Bukkit.getOnlinePlayers()) {
            AttributeInstance attr = p.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attr != null) {
                attr.setBaseValue(oldCombat ? 100.0 : 4.0);
            }
        }

        // 2. HEALTH_INDICATORS handling
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        if (activeScenarios.contains(Scenario.HEALTH_INDICATORS)) {
            if (healthObjective == null) {
                healthObjective = board.getObjective("nr_health");
                if (healthObjective == null) {
                    healthObjective = board.registerNewObjective("nr_health", Criteria.HEALTH, Component.text("❤"));
                }
                healthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            }
        } else {
            if (healthObjective != null) {
                healthObjective.unregister();
                healthObjective = null;
            }
        }
    }

    public void clear() {
        activeScenarios.clear();
        applyScenarios();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && activeScenarios.contains(Scenario.NO_FALL_DAMAGE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (activeScenarios.contains(Scenario.NO_NATURAL_REGEN) && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWindChargeLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof WindCharge)) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        if (activeScenarios.contains(Scenario.WIND_RESET)) {
            // "Minemen tech": Falling player looking down (pitch >= 60 deg) zeroes fall velocity
            if (player.getFallDistance() > 3.0f && player.getLocation().getPitch() >= 60.0f) {
                Vector v = player.getVelocity();
                v.setY(0.0);
                player.setVelocity(v);
                player.setFallDistance(0.0f);
            }
        }
    }
}
