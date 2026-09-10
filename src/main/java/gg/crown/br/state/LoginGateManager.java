package gg.crown.br.state;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;

public class LoginGateManager implements Listener {

    private final NightRoyalePlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private boolean open = false;
    private int purgeCap = 60;

    public LoginGateManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public int getPurgeCap() {
        return purgeCap;
    }

    public void setPurgeCap(int purgeCap) {
        this.purgeCap = Math.max(2, purgeCap);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        boolean isAdmin = Bukkit.getOfflinePlayer(uuid).isOp();

        if (isAdmin) {
            return;
        }

        if (!open) {
            String msg = plugin.getConfig().getString(
                    "login-gate.closed-message",
                    "<red>No games are available to join!</red>\n<white>Join our Discord to be notified: </white><gradient:#A855F7:#E085FF><b>discord.gg/nightroyale</b></gradient>"
            );
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, miniMessage.deserialize(msg));
            return;
        }

        int online = Bukkit.getOnlinePlayers().size();
        if (online >= purgeCap) {
            String fullMsg = plugin.getConfig().getString(
                    "login-gate.full-message",
                    "<red>Server Full!</red>\n<white>The player cap for this game has been reached.</white>"
            );
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, miniMessage.deserialize(fullMsg));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLogin(PlayerLoginEvent event) {
        if (event.getPlayer().hasPermission("nightroyale.admin")) {
            event.allow();
        }
    }
}
