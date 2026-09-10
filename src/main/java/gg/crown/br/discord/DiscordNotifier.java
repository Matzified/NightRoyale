package gg.crown.br.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gg.crown.br.NightRoyalePlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class DiscordNotifier {

    private final NightRoyalePlugin plugin;
    private final HttpClient httpClient;

    public DiscordNotifier(NightRoyalePlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newHttpClient();
    }

    private String getWebhookUrl() {
        return plugin.getConfig().getString("discord.webhook-url", "");
    }

    private String getGamesRoleId() {
        return plugin.getConfig().getString("discord.games-role-id", "");
    }

    private String getServerIp() {
        return plugin.getConfig().getString("discord.server-ip", "play.nightroyale.net");
    }

    public void sendFiveMinuteWarning(int onlinePlayers, int purgeCap, String mode) {
        String url = getWebhookUrl();
        if (url == null || url.isBlank()) return;

        String roleId = getGamesRoleId();
        String content = (roleId != null && !roleId.isBlank()) ? "<@&" + roleId + ">" : "";

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "⚠️ PURGE STARTING IN 5 MINUTES!");
        embed.addProperty("description", "The purge countdown has entered the final 5 minutes. Connect now!");
        embed.addProperty("color", 0xFFA500); // Orange

        JsonArray fields = new JsonArray();
        fields.add(createField("Mode", mode, true));
        fields.add(createField("Players Online", onlinePlayers + " / " + purgeCap, true));
        fields.add(createField("Server IP", getServerIp(), true));
        embed.add("fields", fields);

        sendPayload(content, embed);
    }

    public void sendPurgeBegun(int onlinePlayers, int rosterSize, String mode, List<String> scenarios) {
        String url = getWebhookUrl();
        if (url == null || url.isBlank()) return;

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "⚔️ THE PURGE HAS BEGUN!");
        embed.addProperty("description", "Grace period is over. All combatants have been deployed into the arena!");
        embed.addProperty("color", 0xDC143C); // Crimson Red

        JsonArray fields = new JsonArray();
        fields.add(createField("Mode", mode, true));
        fields.add(createField("Match Roster", rosterSize + " Players", true));
        fields.add(createField("Scenarios", scenarios.isEmpty() ? "None" : String.join(", ", scenarios), false));
        embed.add("fields", fields);

        sendPayload("", embed);
    }

    public void sendGameOver(List<String> winnerNames, String teamMvp, String gameMvp, int gameMvpKills, long durationSeconds, String mode) {
        String url = getWebhookUrl();
        if (url == null || url.isBlank()) return;

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "🏆 MATCH COMPLETED — GAME OVER 🏆");
        embed.addProperty("color", 0x9B59B6); // Sleek Purple

        JsonArray fields = new JsonArray();

        // 1. Winners
        StringBuilder winnersStr = new StringBuilder();
        for (String winner : winnerNames) {
            winnersStr.append("• **").append(winner).append("**");
            if (teamMvp != null && winner.equalsIgnoreCase(teamMvp)) {
                winnersStr.append(" — *Team MVP* 🌟");
            }
            winnersStr.append("\n");
        }
        if (winnersStr.isEmpty()) winnersStr.append("None (Draw / All Eliminated)");
        fields.add(createField("👑 Winner(s)", winnersStr.toString(), false));

        // 2. Game MVP
        if (gameMvp != null && !gameMvp.isBlank()) {
            fields.add(createField("⚔️ Game MVP", "**" + gameMvp + "** with **" + gameMvpKills + " kills**", false));
        }

        // 3. Match Stats
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        fields.add(createField("Game Mode", mode, true));
        fields.add(createField("Duration", String.format("%02dm %02ds", minutes, seconds), true));

        embed.add("fields", fields);

        sendPayload("", embed);
    }

    private JsonObject createField(String name, String value, boolean inline) {
        JsonObject f = new JsonObject();
        f.addProperty("name", name);
        f.addProperty("value", value);
        f.addProperty("inline", inline);
        return f;
    }

    private void sendPayload(String content, JsonObject embed) {
        String url = getWebhookUrl();
        if (url == null || url.isBlank()) return;

        JsonObject payload = new JsonObject();
        if (content != null && !content.isBlank()) {
            payload.addProperty("content", content);
        }
        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(t -> {
                    plugin.getLogger().warning("Failed to send Discord webhook: " + t.getMessage());
                    return null;
                });
    }
}
