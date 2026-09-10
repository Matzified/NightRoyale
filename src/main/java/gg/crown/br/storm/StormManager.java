package gg.crown.br.storm;

import gg.crown.br.NightRoyalePlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StormManager {

    private final NightRoyalePlugin plugin;
    private final Map<UUID, Integer> outOfBoundsSeconds = new HashMap<>();

    private Location stormCenter;
    private BukkitTask damageTask;
    private WorldBorder border;

    private double startRadius = 550.0;
    private double finalRadius = 20.0;
    private double damageStart = 1.0;
    private double damageRamp = 0.5;
    private double damageMax = 8.0;

    public StormManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public Location getStormCenter() {
        return stormCenter;
    }

    public void setStormCenter(Location stormCenter) {
        this.stormCenter = stormCenter;
    }

    public void setupStorm(World world, Location center, double startRad, double finalRad) {
        this.startRadius = startRad;
        this.finalRadius = finalRad;
        this.stormCenter = center != null ? center : (world.getSpawnLocation());

        this.border = world.getWorldBorder();
        this.border.setCenter(this.stormCenter.getX(), this.stormCenter.getZ());
        this.border.setSize(this.startRadius * 2); // Radius to diameter
        this.border.setDamageAmount(0.0); // We handle custom ramped damage with particles

        outOfBoundsSeconds.clear();
    }

    public void startClosing(int totalStormMinutes) {
        if (border == null) return;

        // Closes over half the total storm minutes
        long closingSeconds = Math.max(30, (totalStormMinutes * 60L) / 2);
        border.setSize(finalRadius * 2, closingSeconds);

        startDamageTask();
    }

    public void shrinkTo(double targetRadius, long seconds) {
        if (border == null) return;
        border.setSize(targetRadius * 2, Math.max(1, seconds));
        startDamageTask();
    }

    public void setRadius(double radius) {
        if (border == null) return;
        border.setSize(radius * 2);
        startDamageTask();
    }

    public double getCurrentRadius() {
        return border != null ? border.getSize() / 2.0 : startRadius;
    }

    private void startDamageTask() {
        if (damageTask != null) damageTask.cancel();

        damageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (border == null) return;

            double borderSize = border.getSize() / 2.0;
            double centerX = border.getCenter().getX();
            double centerZ = border.getCenter().getZ();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!plugin.getMatchManager().isMatchWorld(player.getWorld())) continue;
                if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) continue;
                if (plugin.getSpectatorManager() != null && plugin.getSpectatorManager().isSpectator(player)) continue;

                Location loc = player.getLocation();
                double dx = Math.abs(loc.getX() - centerX);
                double dz = Math.abs(loc.getZ() - centerZ);

                boolean outside = dx > borderSize || dz > borderSize;
                if (outside) {
                    int ticks = outOfBoundsSeconds.getOrDefault(player.getUniqueId(), 0) + 1;
                    outOfBoundsSeconds.put(player.getUniqueId(), ticks);

                    double damage = Math.min(damageMax, damageStart + (ticks * damageRamp));
                    player.damage(damage);

                    // Red dust particles only (no action bar)
                    Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.5f);
                    player.getWorld().spawnParticle(Particle.DUST, loc.add(0, 1, 0), 10, 0.4, 0.5, 0.4, dust);
                } else {
                    outOfBoundsSeconds.remove(player.getUniqueId());
                }
            }
        }, 20L, 20L); // 1-second interval
    }

    public void stop() {
        if (damageTask != null) {
            damageTask.cancel();
            damageTask = null;
        }
        outOfBoundsSeconds.clear();
    }
}
