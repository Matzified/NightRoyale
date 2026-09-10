package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.loot.ChestTier;
import gg.crown.br.mode.GameMode;
import gg.crown.br.mode.Scenario;
import gg.crown.br.state.MatchState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NightRoyaleCommand implements CommandExecutor, TabCompleter {

    private final NightRoyalePlugin plugin;

    public NightRoyaleCommand(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("nightroyale.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("--- Night Royale Admin Commands ---", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/nr open / close — Toggle login whitelist gate", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr match <key=val...> — Dispatch match parameters from bot", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr begin / stop — Force start or cancel active match", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr chest refill — Refill all chests in arena", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr setlobby / setarena / setstormcenter — Location markers", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr ghaststart / ghastend — Flight vector markers", NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "open" -> {
                plugin.getLoginGateManager().setOpen(true);
                sender.sendMessage(Component.text("✔ Login gate is now OPEN. Public can connect.", NamedTextColor.GREEN));
                return true;
            }
            case "close" -> {
                plugin.getLoginGateManager().setOpen(false);
                sender.sendMessage(Component.text("✔ Login gate is now CLOSED. Non-admins will be turned away.", NamedTextColor.RED));
                return true;
            }
            case "begin" -> {
                plugin.getMatchManager().transitionTo(MatchState.ACTIVE);
                sender.sendMessage(Component.text("⚡ Match countdown skipped! ACTIVE battle started.", NamedTextColor.GREEN));
                return true;
            }
            case "stop" -> {
                plugin.getMatchManager().transitionTo(MatchState.RESET);
                sender.sendMessage(Component.text("🛑 Match cancelled. Resetting arena.", NamedTextColor.RED));
                return true;
            }
            case "setlobby" -> {
                if (!(sender instanceof Player p)) return true;
                plugin.setLobbyLocation(p.getLocation());
                sender.sendMessage(Component.text("✔ Lobby location set to your current position.", NamedTextColor.GREEN));
                return true;
            }
            case "setarena" -> {
                if (!(sender instanceof Player p)) return true;
                plugin.setArenaLocation(p.getLocation());
                sender.sendMessage(Component.text("✔ Arena location set to your current position.", NamedTextColor.GREEN));
                return true;
            }
            case "setstormcenter" -> {
                if (!(sender instanceof Player p)) return true;
                plugin.getStormManager().setStormCenter(p.getLocation());
                plugin.saveStormCenterLocation(p.getLocation());
                sender.sendMessage(Component.text("✔ Storm center set to your current position.", NamedTextColor.GREEN));
                return true;
            }
            case "ghaststart" -> {
                if (!(sender instanceof Player p)) return true;
                plugin.setGhastStartLocation(p.getLocation());
                sender.sendMessage(Component.text("✔ Ghast bus start point set.", NamedTextColor.GREEN));
                return true;
            }
            case "ghastend" -> {
                if (!(sender instanceof Player p)) return true;
                plugin.setGhastEndLocation(p.getLocation());
                sender.sendMessage(Component.text("✔ Ghast bus end point set.", NamedTextColor.GREEN));
                return true;
            }
            case "match" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /nr match <key=value...>", NamedTextColor.RED));
                    return true;
                }
                Map<String, String> params = new HashMap<>();
                for (int i = 1; i < args.length; i++) {
                    String[] split = args[i].split("=", 2);
                    if (split.length == 2) {
                        params.put(split[0].toLowerCase(), split[1]);
                    }
                }
                plugin.getMatchManager().configureAndStartMatch(params);
                sender.sendMessage(Component.text("✔ Configured match settings from command (" + params.size() + " parameters).", NamedTextColor.GREEN));
                return true;
            }
            case "chest" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("refill")) {
                    plugin.getChestManager().refillAllChests(
                            plugin.getMatchManager().getCurrentMode(),
                            plugin.getScenarioManager().getActiveScenarios()
                    );
                    sender.sendMessage(Component.text("✔ Refilled all registered chests in arena.", NamedTextColor.GREEN));
                    return true;
                }
            }
            case "addchest" -> {
                if (!(sender instanceof Player p)) return true;
                Block target = p.getTargetBlockExact(5);
                if (target == null || !(target.getState() instanceof org.bukkit.block.Chest)) {
                    sender.sendMessage(Component.text("Please look directly at a chest block.", NamedTextColor.RED));
                    return true;
                }
                ChestTier tier = ChestTier.TIER_1;
                if (args.length >= 2) {
                    try {
                        tier = ChestTier.valueOf("TIER_" + args[1]);
                    } catch (IllegalArgumentException ignored) {}
                }
                plugin.getChestManager().registerChest(target.getLocation(), tier);
                sender.sendMessage(Component.text("✔ Registered " + tier.getStars() + " chest.", NamedTextColor.GREEN));
                return true;
            }
            case "clearitems" -> {
                if (plugin.getArenaWorldManager().getActiveMatchWorld() != null) {
                    int removed = plugin.getFoliageGuardian().clearItems(plugin.getArenaWorldManager().getActiveMatchWorld());
                    sender.sendMessage(Component.text("✔ Removed " + removed + " dropped items from match world.", NamedTextColor.GREEN));
                }
                return true;
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getRankManager().loadRanks();
                sender.sendMessage(Component.text("✔ Configuration reloaded successfully.", NamedTextColor.GREEN));
                return true;
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("open", "close", "begin", "stop", "match", "chest", "addchest", "setlobby", "setarena", "setstormcenter", "ghaststart", "ghastend", "clearitems", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("chest")) {
            return List.of("refill");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("addchest")) {
            return List.of("1", "2", "3", "4");
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).toList();
    }
}
