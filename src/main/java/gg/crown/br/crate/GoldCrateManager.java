package gg.crown.br.crate;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GoldCrateManager implements Listener {

    private final NightRoyalePlugin plugin;
    private final Map<UUID, Double> damageTracker = new HashMap<>();

    private Location crateLocation;
    private TextDisplay crateHologram;
    private BossBar bossBar;
    private double currentHealth = 100.0;
    private BukkitTask spawnTask;

    public GoldCrateManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public void armCrateTimer(Location stormCenter) {
        cancel();
        // 10 minutes (600s)
        spawnTask = Bukkit.getScheduler().runTaskLater(plugin, () -> spawnCrate(stormCenter), 20L * 600);
    }

    public void spawnCrate(Location stormCenter) {
        if (stormCenter == null || stormCenter.getWorld() == null) return;

        Location spawnLoc = stormCenter.getWorld().getHighestBlockAt(stormCenter).getLocation();
        spawnLoc.getBlock().setType(Material.GOLD_BLOCK);
        crateLocation = spawnLoc;
        currentHealth = 100.0;
        damageTracker.clear();

        crateHologram = spawnLoc.getWorld().spawn(spawnLoc.clone().add(0.5, 1.3, 0.5), TextDisplay.class, d -> {
            d.text(Component.text("★ GOLD CRATE ★", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            d.setBillboard(Display.Billboard.CENTER);
        });

        bossBar = BossBar.bossBar(
                Component.text("Gold Crate [100/100 HP]", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                1.0f,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bossBar);
            p.sendMessage(Component.text("⚡ The Gold Crate has spawned at the center of the storm!", NamedTextColor.GOLD));
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageBlock(BlockDamageEvent event) {
        if (crateLocation == null || !event.getBlock().getLocation().equals(crateLocation)) return;

        Player player = event.getPlayer();
        applyDamage(player, 10.0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakBlock(BlockBreakEvent event) {
        if (crateLocation == null || !event.getBlock().getLocation().equals(crateLocation)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        applyDamage(player, 25.0);
    }

    private void applyDamage(Player player, double amount) {
        currentHealth = Math.max(0.0, currentHealth - amount);
        damageTracker.put(player.getUniqueId(), damageTracker.getOrDefault(player.getUniqueId(), 0.0) + amount);

        if (bossBar != null) {
            bossBar.progress((float) (currentHealth / 100.0));
            bossBar.name(Component.text("Gold Crate [" + (int) currentHealth + "/100 HP]", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        }

        player.playSound(crateLocation, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.0f);

        if (currentHealth <= 0.0) {
            breakCrate();
        }
    }

    private void breakCrate() {
        if (crateLocation != null) {
            crateLocation.getBlock().setType(Material.AIR);
            crateLocation.getWorld().playSound(crateLocation, Sound.ENTITY_WITHER_DEATH, 1.0f, 1.2f);
        }

        if (crateHologram != null) {
            crateHologram.remove();
            crateHologram = null;
        }

        if (bossBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bossBar);
            }
            bossBar = null;
        }

        // Determine top damager
        UUID topDamager = damageTracker.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (topDamager != null) {
            Player winner = Bukkit.getPlayer(topDamager);
            if (winner != null && winner.isOnline()) {
                winner.getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
                winner.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
                winner.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 16));

                Bukkit.broadcast(Component.text("✦ " + winner.getName() + " broke the Gold Crate and claimed the rewards!", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            }
        }

        crateLocation = null;
        damageTracker.clear();
    }

    public void cancel() {
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        if (bossBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bossBar);
            }
            bossBar = null;
        }
        if (crateHologram != null) {
            crateHologram.remove();
            crateHologram = null;
        }
        if (crateLocation != null) {
            crateLocation.getBlock().setType(Material.AIR);
            crateLocation = null;
        }
        damageTracker.clear();
    }
}
