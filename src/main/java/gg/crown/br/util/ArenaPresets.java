package gg.crown.br.util;

import gg.crown.br.NightRoyalePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ArenaPresets {

    private ArenaPresets() {}

    private static File file(NightRoyalePlugin plugin) {
        return new File(plugin.getDataFolder(), "arenas.yml");
    }

    public static void save(NightRoyalePlugin plugin, String name) {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file(plugin));
        String base = "presets." + name.toLowerCase();
        FileConfiguration live = plugin.getConfig();
        copy(live, "arena-pos1", data, base + ".pos1");
        copy(live, "arena-pos2", data, base + ".pos2");
        copy(live, "arena", data, base + ".arena");
        copy(live, "storm-center", data, base + ".storm-center");
        data.set(base + ".spawns", live.getStringList("spawns"));
        write(plugin, data);
    }

    public static boolean load(NightRoyalePlugin plugin, String name) {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file(plugin));
        String base = "presets." + name.toLowerCase();
        if (!data.isConfigurationSection(base)) {
            return false;
        }
        FileConfiguration live = plugin.getConfig();
        copy(data, base + ".pos1", live, "arena-pos1");
        copy(data, base + ".pos2", live, "arena-pos2");
        copy(data, base + ".arena", live, "arena");
        copy(data, base + ".storm-center", live, "storm-center");
        live.set("spawns", data.getStringList(base + ".spawns"));
        plugin.saveConfig();
        plugin.loadLocations();
        return true;
    }

    public static List<String> list(NightRoyalePlugin plugin) {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file(plugin));
        ConfigurationSection section = data.getConfigurationSection("presets");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    private static void copy(FileConfiguration from, String fromPath, FileConfiguration to, String toPath) {
        if (!from.isConfigurationSection(fromPath)) {
            to.set(toPath, null);
            return;
        }
        to.set(toPath + ".world", from.getString(fromPath + ".world"));
        to.set(toPath + ".x", from.getDouble(fromPath + ".x"));
        to.set(toPath + ".y", from.getDouble(fromPath + ".y"));
        to.set(toPath + ".z", from.getDouble(fromPath + ".z"));
        to.set(toPath + ".yaw", from.getDouble(fromPath + ".yaw"));
        to.set(toPath + ".pitch", from.getDouble(fromPath + ".pitch"));
    }

    private static void write(NightRoyalePlugin plugin, FileConfiguration data) {
        try {
            data.save(file(plugin));
        } catch (IOException e) {
            plugin.getLogger().warning("Couldn't save arenas.yml: " + e.getMessage());
        }
    }
}
