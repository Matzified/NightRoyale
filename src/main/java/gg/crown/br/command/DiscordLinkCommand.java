package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DiscordLinkCommand implements CommandExecutor {

    private final NightRoyalePlugin plugin;

    public DiscordLinkCommand(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String link = plugin.getConfig().getString("scoreboard.discord",
                plugin.getConfig().getString("scoreboard.discord-link", "discord.gg/nightroyale"));
        String fullUrl = link.startsWith("http") ? link : "https://" + link;

        Component card = MiniMessage.miniMessage().deserialize(
                "\n" +
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                "       <gradient:#A855F7:#E085FF><b>✦ ɴɪɢʜᴛ ʀᴏʏᴀʟᴇ ᴅɪsᴄᴏʀᴅ ✦</b></gradient>       \n" +
                "  <gray>Join our official Discord community for scrims,</gray>\n" +
                "  <gray>tournaments, announcements, and match pings!</gray>\n\n" +
                "  <click:open_url:'" + fullUrl + "'><hover:show_text:'<#E085FF><b>Click to open Discord invite!</b></#E085FF>'><gradient:#A855F7:#E085FF><b>▶ [CLICK HERE TO JOIN DISCORD] ◀</b></gradient></hover></click>\n" +
                "  <dark_gray>»</dark_gray> <click:open_url:'" + fullUrl + "'><hover:show_text:'<#E085FF><b>" + fullUrl + "</b></#E085FF>'><underlined><#E085FF>" + link + "</#E085FF></underlined></hover></click>\n" +
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n"
        );

        sender.sendMessage(card);

        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        }

        return true;
    }
}
