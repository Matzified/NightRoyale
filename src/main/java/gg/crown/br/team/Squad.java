package gg.crown.br.team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class Squad {

    private final UUID id;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> pendingInvites = new HashSet<>();
    private UUID leader;

    public Squad(Player leader) {
        this.id = UUID.randomUUID();
        this.leader = leader.getUniqueId();
        this.members.add(leader.getUniqueId());
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getPendingInvites() {
        return pendingInvites;
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public void addMember(Player player) {
        members.add(player.getUniqueId());
        pendingInvites.remove(player.getUniqueId());
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        if (uuid.equals(leader) && !members.isEmpty()) {
            leader = members.iterator().next();
        }
    }

    public int size() {
        return members.size();
    }

    public boolean isAlive() {
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !p.isDead()) {
                return true;
            }
        }
        return false;
    }
}
