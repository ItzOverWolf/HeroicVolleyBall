package me.heroicstudios.heroicvolleyball.wand;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.message.Messages;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class WandListener implements Listener {

    private final WandManager wandManager;
    private final Messages messages;

    public WandListener(WandManager wandManager, Messages messages) {
        this.wandManager = wandManager;
        this.messages = messages;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();

        if (hand == null) {
            return;
        }

        boolean mainHand = hand == EquipmentSlot.HAND;
        boolean holdingWand = wandManager.isWand(player.getInventory().getItem(hand));

        if (!mainHand || !holdingWand) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            wandManager.setFirstPoint(player.getUniqueId(), block.getLocation());

            messages.send(player, "wand.point1", "location", locationString(block));

            if (wandManager.hasCompleteSelection(player.getUniqueId())) {
                messages.send(player, "wand.both-points");
            }
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (player.isSneaking()) {
                wandManager.openPositionMenu(player, block.getLocation());
            } else {
                wandManager.setSecondPoint(player.getUniqueId(), block.getLocation());

                messages.send(player, "wand.point2", "location", locationString(block));

                if (wandManager.hasCompleteSelection(player.getUniqueId())) {
                    messages.send(player, "wand.both-points");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (wandManager.isWand(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    private String locationString(Block block) {
        return block.getX() + ", " + block.getY() + ", " + block.getZ();
    }
}
