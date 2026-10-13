package me.heroicstudios.heroicvolleyball.tool;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public final class ToolManager {

    private final HeroicVolleyBall plugin;
    private final NamespacedKey toolKey;
    private final NamespacedKey tierKey;

    public ToolManager(HeroicVolleyBall plugin) {
        this.plugin = plugin;
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

        String tierString = "<" + tier.getColor().asHexString() + ">" + tier.getDisplayName()
                + "</" + tier.getColor().asHexString() + ">";
        String loreKey = switch (tool) {
            case SHOVEL -> "tool.shovel-lore";
            case SWORD -> "tool.sword-lore";
            default -> "tool.shield-lore";
        };
        meta.lore(plugin.getMessages().getLines(loreKey, "tier", tierString));

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

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

    public void cycleTier(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        VolleyTool tool = getTool(item);
        if (tool == null || !isCycleable(tool)) {
            return;
        }
        VolleyTier next = getTier(item).next();
        player.getInventory().setItemInMainHand(create(tool, next));
        String label = tool == VolleyTool.SWORD ? "Spike" : "Up";
        String coloredTier = "<" + next.getColor().asHexString() + ">" + next.getDisplayName()
                + "</" + next.getColor().asHexString() + ">";
        plugin.getMessages().send(player, "tool.power-set", "tool", label, "tier", coloredTier);
    }

    public boolean isCycleable(VolleyTool tool) {
        return tool == VolleyTool.SHOVEL || tool == VolleyTool.SWORD;
    }

    public void giveKit(Player player) {
        player.getInventory().addItem(create(VolleyTool.SHOVEL, VolleyTier.WOODEN));
        player.getInventory().addItem(create(VolleyTool.SWORD, VolleyTier.WOODEN));
        player.getInventory().addItem(create(VolleyTool.SHIELD, VolleyTier.WOODEN));
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
