package gg.crown.br.deploy;

import gg.crown.br.NightRoyalePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class DeploymentManager implements Listener {

    private final NightRoyalePlugin plugin;
    private final Map<UUID, ItemStack> savedChestplates = new HashMap<>();
    private final Map<UUID, Long> dismountTimestamps = new HashMap<>();
    private final Set<UUID> glidingPlayers = new HashSet<>();
    private final Set<Ghast> activeGhasts = new HashSet<>();

    private BukkitTask trailTask;
    private BukkitTask flightTask;

    public DeploymentManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public void deployPlayers(List<Player> players, Location startLoc, Location endLoc) {
        clearAll();

        if (startLoc == null) {
            // Default hover deployment if no path is configured
            for (Player player : players) {
                Location loc = player.getLocation().add(0, 40, 0);
                Ghast ghast = spawnBusGhast(loc);
                ghast.addPassenger(player);
                activeGhasts.add(ghast);
            }
        } else {
            // Group players up to 4 per ghast
            List<List<Player>> groups = new ArrayList<>();
            for (int i = 0; i < players.size(); i += 4) {
                groups.add(players.subList(i, Math.min(i + 4, players.size())));
            }

            Vector direction = endLoc != null ? endLoc.toVector().subtract(startLoc.toVector()).normalize().multiply(0.8) : new Vector(0, 0, 0);

            for (List<Player> group : groups) {
                Ghast ghast = spawnBusGhast(startLoc.clone().add(0, 30, 0));
                for (Player p : group) {
                    ghast.addPassenger(p);
                }
                activeGhasts.add(ghast);
            }

            if (endLoc != null) {
                flightTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    for (Ghast ghast : activeGhasts) {
                        if (ghast.isValid()) {
                            ghast.setVelocity(direction);
                        }
                    }
                }, 1L, 2L);
            }
        }

        // Particle trail task for gliders
        trailTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : glidingPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && player.isGliding()) {
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }, 2L, 2L);

        // Auto-eject after 11 seconds
        Bukkit.getScheduler().runTaskLater(plugin, this::autoEjectAll, 20L * 11);
    }

    private Ghast spawnBusGhast(Location loc) {
        Ghast ghast = (Ghast) loc.getWorld().spawnEntity(loc, EntityType.GHAST);
        ghast.setAI(false);
        ghast.setSilent(true);
        ghast.setInvulnerable(true);
        ghast.setGravity(false);
        return ghast;
    }

    public void autoEjectAll() {
        for (Ghast ghast : activeGhasts) {
            if (ghast.isValid()) {
                for (Entity passenger : new ArrayList<>(ghast.getPassengers())) {
                    if (passenger instanceof Player p) {
                        ejectPlayer(p);
                    }
                }
                ghast.remove();
            }
        }
        activeGhasts.clear();
    }

    private void ejectPlayer(Player player) {
        if (!player.isInsideVehicle()) return;
        player.leaveVehicle();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDismounted() instanceof Ghast ghast)) return;

        if (!activeGhasts.contains(ghast)) return;

        // Play ambient sound to nearby players
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_AMBIENT, 1.2f, 1.0f);

        // Save exact chestplate & equip Elytra
        ItemStack currentChest = player.getInventory().getChestplate();
        if (currentChest != null) {
            savedChestplates.put(player.getUniqueId(), currentChest.clone());
        }

        ItemStack elytra = new ItemStack(Material.ELYTRA);
        player.getInventory().setChestplate(elytra);
        player.setGliding(true);

        glidingPlayers.add(player.getUniqueId());
        dismountTimestamps.put(player.getUniqueId(), System.currentTimeMillis());

        // Check if ghast is now empty
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (ghast.isValid() && ghast.getPassengers().isEmpty()) {
                activeGhasts.remove(ghast);
                ghast.remove();
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!glidingPlayers.contains(player.getUniqueId())) return;

        Long dismountedAt = dismountTimestamps.get(player.getUniqueId());
        // 1.5 second grace window to avoid false positive at dismount
        if (dismountedAt != null && System.currentTimeMillis() - dismountedAt < 1500) {
            return;
        }

        if (player.isOnGround() || player.isInWater() || player.isSwimming() || player.isClimbing()) {
            landPlayer(player);
        }
    }

    private void landPlayer(Player player) {
        glidingPlayers.remove(player.getUniqueId());
        dismountTimestamps.remove(player.getUniqueId());

        player.setGliding(false);
        player.setFallDistance(0.0f);

        // Restore exact original chestplate
        ItemStack original = savedChestplates.remove(player.getUniqueId());
        if (original != null) {
            player.getInventory().setChestplate(original);
        } else {
            player.getInventory().setChestplate(null);
        }

        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (glidingPlayers.contains(p.getUniqueId())) {
            landPlayer(p);
        }
    }

    public void clearAll() {
        if (flightTask != null) flightTask.cancel();
        if (trailTask != null) trailTask.cancel();
        autoEjectAll();
        savedChestplates.clear();
        dismountTimestamps.clear();
        glidingPlayers.clear();
    }
}
