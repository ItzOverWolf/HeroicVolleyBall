package me.heroicstudios.heroicvolleyball.gui;

import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class GuiListener implements Listener {

    private final PositionManager positionManager;
    private final Messages messages;

    public GuiListener(PositionManager positionManager, Messages messages) {
        this.positionManager = positionManager;
        this.messages = messages;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getClickedInventory();
        if (inventory == null) {
            return;
        }

        InventoryHolder holder = inventory.getHolder();
        int slot = event.getSlot();

        if (holder instanceof TeamSelectGui teamSelect) {
            event.setCancelled(true);
            if (slot == 11) {
                new PositionGui(Team.RED, teamSelect.getTarget(), positionManager).open(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            } else if (slot == 15) {
                new PositionGui(Team.BLUE, teamSelect.getTarget(), positionManager).open(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            }
            return;
        }

        if (holder instanceof PositionGui positionGui) {
            event.setCancelled(true);

            int positionNumber = PositionLayout.position(slot);
            if (positionNumber == -1) {
                return;
            }

            Location target = positionGui.getTarget();
            Team team = positionGui.getTeam();
            positionManager.savePosition("default", team, positionNumber, target);

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            String row = (positionNumber >= 2 && positionNumber <= 4) ? "Front Row" : "Back Row";
            String teamTag = Messages.teamDisplay(team);
            messages.actionBar(player, "gui.saved-actionbar",
                    "position", String.valueOf(positionNumber), "row", row, "team", teamTag);
            messages.send(player, "gui.saved-chat",
                    "position", String.valueOf(positionNumber),
                    "row", row,
                    "team", teamTag,
                    "location", target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ());
            player.closeInventory();
        }
    }
}
