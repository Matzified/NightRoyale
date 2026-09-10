package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ModerationCommands implements CommandExecutor {

    private final NightRoyalePlugin plugin;

    public ModerationCommands(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("report")) {
            if (args.length < 2) {
                sender.sendMessage(Component.text("Usage: /report <player> <reason>", NamedTextColor.RED));
                return true;
            }
            String target = args[0];
            String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

            sender.sendMessage(Component.text("✔ Report submitted against " + target + ". Online staff have been alerted.", NamedTextColor.GREEN));

            Component staffAlert = Component.text("[REPORT] ", NamedTextColor.RED)
                    .append(Component.text(sender.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" reported ", NamedTextColor.GRAY))
                    .append(Component.text(target, NamedTextColor.RED))
                    .append(Component.text(": " + reason, NamedTextColor.WHITE));

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plugin.getRankManager().isStaff(p)) {
                    p.sendMessage(staffAlert);
                }
            }
            return true;
        }

        if (cmd.equals("mute")) {
            if (!sender.hasPermission("nightroyale.staff")) {
                sender.sendMessage(Component.text("You do not have permission to mute players.", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(Component.text("Usage: /mute <player> [minutes] [reason]", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }

            long durationMinutes = 15;
            String reason = "Breaking rules";

            if (args.length >= 2) {
                try {
                    durationMinutes = Long.parseLong(args[1]);
                    if (args.length >= 3) {
                        reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                    }
                } catch (NumberFormatException e) {
                    reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                }
            }

            plugin.getModerationManager().mutePlayer(target.getUniqueId(), durationMinutes * 60_000L, reason);
            sender.sendMessage(Component.text("✔ Muted " + target.getName() + " for " + durationMinutes + "m. Reason: " + reason, NamedTextColor.GREEN));
            target.sendMessage(Component.text("You have been muted for " + durationMinutes + "m. Reason: " + reason, NamedTextColor.RED));
            return true;
        }

        if (cmd.equals("unmute")) {
            if (!sender.hasPermission("nightroyale.staff")) {
                sender.sendMessage(Component.text("You do not have permission to unmute players.", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(Component.text("Usage: /unmute <player>", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }

            plugin.getModerationManager().unmutePlayer(target.getUniqueId());
            sender.sendMessage(Component.text("✔ Unmuted " + target.getName(), NamedTextColor.GREEN));
            target.sendMessage(Component.text("You have been unmuted.", NamedTextColor.GREEN));
            return true;
        }

        return true;
    }
}
