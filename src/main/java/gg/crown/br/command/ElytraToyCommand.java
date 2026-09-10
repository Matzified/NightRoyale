package gg.crown.br.command;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ElytraToyCommand implements CommandExecutor {

    private final NightRoyalePlugin plugin;

    public ElytraToyCommand(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("nightroyale.admin")) {
            player.sendMessage(Component.text("Lobby movement toys are admin-only.", NamedTextColor.RED));
            return true;
        }

        player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        player.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 64));
        player.sendMessage(Component.text("✔ Equipped lobby Elytra and Wind Charges.", NamedTextColor.GREEN));
        return true;
    }
}
