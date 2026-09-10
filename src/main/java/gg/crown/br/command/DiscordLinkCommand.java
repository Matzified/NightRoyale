package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DiscordLinkCommand implements CommandExecutor {

    private final NightRoyalePlugin plugin;

    public DiscordLinkCommand(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String link = plugin.getConfig().getString("scoreboard.discord-link", "discord.gg/nightroyale");
        Component message = Component.text("Join our Discord community: ", NamedTextColor.GRAY)
                .append(MiniMessage.miniMessage().deserialize("<gradient:#A855F7:#E085FF><b>" + link + "</b></gradient>")
                        .clickEvent(ClickEvent.openUrl("https://" + link)));

        sender.sendMessage(message);
        return true;
    }
}
