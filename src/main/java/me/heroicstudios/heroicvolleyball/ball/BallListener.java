package me.heroicstudios.heroicvolleyball.ball;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import me.heroicstudios.heroicvolleyball.tool.ToolManager;
import me.heroicstudios.heroicvolleyball.tool.VolleyTool;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public final class BallListener implements Listener {

    private final BallManager ballManager;
    private final ToolManager toolManager;

    public BallListener(BallManager ballManager, ToolManager toolManager) {
        this.ballManager = ballManager;
        this.toolManager = toolManager;
    }

    @EventHandler
    public void onPreAttack(PrePlayerAttackEntityEvent event) {
        if (!ballManager.isBall(event.getAttacked())) {
            return;
        }

        event.setCancelled(true);
        handleHit(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getDamageSource().getDirectEntity() != null
                && ballManager.isBall(event.getDamageSource().getDirectEntity())) {
            event.setCancelled(true);
            return;
        }

        if (!ballManager.isBall(event.getEntity())) {
            return;
        }

        event.setCancelled(true);

        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player player) {
            handleHit(player);
        }
    }

    private void handleHit(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        VolleyTool tool = toolManager.getTool(item);
        if (tool == null) {
            return;
        }
        ballManager.onPlayerHit(player, tool, toolManager.getTier(item));
    }
}
