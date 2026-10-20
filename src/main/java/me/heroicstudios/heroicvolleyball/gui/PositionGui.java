package me.heroicstudios.heroicvolleyball.gui;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Side;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class PositionGui implements InventoryHolder {

    private final Side side;
    private final Location target;
    private final Arena arena;
    private final ArenaManager arenaManager;
    private final Inventory inventory;

    public PositionGui(Side side, Location target, Arena arena, ArenaManager arenaManager) {
        this.side = side;
        this.target = target;
        this.arena = arena;
        this.arenaManager = arenaManager;
        this.inventory = Bukkit.createInventory(this, PositionLayout.SIZE,
                me.heroicstudios.heroicvolleyball.util.Text.parse("<#f08e26><bold>SET POSITION"));
        PositionLayout.fillFrame(inventory);
        fillPositions();
    }

    private void fillPositions() {
        Material material = side.getPane();
        for (int position = 1; position <= 6; position++) {
            String row = (position >= 2 && position <= 4) ? "Front row" : "Back row";
            List<Component> lore = new ArrayList<>();
            lore.add(line("Arena: " + arena.getName(), NamedTextColor.WHITE));
            lore.add(line(row, NamedTextColor.AQUA));

            Location existing = arena.getPosition(side, position);
            if (existing != null) {
                lore.add(line("Currently at " + existing.getBlockX() + ", " + existing.getBlockY() + ", "
                        + existing.getBlockZ(), NamedTextColor.GRAY));
            } else {
                lore.add(line("Not set yet", NamedTextColor.DARK_GRAY));
            }
            lore.add(line("Click to set here", NamedTextColor.GREEN));

            inventory.setItem(PositionLayout.slot(position),
                    item(material, "Position " + position, arena.teamColor(side), lore));
        }
    }

    private ItemStack item(Material material, String name, TextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    public Side getSide() {
        return side;
    }

    public Location getTarget() {
        return target;
    }

    public Arena getArena() {
        return arena;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
