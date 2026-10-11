package me.heroicstudios.heroicvolleyball.arena;

import java.util.EnumMap;
import java.util.Map;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public final class ArenaManager {

    private final HeroicVolleyBall plugin;
    private final Map<Team, Arena> arenas = new EnumMap<>(Team.class);

    public ArenaManager(HeroicVolleyBall plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        arenas.clear();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("arenas");
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

            arenas.put(team, new Arena(
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

    public void save() {
        plugin.getConfig().set("arenas", null);

        for (Map.Entry<Team, Arena> entry : arenas.entrySet()) {
            Arena arena = entry.getValue();
            String path = "arenas." + arena.getTeam().name().toLowerCase();

            plugin.getConfig().set(path + ".world", arena.getWorld());
            plugin.getConfig().set(path + ".min.x", arena.getMinX());
            plugin.getConfig().set(path + ".min.y", arena.getMinY());
            plugin.getConfig().set(path + ".min.z", arena.getMinZ());
            plugin.getConfig().set(path + ".max.x", arena.getMaxX());
            plugin.getConfig().set(path + ".max.y", arena.getMaxY());
            plugin.getConfig().set(path + ".max.z", arena.getMaxZ());
        }

        plugin.saveConfig();
    }

    public void setArena(Team team, Arena arena) {
        arenas.put(team, arena);
        save();
    }

    public @Nullable Arena getArena(Team team) {
        return arenas.get(team);
    }

    public boolean isSet(Team team) {
        return arenas.containsKey(team);
    }

    public @Nullable Team getArenaAt(Location location) {
        for (Arena arena : arenas.values()) {
            if (arena.contains(location)) {
                return arena.getTeam();
            }
        }
        return null;
    }
}
