package me.heroicstudios.heroicvolleyball.gui;

import me.heroicstudios.heroicvolleyball.arena.Team;
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

    public GuiListener(PositionManager positionManager) {
        this.positionManager = positionManager;
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
            player.sendActionBar(Component.text("Saved ", NamedTextColor.GREEN)
                    .append(Component.text("Position " + positionNumber, team.getColor()))
                    .append(Component.text(" (" + row + ") for " + team.getDisplayName(), NamedTextColor.GRAY)));
            player.sendMessage(Component.text("Saved Position " + positionNumber + " (" + row + ") for Team "
                            + team.getDisplayName() + " at ", NamedTextColor.GREEN)
                    .append(Component.text(target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ(),
                            NamedTextColor.AQUA)));
            player.closeInventory();
        }
    }
}
