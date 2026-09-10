package gg.crown.br.loot;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.mode.Scenario;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LootConsolidationListener implements Listener {

    private final NightRoyalePlugin plugin;

    public LootConsolidationListener(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Item entityItem = event.getItem();
        ItemStack picked = entityItem.getItemStack();

        // 1. Elytra Auto-Merging (Durability Absorption)
        if (picked.getType() == Material.ELYTRA) {
            ItemStack existing = findExistingItem(player, Material.ELYTRA);
            if (existing != null && existing != picked) {
                event.setCancelled(true);
                entityItem.remove();
                absorbDurability(player, existing, picked, "Elytra");
                return;
            }
        }

        // 2. Netherite Spear Auto-Merging
        if (isSpear(picked)) {
            ItemStack existing = findExistingSpear(player);
            if (existing != null && existing != picked) {
                event.setCancelled(true);
                entityItem.remove();
                absorbDurability(player, existing, picked, "Spear");
                return;
            }
        }

        // 3. Stackable Shields
        if (picked.getType() == Material.SHIELD) {
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && invItem.getType() == Material.SHIELD) {
                    int space = 64 - invItem.getAmount();
                    if (space > 0) {
                        int toAdd = Math.min(space, picked.getAmount());
                        invItem.setAmount(invItem.getAmount() + toAdd);
                        picked.setAmount(picked.getAmount() - toAdd);
                        if (picked.getAmount() <= 0) {
                            event.setCancelled(true);
                            entityItem.remove();
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void absorbDurability(Player player, ItemStack target, ItemStack absorbed, String typeName) {
        if (target.getItemMeta() instanceof Damageable targetDam && absorbed.getItemMeta() instanceof Damageable absorbedDam) {
            int maxDurability = target.getType().getMaxDurability();
            if (target.getType() == Material.ELYTRA && plugin.getMatchManager().hasScenario(Scenario.DOUBLE_ELYTRA)) {
                maxDurability *= 2;
            }

            int absorbedRemaining = Math.max(0, absorbed.getType().getMaxDurability() - absorbedDam.getDamage());
            int newDamage = Math.max(0, targetDam.getDamage() - absorbedRemaining);

            targetDam.setDamage(newDamage);
            target.setItemMeta(targetDam);

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.4f);
            player.sendActionBar(Component.text("✦ " + typeName + " merged! Durability absorbed.", NamedTextColor.GREEN));
        }
    }

    private ItemStack findExistingItem(Player player, Material mat) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) return item;
        }
        if (player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() == mat) {
            return player.getInventory().getChestplate();
        }
        return null;
    }

    private ItemStack findExistingSpear(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSpear(item)) return item;
        }
        return null;
    }

    private boolean isSpear(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(plugin.getChestManager().getSpearKey(), PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(plugin.getChestManager().getGoldenHeadKey(), PersistentDataType.BYTE)) {
                Player player = event.getPlayer();
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 5, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 120, 0));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
            }
        }
    }
}
