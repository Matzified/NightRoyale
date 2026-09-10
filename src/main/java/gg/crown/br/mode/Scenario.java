package gg.crown.br.mode;

public enum Scenario {
    // Universal
    NO_FALL_DAMAGE("No Fall Damage", false),
    CRATE("Gold Crate", false),
    NO_NATURAL_REGEN("No Natural Regen", false),
    HEALTH_INDICATORS("Health Indicators", false),
    LOCATOR_BAR("Locator Bar", false),
    OP("OP Chests", false),
    OLD_COMBAT("1.8 Old Combat", false),

    // Mace Royale Only
    SPEARS("Netherite Spears", true),
    ELYTRA("Elytra Loot", true),
    TWO_MACES("Two Maces", true),
    DOUBLE_ELYTRA("Double Elytra", true),
    WIND_RESET("Wind Reset Tech", true);

    private final String displayName;
    private final boolean maceOnly;

    Scenario(String displayName, boolean maceOnly) {
        this.displayName = displayName;
        this.maceOnly = maceOnly;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isMaceOnly() {
        return maceOnly;
    }

    public static Scenario fromKey(String key) {
        if (key == null) return null;
        String sanitized = key.trim().toUpperCase().replace("-", "_");
        for (Scenario scenario : values()) {
            if (scenario.name().equalsIgnoreCase(sanitized)) {
                return scenario;
            }
        }
        return null;
    }
}
