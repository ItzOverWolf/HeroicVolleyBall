package me.heroicstudios.heroicvolleyball.gui;

import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Side;
import me.heroicstudios.heroicvolleyball.message.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

public final class GuiListener implements Listener {

    private final ArenaManager arenaManager;
    private final Messages messages;

    public GuiListener(ArenaManager arenaManager, Messages messages) {
        this.arenaManager = arenaManager;
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
            Side side = sideAt(slot);
            if (side == null) {
                return;
            }
            Arena arena = resolveArena(teamSelect.getTarget());
            if (arena == null) {
                messages.send(player, "arena.none");
                return;
            }
            new PositionGui(side, teamSelect.getTarget(), arena, arenaManager).open(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            return;
        }

        if (holder instanceof PositionGui positionGui) {
            event.setCancelled(true);

            int positionNumber = PositionLayout.position(slot);
            if (positionNumber == -1) {
                return;
            }

            Location target = positionGui.getTarget();
            Side side = positionGui.getSide();
            Arena arena = positionGui.getArena();
            arena.setPosition(side, positionNumber, target);
            arenaManager.save(arena);

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            String row = (positionNumber >= 2 && positionNumber <= 4) ? "Front Row" : "Back Row";
            String teamTag = Messages.teamDisplay(side);
            messages.actionBar(player, "gui.saved-actionbar",
                    "position", String.valueOf(positionNumber), "row", row, "team", teamTag,
                    "arena", arena.getName());
            messages.send(player, "gui.saved-chat",
                    "position", String.valueOf(positionNumber),
                    "row", row,
                    "team", teamTag,
                    "arena", arena.getName(),
                    "location", target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ());
            player.closeInventory();
        }
    }

    private @Nullable Arena resolveArena(Location target) {
        Arena arena = arenaManager.arenaAt(target);
        return arena != null ? arena : arenaManager.getDefault();
    }

    private @Nullable Side sideAt(int slot) {
        if (slot == 11) {
            return Side.RED;
        }
        if (slot == 15) {
            return Side.BLUE;
        }
        return null;
    }
}
