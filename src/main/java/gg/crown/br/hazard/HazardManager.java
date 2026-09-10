package gg.crown.br.hazard;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class HazardManager implements Listener {

    private final NightRoyalePlugin plugin;
    private final Set<Location> playerPlacedWater = new HashSet<>();
    private final Map<UUID, Long> lastWaterDamage = new HashMap<>();
    private BukkitTask waterTickTask;

    public HazardManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        waterTickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkWaterHazard, 10L, 10L);
    }

    public void stop() {
        if (waterTickTask != null) {
            waterTickTask.cancel();
            waterTickTask = null;
        }
        playerPlacedWater.clear();
        lastWaterDamage.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.getBucket() == Material.WATER_BUCKET) {
            Block block = event.getBlockClicked().getRelative(event.getBlockFace());
            playerPlacedWater.add(block.getLocation());
        }
    }

    private void checkWaterHazard() {
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getMatchManager().isMatchWorld(player.getWorld())) continue;
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;

            Block block = player.getLocation().getBlock();
            if (block.getType() == Material.WATER && !playerPlacedWater.contains(block.getLocation())) {
                long last = lastWaterDamage.getOrDefault(player.getUniqueId(), 0L);
                if (now - last >= 1000) { // Throttled to 1s per player
                    lastWaterDamage.put(player.getUniqueId(), now);
                    player.damage(2.0); // 1 heart per second
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPearlHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;

        Block hitBlock = event.getHitBlock();
        if (hitBlock != null && hitBlock.getType() == Material.WATER && !playerPlacedWater.contains(hitBlock.getLocation())) {
            event.setCancelled(true);
            pearl.remove();
            player.sendMessage(Component.text("Map water is hazardous! Ender pearl dissolved.", NamedTextColor.RED));
        }
    }
}
