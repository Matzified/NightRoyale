package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.loot.ChestTier;
import gg.crown.br.mode.GameMode;
import gg.crown.br.mode.Scenario;
import gg.crown.br.state.MatchState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
            sender.sendMessage(Component.text("/nr storm <start|stop|shrink|size|center> — Storm controls", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr spectate [player] — Toggle spectator mode with GUI & IGN search", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr motd — Preview current server list ping MOTD", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr test <kit|chest|upgrade|crate|ghast|storm|spectate|webhook|motd> — Dev test suite", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr chest refill — Refill all chests in arena", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr setlobby / setarena / setstormcenter — Location markers", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr pos1 / pos2 — Set arena bounding box positions", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr addspawn / clearspawns — Manage arena spawn points", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr arena <build|save|saveas|use|list> — Multi-arena presets", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr degrass — Clear decorative grass/ferns from arena bounds", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/nr ghaststart / ghastend / clearghast — Flight vector markers", NamedTextColor.YELLOW));
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
            case "clearghast" -> {
                plugin.setGhastStartLocation(null);
                plugin.setGhastEndLocation(null);
                sender.sendMessage(Component.text("✔ Cleared ghast bus flight path.", NamedTextColor.GREEN));
                return true;
            }
            case "pos1" -> {
                if (!(sender instanceof Player p)) return true;
                plugin.setArenaPos1(p.getLocation());
                sender.sendMessage(Component.text("✔ Arena position 1 set.", NamedTextColor.GREEN));
                return true;
            }
            case "pos2" -> {
                if (!(sender instanceof Player p)) return true;
                plugin.setArenaPos2(p.getLocation());
                sender.sendMessage(Component.text("✔ Arena position 2 set.", NamedTextColor.GREEN));
                return true;
            }
            case "addspawn" -> {
                if (!(sender instanceof Player p)) return true;
                gg.crown.br.util.LocationUtil.addSpawn(plugin, p.getLocation());
                sender.sendMessage(Component.text("✔ Added spawn location (" + p.getLocation().getBlockX() + ", " + p.getLocation().getBlockY() + ", " + p.getLocation().getBlockZ() + ").", NamedTextColor.GREEN));
                return true;
            }
            case "clearspawns" -> {
                gg.crown.br.util.LocationUtil.clearSpawns(plugin);
                sender.sendMessage(Component.text("✔ Cleared all spawn locations.", NamedTextColor.GREEN));
                return true;
            }
            case "arena" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /nr arena <build|save|saveas|use|list> [name]", NamedTextColor.RED));
                    return true;
                }
                String arenaSub = args[1].toLowerCase();
                switch (arenaSub) {
                    case "build" -> {
                        if (!(sender instanceof Player p)) return true;
                        World template = plugin.getArenaWorldManager().getTemplateWorld();
                        if (template != null) {
                            p.teleport(template.getSpawnLocation());
                            sender.sendMessage(Component.text("✔ Teleported to arena template world (nr_arena_template).", NamedTextColor.GREEN));
                        } else {
                            sender.sendMessage(Component.text("Arena template world is not loaded.", NamedTextColor.RED));
                        }
                        return true;
                    }
                    case "save" -> {
                        World template = plugin.getArenaWorldManager().getTemplateWorld();
                        if (template != null) {
                            template.save();
                            sender.sendMessage(Component.text("✔ Arena template world saved successfully.", NamedTextColor.GREEN));
                        }
                        return true;
                    }
                    case "saveas" -> {
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /nr arena saveas <name>", NamedTextColor.RED));
                            return true;
                        }
                        String name = args[2].toLowerCase();
                        gg.crown.br.util.ArenaPresets.save(plugin, name);
                        sender.sendMessage(Component.text("✔ Saved current arena configuration as preset '" + name + "'.", NamedTextColor.GREEN));
                        return true;
                    }
                    case "use" -> {
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /nr arena use <name>", NamedTextColor.RED));
                            return true;
                        }
                        String name = args[2].toLowerCase();
                        if (gg.crown.br.util.ArenaPresets.load(plugin, name)) {
                            sender.sendMessage(Component.text("✔ Loaded arena preset '" + name + "'.", NamedTextColor.GREEN));
                        } else {
                            sender.sendMessage(Component.text("Preset '" + name + "' not found. Use /nr arena list to view presets.", NamedTextColor.RED));
                        }
                        return true;
                    }
                    case "list" -> {
                        List<String> presets = gg.crown.br.util.ArenaPresets.list(plugin);
                        sender.sendMessage(Component.text("Available Arena Presets: " + (presets.isEmpty() ? "None" : String.join(", ", presets)), NamedTextColor.LIGHT_PURPLE));
                        return true;
                    }
                    default -> {
                        sender.sendMessage(Component.text("Unknown arena action: " + arenaSub, NamedTextColor.RED));
                        return true;
                    }
                }
            }
            case "degrass" -> {
                Location a = plugin.getArenaPos1();
                Location b = plugin.getArenaPos2();
                if (a == null || b == null) {
                    sender.sendMessage(Component.text("Please set /nr pos1 and /nr pos2 first.", NamedTextColor.RED));
                    return true;
                }
                int removed = plugin.getFoliageGuardian().degrass(a, b);
                sender.sendMessage(Component.text("✔ Cleared " + removed + " grass/fern blocks from arena bounds.", NamedTextColor.GREEN));
                return true;
            }
            case "teamsize" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /nr teamsize <solo|duos|trios>", NamedTextColor.RED));
                    return true;
                }
                int size = switch (args[1].toUpperCase()) {
                    case "DUOS", "2" -> 2;
                    case "TRIOS", "3" -> 3;
                    default -> 1;
                };
                plugin.getTeamManager().setMaxTeamSize(size);
                sender.sendMessage(Component.text("✔ Max team size set to: " + size, NamedTextColor.GREEN));
                return true;
            }
            case "mode" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /nr mode <smp|mace|cart|uhc>", NamedTextColor.RED));
                    return true;
                }
                GameMode mode = GameMode.fromString(args[1]);
                sender.sendMessage(Component.text("✔ Default mode set to: " + mode.name(), NamedTextColor.GREEN));
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
            case "storm" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /nr storm <start|stop|shrink|size|center> [args...]", NamedTextColor.RED));
                    return true;
                }
                String stormSub = args[1].toLowerCase();
                switch (stormSub) {
                    case "start" -> {
                        World w = (sender instanceof Player p) ? p.getWorld() : plugin.getArenaWorldManager().getActiveMatchWorld();
                        if (w == null) {
                            sender.sendMessage(Component.text("No active world found to start storm on.", NamedTextColor.RED));
                            return true;
                        }
                        int durationMins = 10;
                        if (args.length >= 3) {
                            try { durationMins = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                        }
                        Location center = plugin.getStormManager().getStormCenter();
                        if (center == null && sender instanceof Player p) {
                            center = p.getLocation();
                            plugin.getStormManager().setStormCenter(center);
                        }
                        plugin.getStormManager().setupStorm(w, center, 550.0, 20.0);
                        plugin.getStormManager().startClosing(durationMins);
                        sender.sendMessage(Component.text("✔ Storm initialized and closing over " + durationMins + " minutes!", NamedTextColor.GREEN));
                        return true;
                    }
                    case "stop" -> {
                        plugin.getStormManager().stop();
                        sender.sendMessage(Component.text("✔ Storm stopped.", NamedTextColor.YELLOW));
                        return true;
                    }
                    case "shrink" -> {
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /nr storm shrink <targetRadius> [seconds]", NamedTextColor.RED));
                            return true;
                        }
                        try {
                            double rad = Double.parseDouble(args[2]);
                            long secs = args.length >= 4 ? Long.parseLong(args[3]) : 15L;
                            if (plugin.getStormManager().getStormCenter() == null && sender instanceof Player p) {
                                plugin.getStormManager().setupStorm(p.getWorld(), p.getLocation(), plugin.getStormManager().getCurrentRadius(), 20.0);
                            }
                            plugin.getStormManager().shrinkTo(rad, secs);
                            sender.sendMessage(Component.text("✔ Storm shrinking to " + rad + "m over " + secs + "s!", NamedTextColor.GREEN));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("Invalid radius or seconds number.", NamedTextColor.RED));
                        }
                        return true;
                    }
                    case "size" -> {
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /nr storm size <radius>", NamedTextColor.RED));
                            return true;
                        }
                        try {
                            double rad = Double.parseDouble(args[2]);
                            if (plugin.getStormManager().getStormCenter() == null && sender instanceof Player p) {
                                plugin.getStormManager().setupStorm(p.getWorld(), p.getLocation(), rad, 20.0);
                            }
                            plugin.getStormManager().setRadius(rad);
                            sender.sendMessage(Component.text("✔ Storm radius set to " + rad + "m (diameter " + (rad * 2) + "m).", NamedTextColor.GREEN));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("Invalid radius number.", NamedTextColor.RED));
                        }
                        return true;
                    }
                    case "center" -> {
                        if (sender instanceof Player p) {
                            plugin.getStormManager().setStormCenter(p.getLocation());
                            plugin.saveStormCenterLocation(p.getLocation());
                            sender.sendMessage(Component.text("✔ Storm center set to (" + p.getLocation().getBlockX() + ", " + p.getLocation().getBlockZ() + ")", NamedTextColor.GREEN));
                        } else {
                            Location c = plugin.getStormManager().getStormCenter();
                            sender.sendMessage(Component.text("Current storm center: " + (c != null ? c.getBlockX() + ", " + c.getBlockZ() : "Not set"), NamedTextColor.YELLOW));
                        }
                        return true;
                    }
                    default -> {
                        sender.sendMessage(Component.text("Unknown storm action: " + stormSub, NamedTextColor.RED));
                        return true;
                    }
                }
            }
            case "spectate" -> {
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(Component.text("Console must specify a player.", NamedTextColor.RED));
                    return true;
                }
                plugin.getSpectatorManager().toggleSpectator(target);
                sender.sendMessage(Component.text("✔ Toggled spectator mode for " + target.getName(), NamedTextColor.GREEN));
                return true;
            }
            case "motd" -> {
                Component motd = plugin.getMotdManager().buildMotd();
                sender.sendMessage(Component.text("--- Server MOTD Preview ---", NamedTextColor.GOLD));
                sender.sendMessage(motd);
                return true;
            }
            case "test" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("--- Night Royale Test Commands ---", NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("/nr test kit <smp|mace|cart|uhc> — Equip kit directly", NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("/nr test chest <1|2|3|4> — Open virtual rolled loot chest", NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("/nr test upgrade — Test armor upgrade progression", NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("/nr test crate — Spawn a gold crate drop nearby", NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("/nr test ghast — Launch test battle bus deployment", NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("/nr test storm <radius> [seconds] — Test storm shrink", NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("/nr test spectate — Toggle spectator mode with GUI & IGN search", NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("/nr test webhook <5m|purge|gameover> — Test Discord notification", NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("/nr test hazard — Test hazard effects", NamedTextColor.YELLOW));
                    return true;
                }
                String testSub = args[1].toLowerCase();
                switch (testSub) {
                    case "kit" -> {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
                            return true;
                        }
                        if (args.length < 3) {
                            p.sendMessage(Component.text("Usage: /nr test kit <smp|mace|cart|uhc>", NamedTextColor.RED));
                            return true;
                        }
                        try {
                            GameMode mode = GameMode.valueOf(args[2].toUpperCase());
                            plugin.getKitManager().equipKit(p, mode);
                            p.sendMessage(Component.text("✔ Equipped kit for mode: " + mode.name(), NamedTextColor.GREEN));
                        } catch (IllegalArgumentException e) {
                            p.sendMessage(Component.text("Unknown mode! Available: smp, mace, cart, uhc", NamedTextColor.RED));
                        }
                        return true;
                    }
                    case "chest" -> {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
                            return true;
                        }
                        int tierNum = 1;
                        if (args.length >= 3) {
                            try { tierNum = Math.max(1, Math.min(4, Integer.parseInt(args[2]))); } catch (NumberFormatException ignored) {}
                        }
                        ChestTier tier = ChestTier.valueOf("TIER_" + tierNum);
                        Inventory inv = plugin.getChestManager().createPreviewChest(
                                tier,
                                plugin.getMatchManager().getCurrentMode(),
                                plugin.getScenarioManager().getActiveScenarios()
                        );
                        p.openInventory(inv);
                        p.sendMessage(Component.text("✔ Opened rolled preview of " + tier.getStars() + " chest.", NamedTextColor.GREEN));
                        return true;
                    }
                    case "upgrade" -> {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
                            return true;
                        }
                        if (p.getInventory().getHelmet() == null) {
                            p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
                            p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                            p.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                            p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
                        }
                        boolean upgraded = plugin.getArmorUpgradeManager().triggerUpgrade(p);
                        if (upgraded) {
                            p.sendMessage(Component.text("✔ Successfully upgraded one armor piece!", NamedTextColor.GREEN));
                        } else {
                            p.sendMessage(Component.text("Could not upgrade armor (all pieces may already be Netherite).", NamedTextColor.RED));
                        }
                        return true;
                    }
                    case "crate" -> {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
                            return true;
                        }
                        plugin.getGoldCrateManager().spawnCrate(p.getLocation());
                        p.sendMessage(Component.text("✔ Spawned Gold Crate at your location.", NamedTextColor.GOLD));
                        return true;
                    }
                    case "ghast" -> {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
                            return true;
                        }
                        Location start = plugin.getGhastStartLocation();
                        Location end = plugin.getGhastEndLocation();
                        plugin.getDeploymentManager().deployPlayers(List.of(p), start, end);
                        p.sendMessage(Component.text("✔ Launched test ghast battle bus deployment.", NamedTextColor.GREEN));
                        return true;
                    }
                    case "storm" -> {
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /nr test storm <radius> [seconds]", NamedTextColor.RED));
                            return true;
                        }
                        try {
                            double rad = Double.parseDouble(args[2]);
                            long secs = args.length >= 4 ? Long.parseLong(args[3]) : 15L;
                            if (sender instanceof Player p && plugin.getStormManager().getStormCenter() == null) {
                                plugin.getStormManager().setupStorm(p.getWorld(), p.getLocation(), plugin.getStormManager().getCurrentRadius(), 20.0);
                            }
                            plugin.getStormManager().shrinkTo(rad, secs);
                            sender.sendMessage(Component.text("✔ Storm test shrinking to " + rad + "m over " + secs + "s.", NamedTextColor.GREEN));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("Invalid radius number.", NamedTextColor.RED));
                        }
                        return true;
                    }
                    case "spectate" -> {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
                            return true;
                        }
                        plugin.getSpectatorManager().toggleSpectator(p);
                        return true;
                    }
                    case "webhook" -> {
                        String type = args.length >= 3 ? args[2].toLowerCase() : "5m";
                        switch (type) {
                            case "5m" -> {
                                plugin.getDiscordNotifier().sendFiveMinuteWarning(Bukkit.getOnlinePlayers().size(), 100, plugin.getMatchManager().getCurrentMode().name());
                                sender.sendMessage(Component.text("✔ Sent 5-minute warning webhook.", NamedTextColor.GREEN));
                            }
                            case "purge" -> {
                                plugin.getDiscordNotifier().sendPurgeBegun(Bukkit.getOnlinePlayers().size(), Bukkit.getOnlinePlayers().size(), plugin.getMatchManager().getCurrentMode().name(), List.of("OP_POTIONS", "OLD_COMBAT"));
                                sender.sendMessage(Component.text("✔ Sent Purge Begun webhook.", NamedTextColor.GREEN));
                            }
                            case "gameover" -> {
                                plugin.getDiscordNotifier().sendGameOver(List.of("PlayerOne", "PlayerTwo"), "PlayerOne", "PlayerOne", 8, 480L, plugin.getMatchManager().getCurrentMode().name());
                                sender.sendMessage(Component.text("✔ Sent Game Over webhook.", NamedTextColor.GREEN));
                            }
                            default -> sender.sendMessage(Component.text("Unknown webhook type: " + type + " (Use 5m, purge, gameover)", NamedTextColor.RED));
                        }
                        return true;
                    }
                    case "hazard" -> {
                        if (sender instanceof Player p) {
                            p.getWorld().strikeLightning(p.getLocation().add(5, 0, 5));
                            p.sendMessage(Component.text("✔ Hazard visual test dispatched.", NamedTextColor.YELLOW));
                        }
                        return true;
                    }
                    case "motd" -> {
                        Component motd = plugin.getMotdManager().buildMotd();
                        sender.sendMessage(Component.text("--- Server MOTD Preview ---", NamedTextColor.GOLD));
                        sender.sendMessage(motd);
                        return true;
                    }
                    default -> {
                        sender.sendMessage(Component.text("Unknown test subcommand: " + testSub, NamedTextColor.RED));
                        return true;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("open", "close", "begin", "stop", "match", "storm", "spectate", "motd", "test", "chest", "addchest", "setlobby", "setarena", "setstormcenter", "ghaststart", "ghastend", "clearghast", "pos1", "pos2", "addspawn", "clearspawns", "arena", "degrass", "teamsize", "mode", "clearitems", "reload"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("storm")) {
                return filter(List.of("start", "stop", "shrink", "size", "center"), args[1]);
            }
            if (args[0].equalsIgnoreCase("spectate")) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
            }
            if (args[0].equalsIgnoreCase("test")) {
                return filter(List.of("kit", "chest", "upgrade", "crate", "ghast", "storm", "spectate", "webhook", "hazard", "motd"), args[1]);
            }
            if (args[0].equalsIgnoreCase("chest")) {
                return List.of("refill");
            }
            if (args[0].equalsIgnoreCase("addchest")) {
                return List.of("1", "2", "3", "4");
            }
            if (args[0].equalsIgnoreCase("arena")) {
                return filter(List.of("build", "save", "saveas", "use", "list"), args[1]);
            }
            if (args[0].equalsIgnoreCase("teamsize")) {
                return filter(List.of("solo", "duos", "trios"), args[1]);
            }
            if (args[0].equalsIgnoreCase("mode")) {
                return filter(List.of("smp", "mace", "cart", "uhc"), args[1]);
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("test")) {
                if (args[1].equalsIgnoreCase("kit")) {
                    return filter(List.of("smp", "mace", "cart", "uhc"), args[2]);
                }
                if (args[1].equalsIgnoreCase("chest")) {
                    return filter(List.of("1", "2", "3", "4"), args[2]);
                }
                if (args[1].equalsIgnoreCase("webhook")) {
                    return filter(List.of("5m", "purge", "gameover"), args[2]);
                }
                if (args[1].equalsIgnoreCase("storm")) {
                    return filter(List.of("50", "100", "200", "300"), args[2]);
                }
            }
            if (args[0].equalsIgnoreCase("storm")) {
                if (args[1].equalsIgnoreCase("shrink") || args[1].equalsIgnoreCase("size")) {
                    return filter(List.of("50", "100", "200", "300"), args[2]);
                }
                if (args[1].equalsIgnoreCase("start")) {
                    return filter(List.of("5", "10", "15", "20"), args[2]);
                }
            }
        }
        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("storm") && args[1].equalsIgnoreCase("shrink")) {
                return filter(List.of("10", "15", "30", "60"), args[3]);
            }
            if (args[0].equalsIgnoreCase("test") && args[1].equalsIgnoreCase("storm")) {
                return filter(List.of("10", "15", "30", "60"), args[3]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).toList();
    }
}
