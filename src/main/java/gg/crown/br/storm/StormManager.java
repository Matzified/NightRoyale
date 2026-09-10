package gg.crown.br.storm;

import gg.crown.br.NightRoyalePlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StormManager {

    private final NightRoyalePlugin plugin;
    private final Map<UUID, Integer> outOfBoundsSeconds = new HashMap<>();

    private Location stormCenter;
    private World activeWorld;
    private WorldBorder border;

    private double currentRadius = 550.0;
    private double startRadius = 550.0;
    private double targetRadius = 20.0;
    private double finalRadius = 20.0;

    private boolean isClosing = false;
    private long closingStartMs = 0;
    private long closingDurationMs = 0;

    private double damageStart = 1.0;
    private double damageRamp = 0.5;
    private double damageMax = 10.0;

    private BukkitTask damageTask;
    private BukkitTask particleTask;

    private static final Particle.DustOptions PURPLE_DUST = new Particle.DustOptions(Color.fromRGB(168, 85, 247), 1.3f);
    private static final Particle.DustOptions RED_HURT_DUST = new Particle.DustOptions(Color.fromRGB(220, 20, 20), 1.5f);

    public StormManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
        loadConfigValues();
    }

    public void loadConfigValues() {
        FileConfiguration cfg = plugin.getConfig();
        this.startRadius = cfg.getDouble("storm.start-radius", 550.0);
        this.finalRadius = cfg.getDouble("storm.final-radius", 20.0);
        this.damageStart = cfg.getDouble("storm.damage-start", 1.0);
        this.damageRamp = cfg.getDouble("storm.damage-ramp", 0.5);
        this.damageMax = cfg.getDouble("storm.damage-max", 10.0);
    }

    public Location getStormCenter() {
        return stormCenter;
    }

    public void setStormCenter(Location stormCenter) {
        this.stormCenter = stormCenter;
        if (border != null && stormCenter != null) {
            border.setCenter(stormCenter.getX(), stormCenter.getZ());
        }
    }

    public void setupStorm(World world, Location center, double startRad, double finalRad) {
        stop();
        this.activeWorld = world;
        this.startRadius = startRad;
        this.currentRadius = startRad;
        this.targetRadius = startRad;
        this.finalRadius = finalRad;
        this.isClosing = false;
        this.stormCenter = center != null ? center : world.getSpawnLocation();

        // Circumscribed square WorldBorder so vanilla forcefield texture displays in distance without blocking circular movement
        this.border = world.getWorldBorder();
        this.border.setCenter(this.stormCenter.getX(), this.stormCenter.getZ());
        this.border.setSize(this.currentRadius * 2.0 * 1.45);
        this.border.setDamageAmount(0.0);
        this.border.setWarningDistance(10);

        outOfBoundsSeconds.clear();
        startTasks();
    }

    public void startClosing(int totalStormMinutes) {
        long closingSeconds = Math.max(30, (totalStormMinutes * 60L) / 2);
        shrinkTo(finalRadius, closingSeconds);
    }

    public void shrinkTo(double targetRad, long seconds) {
        this.startRadius = getCurrentRadius();
        this.currentRadius = this.startRadius;
        this.targetRadius = Math.max(1.0, targetRad);
        this.closingStartMs = System.currentTimeMillis();
        this.closingDurationMs = Math.max(1, seconds) * 1000L;
        this.isClosing = true;

        if (border != null) {
            border.setSize(this.targetRadius * 2.0 * 1.45, Math.max(1, seconds));
        }

        startTasks();
    }

    public void setRadius(double radius) {
        this.currentRadius = Math.max(1.0, radius);
        this.targetRadius = this.currentRadius;
        this.startRadius = this.currentRadius;
        this.isClosing = false;

        if (border != null) {
            border.setSize(this.currentRadius * 2.0 * 1.45);
        }

        startTasks();
    }

    public void closeNow() {
        shrinkTo(finalRadius, 1);
    }

    public double getCurrentRadius() {
        if (!isClosing || closingDurationMs <= 0) {
            return currentRadius;
        }
        long elapsed = System.currentTimeMillis() - closingStartMs;
        if (elapsed >= closingDurationMs) {
            currentRadius = targetRadius;
            isClosing = false;
            return currentRadius;
        }
        double progress = (double) elapsed / (double) closingDurationMs;
        return startRadius + (targetRadius - startRadius) * progress;
    }

    public boolean isOutside(Location loc) {
        if (stormCenter == null || loc.getWorld() == null) return false;
        if (activeWorld != null && !loc.getWorld().equals(activeWorld)) return false;

        double dx = loc.getX() - stormCenter.getX();
        double dz = loc.getZ() - stormCenter.getZ();
        double radius = getCurrentRadius();
        return (dx * dx + dz * dz) > (radius * radius);
    }

    public double getDistanceToCenter(Location loc) {
        if (stormCenter == null || loc.getWorld() == null) return 0;
        double dx = loc.getX() - stormCenter.getX();
        double dz = loc.getZ() - stormCenter.getZ();
        return Math.hypot(dx, dz);
    }

    public boolean isRunning() {
        return activeWorld != null;
    }

    private void startTasks() {
        if (damageTask == null) {
            damageTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickDamage, 20L, 20L);
        }
        if (particleTask == null) {
            particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickParticles, 4L, 4L);
        }
    }

    private void tickDamage() {
        if (stormCenter == null) return;
        double radius = getCurrentRadius();
        double centerX = stormCenter.getX();
        double centerZ = stormCenter.getZ();
        double radiusSq = radius * radius;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getMatchManager().isMatchWorld(player.getWorld())) continue;
            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) continue;
            if (plugin.getSpectatorManager() != null && plugin.getSpectatorManager().isSpectator(player)) continue;

            Location loc = player.getLocation();
            double dx = loc.getX() - centerX;
            double dz = loc.getZ() - centerZ;
            double distSq = dx * dx + dz * dz;

            if (distSq > radiusSq) {
                int ticks = outOfBoundsSeconds.getOrDefault(player.getUniqueId(), 0) + 1;
                outOfBoundsSeconds.put(player.getUniqueId(), ticks);

                double damage = Math.min(damageMax, damageStart + (ticks * damageRamp));
                player.damage(damage);

                // Red damage particles and electrical zap sound
                player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 12, 0.3, 0.5, 0.3, RED_HURT_DUST);
                player.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.25f, 2.0f);
            } else {
                outOfBoundsSeconds.remove(player.getUniqueId());
            }
        }
    }

    private void tickParticles() {
        if (stormCenter == null) return;
        double radius = getCurrentRadius();
        double centerX = stormCenter.getX();
        double centerZ = stormCenter.getZ();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getMatchManager().isMatchWorld(player.getWorld())) continue;

            Location pLoc = player.getLocation();
            double dx = pLoc.getX() - centerX;
            double dz = pLoc.getZ() - centerZ;
            double dist = Math.hypot(dx, dz);

            // Render circular wall particles only if player is within 40 blocks of the circle perimeter
            if (Math.abs(dist - radius) <= 40.0) {
                double playerAngle = Math.atan2(dz, dx);
                // Angular span: view arc of ~35 blocks along the perimeter
                double span = Math.min(Math.PI * 0.75, Math.max(Math.PI / 8.0, 35.0 / radius));
                double step = Math.max(0.02, 1.2 / radius);

                double py = pLoc.getY();
                for (double a = playerAngle - span; a <= playerAngle + span; a += step) {
                    double px = centerX + radius * Math.cos(a);
                    double pz = centerZ + radius * Math.sin(a);

                    // Send particle packets directly to this player (client-side only, no server broadcast lag)
                    player.spawnParticle(Particle.DUST, px, py - 0.5, pz, 1, 0, 0, 0, PURPLE_DUST);
                    player.spawnParticle(Particle.DUST, px, py + 1.2, pz, 1, 0, 0, 0, PURPLE_DUST);
                    player.spawnParticle(Particle.DUST, px, py + 3.0, pz, 1, 0, 0, 0, PURPLE_DUST);
                }

                // Ambient warning hum if standing right on the storm edge
                if (dist >= radius - 2.5 && dist <= radius + 2.5) {
                    if (player.getTicksLived() % 20 == 0) {
                        player.playSound(pLoc, Sound.BLOCK_BEACON_AMBIENT, 0.35f, 1.9f);
                    }
                }
            }
        }
    }

    public void stop() {
        if (damageTask != null) {
            damageTask.cancel();
            damageTask = null;
        }
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        outOfBoundsSeconds.clear();
        isClosing = false;
        activeWorld = null;
    }
}
