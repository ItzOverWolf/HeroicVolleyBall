package me.heroicstudios.heroicvolleyball.arena;

import java.util.EnumMap;
import java.util.Map;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.util.YamlFile;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

public final class ArenaManager {

    private final HeroicVolleyBall plugin;
    private final YamlFile arenasFile;
    private final Map<Team, Arena> arenas = new EnumMap<>(Team.class);

    public ArenaManager(HeroicVolleyBall plugin) {
        this.plugin = plugin;
        this.arenasFile = new YamlFile(plugin, "arenas.yml");
        migrateFromConfig();
        load();
    }

    private void migrateFromConfig() {
        FileConfiguration config = plugin.getConfig();
        if (!config.isConfigurationSection("arenas")) {
            return;
        }
        ConfigurationSection section = config.getConfigurationSection("arenas");
        for (String key : section.getKeys(false)) {
            if (!arenasFile.config().contains("arenas." + key)) {
                arenasFile.config().set("arenas." + key, section.get(key));
            }
        }
        arenasFile.save();
        config.set("arenas", null);
        plugin.saveConfig();
    }

    public void load() {
        arenasFile.reload();
        arenas.clear();

        ConfigurationSection root = arenasFile.config().getConfigurationSection("arenas");
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
        arenasFile.config().set("arenas", null);

        for (Map.Entry<Team, Arena> entry : arenas.entrySet()) {
            Arena arena = entry.getValue();
            String path = "arenas." + arena.getTeam().name().toLowerCase();

            arenasFile.config().set(path + ".world", arena.getWorld());
            arenasFile.config().set(path + ".min.x", arena.getMinX());
            arenasFile.config().set(path + ".min.y", arena.getMinY());
            arenasFile.config().set(path + ".min.z", arena.getMinZ());
            arenasFile.config().set(path + ".max.x", arena.getMaxX());
            arenasFile.config().set(path + ".max.y", arena.getMaxY());
            arenasFile.config().set(path + ".max.z", arena.getMaxZ());
        }

        arenasFile.save();
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
