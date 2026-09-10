package gg.crown.br.loot;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ArmorUpgradeManager implements Listener {

    private final NightRoyalePlugin plugin;

    public ArmorUpgradeManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean triggerUpgrade(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] armor = inv.getArmorContents();

        List<Integer> upgradeableIndices = new ArrayList<>();
        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            if (piece != null && !piece.getType().isAir() && getNextTier(piece.getType()) != null) {
                upgradeableIndices.add(i);
            }
        }

        if (upgradeableIndices.isEmpty()) {
            return false;
        }

        int targetIndex = upgradeableIndices.get(ThreadLocalRandom.current().nextInt(upgradeableIndices.size()));
        ItemStack currentPiece = armor[targetIndex];
        Material nextMat = getNextTier(currentPiece.getType());
        if (nextMat == null) return false;

        ItemStack upgraded = currentPiece.clone();
        upgraded.setType(nextMat);

        ItemMeta meta = upgraded.getItemMeta();
        if (meta != null) {
            String tierName = nextMat.name().startsWith("DIAMOND") ? "Diamond" : "Netherite";
            String pieceName = switch (targetIndex) {
                case 0 -> "Boots";
                case 1 -> "Leggings";
                case 2 -> "Chestplate";
                case 3 -> "Helmet";
                default -> "Armor";
            };
            meta.displayName(Component.text(tierName + " " + pieceName, NamedTextColor.AQUA));
            upgraded.setItemMeta(meta);
        }

        armor[targetIndex] = upgraded;
        inv.setArmorContents(armor);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, player.getLocation().add(0, 1, 0), 25, 0.4, 0.5, 0.4, 0.5);
        player.sendActionBar(Component.text("✦ Armor piece upgraded to " + nextMat.name().split("_")[0] + "!", NamedTextColor.LIGHT_PURPLE));
        return true;
    }

    private Material getNextTier(Material current) {
        return switch (current) {
            case IRON_HELMET -> Material.DIAMOND_HELMET;
            case DIAMOND_HELMET -> Material.NETHERITE_HELMET;
            case IRON_CHESTPLATE -> Material.DIAMOND_CHESTPLATE;
            case DIAMOND_CHESTPLATE -> Material.NETHERITE_CHESTPLATE;
            case IRON_LEGGINGS -> Material.DIAMOND_LEGGINGS;
            case DIAMOND_LEGGINGS -> Material.NETHERITE_LEGGINGS;
            case IRON_BOOTS -> Material.DIAMOND_BOOTS;
            case DIAMOND_BOOTS -> Material.NETHERITE_BOOTS;
            default -> null;
        };
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();

        if (isArmorUpgradeItem(item)) {
            if (triggerUpgrade(player)) {
                event.setCancelled(true);
                if (item.getAmount() <= 1) {
                    event.getItem().remove();
                } else {
                    item.subtract(1);
                    event.getItem().setItemStack(item);
                }
            }
            // If cannot upgrade directly, don't cancel: item enters inventory for manual use or team passing
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && isArmorUpgradeItem(item)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            if (triggerUpgrade(player)) {
                item.subtract(1);
            } else {
                player.sendMessage(Component.text("No upgradeable armor equipped!", NamedTextColor.RED));
            }
        }
    }

    public boolean isArmorUpgradeItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(plugin.getChestManager().getArmorUpgradeKey(), PersistentDataType.BYTE);
    }
}
