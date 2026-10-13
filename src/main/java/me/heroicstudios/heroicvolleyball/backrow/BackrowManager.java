package me.heroicstudios.heroicvolleyball.backrow;

import java.util.EnumMap;
import java.util.Map;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.util.YamlFile;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public final class BackrowManager {

    private final YamlFile backrowFile;
    private final Map<Team, Arena> backrows = new EnumMap<>(Team.class);

    public BackrowManager(HeroicVolleyBall plugin) {
        this.backrowFile = new YamlFile(plugin, "backrow.yml");
        load();
    }

    public void load() {
        backrowFile.reload();
        backrows.clear();

        ConfigurationSection root = backrowFile.config().getConfigurationSection("backrow");
        if (root == null) {
            return;
        }

        for (Team team : Team.values()) {
            ConfigurationSection section = root.getConfigurationSection(team.name().toLowerCase());
            if (section == null) {
                continue;
            }
            String world = section.getString("world");
            if (world == null) {
                continue;
            }
            backrows.put(team, new Arena(
                    team,
                    world,
                    section.getInt("min.x"),
                    section.getInt("min.y"),
                    section.getInt("min.z"),
                    section.getInt("max.x"),
                    section.getInt("max.y"),
                    section.getInt("max.z")
            ));
        }
    }

    public void setBackrow(Team team, Location first, Location second) {
        backrows.put(team, new Arena(team, first.getWorld(), first, second));
        save();
    }

    public void clear(Team team) {
        backrows.remove(team);
        save();
    }

    private void save() {
        backrowFile.config().set("backrow", null);
        for (Map.Entry<Team, Arena> entry : backrows.entrySet()) {
            Arena backrow = entry.getValue();
            String path = "backrow." + entry.getKey().name().toLowerCase();
            backrowFile.config().set(path + ".world", backrow.getWorld());
            backrowFile.config().set(path + ".min.x", backrow.getMinX());
            backrowFile.config().set(path + ".min.y", backrow.getMinY());
            backrowFile.config().set(path + ".min.z", backrow.getMinZ());
            backrowFile.config().set(path + ".max.x", backrow.getMaxX());
            backrowFile.config().set(path + ".max.y", backrow.getMaxY());
            backrowFile.config().set(path + ".max.z", backrow.getMaxZ());
        }
        backrowFile.save();
    }

    public @Nullable Arena getBackrow(Team team) {
        return backrows.get(team);
    }

    public boolean isSet(Team team) {
        return backrows.containsKey(team);
    }
}