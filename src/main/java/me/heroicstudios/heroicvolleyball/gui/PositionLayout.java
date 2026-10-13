package me.heroicstudios.heroicvolleyball.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class PositionLayout {

    public static final int SIZE = 36;

    private static final int[] SLOTS = new int[7];

    static {
        SLOTS[4] = 11;
        SLOTS[3] = 13;
        SLOTS[2] = 15;
        SLOTS[5] = 20;
        SLOTS[6] = 22;
        SLOTS[1] = 24;
    }

    private PositionLayout() {
    }

    public static int slot(int position) {
        return position >= 1 && position <= 6 ? SLOTS[position] : -1;
    }

    public static int position(int slot) {
        for (int position = 1; position <= 6; position++) {
            if (SLOTS[position] == slot) {
                return position;
            }
        }
        return -1;
    }

    public static void fillFrame(Inventory inventory) {
        ItemStack frame = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = frame.getItemMeta();
        meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
        frame.setItemMeta(meta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, frame);
        }
    }
}
