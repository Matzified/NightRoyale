package gg.crown.br.world;

import gg.crown.br.NightRoyalePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.LeavesDecayEvent;

public class FoliageGuardian implements Listener {

    private final NightRoyalePlugin plugin;

    public FoliageGuardian(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        Material mat = event.getBlock().getType();
        if (mat.name().contains("CORAL") || mat == Material.ICE) {
            event.setCancelled(true);
        }
    }

    public int clearItems(World world) {
        int count = 0;
        for (Item item : world.getEntitiesByClass(Item.class)) {
            item.remove();
            count++;
        }
        return count;
    }

    public int degrass(Location pos1, Location pos2) {
        if (pos1 == null || pos2 == null || pos1.getWorld() == null) return 0;
        World world = pos1.getWorld();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int removed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int highest = Math.min(maxY, world.getHighestBlockYAt(x, z) + 2);
                for (int y = highest; y >= minY; y--) {
                    Block b = world.getBlockAt(x, y, z);
                    Material mat = b.getType();
                    if (mat == Material.SHORT_GRASS || mat == Material.TALL_GRASS || mat == Material.FERN || mat == Material.LARGE_FERN) {
                        b.setType(Material.AIR, false);
                        removed++;
                    } else if (mat.isSolid() && y < highest - 10) {
                        // Terrain goes deep underground, foliage does not grow below solid rock
                        break;
                    }
                }
            }
        }
        return removed;
    }
}
