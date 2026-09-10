package gg.crown.br.loot;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.mode.GameMode;
import gg.crown.br.mode.Scenario;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ChestManager {

    private final NightRoyalePlugin plugin;
    private final NamespacedKey holoKey;
    private final NamespacedKey spearKey;
    private final NamespacedKey armorUpgradeKey;
    private final NamespacedKey goldenHeadKey;

    private final Map<Location, ChestTier> registeredChests = new HashMap<>();
    private final Map<Location, TextDisplay> activeHolograms = new HashMap<>();

    public ChestManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
        this.holoKey = new NamespacedKey(plugin, "chest_holo");
        this.spearKey = new NamespacedKey(plugin, "netherite_spear");
        this.armorUpgradeKey = new NamespacedKey(plugin, "armor_upgrade");
        this.goldenHeadKey = new NamespacedKey(plugin, "golden_head");
    }

    public NamespacedKey getSpearKey() {
        return spearKey;
    }

    public NamespacedKey getArmorUpgradeKey() {
        return armorUpgradeKey;
    }

    public NamespacedKey getGoldenHeadKey() {
        return goldenHeadKey;
    }

    public void registerChest(Location loc, ChestTier tier) {
        registeredChests.put(loc.getBlock().getLocation(), tier);
    }

    public void clearAll() {
        activeHolograms.values().forEach(TextDisplay::remove);
        activeHolograms.clear();
        registeredChests.clear();
    }

    public void refillAllChests(GameMode mode, Set<Scenario> scenarios) {
        for (Map.Entry<Location, ChestTier> entry : registeredChests.entrySet()) {
            Location loc = entry.getKey();
            ChestTier tier = entry.getValue();

            Block block = loc.getBlock();
            if (block.getState() instanceof Chest chest) {
                fillChest(chest.getBlockInventory(), tier, mode, scenarios);
                updateHologram(loc, tier);
            }
        }
    }

    private void updateHologram(Location loc, ChestTier tier) {
        TextDisplay display = activeHolograms.get(loc);
        if (display == null || !display.isValid()) {
            Location spawnLoc = loc.clone().add(0.5, 1.2, 0.5);
            display = loc.getWorld().spawn(spawnLoc, TextDisplay.class, d -> {
                d.text(tier.getHologram());
                d.setBillboard(Display.Billboard.CENTER);
                d.getPersistentDataContainer().set(holoKey, PersistentDataType.BYTE, (byte) 1);
            });
            activeHolograms.put(loc, display);
        } else {
            display.text(tier.getHologram());
        }
    }

    public void fillChest(Inventory inv, ChestTier originalTier, GameMode mode, Set<Scenario> scenarios) {
        inv.clear();
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // OP scenario rolls 1 tier higher
        ChestTier effectiveTier = scenarios.contains(Scenario.OP) ? originalTier.nextTier() : originalTier;

        List<ItemStack> items = new ArrayList<>();
        switch (mode) {
            case SMP -> generateSmpLoot(items, effectiveTier, rand);
            case MACE -> generateMaceLoot(items, effectiveTier, scenarios, rand);
            case CART -> generateCartLoot(items, effectiveTier, rand);
            case UHC -> generateUhcLoot(items, effectiveTier, rand);
        }

        // Apply Universal Book Cap: 1★/2★ = max 1 book, 3★/4★ = max 2 books
        int maxBooks = (effectiveTier.getLevel() <= 2) ? 1 : 2;
        int bookCount = 0;
        Iterator<ItemStack> it = items.iterator();
        while (it.hasNext()) {
            ItemStack item = it.next();
            if (item.getType() == Material.ENCHANTED_BOOK) {
                bookCount++;
                if (bookCount > maxBooks) {
                    it.remove();
                }
            }
        }

        // Place items in random slots
        int invSize = inv.getSize();
        List<Integer> slots = new ArrayList<>(invSize);
        for (int i = 0; i < invSize; i++) slots.add(i);
        Collections.shuffle(slots, rand);

        for (int i = 0; i < items.size() && i < slots.size(); i++) {
            inv.setItem(slots.get(i), items.get(i));
        }
    }

    public Inventory createPreviewChest(ChestTier tier, GameMode mode, Set<Scenario> scenarios) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(tier.getStars() + " Chest Preview (" + mode.name() + ")", tier.getColor()).decorate(TextDecoration.BOLD));
        fillChest(inv, tier, mode, scenarios);
        return inv;
    }

    private void generateSmpLoot(List<ItemStack> items, ChestTier tier, ThreadLocalRandom rand) {
        // Golden apples have a chance across all tiers
        int gapples = rand.nextInt(0, 10);
        if (gapples > 0) items.add(new ItemStack(Material.GOLDEN_APPLE, gapples));

        int pearls = rand.nextInt(0, 6);
        if (pearls > 0) items.add(new ItemStack(Material.ENDER_PEARL, pearls));

        if (rand.nextBoolean()) addBuffPotion(items, true, rand);

        if (tier == ChestTier.TIER_1) {
            if (rand.nextDouble() < 0.50) items.add(createEnchantBook(Enchantment.SHARPNESS, 1));
        } else if (tier == ChestTier.TIER_2) {
            if (rand.nextDouble() < 0.50) items.add(createEnchantBook(Enchantment.SHARPNESS, 1));
            if (rand.nextDouble() < 0.15) items.add(createEnchantBook(Enchantment.FIRE_ASPECT, 1));
            if (rand.nextDouble() < 0.05) items.add(new ItemStack(Material.TOTEM_OF_UNDYING));
            if (rand.nextDouble() < 0.05) items.add(createArmorUpgradeItem());
        } else { // 3★ & 4★
            // Guaranteed premium (totem or armor upgrade, ~35% second)
            items.add(rand.nextBoolean() ? new ItemStack(Material.TOTEM_OF_UNDYING) : createArmorUpgradeItem());
            if (rand.nextDouble() < 0.35) {
                items.add(rand.nextBoolean() ? new ItemStack(Material.TOTEM_OF_UNDYING) : createArmorUpgradeItem());
            }
            if (rand.nextDouble() < 0.20) items.add(createEnchantBook(Enchantment.SHARPNESS, 2));
            if (rand.nextDouble() < 0.25) items.add(createEnchantBook(Enchantment.FIRE_ASPECT, 2));

            // Layer in two bonus rolls of tier 1 or 2
            generateSmpLoot(items, rand.nextBoolean() ? ChestTier.TIER_1 : ChestTier.TIER_2, rand);
            generateSmpLoot(items, rand.nextBoolean() ? ChestTier.TIER_1 : ChestTier.TIER_2, rand);
        }
    }

    private void generateMaceLoot(List<ItemStack> items, ChestTier tier, Set<Scenario> scenarios, ThreadLocalRandom rand) {
        boolean twoMaces = scenarios.contains(Scenario.TWO_MACES);
        boolean allowSpears = scenarios.contains(Scenario.SPEARS);
        boolean allowElytra = scenarios.contains(Scenario.ELYTRA);

        if (tier == ChestTier.TIER_1) {
            items.add(new ItemStack(Material.WIND_CHARGE, rand.nextInt(6, 13)));
            items.add(new ItemStack(Material.GOLDEN_APPLE, rand.nextInt(2, 4)));
            items.add(new ItemStack(Material.ENDER_PEARL, rand.nextInt(1, 4)));
            items.add(new ItemStack(Material.COOKED_BEEF, rand.nextInt(2, 5)));
            if (rand.nextBoolean()) addBuffPotion(items, false, rand);

            if (rand.nextDouble() < 0.30) items.add(createEnchantBook(Enchantment.SHARPNESS, 1));
            if (rand.nextDouble() < 0.15) items.add(createEnchantBook(Enchantment.WIND_BURST, 1));
            if (rand.nextDouble() < 0.45) items.add(createEnchantBook(Enchantment.DENSITY, 1));
            if (twoMaces && rand.nextDouble() < 0.45) items.add(createEnchantBook(Enchantment.BREACH, 1));

            if (allowSpears && rand.nextDouble() < 0.70) items.add(createSpear(0.75, 0.95, rand));
            if (allowElytra && rand.nextDouble() < 0.60) items.add(createElytra(0.75, 0.95, rand));
            if (rand.nextDouble() < 0.35) items.add(new ItemStack(Material.SHIELD, rand.nextInt(1, 4)));

        } else if (tier == ChestTier.TIER_2) {
            items.add(new ItemStack(Material.WIND_CHARGE, rand.nextInt(10, 19)));
            items.add(new ItemStack(Material.GOLDEN_APPLE, rand.nextInt(2, 5)));
            items.add(new ItemStack(Material.ENDER_PEARL, rand.nextInt(2, 5)));
            items.add(new ItemStack(Material.COOKED_BEEF, rand.nextInt(3, 7)));
            if (rand.nextBoolean()) addBuffPotion(items, false, rand);

            if (rand.nextDouble() < 0.20) items.add(createEnchantBook(Enchantment.SHARPNESS, 1));
            if (rand.nextDouble() < 0.10) items.add(createEnchantBook(Enchantment.WIND_BURST, 1));
            if (rand.nextDouble() < 0.35) items.add(createEnchantBook(Enchantment.DENSITY, 1));
            if (twoMaces) {
                if (rand.nextDouble() < 0.35) items.add(createEnchantBook(Enchantment.BREACH, 1));
                if (rand.nextDouble() < 0.05) items.add(createBreachMace(rand));
            }

            if (allowElytra && rand.nextDouble() < 0.45) items.add(createElytra(0.55, 0.80, rand));
            if (allowSpears && rand.nextDouble() < 0.45) items.add(createSpear(0.55, 0.80, rand));
            if (rand.nextDouble() < 0.05) items.add(new ItemStack(Material.TOTEM_OF_UNDYING));
            if (rand.nextDouble() < 0.05) items.add(createArmorUpgradeItem());
            if (rand.nextDouble() < 0.30) items.add(new ItemStack(Material.SHIELD, rand.nextInt(2, 5)));

        } else { // 3★ & 4★
            items.add(new ItemStack(Material.WIND_CHARGE, rand.nextInt(16, 25))); // Hard cap 24
            items.add(new ItemStack(Material.GOLDEN_APPLE, rand.nextInt(3, 6)));
            items.add(new ItemStack(Material.ENDER_PEARL, rand.nextInt(3, 7)));
            items.add(new ItemStack(Material.COOKED_BEEF, rand.nextInt(4, 9)));

            int pots = rand.nextInt(0, 3);
            for (int p = 0; p < pots; p++) addBuffPotion(items, false, rand);

            if (rand.nextDouble() < 0.25) items.add(createEnchantBook(Enchantment.SHARPNESS, 2));
            if (rand.nextDouble() < 0.12) items.add(createEnchantBook(Enchantment.WIND_BURST, 2));
            if (rand.nextDouble() < 0.40) items.add(createEnchantBook(Enchantment.DENSITY, 2));
            if (twoMaces) {
                if (rand.nextDouble() < 0.40) items.add(createEnchantBook(Enchantment.BREACH, 2));
                if (rand.nextDouble() < 0.10) items.add(createBreachMace(rand));
            }

            if (allowElytra && rand.nextDouble() < 0.28) items.add(createElytra(0.55, 0.80, rand));
            if (allowSpears && rand.nextDouble() < 0.28) items.add(createSpear(0.55, 0.80, rand));
            if (rand.nextDouble() < 0.25) {
                items.add(new ItemStack(Material.SHIELD, rand.nextInt(2, 4)));
                items.add(new ItemStack(Material.SHIELD, rand.nextInt(1, 3)));
            }

            items.add(rand.nextBoolean() ? new ItemStack(Material.TOTEM_OF_UNDYING) : createArmorUpgradeItem());
            if (rand.nextDouble() < 0.35) {
                items.add(rand.nextBoolean() ? new ItemStack(Material.TOTEM_OF_UNDYING) : createArmorUpgradeItem());
            }
        }
    }

    private void generateCartLoot(List<ItemStack> items, ChestTier tier, ThreadLocalRandom rand) {
        if (tier == ChestTier.TIER_1) {
            if (rand.nextDouble() < 0.60) items.add(new ItemStack(Material.TNT_MINECART, rand.nextInt(1, 3)));
            if (rand.nextDouble() < 0.45) items.add(new ItemStack(Material.ENDER_PEARL, rand.nextInt(1, 4)));
            if (rand.nextBoolean()) addBuffPotion(items, true, rand);
            if (rand.nextDouble() < 0.55) items.add(new ItemStack(Material.GOLDEN_APPLE, rand.nextInt(1, 4)));
            if (rand.nextDouble() < 0.45) items.add(new ItemStack(Material.RAIL, rand.nextInt(1, 5)));
            if (rand.nextDouble() < 0.35) items.add(new ItemStack(Material.COBWEB, rand.nextInt(1, 3)));
            if (rand.nextDouble() < 0.45) items.add(new ItemStack(Material.OAK_PLANKS, rand.nextInt(4, 9)));
            if (rand.nextDouble() < 0.35) items.add(createEnchantBook(Enchantment.SHARPNESS, 1));

        } else if (tier == ChestTier.TIER_2) {
            if (rand.nextDouble() < 0.70) items.add(new ItemStack(Material.TNT_MINECART, rand.nextInt(1, 4)));
            if (rand.nextDouble() < 0.55) items.add(new ItemStack(Material.ENDER_PEARL, rand.nextInt(2, 5)));
            if (rand.nextBoolean()) addBuffPotion(items, true, rand);
            if (rand.nextDouble() < 0.35) items.add(createEnchantBook(Enchantment.POWER, 1));
            if (rand.nextDouble() < 0.35) items.add(new ItemStack(Material.COBWEB, rand.nextInt(1, 3)));
            if (rand.nextDouble() < 0.55) items.add(new ItemStack(Material.RAIL, rand.nextInt(2, 6)));
            if (rand.nextDouble() < 0.60) items.add(new ItemStack(Material.GOLDEN_APPLE, rand.nextInt(2, 5)));
            if (rand.nextDouble() < 0.55) items.add(new ItemStack(Material.OAK_PLANKS, rand.nextInt(6, 11)));
            if (rand.nextDouble() < 0.30) items.add(createEnchantBook(Enchantment.SHARPNESS, 1));
            if (rand.nextDouble() < 0.075) items.add(new ItemStack(Material.TOTEM_OF_UNDYING));

        } else { // 3★ & 4★
            items.add(rand.nextBoolean() ? new ItemStack(Material.TOTEM_OF_UNDYING) : createArmorUpgradeItem());
            if (rand.nextDouble() < 0.80) items.add(new ItemStack(Material.TNT_MINECART, rand.nextInt(2, 4)));
            if (rand.nextDouble() < 0.65) items.add(new ItemStack(Material.ENDER_PEARL, rand.nextInt(3, 7)));
            int pots = rand.nextInt(0, 3);
            for (int p = 0; p < pots; p++) addBuffPotion(items, true, rand);
            if (rand.nextDouble() < 0.55) items.add(createEnchantBook(Enchantment.POWER, 1));
            if (rand.nextDouble() < 0.45) items.add(new ItemStack(Material.COBWEB, rand.nextInt(2, 4)));
            if (rand.nextDouble() < 0.65) items.add(new ItemStack(Material.RAIL, rand.nextInt(3, 7)));
            if (rand.nextDouble() < 0.70) items.add(new ItemStack(Material.GOLDEN_APPLE, rand.nextInt(3, 6)));
            if (rand.nextDouble() < 0.65) items.add(new ItemStack(Material.OAK_PLANKS, rand.nextInt(10, 17)));
            if (rand.nextDouble() < 0.45) items.add(createEnchantBook(Enchantment.SHARPNESS, 2));
        }
    }

    private void generateUhcLoot(List<ItemStack> items, ChestTier tier, ThreadLocalRandom rand) {
        items.add(new ItemStack(Material.COBWEB, rand.nextInt(1, 4)));

        if (tier == ChestTier.TIER_1) {
            items.add(new ItemStack(Material.GOLDEN_APPLE, rand.nextInt(1, 3)));
            items.add(new ItemStack(Material.OAK_PLANKS, rand.nextInt(8, 17)));
            if (rand.nextDouble() < 0.40) items.add(new ItemStack(Material.BOW));
            if (rand.nextDouble() < 0.40) items.add(new ItemStack(Material.ARROW, rand.nextInt(8, 17)));
            if (rand.nextDouble() < 0.40) items.add(new ItemStack(Material.LAVA_BUCKET));
            if (rand.nextDouble() < 0.35) items.add(new ItemStack(Material.WATER_BUCKET));

        } else if (tier == ChestTier.TIER_2) {
            items.add(new ItemStack(Material.GOLDEN_APPLE, rand.nextInt(2, 4)));
            items.add(new ItemStack(Material.OAK_PLANKS, rand.nextInt(12, 25)));
            if (rand.nextDouble() < 0.20) items.add(createGoldenHead());
            if (rand.nextDouble() < 0.55) items.add(new ItemStack(Material.LAVA_BUCKET));
            if (rand.nextDouble() < 0.50) items.add(new ItemStack(Material.WATER_BUCKET));
            if (rand.nextDouble() < 0.25) items.add(createHealingPotion(false));

        } else { // 3★ & 4★
            items.add(new ItemStack(Material.GOLDEN_APPLE, rand.nextInt(2, 5)));
            items.add(new ItemStack(Material.OAK_PLANKS, rand.nextInt(16, 33)));
            if (rand.nextDouble() < 0.35) items.add(createGoldenHead());
            if (rand.nextDouble() < 0.65) items.add(new ItemStack(Material.LAVA_BUCKET));
            if (rand.nextDouble() < 0.55) items.add(new ItemStack(Material.WATER_BUCKET));
            if (rand.nextDouble() < 0.30) items.add(createHealingPotion(true));
        }
    }

    private void addBuffPotion(List<ItemStack> items, boolean allowFireRes, ThreadLocalRandom rand) {
        List<PotionType> pool = new ArrayList<>();
        pool.add(PotionType.STRONG_SWIFTNESS);
        pool.add(PotionType.STRONG_STRENGTH);
        if (allowFireRes) pool.add(PotionType.LONG_FIRE_RESISTANCE);

        PotionType type = pool.get(rand.nextInt(pool.size()));
        ItemStack pot = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) pot.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(type);
            pot.setItemMeta(meta);
        }
        items.add(pot);
    }

    private ItemStack createHealingPotion(boolean strong) {
        ItemStack pot = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) pot.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(strong ? PotionType.STRONG_HEALING : PotionType.HEALING);
            pot.setItemMeta(meta);
        }
        return pot;
    }

    public ItemStack createGoldenHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Golden Head", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(goldenHeadKey, PersistentDataType.BYTE, (byte) 1);
            head.setItemMeta(meta);
        }
        return head;
    }

    public ItemStack createArmorUpgradeItem() {
        ItemStack item = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Armor Upgrade", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Upgrades a random equipped armor piece", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.getPersistentDataContainer().set(armorUpgradeKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBreachMace(ThreadLocalRandom rand) {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Breach Mace", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            meta.addEnchant(Enchantment.BREACH, rand.nextInt(1, 3), true);
            mace.setItemMeta(meta);
        }
        return mace;
    }

    private ItemStack createSpear(double minDamagePercent, double maxDamagePercent, ThreadLocalRandom rand) {
        // Netherite Spear with Lunge III baked in
        ItemStack spear = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = spear.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Netherite Spear", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Lunge III", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            meta.getPersistentDataContainer().set(spearKey, PersistentDataType.BYTE, (byte) 1);

            int maxDurability = Material.NETHERITE_SWORD.getMaxDurability();
            double usedPercent = minDamagePercent + (maxDamagePercent - minDamagePercent) * rand.nextDouble();
            if (meta instanceof Damageable damageable) {
                damageable.setDamage((int) (maxDurability * usedPercent));
            }
            spear.setItemMeta(meta);
        }
        return spear;
    }

    private ItemStack createElytra(double minDamagePercent, double maxDamagePercent, ThreadLocalRandom rand) {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = elytra.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int max = Material.ELYTRA.getMaxDurability();
            double usedPercent = minDamagePercent + (maxDamagePercent - minDamagePercent) * rand.nextDouble();
            damageable.setDamage((int) (max * usedPercent));
            elytra.setItemMeta(meta);
        }
        return elytra;
    }

    private ItemStack createEnchantBook(Enchantment ench, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(ench, level, true);
            book.setItemMeta(meta);
        }
        return book;
    }
}
