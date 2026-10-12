package me.heroicstudios.heroicvolleyball.gui;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class TeamSelectGui implements InventoryHolder {

    private final Inventory inventory;

    public TeamSelectGui() {
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Which team?", NamedTextColor.DARK_GRAY));
        fill();
    }

    private void fill() {
        ItemStack red = new ItemStack(Material.RED_WOOL);
        ItemMeta redMeta = red.getItemMeta();
        redMeta.displayName(Component.text("Team Red", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
        redMeta.lore(List.of());
        red.setItemMeta(redMeta);
        inventory.setItem(11, red);

        ItemStack blue = new ItemStack(Material.BLUE_WOOL);
        ItemMeta blueMeta = blue.getItemMeta();
        blueMeta.displayName(Component.text("Team Blue", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
        blueMeta.lore(List.of());
        blue.setItemMeta(blueMeta);
        inventory.setItem(15, blue);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
