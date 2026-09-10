package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.state.MatchState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LobbyCommand implements CommandExecutor {

    private final NightRoyalePlugin plugin;

    public LobbyCommand(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        if (plugin.getSpectatorManager().isSpectator(player)) {
            plugin.getSpectatorManager().returnToLobby(player);
            return true;
        }

        MatchState state = plugin.getMatchManager().getState();
        if (state == MatchState.ACTIVE && plugin.getMatchManager().isAliveCombatant(player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
                plugin.getMatchManager().handleCombatantDisconnect(player);
                plugin.getSpectatorManager().returnToLobby(player);
                player.sendMessage(Component.text("You forfeited the match and returned to the lobby.", NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("You are currently alive in an active game! Type /" + label + " confirm to forfeit and return to the lobby.", NamedTextColor.RED));
            }
            return true;
        }

        // Out of match or already waiting
        plugin.getSpectatorManager().returnToLobby(player);
        return true;
    }
}
