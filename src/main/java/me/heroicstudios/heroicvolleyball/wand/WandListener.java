package me.heroicstudios.heroicvolleyball.wand;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    public WandListener(WandManager wandManager) {
        this.wandManager = wandManager;
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

            player.sendMessage(Component.text("Point 1 set to ", NamedTextColor.GRAY)
                    .append(Component.text(locationString(block), NamedTextColor.AQUA)));

            if (wandManager.hasCompleteSelection(player.getUniqueId())) {
                player.sendMessage(Component.text("Both points set! Use /setarena <red|blue>.", NamedTextColor.GREEN));
            }
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (player.isSneaking()) {
                wandManager.openPositionMenu(player, block.getLocation());
            } else {
                wandManager.setSecondPoint(player.getUniqueId(), block.getLocation());

                player.sendMessage(Component.text("Point 2 set to ", NamedTextColor.GRAY)
                        .append(Component.text(locationString(block), NamedTextColor.AQUA)));

                if (wandManager.hasCompleteSelection(player.getUniqueId())) {
                    player.sendMessage(Component.text("Both points set! Use /setarena <red|blue>.", NamedTextColor.GREEN));
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
