package gg.crown.br.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import gg.crown.br.NightRoyalePlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class MatchRecorder {

    private final NightRoyalePlugin plugin;
    private final File matchesFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public MatchRecorder(NightRoyalePlugin plugin) {
        this.plugin = plugin;
        this.matchesFile = new File(plugin.getDataFolder(), "matches.json");
    }

    public record BoardEntry(String name, String uuid, int kills, int place, int rating) {}

    public record MatchEntry(
            int game,
            long time,
            int players,
            String winner,
            List<BoardEntry> board
    ) {}

    public void recordMatch(MatchEntry entry) {
        List<MatchEntry> entries = loadMatches();
        entries.add(0, entry); // Prepend newest

        // Keep last 100 matches
        if (entries.size() > 100) {
            entries = new ArrayList<>(entries.subList(0, 100));
        }

        saveMatches(entries);
    }

    public List<MatchEntry> loadMatches() {
        if (!matchesFile.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(matchesFile)) {
            Type listType = new TypeToken<ArrayList<MatchEntry>>(){}.getType();
            List<MatchEntry> list = gson.fromJson(reader, listType);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load matches.json: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveMatches(List<MatchEntry> entries) {
        try {
            if (!matchesFile.exists()) {
                matchesFile.getParentFile().mkdirs();
                matchesFile.createNewFile();
            }
            try (FileWriter writer = new FileWriter(matchesFile)) {
                gson.toJson(entries, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save matches.json: " + e.getMessage());
        }
    }
}
