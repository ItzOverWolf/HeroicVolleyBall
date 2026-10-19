package me.heroicstudios.heroicvolleyball.ball;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import me.heroicstudios.heroicvolleyball.tool.ToolManager;
import me.heroicstudios.heroicvolleyball.tool.VolleyTool;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class BallListener implements Listener {

    private final BallManager ballManager;
    private final ToolManager toolManager;

    public BallListener(BallManager ballManager, ToolManager toolManager) {
        this.ballManager = ballManager;
        this.toolManager = toolManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ballManager.applyBallVisibility(event.getPlayer());
        ballManager.notifyVersionMismatch(event.getPlayer());
    }

    // Ghost balls saved to disk by older builds live in chunks that may not be
    // loaded yet at startup - sweep them the moment their entities come in.
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (ballManager.isGhostBall(entity)) {
                entity.remove();
            }
        }
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

    @EventHandler(ignoreCancelled = true)
    public void onInteractBall(PlayerInteractEntityEvent event) {
        if (!ballManager.isBall(event.getRightClicked())) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.getType().isAir()) {
            return;
        }
        if (!ballManager.canPickupBall()) {
            ballManager.sendPickupDenied(player);
            return;
        }
        ballManager.pickupBall(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlaceBall(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!ballManager.isBallItem(item)) {
            return;
        }

        event.setCancelled(true);
        ballManager.placeBall(event.getPlayer(), event.getClickedBlock());
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
