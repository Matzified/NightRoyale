package gg.crown.br.loot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum ChestTier {
    TIER_1("★", TextColor.color(0xCD, 0x7F, 0x32), 1),       // Bronze
    TIER_2("★★", TextColor.color(0xC0, 0xC0, 0xC0), 2),     // Silver
    TIER_3("★★★", TextColor.color(0xFF, 0xD7, 0x00), 3),    // Gold
    TIER_4("★★★★", TextColor.color(0xE0, 0x85, 0xFF), 4);  // Light Purple / Mythic

    private final String stars;
    private final TextColor color;
    private final int level;

    ChestTier(String stars, TextColor color, int level) {
        this.stars = stars;
        this.color = color;
        this.level = level;
    }

    public String getStars() {
        return stars;
    }

    public TextColor getColor() {
        return color;
    }

    public int getLevel() {
        return level;
    }

    public Component getHologram() {
        return Component.text(stars, color).decorate(TextDecoration.BOLD);
    }

    public ChestTier nextTier() {
        return switch (this) {
            case TIER_1 -> TIER_2;
            case TIER_2 -> TIER_3;
            case TIER_3 -> TIER_4;
            case TIER_4 -> TIER_4;
        };
    }
}
