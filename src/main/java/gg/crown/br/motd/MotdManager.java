package gg.crown.br.motd;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.state.MatchState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MotdManager implements Listener {

    private final NightRoyalePlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MotdManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(PaperServerListPingEvent event) {
        if (!plugin.getConfig().getBoolean("motd.enabled", true)) {
            return;
        }

        Component motdComponent = buildMotd();
        event.motd(motdComponent);

        int purgeCap = plugin.getLoginGateManager().getPurgeCap();
        if (purgeCap > 0) {
            event.setMaxPlayers(purgeCap);
        }
    }

    public Component buildMotd() {
        boolean isOpen = plugin.getLoginGateManager().isOpen();
        MatchState state = plugin.getMatchManager().getState();

        String line1Template;
        String line2Template;

        if (!isOpen) {
            // Closed login gate (between matches)
            line1Template = plugin.getConfig().getString(
                    "motd.closed.line1",
                    "        <gradient:#A855F7:#E085FF><b>✦ ɴɪɢʜᴛ ʀᴏʏᴀʟᴇ ✦</b></gradient>  <dark_gray>•</dark_gray>  <#E085FF><b>1.21.x ʙᴀᴛᴛʟᴇ ʀᴏʏᴀʟᴇ</b></#E085FF>"
            );
            line2Template = plugin.getConfig().getString(
                    "motd.closed.line2",
                    "   <red><b>ɢᴀᴛᴇ ᴄʟᴏsᴇᴅ</b></red> <dark_gray>»</dark_gray> <gray>ᴊᴏɪɴ</gray> <gradient:#A855F7:#E085FF><b>discord.gg/nightroyale</b></gradient> <gray>ᴛᴏ ᴘʟᴀʏ</gray>"
            );
        } else if (state == MatchState.ACTIVE || state == MatchState.ENDING) {
            // Live match in progress
            line1Template = plugin.getConfig().getString(
                    "motd.active.line1",
                    "        <gradient:#A855F7:#E085FF><b>✦ ɴɪɢʜᴛ ʀᴏʏᴀʟᴇ ✦</b></gradient>  <dark_gray>•</dark_gray>  <#E085FF><b>{MODE} ʙᴀᴛᴛʟᴇ ʀᴏʏᴀʟᴇ</b></#E085FF>"
            );
            line2Template = plugin.getConfig().getString(
                    "motd.active.line2",
                    "   <gold><b>⚔ ɢᴀᴍᴇ ɪɴ ᴘʀᴏɢʀᴇss</b></gold> <dark_gray>»</dark_gray> <#E085FF><b>{ALIVE} ᴄᴏᴍʙᴀᴛᴀɴᴛs ᴀʟɪᴠᴇ</b></#E085FF> <dark_gray>•</dark_gray> <gray>sᴘᴇᴄᴛᴀᴛᴇ ɴᴏᴡ</gray>"
            );
        } else {
            // Open join window (Countdown / Recruitment / Lobby)
            line1Template = plugin.getConfig().getString(
                    "motd.open.line1",
                    "        <gradient:#A855F7:#E085FF><b>✦ ɴɪɢʜᴛ ʀᴏʏᴀʟᴇ ✦</b></gradient>  <dark_gray>•</dark_gray>  <#E085FF><b>{MODE} ʙᴀᴛᴛʟᴇ ʀᴏʏᴀʟᴇ</b></#E085FF>"
            );
            line2Template = plugin.getConfig().getString(
                    "motd.open.line2",
                    "   <green><b>● ɢᴀᴍᴇ sᴛᴀʀᴛɪɴɢ</b></green> <dark_gray>»</dark_gray> <yellow>ᴊᴏɪɴ ɴᴏᴡ!</yellow> <dark_gray>•</dark_gray> <gray>ᴄᴀᴘ:</gray> <#E085FF>{ONLINE}/{CAP}</#E085FF>"
            );
        }

        String modeName = toSmallCaps(plugin.getMatchManager().getCurrentMode().name());
        int online = Bukkit.getOnlinePlayers().size();
        int cap = plugin.getLoginGateManager().getPurgeCap();
        int alive = plugin.getMatchManager().getAliveCount();
        long countdown = plugin.getMatchManager().getCountdownRemainingSeconds();

        String line1 = applyPlaceholders(line1Template, modeName, online, cap, alive, countdown);
        String line2 = applyPlaceholders(line2Template, modeName, online, cap, alive, countdown);

        return miniMessage.deserialize(line1 + "\n" + line2);
    }

    private String applyPlaceholders(String text, String mode, int online, int cap, int alive, long countdown) {
        return text.replace("{MODE}", mode)
                .replace("{ONLINE}", String.valueOf(online))
                .replace("{CAP}", String.valueOf(cap))
                .replace("{ALIVE}", String.valueOf(alive))
                .replace("{TIME}", formatCountdown(countdown));
    }

    private String formatCountdown(long seconds) {
        if (seconds <= 0) return "00:00";
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    public static String toSmallCaps(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            char lower = Character.toLowerCase(c);
            switch (lower) {
                case 'a' -> sb.append('ᴀ');
                case 'b' -> sb.append('ʙ');
                case 'c' -> sb.append('ᴄ');
                case 'd' -> sb.append('ᴅ');
                case 'e' -> sb.append('ᴇ');
                case 'f' -> sb.append('ғ');
                case 'g' -> sb.append('ɢ');
                case 'h' -> sb.append('ʜ');
                case 'i' -> sb.append('ɪ');
                case 'j' -> sb.append('ᴊ');
                case 'k' -> sb.append('ᴋ');
                case 'l' -> sb.append('ʟ');
                case 'm' -> sb.append('ᴍ');
                case 'n' -> sb.append('ɴ');
                case 'o' -> sb.append('ᴏ');
                case 'p' -> sb.append('ᴘ');
                case 'q' -> sb.append('ǫ');
                case 'r' -> sb.append('ʀ');
                case 's' -> sb.append('s');
                case 't' -> sb.append('ᴛ');
                case 'u' -> sb.append('ᴜ');
                case 'v' -> sb.append('ᴠ');
                case 'w' -> sb.append('ᴡ');
                case 'x' -> sb.append('x');
                case 'y' -> sb.append('ʏ');
                case 'z' -> sb.append('ᴢ');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
