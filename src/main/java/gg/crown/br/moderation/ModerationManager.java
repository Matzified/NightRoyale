package gg.crown.br.moderation;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.rank.Rank;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModerationManager implements Listener {

    private final NightRoyalePlugin plugin;

    // Chat is locked (disabled) by default
    private boolean chatEnabled = false;

    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<UUID, Long> mutedUntil = new HashMap<>();
    private final Map<UUID, String> muteReasons = new HashMap<>();

    public ModerationManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public void setChatEnabled(boolean chatEnabled) {
        this.chatEnabled = chatEnabled;
        Component msg = chatEnabled
                ? Component.text("✔ Global chat has been enabled by staff.", NamedTextColor.GREEN)
                : Component.text("✖ Global chat has been disabled by staff.", NamedTextColor.RED);
        Bukkit.broadcast(msg);
    }

    public boolean isMuted(UUID uuid) {
        Long until = mutedUntil.get(uuid);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            mutedUntil.remove(uuid);
            muteReasons.remove(uuid);
            return false;
        }
        return true;
    }

    public void mutePlayer(UUID uuid, long durationMillis, String reason) {
        mutedUntil.put(uuid, System.currentTimeMillis() + durationMillis);
        muteReasons.put(uuid, reason);
    }

    public void unmutePlayer(UUID uuid) {
        mutedUntil.remove(uuid);
        muteReasons.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        boolean isStaff = plugin.getRankManager().isStaff(player);

        // 1. Mute check
        if (isMuted(player.getUniqueId())) {
            event.setCancelled(true);
            String reason = muteReasons.getOrDefault(player.getUniqueId(), "Breaking rules");
            player.sendMessage(Component.text("You are muted. Reason: " + reason, NamedTextColor.RED));
            return;
        }

        // 2. Lobby chat check: Disable chat in lobby for members
        Rank rank = plugin.getRankManager().getRank(player);
        boolean inLobby = !plugin.getMatchManager().isMatchWorld(player.getWorld()) 
                       || plugin.getMatchManager().getState() != gg.crown.br.state.MatchState.ACTIVE;
        if (inLobby && rank == Rank.MEMBER && !isStaff) {
            event.setCancelled(true);
            player.sendMessage(Component.text("✖ Chat in the lobby is disabled for members.", NamedTextColor.RED));
            return;
        }

        // 3. Global chat disabled check (staff exempt)
        if (!chatEnabled && !isStaff) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Chat is currently disabled.", NamedTextColor.RED));
            return;
        }

        // 4. Cooldown check
        if (!isStaff) {
            long cooldownMs = (rank == Rank.MIDNIGHT || rank == Rank.TWILIGHT) ? 1000L : 3000L;

            long now = System.currentTimeMillis();
            long last = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);

            if (now - last < cooldownMs) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Please wait before sending another message.", NamedTextColor.RED));
                return;
            }
            lastMessageTime.put(player.getUniqueId(), now);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLobbyToyInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getMatchManager().isMatchWorld(player.getWorld())) return;

        // In lobby: elytra & wind toy restricted to admin/owner
        if (event.getItem() != null) {
            String typeName = event.getItem().getType().name();
            if (typeName.contains("WIND_CHARGE") || typeName.contains("ELYTRA")) {
                if (!plugin.getRankManager().isStaff(player)) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("Movement toys are reserved for Staff & Owners in the lobby.", NamedTextColor.RED));
                }
            }
        }
    }
}
