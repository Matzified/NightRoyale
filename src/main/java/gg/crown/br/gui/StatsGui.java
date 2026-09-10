package gg.crown.br.gui;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.rank.Rank;
import gg.crown.br.stats.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StatsGui implements InventoryHolder {

    private final NightRoyalePlugin plugin;
    private final OfflinePlayer target;
    private final Inventory inventory;

    public StatsGui(NightRoyalePlugin plugin, OfflinePlayer target) {
        this.plugin = plugin;
        this.target = target;
        String targetName = target.getName() != null ? target.getName() : "Player";
        Component title = MiniMessage.miniMessage().deserialize(
                "<gradient:#A855F7:#E085FF><b>✦ " + targetName + "'s Profile ✦</b></gradient>"
        );
        this.inventory = Bukkit.createInventory(this, 54, title);
        buildInventory();
    }

    private void buildInventory() {
        PlayerStats stats = plugin.getStatsManager().getStats(target.getUniqueId());
        Rank rank = plugin.getRankManager().getRank(target.getUniqueId());
        String name = target.getName() != null ? target.getName() : "Player";
        boolean isOnline = target.isOnline();

        // 1. Fill background with sleek dark glass borders
        ItemStack purpleGlass = createGlass(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack blackGlass = createGlass(Material.BLACK_STAINED_GLASS_PANE);

        int[] purpleSlots = {0, 1, 7, 8, 45, 46, 52, 53};
        for (int slot : purpleSlots) {
            inventory.setItem(slot, purpleGlass);
        }

        int[] blackSlots = {2, 3, 5, 6, 9, 12, 16, 17, 18, 21, 25, 26, 27, 30, 34, 35, 36, 39, 40, 41, 42, 43, 44, 47, 48, 50, 51};
        for (int slot : blackSlots) {
            inventory.setItem(slot, blackGlass);
        }

        // 2. Top Profile Header Banner (Slot 4)
        ItemStack banner = new ItemStack(Material.NETHER_STAR);
        ItemMeta bannerMeta = banner.getItemMeta();
        if (bannerMeta != null) {
            bannerMeta.displayName(MiniMessage.miniMessage().deserialize(
                    "<gradient:#A855F7:#E085FF><b>✦ " + name + "'s Night Royale Dossier ✦</b></gradient>"
            ).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Status:</gray> " + (isOnline ? "<green>● Online</green>" : "<gray>○ Offline</gray>")).decoration(TextDecoration.ITALIC, false));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Server Rank:</gray> ").append(rank.getTagComponent()).append(Component.text(rank.getDisplayName(), rank.getColor())).decoration(TextDecoration.ITALIC, false));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Player Level:</gray> <green>Level " + stats.getLevel() + "</green>").decoration(TextDecoration.ITALIC, false));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Competitive Rating:</gray> <light_purple>" + stats.getRating() + " Elo</light_purple>").decoration(TextDecoration.ITALIC, false));
            bannerMeta.lore(lore);
            banner.setItemMeta(bannerMeta);
        }
        inventory.setItem(4, banner);

        // 3. Minecraft Skin Column / Paperdoll Mannequin (Slots 10, 19, 28, 37)
        // Head / Skin Hat Overlay (Slot 10)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(target);
            skullMeta.displayName(MiniMessage.miniMessage().deserialize(
                    "<gradient:#A855F7:#E085FF><b>" + name + "</b></gradient>"
            ).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Skin Model:</gray> <green>✔ 3D Skin & Hat Layer</green>").decoration(TextDecoration.ITALIC, false));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>UUID:</gray> <dark_gray>" + target.getUniqueId() + "</dark_gray>").decoration(TextDecoration.ITALIC, false));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Rank:</gray> ").append(rank.getTagComponent()).decoration(TextDecoration.ITALIC, false));
            skullMeta.lore(lore);
            head.setItemMeta(skullMeta);
        }
        inventory.setItem(10, head);

        // Torso / Chestplate Layer (Slot 19)
        Player onlinePlayer = target.getPlayer();
        ItemStack chestItem = (onlinePlayer != null && onlinePlayer.getInventory().getChestplate() != null)
                ? onlinePlayer.getInventory().getChestplate().clone()
                : new ItemStack(Material.NETHERITE_CHESTPLATE);
        setMannequinMeta(chestItem, "Torso & Chest Armor", "Chestplate");
        inventory.setItem(19, chestItem);

        // Legs / Pants Layer (Slot 28)
        ItemStack legsItem = (onlinePlayer != null && onlinePlayer.getInventory().getLeggings() != null)
                ? onlinePlayer.getInventory().getLeggings().clone()
                : new ItemStack(Material.NETHERITE_LEGGINGS);
        setMannequinMeta(legsItem, "Legs & Pants Armor", "Leggings");
        inventory.setItem(28, legsItem);

        // Feet / Boots Layer (Slot 37)
        ItemStack bootsItem = (onlinePlayer != null && onlinePlayer.getInventory().getBoots() != null)
                ? onlinePlayer.getInventory().getBoots().clone()
                : new ItemStack(Material.NETHERITE_BOOTS);
        setMannequinMeta(bootsItem, "Feet & Footwear Armor", "Boots");
        inventory.setItem(37, bootsItem);

        // 4. Statistics Module Cards
        double kpg = stats.getGames() > 0 ? (double) stats.getKills() / stats.getGames() : stats.getKills();
        double winRate = stats.getGames() > 0 ? ((double) stats.getWins() / stats.getGames()) * 100.0 : 0.0;
        int losses = Math.max(0, stats.getGames() - stats.getWins());
        double kd = losses > 0 ? (double) stats.getKills() / losses : stats.getKills();

        // Combat Stats (Slot 14)
        ItemStack combatCard = createCard(Material.DIAMOND_SWORD,
                "<red><b>⚔ Combat Statistics</b></red>",
                List.of(
                        "",
                        "<gray>• Total Kills:</gray> <red><b>" + stats.getKills() + "</b></red>",
                        "<gray>• Kills / Match:</gray> <yellow><b>" + String.format("%.2f", kpg) + "</b></yellow>",
                        "<gray>• Combat Score:</gray> <red><b>" + (stats.getKills() * 10) + " pts</b></red>",
                        "<gray>• K/D Ratio:</gray> <yellow><b>" + String.format("%.2f", kd) + "</b></yellow>"
                )
        );
        inventory.setItem(14, combatCard);

        // Victory Stats (Slot 15)
        ItemStack victoryCard = createCard(Material.TOTEM_OF_UNDYING,
                "<gold><b>🏆 Victories & Matches</b></gold>",
                List.of(
                        "",
                        "<gray>• Total Wins:</gray> <gold><b>" + stats.getWins() + "</b></gold>",
                        "<gray>• Matches Played:</gray> <aqua><b>" + stats.getGames() + "</b></aqua>",
                        "<gray>• Win Rate:</gray> <yellow><b>" + String.format("%.1f", winRate) + "%</b></yellow>",
                        "<gray>• Matches Lost:</gray> <gray><b>" + losses + "</b></gray>"
                )
        );
        inventory.setItem(15, victoryCard);

        // Level & Progress (Slot 23)
        int xpInLevel = stats.getXp() % 1000;
        int xpRemaining = 1000 - xpInLevel;
        String progressBar = buildProgressBar(xpInLevel, 1000, 10);
        ItemStack levelCard = createCard(Material.EXPERIENCE_BOTTLE,
                "<green><b>⭐ Progression & Level</b></green>",
                List.of(
                        "",
                        "<gray>• Level:</gray> <green><b>Level " + stats.getLevel() + "</b></green>",
                        "<gray>• Total XP:</gray> <aqua><b>" + stats.getXp() + " XP</b></aqua>",
                        "<gray>• Progress:</gray> <green>" + xpInLevel + "/1000 XP</green>",
                        "<gray>• Bar:</gray> " + progressBar,
                        "<gray>• Next Level In:</gray> <dark_gray>" + xpRemaining + " XP</dark_gray>"
                )
        );
        inventory.setItem(23, levelCard);

        // Economy (Slot 24)
        ItemStack economyCard = createCard(Material.GOLD_INGOT,
                "<yellow><b>⛃ Currency & Coins</b></yellow>",
                List.of(
                        "",
                        "<gray>• Coin Balance:</gray> <gold><b>" + stats.getCoins() + " ⛃</b></gold>",
                        "<gray>• Reward / Win:</gray> <gray>+250 Coins</gray>",
                        "<gray>• Reward / Kill:</gray> <gray>+20 Coins</gray>"
                )
        );
        inventory.setItem(24, economyCard);

        // Competitive Standing (Slot 32)
        String tier = getRatingTier(stats.getRating());
        ItemStack ratingCard = createCard(Material.AMETHYST_SHARD,
                "<light_purple><b>✦ Competitive Elo & Tier</b></light_purple>",
                List.of(
                        "",
                        "<gray>• Competitive Rating:</gray> <light_purple><b>" + stats.getRating() + " Elo</b></light_purple>",
                        "<gray>• Ranking Tier:</gray> " + tier,
                        "<gray>• Leaderboard Score:</gray> <yellow><b>" + ((stats.getWins() * 100) + (stats.getKills() * 10)) + "</b></yellow>"
                )
        );
        inventory.setItem(32, ratingCard);

        // Rank Standing & Permissions (Slot 33)
        boolean hasPriority = rank != Rank.MEMBER || target.isOp();
        ItemStack rankCard = createCard(Material.BOOK,
                "<aqua><b>📜 Rank Privileges</b></aqua>",
                List.of(
                        "",
                        "<gray>• Server Rank:</gray> <white>" + rank.getDisplayName() + "</white>",
                        "<gray>• Purge Priority:</gray> " + (hasPriority ? "<green>✔ ENABLED</green>" : "<red>✖ None</red>"),
                        "<gray>• Chat Cooldown:</gray> " + (rank == Rank.MIDNIGHT || rank == Rank.TWILIGHT ? "<aqua>1s</aqua>" : "<gray>3s</gray>"),
                        "<gray>• Lobby Chat:</gray> " + (rank != Rank.MEMBER ? "<green>Allowed</green>" : "<red>Disabled</red>")
                )
        );
        inventory.setItem(33, rankCard);

        // Close Button (Slot 49)
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(MiniMessage.miniMessage().deserialize("<red><b>Close Profile</b></red>").decoration(TextDecoration.ITALIC, false));
            closeBtn.setItemMeta(closeMeta);
        }
        inventory.setItem(49, closeBtn);
    }

    private void setMannequinMeta(ItemStack item, String displayName, String slotName) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<#E085FF><b>" + displayName + "</b></#E085FF>").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Mannequin Slot:</gray> <white>" + slotName + "</white>").decoration(TextDecoration.ITALIC, false));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Skin Layer:</gray> <green>✔ Active</green>").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
    }

    private ItemStack createCard(Material material, String title, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(title).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                if (line.isEmpty()) {
                    lore.add(Component.empty());
                } else {
                    lore.add(MiniMessage.miniMessage().deserialize(line).decoration(TextDecoration.ITALIC, false));
                }
            }
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGlass(Material mat) {
        ItemStack glass = new ItemStack(mat);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private String buildProgressBar(int current, int max, int totalBars) {
        float progress = Math.min(1.0f, (float) current / (float) max);
        int filled = Math.round(progress * totalBars);
        StringBuilder sb = new StringBuilder("<green>");
        for (int i = 0; i < filled; i++) sb.append("■");
        sb.append("</green><dark_gray>");
        for (int i = filled; i < totalBars; i++) sb.append("□");
        sb.append("</dark_gray>");
        return sb.toString();
    }

    private String getRatingTier(int rating) {
        if (rating >= 3000) return "<gradient:#A855F7:#E085FF><b>Grandmaster</b></gradient>";
        if (rating >= 2000) return "<aqua><b>Master</b></aqua>";
        if (rating >= 1000) return "<gold><b>Elite</b></gold>";
        if (rating >= 500) return "<yellow><b>Challenger</b></yellow>";
        return "<gray><b>Novice</b></gray>";
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
