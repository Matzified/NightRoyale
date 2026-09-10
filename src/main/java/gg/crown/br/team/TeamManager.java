package gg.crown.br.team;

import gg.crown.br.NightRoyalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

public class TeamManager implements Listener {

    private final NightRoyalePlugin plugin;
    private final Map<UUID, Squad> playerSquads = new HashMap<>();
    private final Set<Squad> squads = new HashSet<>();
    private int maxTeamSize = 1;

    public TeamManager(NightRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    public void setMaxTeamSize(int maxTeamSize) {
        this.maxTeamSize = Math.max(1, maxTeamSize);
    }

    public Squad getSquad(Player player) {
        return playerSquads.get(player.getUniqueId());
    }

    public Squad getOrCreateSquad(Player player) {
        Squad squad = playerSquads.get(player.getUniqueId());
        if (squad == null) {
            squad = new Squad(player);
            squads.add(squad);
            playerSquads.put(player.getUniqueId(), squad);
        }
        return squad;
    }

    public void clearAll() {
        playerSquads.clear();
        squads.clear();
    }

    public boolean invite(Player leader, Player target) {
        if (maxTeamSize <= 1) {
            leader.sendMessage(Component.text("Teaming is disabled in Solo mode.", NamedTextColor.RED));
            return false;
        }

        Squad squad = getOrCreateSquad(leader);
        if (squad.size() >= maxTeamSize) {
            leader.sendMessage(Component.text("Your squad is already full (max " + maxTeamSize + ").", NamedTextColor.RED));
            return false;
        }

        squad.getPendingInvites().add(target.getUniqueId());
        target.sendMessage(Component.text(leader.getName() + " invited you to their squad! Use /team accept " + leader.getName() + " to join.", NamedTextColor.GREEN));
        leader.sendMessage(Component.text("Invited " + target.getName() + " to your squad.", NamedTextColor.YELLOW));
        return true;
    }

    public boolean accept(Player player, Player leader) {
        if (maxTeamSize <= 1) return false;

        Squad targetSquad = playerSquads.get(leader.getUniqueId());
        if (targetSquad == null || !targetSquad.getPendingInvites().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have a pending invite from " + leader.getName() + ".", NamedTextColor.RED));
            return false;
        }

        if (targetSquad.size() >= maxTeamSize) {
            player.sendMessage(Component.text("That squad is already full.", NamedTextColor.RED));
            return false;
        }

        // Leave existing squad if single
        Squad oldSquad = playerSquads.get(player.getUniqueId());
        if (oldSquad != null) {
            leave(player);
        }

        targetSquad.addMember(player);
        playerSquads.put(player.getUniqueId(), targetSquad);

        player.sendMessage(Component.text("You joined " + leader.getName() + "'s squad!", NamedTextColor.GREEN));
        leader.sendMessage(Component.text(player.getName() + " joined your squad!", NamedTextColor.GREEN));
        return true;
    }

    public void leave(Player player) {
        Squad squad = playerSquads.remove(player.getUniqueId());
        if (squad != null) {
            squad.removeMember(player.getUniqueId());
            if (squad.size() == 0) {
                squads.remove(squad);
            }
            player.sendMessage(Component.text("You left your squad.", NamedTextColor.YELLOW));
        }
    }

    public void autoFillUnpaired(List<Player> players) {
        if (maxTeamSize <= 1) return;

        List<Player> unassigned = new ArrayList<>();
        for (Player p : players) {
            Squad s = playerSquads.get(p.getUniqueId());
            if (s == null || s.size() == 1) {
                unassigned.add(p);
            }
        }

        Collections.shuffle(unassigned);
        Squad current = null;
        for (Player p : unassigned) {
            if (current == null || current.size() >= maxTeamSize) {
                current = getOrCreateSquad(p);
            } else {
                Squad old = playerSquads.get(p.getUniqueId());
                if (old != null && old != current) {
                    old.removeMember(p.getUniqueId());
                    if (old.size() == 0) squads.remove(old);
                }
                current.addMember(p);
                playerSquads.put(p.getUniqueId(), current);
            }
        }
    }

    public Set<Squad> getActiveSquads() {
        Set<Squad> active = new HashSet<>();
        for (Squad s : squads) {
            if (s.isAlive()) {
                active.add(s);
            }
        }
        return active;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker != null && !attacker.equals(victim)) {
            Squad s1 = playerSquads.get(attacker.getUniqueId());
            Squad s2 = playerSquads.get(victim.getUniqueId());

            if (s1 != null && s1 == s2) {
                event.setCancelled(true);
                attacker.sendMessage(Component.text("You can't hit your teammate.", NamedTextColor.RED));
            }
        }
    }
}
