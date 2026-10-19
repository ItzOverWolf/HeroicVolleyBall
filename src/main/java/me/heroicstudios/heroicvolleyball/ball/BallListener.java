package me.heroicstudios.heroicvolleyball.ball;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import me.heroicstudios.heroicvolleyball.game.GameSession;
import me.heroicstudios.heroicvolleyball.game.SessionManager;
import me.heroicstudios.heroicvolleyball.tool.ToolManager;
import me.heroicstudios.heroicvolleyball.tool.VolleyTool;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BallListener implements Listener {

    private final SessionManager sessionManager;
    private final ToolManager toolManager;
    // Ball hits already handled this server tick, per player. Keeps the swing
    // fallback and the vanilla attack/damage events from processing the same
    // click twice (whichever arrives first wins).
    private final Map<UUID, Integer> handledHitTick = new HashMap<>();

    public BallListener(SessionManager sessionManager, ToolManager toolManager) {
        this.sessionManager = sessionManager;
        this.toolManager = toolManager;
    }

    @EventHandler
    public void onPreAttack(PrePlayerAttackEntityEvent event) {
        GameSession session = sessionManager.getByBall(event.getAttacked());
        if (session == null) {
            return;
        }

        event.setCancelled(true);
        if (markTick(event.getPlayer())) {
            handleHit(session, event.getPlayer());
        }
    }

    // Fallback for clicks the vanilla attack packet never turns into a hit:
    // fast-falling ball, client and server disagree about its position. A
    // swing whose crosshair ray reaches the server-side ball counts instead.
    @EventHandler
    public void onSwing(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (alreadyHandled(player)) {
            return;
        }
        if (toolManager.getTool(player.getInventory().getItemInMainHand()) == null) {
            return;
        }
        for (GameSession session : sessionManager.sessions()) {
            if (!session.getBall().tryReachHit(player)) {
                continue;
            }
            handledHitTick.put(player.getUniqueId(), Bukkit.getCurrentTick());
            handleHit(session, player);
            return;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handledHitTick.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getDamageSource().getDirectEntity() != null
                && sessionManager.getByBall(event.getDamageSource().getDirectEntity()) != null) {
            event.setCancelled(true);
            return;
        }

        GameSession session = sessionManager.getByBall(event.getEntity());
        if (session == null) {
            return;
        }

        event.setCancelled(true);

        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player player) {
            if (markTick(player)) {
                handleHit(session, player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractBall(PlayerInteractEntityEvent event) {
        GameSession session = sessionManager.getByBall(event.getRightClicked());
        if (session == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.getType().isAir()) {
            return;
        }
        if (!session.getBall().canPickupBall()) {
            session.getBall().sendPickupDenied(player);
            return;
        }
        session.getBall().pickupBall(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlaceBall(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        GameSession session = resolveSession(player);
        if (session == null) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!session.getBall().isBallItem(item)) {
            return;
        }

        event.setCancelled(true);
        session.getBall().placeBall(player, event.getClickedBlock());
    }

    private @Nullable GameSession resolveSession(Player player) {
        GameSession session = sessionManager.get(player);
        if (session != null) {
            return session;
        }
        return sessionManager.getByBall(player);
    }

    private boolean alreadyHandled(Player player) {
        Integer previous = handledHitTick.get(player.getUniqueId());
        return previous != null && previous == Bukkit.getCurrentTick();
    }

    private boolean markTick(Player player) {
        if (alreadyHandled(player)) {
            return false;
        }
        handledHitTick.put(player.getUniqueId(), Bukkit.getCurrentTick());
        return true;
    }

    private void handleHit(GameSession session, Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        VolleyTool tool = toolManager.getTool(item);
        if (tool == null) {
            return;
        }
        session.getBall().onPlayerHit(player, tool, toolManager.getTier(item));
    }
}
