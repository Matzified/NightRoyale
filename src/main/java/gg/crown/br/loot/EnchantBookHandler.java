package gg.crown.br.loot;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Map;

public class EnchantBookHandler implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onApplyBook(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor.getType() != Material.ENCHANTED_BOOK || current == null || current.getType().isAir()) {
            return;
        }

        if (cursor.getItemMeta() instanceof EnchantmentStorageMeta storageMeta) {
            Map<Enchantment, Integer> enchants = storageMeta.getStoredEnchants();
            if (enchants.isEmpty()) return;

            boolean applied = false;
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                Enchantment ench = entry.getKey();
                int level = entry.getValue();

                if (canApply(ench, current)) {
                    current.addUnsafeEnchantment(ench, Math.max(current.getEnchantmentLevel(ench), level));
                    applied = true;
                }
            }

            if (applied) {
                event.setCancelled(true);
                cursor.subtract(1);
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
            }
        }
    }

    private boolean canApply(Enchantment ench, ItemStack item) {
        Material mat = item.getType();
        String name = ench.getKey().getKey().toLowerCase();

        if (name.contains("sharpness") || name.contains("fire_aspect")) {
            return mat.name().endsWith("_SWORD") || mat.name().endsWith("_AXE");
        }
        if (name.contains("density") || name.contains("breach") || name.contains("wind_burst")) {
            return mat == Material.MACE;
        }
        if (name.contains("power") || name.contains("flame") || name.contains("infinity")) {
            return mat == Material.BOW;
        }
        if (name.contains("feather_falling")) {
            return mat.name().endsWith("_BOOTS");
        }
        return ench.canEnchantItem(item);
    }
}
