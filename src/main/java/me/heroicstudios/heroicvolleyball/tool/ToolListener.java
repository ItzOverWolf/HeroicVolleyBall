package me.heroicstudios.heroicvolleyball.tool;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class ToolListener implements Listener {

    private final ToolManager toolManager;

    public ToolListener(ToolManager toolManager) {
        this.toolManager = toolManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        VolleyTool tool = toolManager.getTool(item);
        if (tool == null || !toolManager.isCycleable(tool)) {
            return;
        }

        event.setCancelled(true);
        toolManager.cycleTier(player);
    }
}
