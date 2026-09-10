package gg.crown.br.util;

import gg.crown.br.NightRoyalePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class LocationUtil {

    private LocationUtil() {}

    public static void write(NightRoyalePlugin plugin, String path, Location loc) {
        FileConfiguration c = plugin.getConfig();
        if (loc == null) {
            c.set(path, null);
            plugin.saveConfig();
            return;
        }
        c.set(path + ".world", loc.getWorld().getName());
        c.set(path + ".x", loc.getX());
        c.set(path + ".y", loc.getY());
        c.set(path + ".z", loc.getZ());
        c.set(path + ".yaw", (double) loc.getYaw());
        c.set(path + ".pitch", (double) loc.getPitch());
        plugin.saveConfig();
    }

    public static Location read(NightRoyalePlugin plugin, String path) {
        FileConfiguration c = plugin.getConfig();
        if (!c.isConfigurationSection(path)) {
            return null;
        }
        World w = Bukkit.getWorld(c.getString(path + ".world", ""));
        if (w == null) {
            return null;
        }
        return new Location(w, c.getDouble(path + ".x"), c.getDouble(path + ".y"), c.getDouble(path + ".z"),
                (float) c.getDouble(path + ".yaw"), (float) c.getDouble(path + ".pitch"));
    }

    public static Location readIgnoringWorld(NightRoyalePlugin plugin, String path) {
        FileConfiguration c = plugin.getConfig();
        if (!c.isConfigurationSection(path)) {
            return null;
        }
        return new Location(null, c.getDouble(path + ".x"), c.getDouble(path + ".y"), c.getDouble(path + ".z"),
                (float) c.getDouble(path + ".yaw"), (float) c.getDouble(path + ".pitch"));
    }

    public static boolean isInArena(NightRoyalePlugin plugin, Location loc) {
        Location a = LocationUtil.read(plugin, "arena-pos1");
        Location b = LocationUtil.read(plugin, "arena-pos2");
        if (a == null || b == null) {
            return true;
        }
        if (loc.getWorld() == null || !loc.getWorld().equals(a.getWorld())) {
            return false;
        }
        return loc.getX() >= Math.min(a.getX(), b.getX()) && loc.getX() <= Math.max(a.getX(), b.getX())
                && loc.getY() >= Math.min(a.getY(), b.getY()) && loc.getY() <= Math.max(a.getY(), b.getY())
                && loc.getZ() >= Math.min(a.getZ(), b.getZ()) && loc.getZ() <= Math.max(a.getZ(), b.getZ());
    }

    public static void addSpawn(NightRoyalePlugin plugin, Location loc) {
        FileConfiguration c = plugin.getConfig();
        List<String> spawns = c.getStringList("spawns");
        spawns.add(serialize(loc));
        c.set("spawns", spawns);
        plugin.saveConfig();
    }

    public static List<Location> readSpawns(NightRoyalePlugin plugin) {
        List<Location> out = new ArrayList<>();
        for (String s : plugin.getConfig().getStringList("spawns")) {
            Location l = deserialize(s);
            if (l != null) out.add(l);
        }
        return out;
    }

    public static void clearSpawns(NightRoyalePlugin plugin) {
        plugin.getConfig().set("spawns", new ArrayList<String>());
        plugin.saveConfig();
    }

    private static String serialize(Location l) {
        return l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getYaw() + "," + l.getPitch();
    }

    private static Location deserialize(String s) {
        String[] p = s.split(",");
        if (p.length < 6) {
            return null;
        }
        World w = Bukkit.getWorld(p[0]);
        if (w == null) {
            return null;
        }
        return new Location(w, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                Float.parseFloat(p[4]), Float.parseFloat(p[5]));
    }
}
