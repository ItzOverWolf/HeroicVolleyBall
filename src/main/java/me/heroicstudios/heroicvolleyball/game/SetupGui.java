package me.heroicstudios.heroicvolleyball.game;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.Side;
import me.heroicstudios.heroicvolleyball.gui.PositionLayout;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class SetupGui implements Listener {

    private static final int RED_SIDE_SLOT = 29;
    private static final int BLUE_SIDE_SLOT = 31;

    private final HeroicVolleyBall plugin;
    private final SessionManager sessionManager;

    public SetupGui(HeroicVolleyBall plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    public void openSolo(Player player, Arena arena) {
        if (sessionManager.get(player) != null) {
            plugin.getMessages().send(player, "game.already-in-game");
            return;
        }
        if (arena == null) {
            plugin.getMessages().send(player, "arena.not-in-arena");
            return;
        }
        if (!arena.isComplete()) {
            plugin.getMessages().send(player, "arena.incomplete", "arena", arena.getName());
            return;
        }
        Holder holder = new Holder(arena);
        Inventory inventory = Bukkit.createInventory(holder, PositionLayout.SIZE, me.heroicstudios.heroicvolleyball.util.Text.parse("<#f08e26><bold>CHOOSE YOUR POSITION"));
        holder.inventory = inventory;
        renderSolo(holder, inventory);
        player.openInventory(inventory);
    }

    private void renderSolo(Holder holder, Inventory inventory) {
        PositionLayout.fillFrame(inventory);
        for (int number = 1; number <= 6; number++) {
            ItemStack item = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("Position " + number, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("You will stand here on the side you pick below",
                    NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
            inventory.setItem(PositionLayout.slot(number), item);
        }
        inventory.setItem(RED_SIDE_SLOT, sideButton(Side.RED, holder.side == Side.RED));
        inventory.setItem(BLUE_SIDE_SLOT, sideButton(Side.BLUE, holder.side == Side.BLUE));
    }

    /** A wool button that shows which side the solo player picked. */
    private ItemStack sideButton(Side side, boolean selected) {
        Material material = side.getWool();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(side.getDisplayName() + " side",
                        selected ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(selected
                        ? "Selected - click a position to start here"
                        : "Click to play on this side",
                NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        if (selected) {
            lore.add(Component.text("You will hear the win sound for this side",
                    NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        if (selected) {
            meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != top) {
            return;
        }
        int slot = event.getSlot();
        if (slot == RED_SIDE_SLOT || slot == BLUE_SIDE_SLOT) {
            holder.side = slot == RED_SIDE_SLOT ? Side.RED : Side.BLUE;
            renderSolo(holder, event.getView().getTopInventory());
            plugin.getMessages().actionBar(player, "game.solo-side-switch",
                    "side", holder.side == Side.RED ? "<red>RED" : "<blue>BLUE");
            return;
        }
        int number = PositionLayout.position(slot);
        if (number < 1 || number > 6) {
            return;
        }
        player.closeInventory();
        if (sessionManager.isOccupied(holder.arena)) {
            plugin.getMessages().send(player, "game.already-running", "arena", holder.arena.getName());
            return;
        }
        if (sessionManager.atCapacity()) {
            plugin.getMessages().send(player, "session.capacity");
            return;
        }
        sessionManager.startSolo(holder.arena, player, number, holder.side);
        plugin.getMessages().send(player, "game.solo-started", "position", String.valueOf(number),
                "side", holder.side == Side.RED ? "<red>RED" : "<blue>BLUE");
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // no-op: solo setup simply closes
    }

    private static final class Holder implements InventoryHolder {
        private final Arena arena;
        private Inventory inventory;
        private Side side = Side.RED;

        private Holder(Arena arena) {
            this.arena = arena;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
