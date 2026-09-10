package gg.crown.br.listener;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.state.MatchState;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LobbyListener implements Listener {

    private final NightRoyalePlugin plugin;

    public LobbyListener(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MatchState state = plugin.getMatchManager().getState();

        if (state != MatchState.ACTIVE) {
            player.setGameMode(GameMode.ADVENTURE);
            player.setHealth(20.0);
            player.setFoodLevel(20);

            Location lobby = plugin.getLobbyLocation();
            if (lobby != null) {
                player.teleport(lobby);
            }
        }

        plugin.getScoreboardManager().updatePlayer(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getScoreboardManager().remove(event.getPlayer());
        plugin.getTeamManager().leave(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getMatchManager().isMatchWorld(player.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLobbyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getMatchManager().isMatchWorld(player.getWorld())) {
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                Location lobby = plugin.getLobbyLocation();
                if (lobby != null) {
                    player.teleport(lobby);
                }
            }
            event.setCancelled(true);
        }
    }
}
