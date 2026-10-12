package me.heroicstudios.heroicvolleyball.tool;

import org.jetbrains.annotations.Nullable;

public enum VolleyTool {

    SHOVEL,
    SWORD,
    SHIELD;

    public static @Nullable VolleyTool fromString(String input) {
        if (input.equalsIgnoreCase("spear")) {
            return SWORD;
        }
        for (VolleyTool tool : values()) {
            if (tool.name().equalsIgnoreCase(input)) {
                return tool;
            }
        }
        return null;
    }
}
