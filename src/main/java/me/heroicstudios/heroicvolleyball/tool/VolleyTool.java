package me.heroicstudios.heroicvolleyball.tool;

import org.jetbrains.annotations.Nullable;

public enum VolleyTool {

    SHOVEL,
    SPEAR,
    SHIELD;

    public static @Nullable VolleyTool fromString(String input) {
        for (VolleyTool tool : values()) {
            if (tool.name().equalsIgnoreCase(input)) {
                return tool;
            }
        }
        return null;
    }
}
