package me.heroicstudios.heroicvolleyball.arena;

import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public enum Team {

    RED("Red", NamedTextColor.RED),
    BLUE("Blue", NamedTextColor.BLUE);

    private final String displayName;
    private final NamedTextColor color;

    Team(String displayName, NamedTextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public static @Nullable Team fromString(String input) {
        for (Team team : values()) {
            if (team.name().equalsIgnoreCase(input)) {
                return team;
            }
        }
        return null;
    }
}
