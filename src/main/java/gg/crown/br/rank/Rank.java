package gg.crown.br.rank;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum Rank {
    OWNER("Owner", "\uE100", true, NamedTextColor.DARK_RED, 100),
    MANAGER("Manager", "\uE101", true, NamedTextColor.RED, 90),
    HIGH_STAFF("High-Staff", "\uE102", true, NamedTextColor.GOLD, 80),
    STAFF("Staff", "\uE103", true, NamedTextColor.YELLOW, 70),
    HELPER("Helper", "\uE104", true, NamedTextColor.BLUE, 60),
    GAME_HOST("Game Host", "\uE105", false, NamedTextColor.DARK_PURPLE, 50),
    CREATOR("Creator", "\uE106", false, NamedTextColor.LIGHT_PURPLE, 40),
    MEDIA("Media", "\uE107", false, NamedTextColor.AQUA, 30),
    MIDNIGHT("Midnight", "\uE108", false, NamedTextColor.DARK_AQUA, 20),
    TWILIGHT("Twilight", "\uE109", false, NamedTextColor.DARK_GRAY, 10),
    MEMBER("Member", "", false, NamedTextColor.GRAY, 0);

    private final String displayName;
    private final String glyph;
    private final boolean staff;
    private final NamedTextColor color;
    private final int weight;

    Rank(String displayName, String glyph, boolean staff, NamedTextColor color, int weight) {
        this.displayName = displayName;
        this.glyph = glyph;
        this.staff = staff;
        this.color = color;
        this.weight = weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGlyph() {
        return glyph;
    }

    public boolean isStaff() {
        return staff;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public int getWeight() {
        return weight;
    }

    public Component getTagComponent() {
        if (glyph.isEmpty()) return Component.empty();
        return Component.text(glyph + " ").font(Key.key("nightroyale:main"));
    }

    public String getTabTeamName() {
        return String.format("%02d_%s", ordinal(), name().toLowerCase());
    }

    public static Rank fromString(String name) {
        if (name == null) return MEMBER;
        String sanitized = name.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (Rank rank : values()) {
            if (rank.name().equalsIgnoreCase(sanitized) || rank.displayName.equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return MEMBER;
    }
}
