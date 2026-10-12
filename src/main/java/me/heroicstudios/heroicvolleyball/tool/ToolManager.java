package me.heroicstudios.heroicvolleyball.tool;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public final class ToolManager {

    private final NamespacedKey toolKey;
    private final NamespacedKey tierKey;

    public ToolManager(HeroicVolleyBall plugin) {
        this.toolKey = new NamespacedKey(plugin, "volley_tool");
        this.tierKey = new NamespacedKey(plugin, "volley_tier");
    }

    public ItemStack create(VolleyTool tool, VolleyTier tier) {
        Material material;
        String displayName;
        NamedTextColor color;

        switch (tool) {
            case SHOVEL -> {
                material = shovelMaterial(tier);
                displayName = "Up";
                color = tier.getColor();
            }
            case SWORD -> {
                material = swordMaterial(tier);
                displayName = "Spike";
                color = NamedTextColor.RED;
            }
            default -> {
                material = Material.SHIELD;
                displayName = "Block";
                color = NamedTextColor.BLUE;
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName, color).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (tool == VolleyTool.SHOVEL) {
            lore.add(loreLine("Tier: " + tier.getDisplayName(), tier.getColor()));
            lore.add(loreLine("Hit the ball to launch it straight up", NamedTextColor.GRAY));
            lore.add(loreLine("Sneak + right-click to cycle power", NamedTextColor.DARK_GRAY));
        } else if (tool == VolleyTool.SWORD) {
            lore.add(loreLine("Tier: " + tier.getDisplayName(), tier.getColor()));
            lore.add(loreLine("Hit the ball to spike it forward", NamedTextColor.GRAY));
            lore.add(loreLine("Aim downward from above for a spike", NamedTextColor.DARK_GRAY));
        } else {
            lore.add(loreLine("Hit the ball with your shield to drive it down", NamedTextColor.GRAY));
        }
        meta.lore(lore);
        meta.setUnbreakable(true);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(toolKey, PersistentDataType.STRING, tool.name());
        pdc.set(tierKey, PersistentDataType.STRING, tier.name());

        item.setItemMeta(meta);
        return item;
    }

    public boolean isTool(@Nullable ItemStack item) {
        return getTool(item) != null;
    }

    public @Nullable VolleyTool getTool(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String value = meta.getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return VolleyTool.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public VolleyTier getTier(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return VolleyTier.WOODEN;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return VolleyTier.WOODEN;
        }
        String value = meta.getPersistentDataContainer().get(tierKey, PersistentDataType.STRING);
        if (value == null) {
            return VolleyTier.WOODEN;
        }
        try {
            return VolleyTier.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return VolleyTier.WOODEN;
        }
    }

    public void cycleShovelTier(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        VolleyTier next = getTier(item).next();
        player.getInventory().setItemInMainHand(create(VolleyTool.SHOVEL, next));
        player.sendMessage(Component.text("Up power set to ", NamedTextColor.GRAY)
                .append(Component.text(next.getDisplayName(), next.getColor())));
    }

    private Component loreLine(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private Material shovelMaterial(VolleyTier tier) {
        return switch (tier) {
            case WOODEN -> Material.WOODEN_SHOVEL;
            case IRON -> Material.IRON_SHOVEL;
            case DIAMOND -> Material.DIAMOND_SHOVEL;
        };
    }

    private Material swordMaterial(VolleyTier tier) {
        return switch (tier) {
            case WOODEN -> Material.WOODEN_SWORD;
            case IRON -> Material.IRON_SWORD;
            case DIAMOND -> Material.DIAMOND_SWORD;
        };
    }
}
