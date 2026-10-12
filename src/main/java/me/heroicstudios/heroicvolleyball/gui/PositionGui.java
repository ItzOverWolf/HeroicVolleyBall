package me.heroicstudios.heroicvolleyball.gui;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
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

public final class PositionGui implements InventoryHolder {

    private final Team team;
    private final Location target;
    private final PositionManager positionManager;
    private final Inventory inventory;

    public PositionGui(Team team, Location target, PositionManager positionManager) {
        this.team = team;
        this.target = target;
        this.positionManager = positionManager;
        this.inventory = Bukkit.createInventory(this, PositionLayout.SIZE,
                Component.text("Set " + team.getDisplayName() + " position"));
        PositionLayout.fillFrame(inventory);
        fillPositions();
    }

    private void fillPositions() {
        Material material = team == Team.RED ? Material.RED_STAINED_GLASS_PANE : Material.BLUE_STAINED_GLASS_PANE;
        for (int position = 1; position <= 6; position++) {
            String row = (position >= 2 && position <= 4) ? "Front row" : "Back row";
            List<Component> lore = new ArrayList<>();
            lore.add(line(row, NamedTextColor.AQUA));

            Location existing = positionManager != null ? positionManager.getPosition(team, position) : null;
            if (existing != null) {
                lore.add(line("Currently at " + existing.getBlockX() + ", " + existing.getBlockY() + ", "
                        + existing.getBlockZ(), NamedTextColor.GRAY));
            } else {
                lore.add(line("Not set yet", NamedTextColor.DARK_GRAY));
            }
            lore.add(line("Click to set here", NamedTextColor.GREEN));

            inventory.setItem(PositionLayout.slot(position),
                    item(material, "Position " + position, team.getColor(), lore));
        }
    }

    private ItemStack item(Material material, String name, NamedTextColor color, List<Component> lore) {
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

    public Team getTeam() {
        return team;
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
