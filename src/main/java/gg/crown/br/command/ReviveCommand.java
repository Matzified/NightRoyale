package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.state.MatchState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReviveCommand implements CommandExecutor {

    private final NightRoyalePlugin plugin;

    public ReviveCommand(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("nightroyale.admin")) {
            sender.sendMessage(Component.text("You do not have permission to revive players.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /revive <player>", NamedTextColor.RED));
            return true;
        }

        if (plugin.getMatchManager().getState() != MatchState.ACTIVE) {
            sender.sendMessage(Component.text("You can only revive players during an ACTIVE match.", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        Location safeLoc = plugin.getMatchManager().getSpectatorLocation();
        target.teleport(safeLoc);
        target.setGameMode(GameMode.SURVIVAL);
        target.setHealth(20.0);
        target.setFoodLevel(20);

        plugin.getKitManager().equipKit(target, plugin.getMatchManager().getCurrentMode());
        Bukkit.broadcast(Component.text("✦ " + target.getName() + " was revived back into the match by staff!", NamedTextColor.GREEN));
        return true;
    }
}
