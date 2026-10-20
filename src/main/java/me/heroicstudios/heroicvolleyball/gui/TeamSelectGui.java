package me.heroicstudios.heroicvolleyball.gui;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

public final class TeamSelectGui implements InventoryHolder {

    private final Location target;
    private final Inventory inventory;

    public TeamSelectGui(Location target) {
        this.target = target;
        this.inventory = Bukkit.createInventory(this, PositionLayout.SIZE, me.heroicstudios.heroicvolleyball.util.Text.parse("<#f08e26><bold>CHOOSE YOUR TEAM"));
        PositionLayout.fillFrame(inventory);
        fill();
    }

    private void fill() {
        inventory.setItem(11, teamItem(Material.RED_WOOL, "Team Red", NamedTextColor.RED));
        inventory.setItem(15, teamItem(Material.BLUE_WOOL, "Team Blue", NamedTextColor.BLUE));
    }

    private ItemStack teamItem(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                Component.text("Click to assign this position", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    public Location getTarget() {
        return target;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
