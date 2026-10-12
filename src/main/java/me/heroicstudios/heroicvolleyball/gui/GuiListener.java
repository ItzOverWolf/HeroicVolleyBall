package me.heroicstudios.heroicvolleyball.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class GuiListener implements Listener {

    private final PositionManager positionManager;
    private final Map<UUID, Team> teamSelection = new HashMap<>();
    private final Map<UUID, LocationSelection> locationSelections = new HashMap<>();

    private static final class LocationSelection {
        final String arenaName;
        final org.bukkit.Location targetBlock;

        LocationSelection(String arenaName, org.bukkit.Location targetBlock) {
            this.arenaName = arenaName;
            this.targetBlock = targetBlock;
        }
    }

    public GuiListener(PositionManager positionManager) {
        this.positionManager = positionManager;
    }

    public void storeSelection(UUID uuid, String arenaName, org.bukkit.Location targetBlock) {
        locationSelections.put(uuid, new LocationSelection(arenaName, targetBlock));
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

        if (holder instanceof TeamSelectGui) {
            event.setCancelled(true);
            if (slot == 11) {
                teamSelection.put(player.getUniqueId(), Team.RED);
                player.openInventory(new PositionGui(Team.RED).getInventory());
            } else if (slot == 15) {
                teamSelection.put(player.getUniqueId(), Team.BLUE);
                player.openInventory(new PositionGui(Team.BLUE).getInventory());
            }
            return;
        }

        if (holder instanceof PositionGui positionGui) {
            event.setCancelled(true);
            Team selectedTeam = positionGui.getTeam();
            if (selectedTeam == null) {
                selectedTeam = teamSelection.get(player.getUniqueId());
            }
            if (selectedTeam == null) {
                return;
            }

            LocationSelection locSel = locationSelections.get(player.getUniqueId());
            if (locSel == null) {
                player.closeInventory();
                return;
            }

            int positionNumber = getPositionNumber(slot);
            if (positionNumber == -1) {
                return;
            }

            positionManager.savePosition(locSel.arenaName, selectedTeam, positionNumber, locSel.targetBlock);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            String rowType = (positionNumber >= 2 && positionNumber <= 4) ? "Front Row" : "Back Row";
            String teamName = selectedTeam == Team.RED ? "Red" : "Blue";
            player.sendMessage(Component.text("Set Position " + positionNumber + " (" + rowType + ") for Team " + teamName + "!", NamedTextColor.GREEN));
            player.closeInventory();
            teamSelection.remove(player.getUniqueId());
            locationSelections.remove(player.getUniqueId());
        }
    }

    private int getPositionNumber(int slot) {
        switch (slot) {
            case 11: return 4;
            case 13: return 3;
            case 15: return 2;
            case 29: return 5;
            case 31: return 6;
            case 33: return 1;
            default: return -1;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof PositionGui) {
            teamSelection.remove(player.getUniqueId());
            locationSelections.remove(player.getUniqueId());
        } else if (holder instanceof TeamSelectGui) {
            teamSelection.remove(player.getUniqueId());
            locationSelections.remove(player.getUniqueId());
        }
    }
}
