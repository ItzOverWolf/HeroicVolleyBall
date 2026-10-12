package me.heroicstudios.heroicvolleyball.tool;

import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public enum VolleyTier {

    WOODEN("Wooden", NamedTextColor.GRAY),
    IRON("Iron", NamedTextColor.WHITE),
    DIAMOND("Diamond", NamedTextColor.AQUA);

    private final String displayName;
    private final NamedTextColor color;

    VolleyTier(String displayName, NamedTextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public VolleyTier next() {
        VolleyTier[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static @Nullable VolleyTier fromString(String input) {
        for (VolleyTier tier : values()) {
            if (tier.name().equalsIgnoreCase(input)) {
                return tier;
            }
        }
        return null;
    }
}
