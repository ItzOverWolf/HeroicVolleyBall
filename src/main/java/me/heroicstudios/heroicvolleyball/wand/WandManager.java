package me.heroicstudios.heroicvolleyball.wand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public final class WandManager {

    private final NamespacedKey wandKey;
    private final Map<UUID, Location> firstPoints = new HashMap<>();
    private final Map<UUID, Location> secondPoints = new HashMap<>();

    public WandManager(HeroicVolleyBall plugin) {
        this.wandKey = new NamespacedKey(plugin, "volley_wand");
    }

    public ItemStack createWand() {
        ItemStack item = new ItemStack(Material.STONE_AXE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Volleyball Wand", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Left-click a block to set point 1", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Right-click a block to set point 2", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Then use /setarena <red|blue>", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isWand(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.STONE_AXE || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public void setFirstPoint(UUID uuid, Location location) {
        firstPoints.put(uuid, location);
    }

    public void setSecondPoint(UUID uuid, Location location) {
        secondPoints.put(uuid, location);
    }

    public @Nullable Location getFirstPoint(UUID uuid) {
        return firstPoints.get(uuid);
    }

    public @Nullable Location getSecondPoint(UUID uuid) {
        return secondPoints.get(uuid);
    }

    public boolean hasCompleteSelection(UUID uuid) {
        return firstPoints.containsKey(uuid) && secondPoints.containsKey(uuid);
    }

    public void clear(UUID uuid) {
        firstPoints.remove(uuid);
        secondPoints.remove(uuid);
    }

    public void openPositionMenu(org.bukkit.entity.Player player, Location targetBlock) {
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
        new me.heroicstudios.heroicvolleyball.gui.TeamSelectGui(targetBlock).open(player);
    }
}
