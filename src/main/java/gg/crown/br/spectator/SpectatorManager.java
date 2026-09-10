package gg.crown.br.spectator;

import gg.crown.br.NightRoyalePlugin;
import gg.crown.br.stats.PlayerStats;
import gg.crown.br.team.Squad;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class SpectatorManager implements Listener {

    private final NightRoyalePlugin plugin;
    private final Set<UUID> activeSpectators = new HashSet<>();
    private final Map<UUID, String> activeSearchQueries = new HashMap<>();
    private final Set<UUID> awaitingSearchInput = new HashSet<>();

    public SpectatorManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isSpectator(Player player) {
        return activeSpectators.contains(player.getUniqueId());
    }

    public void toggleSpectator(Player player) {
        if (isSpectator(player)) {
            removeSpectator(player);
            player.sendMessage(Component.text("Spectator mode disabled.", NamedTextColor.YELLOW));
        } else {
            makeSpectator(player);
            player.sendMessage(Component.text("Spectator mode enabled. Right-click compass to open player list, or spyglass to search IGN.", NamedTextColor.GREEN));
        }
    }

    public void makeSpectator(Player player) {
        activeSpectators.add(player.getUniqueId());

        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setCollidable(false);
        player.setInvisible(true);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        // Hide spectator from all active alive combatants
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!activeSpectators.contains(online.getUniqueId())) {
                online.hidePlayer(plugin, player);
            }
        }

        giveSpectatorTools(player);
    }

    public void removeSpectator(Player player) {
        activeSpectators.remove(player.getUniqueId());
        awaitingSearchInput.remove(player.getUniqueId());
        activeSearchQueries.remove(player.getUniqueId());

        player.setAllowFlight(false);
        player.setFlying(false);
        player.setCollidable(true);
        player.setInvisible(false);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

    public void clearAll() {
        for (UUID uuid : new ArrayList<>(activeSpectators)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                removeSpectator(p);
            }
        }
        activeSpectators.clear();
        awaitingSearchInput.clear();
        activeSearchQueries.clear();
    }

    public void giveSpectatorTools(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Slot 0: Compass Teleporter
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta cMeta = compass.getItemMeta();
        if (cMeta != null) {
            cMeta.displayName(Component.text("Player Teleporter", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            cMeta.lore(List.of(Component.text("Right-click to view and spectate alive players", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            compass.setItemMeta(cMeta);
        }
        player.getInventory().setItem(0, compass);

        // Slot 4: Spyglass Search
        ItemStack search = new ItemStack(Material.SPYGLASS);
        ItemMeta sMeta = search.getItemMeta();
        if (sMeta != null) {
            sMeta.displayName(Component.text("Search by IGN", NamedTextColor.AQUA).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            sMeta.lore(List.of(Component.text("Right-click to search and filter players by name", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            search.setItemMeta(sMeta);
        }
        player.getInventory().setItem(4, search);

        // Slot 8: Return to Lobby
        ItemStack bed = new ItemStack(Material.RED_BED);
        ItemMeta bMeta = bed.getItemMeta();
        if (bMeta != null) {
            bMeta.displayName(Component.text("Return to Lobby", NamedTextColor.RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            bMeta.lore(List.of(Component.text("Right-click to return to the waiting lobby", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            bed.setItemMeta(bMeta);
        }
        player.getInventory().setItem(8, bed);

        player.updateInventory();
    }

    public void openSpectatorGUI(Player spectator, String filterQuery) {
        List<Player> alive = new ArrayList<>(plugin.getMatchManager().getAlivePlayers());

        // Fallback for test mode or outside active match roster
        if (alive.isEmpty()) {
            alive.addAll(Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !isSpectator(p) && p.getGameMode() != org.bukkit.GameMode.SPECTATOR)
                    .toList());
        }

        // If still empty (e.g. solo dev test server), include spectator themselves as a preview
        if (alive.isEmpty()) {
            alive.add(spectator);
        }

        // Filter if search query is present
        if (filterQuery != null && !filterQuery.isBlank()) {
            String queryLower = filterQuery.toLowerCase();
            alive = alive.stream()
                    .filter(p -> p.getName().toLowerCase().contains(queryLower))
                    .toList();
        }

        int slots = 54;
        String title = (filterQuery != null && !filterQuery.isBlank())
                ? "Spectate: '" + filterQuery + "' (" + alive.size() + ")"
                : "Spectate Alive Players (" + alive.size() + ")";

        Inventory inv = Bukkit.createInventory(null, slots, Component.text(title, NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));

        if (alive.isEmpty()) {
            ItemStack noMatch = createButton(Material.RED_STAINED_GLASS_PANE, "No Players Found", NamedTextColor.RED, "No online players match '" + filterQuery + "'. Click 'Clear Filter' below.");
            inv.setItem(22, noMatch);
        } else {
            // Populate heads
            for (int i = 0; i < alive.size() && i < 45; i++) {
                Player target = alive.get(i);
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(target);
                    meta.displayName(Component.text(target.getName(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Health: ", NamedTextColor.GRAY).append(Component.text(String.format("%.1f", target.getHealth()) + "/20 ❤", NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("Kills: ", NamedTextColor.GRAY).append(Component.text(plugin.getMatchManager().getMatchKills(target.getUniqueId()), NamedTextColor.YELLOW)).decoration(TextDecoration.ITALIC, false));

                    Squad squad = plugin.getTeamManager().getSquad(target);
                    if (squad != null && squad.size() > 1) {
                        lore.add(Component.text("Squad: ", NamedTextColor.GRAY).append(Component.text(squad.size() + " alive", NamedTextColor.AQUA)).decoration(TextDecoration.ITALIC, false));
                    }

                    lore.add(Component.text("Ping: ", NamedTextColor.GRAY).append(Component.text(target.getPing() + "ms", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.empty());
                    lore.add(Component.text("▶ Left-Click to Spectate", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                    meta.lore(lore);
                    head.setItemMeta(meta);
                }
                inv.setItem(i, head);
            }
        }

        // Bottom control row
        ItemStack randomBtn = createButton(Material.ENDER_PEARL, "Random Player", NamedTextColor.LIGHT_PURPLE, "Teleport to a random alive combatant");
        ItemStack searchBtn = createButton(Material.SPYGLASS, "Search by IGN", NamedTextColor.AQUA, "Click to filter players by typing in chat");
        ItemStack clearSearchBtn = createButton(Material.MILK_BUCKET, "Clear Filter", NamedTextColor.YELLOW, "Reset player search filter");
        ItemStack closeBtn = createButton(Material.BARRIER, "Close", NamedTextColor.RED, "Close this menu");

        inv.setItem(45, randomBtn);
        inv.setItem(48, searchBtn);
        inv.setItem(50, clearSearchBtn);
        inv.setItem(53, closeBtn);

        spectator.openInventory(inv);
        spectator.playSound(spectator.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.8f, 1.2f);
    }

    private ItemStack createButton(Material mat, String name, NamedTextColor color, String loreText) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text(loreText, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isSpectator(player)) return;

        // Cancel all world/block interactions unconditionally for spectators
        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item == null) return;

            if (item.getType() == Material.COMPASS) {
                openSpectatorGUI(player, activeSearchQueries.get(player.getUniqueId()));
            } else if (item.getType() == Material.SPYGLASS) {
                promptSearch(player);
            } else if (item.getType() == Material.RED_BED) {
                removeSpectator(player);
                if (plugin.getLobbyLocation() != null) {
                    player.teleport(plugin.getLobbyLocation());
                }
                player.sendMessage(Component.text("Returned to lobby.", NamedTextColor.YELLOW));
            }
        }
    }

    private void promptSearch(Player player) {
        awaitingSearchInput.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(Component.text("---------------------------------------------", NamedTextColor.DARK_PURPLE));
        player.sendMessage(Component.text("🔍 Type player name/IGN in chat to search:", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("(Type 'cancel' to abort)", NamedTextColor.GRAY));
        player.sendMessage(Component.text("---------------------------------------------", NamedTextColor.DARK_PURPLE));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChatSearch(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingSearchInput.contains(player.getUniqueId())) return;

        event.setCancelled(true);
        awaitingSearchInput.remove(player.getUniqueId());

        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("Search cancelled.", NamedTextColor.RED));
            return;
        }

        activeSearchQueries.put(player.getUniqueId(), message);
        Bukkit.getScheduler().runTask(plugin, () -> openSpectatorGUI(player, message));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isSpectator(player)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        int slot = event.getRawSlot();

        if (slot == 45) { // Random Player
            List<Player> alive = plugin.getMatchManager().getAlivePlayers();
            if (!alive.isEmpty()) {
                Player randomTarget = alive.get(new Random().nextInt(alive.size()));
                player.teleport(randomTarget.getLocation().add(0, 1.5, 0));
                player.sendMessage(Component.text("✦ Teleported to " + randomTarget.getName(), NamedTextColor.AQUA));
                player.closeInventory();
            }
            return;
        }

        if (slot == 48) { // Search by IGN
            promptSearch(player);
            return;
        }

        if (slot == 50) { // Clear filter
            activeSearchQueries.remove(player.getUniqueId());
            openSpectatorGUI(player, null);
            return;
        }

        if (slot == 53) { // Close
            player.closeInventory();
            return;
        }

        // Clicked player head
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta meta) {
            if (meta.getOwningPlayer() != null) {
                Player target = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());
                if (target != null && target.isOnline()) {
                    player.teleport(target.getLocation().add(0, 1.5, 0));
                    player.sendMessage(Component.text("✦ Spectating " + target.getName(), NamedTextColor.GREEN));
                    player.closeInventory();
                } else {
                    player.sendMessage(Component.text("Player is no longer available.", NamedTextColor.RED));
                }
            }
        }
    }

    // Spectator restriction protections
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p && isSpectator(p)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && isSpectator(p)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player p && isSpectator(p)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player joiner = event.getPlayer();
        for (UUID uuid : activeSpectators) {
            Player spectator = Bukkit.getPlayer(uuid);
            if (spectator != null && spectator.isOnline()) {
                joiner.hidePlayer(plugin, spectator);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player p && isSpectator(p)) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFood(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player p && isSpectator(p)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionSplash(org.bukkit.event.entity.PotionSplashEvent event) {
        event.getAffectedEntities().removeIf(e -> e instanceof Player p && isSpectator(p));
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (isSpectator(p)) {
            removeSpectator(p);
        }
    }
}
