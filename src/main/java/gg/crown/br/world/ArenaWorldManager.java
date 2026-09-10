package gg.crown.br.world;

import gg.crown.br.NightRoyalePlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;

public class ArenaWorldManager {

    private final NightRoyalePlugin plugin;
    private static final String TEMPLATE_WORLD_NAME = "nr_arena_template";
    private static final String MATCH_WORLD_NAME = "nr_arena_active";

    private World activeMatchWorld;

    public ArenaWorldManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public World getActiveMatchWorld() {
        return activeMatchWorld;
    }

    public boolean isMatchWorld(World world) {
        return activeMatchWorld != null && activeMatchWorld.equals(world);
    }

    public World createMatchWorld() {
        discardMatchWorld();

        File serverRoot = Bukkit.getWorldContainer();
        File templateDir = new File(serverRoot, TEMPLATE_WORLD_NAME);
        File matchDir = new File(serverRoot, MATCH_WORLD_NAME);

        if (templateDir.exists()) {
            copyDirectory(templateDir.toPath(), matchDir.toPath());
            // Delete uid.dat to avoid collision
            new File(matchDir, "uid.dat").delete();
        }

        WorldCreator creator = new WorldCreator(MATCH_WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        activeMatchWorld = creator.createWorld();

        if (activeMatchWorld != null) {
            applyForcedRules(activeMatchWorld);
        }

        return activeMatchWorld;
    }

    public void applyForcedRules(World world) {
        world.setAutoSave(false);
        world.setTime(18000); // Permanent night
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        // Random tick speed 0 globally on all worlds on server
        for (World w : Bukkit.getWorlds()) {
            w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        }
    }

    public void discardMatchWorld() {
        if (activeMatchWorld != null) {
            Location lobby = plugin.getLobbyLocation();
            for (Player p : activeMatchWorld.getPlayers()) {
                p.teleport(lobby != null ? lobby : Bukkit.getWorlds().get(0).getSpawnLocation());
            }
            Bukkit.unloadWorld(activeMatchWorld, false);
            activeMatchWorld = null;
        }

        File serverRoot = Bukkit.getWorldContainer();
        File matchDir = new File(serverRoot, MATCH_WORLD_NAME);
        if (matchDir.exists()) {
            deleteDirectory(matchDir.toPath());
        }
    }

    private void copyDirectory(Path source, Path target) {
        try {
            Files.walk(source).forEach(src -> {
                Path dest = target.resolve(source.relativize(src));
                try {
                    if (Files.isDirectory(src)) {
                        if (!Files.exists(dest)) Files.createDirectories(dest);
                    } else {
                        if (!src.getFileName().toString().equals("session.lock")) {
                            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (IOException ignored) {}
            });
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to copy template world: " + e.getMessage());
        }
    }

    private void deleteDirectory(Path path) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignored) {}
    }
}
