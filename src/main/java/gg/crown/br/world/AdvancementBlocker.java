package gg.crown.br.world;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementBlocker implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGrantCriterion(PlayerAdvancementCriterionGrantEvent event) {
        // Suppress criteria granting so toast notifications never appear
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement adv = event.getAdvancement();
        AdvancementProgress progress = player.getAdvancementProgress(adv);

        // Immediately revoke every awarded criterion
        for (String criterion : progress.getAwardedCriteria()) {
            progress.revokeCriteria(criterion);
        }
    }
}
