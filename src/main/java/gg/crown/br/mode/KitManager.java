package gg.crown.br.mode;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class KitManager implements Listener {

    private final NightRoyalePlugin plugin;
    private final NamespacedKey kitKey;

    public KitManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
        this.kitKey = new NamespacedKey(plugin, "kit_item");
    }

    public NamespacedKey getKitKey() {
        return kitKey;
    }

    public boolean isKitItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(kitKey, PersistentDataType.BYTE);
    }

    public void tagKitItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(kitKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
    }

    public void equipKit(Player player, GameMode mode) {
        PlayerInventory inv = player.getInventory();
        inv.clear();

        // 1. Universal Armor (All Prot IV, FF IV on boots, Unbreakable)
        ItemStack helmet = createArmorPiece(Material.IRON_HELMET, "Iron Helmet");
        helmet.addUnsafeEnchantment(Enchantment.PROTECTION, 4);

        ItemStack chestplate = createArmorPiece(Material.IRON_CHESTPLATE, "Iron Chestplate");
        chestplate.addUnsafeEnchantment(Enchantment.PROTECTION, 4);

        ItemStack leggings = createArmorPiece(Material.IRON_LEGGINGS, "Iron Leggings");
        leggings.addUnsafeEnchantment(Enchantment.PROTECTION, 4);

        ItemStack boots = createArmorPiece(Material.IRON_BOOTS, "Iron Boots");
        boots.addUnsafeEnchantment(Enchantment.PROTECTION, 4);
        boots.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 4);

        inv.setHelmet(helmet);
        inv.setChestplate(chestplate);
        inv.setLeggings(leggings);
        inv.setBoots(boots);

        // 2. Base Weapons
        ItemStack sword = createTool(Material.DIAMOND_SWORD, "Diamond Sword");
        ItemStack axe = createTool(Material.DIAMOND_AXE, "Diamond Axe");
        ItemStack shield = createTool(Material.SHIELD, "Shield");

        inv.setItem(0, sword);
        inv.setItem(1, axe);
        inv.setItem(2, shield);

        // 3. Mode-Specific Kit Items
        switch (mode) {
            case MACE: {
                ItemStack mace = createTool(Material.MACE, "Density Mace");
                mace.addUnsafeEnchantment(Enchantment.DENSITY, 3);
                inv.setItem(8, mace); // Pinned to slot 9 (index 8) as shown in screenshot
                break;
            }
            case CART: {
                ItemStack bow = createTool(Material.BOW, "Bow");
                bow.addUnsafeEnchantment(Enchantment.POWER, 3);
                bow.addUnsafeEnchantment(Enchantment.FLAME, 1);
                bow.addUnsafeEnchantment(Enchantment.INFINITY, 1);
                inv.setItem(3, bow);

                // Locked slot 9 infinite arrow in inventory grid
                ItemStack arrow = new ItemStack(Material.ARROW, 1);
                ItemMeta arrowMeta = arrow.getItemMeta();
                if (arrowMeta != null) {
                    arrowMeta.displayName(Component.text("Infinite Arrow", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                    arrowMeta.getPersistentDataContainer().set(kitKey, PersistentDataType.BYTE, (byte) 1);
                    arrow.setItemMeta(arrowMeta);
                }
                inv.setItem(9, arrow);
                break;
            }
            case SMP:
            case UHC:
            default:
                break;
        }

        player.updateInventory();
    }

    private ItemStack createArmorPiece(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(kitKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTool(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(kitKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isKitItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isKitItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorClick(InventoryClickEvent event) {
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            if (isKitItem(event.getCurrentItem()) || isKitItem(event.getCursor())) {
                event.setCancelled(true);
                return;
            }
        }
        // Prevent shift-clicking kit armor out of armor slots
        if (event.isShiftClick() && isKitItem(event.getCurrentItem())) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
                event.setCancelled(true);
            }
        }
    }
}
