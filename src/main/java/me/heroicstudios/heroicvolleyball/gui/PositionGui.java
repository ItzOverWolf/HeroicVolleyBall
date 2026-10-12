package me.heroicstudios.heroicvolleyball.gui;

import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class PositionGui implements InventoryHolder {

    private final Team team;
    private final Inventory inventory;

    public PositionGui(Team team) {
        this.team = team;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Select Position", NamedTextColor.DARK_GRAY));
        fillFrame();
        fillPositions();
    }

    private void fillFrame() {
        ItemStack frameItem = createItem(Material.GRAY_STAINED_GLASS_PANE, NamedTextColor.GRAY, "");
        for (int i = 0; i < 54; i++) {
            if (isFrameSlot(i)) {
                inventory.setItem(i, frameItem);
            }
        }
    }

    private boolean isFrameSlot(int slot) {
        return slot < 9 || slot > 44 || slot % 9 == 0 || slot % 9 == 8;
    }

    private void fillPositions() {
        Material glass = team == Team.RED ? Material.RED_STAINED_GLASS_PANE : Material.BLUE_STAINED_GLASS_PANE;

        inventory.setItem(11, createItem(glass, NamedTextColor.WHITE, "Pos 4"));
        inventory.setItem(13, createItem(glass, NamedTextColor.WHITE, "Pos 3"));
        inventory.setItem(15, createItem(glass, NamedTextColor.WHITE, "Pos 2"));
        inventory.setItem(29, createItem(glass, NamedTextColor.WHITE, "Pos 5"));
        inventory.setItem(31, createItem(glass, NamedTextColor.WHITE, "Pos 6"));
        inventory.setItem(33, createItem(glass, NamedTextColor.WHITE, "Pos 1"));
    }

    private ItemStack createItem(Material material, NamedTextColor color, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (name != null && !name.isEmpty()) {
            meta.displayName(Component.text(name, color).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        } else {
            meta.displayName(Component.empty().decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        meta.lore(List.of());
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public Team getTeam() {
        return team;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}