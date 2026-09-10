package gg.crown.br.mode;

public enum GameMode {
    SMP("SMP Royale", "SMP"),
    MACE("Mace Royale", "MACE"),
    CART("Cart Royale", "CART"),
    UHC("UHC Royale", "UHC");

    private final String displayName;
    private final String key;

    GameMode(String displayName, String key) {
        this.displayName = displayName;
        this.key = key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getKey() {
        return key;
    }

    public static GameMode fromString(String input) {
        if (input == null || input.isBlank()) return SMP;
        String sanitized = input.trim().toUpperCase()
                .replace("ROYALE", "")
                .replace(" ", "")
                .replace("_", "");
        for (GameMode mode : values()) {
            if (mode.name().equalsIgnoreCase(sanitized) || mode.key.equalsIgnoreCase(sanitized)) {
                return mode;
            }
        }
        return SMP;
    }
}
