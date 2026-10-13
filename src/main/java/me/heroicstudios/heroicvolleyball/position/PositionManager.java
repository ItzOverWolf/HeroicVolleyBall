package me.heroicstudios.heroicvolleyball.position;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.util.YamlFile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

public final class PositionManager {

    private final HeroicVolleyBall plugin;
    private final YamlFile positionsFile;
    private final Map<UUID, PositionSelection> selections = new HashMap<>();

    public PositionManager(HeroicVolleyBall plugin) {
        this.plugin = plugin;
        this.positionsFile = new YamlFile(plugin, "positions.yml");
        migrateFromConfig();
    }

    private void migrateFromConfig() {
        FileConfiguration config = plugin.getConfig();
        boolean changed = false;

        if (config.isConfigurationSection("positions")) {
            copySection(config.getConfigurationSection("positions"), "positions");
            changed = true;
        }
        if (config.isConfigurationSection("facing")) {
            copySection(config.getConfigurationSection("facing"), "facing");
            changed = true;
        }

        if (changed) {
            positionsFile.save();
            config.set("positions", null);
            config.set("facing", null);
            plugin.saveConfig();
        }
    }

    private void copySection(ConfigurationSection section, String prefix) {
        for (String key : section.getKeys(true)) {
            if (section.isConfigurationSection(key)) {
                continue;
            }
            String path = prefix + "." + key;
            if (!positionsFile.config().contains(path)) {
                positionsFile.config().set(path, section.get(key));
            }
        }
    }

    public void startSelection(UUID uuid, Location targetBlock) {
        selections.put(uuid, new PositionSelection(targetBlock));
    }

    public @Nullable PositionSelection getSelection(UUID uuid) {
        return selections.get(uuid);
    }

    public void clearSelection(UUID uuid) {
        selections.remove(uuid);
    }

    public void savePosition(String arenaName, Team team, int positionNumber, Location location) {
        String path = "positions." + team.name().toLowerCase() + ".position_" + positionNumber;
        positionsFile.config().set(path + ".world", location.getWorld() != null ? location.getWorld().getName() : null);
        positionsFile.config().set(path + ".x", location.getBlockX());
        positionsFile.config().set(path + ".y", location.getBlockY());
        positionsFile.config().set(path + ".z", location.getBlockZ());
        positionsFile.save();
    }

    public void saveFacing(Team team, float yaw) {
        positionsFile.config().set("facing." + team.name().toLowerCase(), yaw);
        positionsFile.save();
    }

    public float getFacing(Team team) {
        return (float) positionsFile.config().getDouble("facing." + team.name().toLowerCase(), 0.0D);
    }

    public @Nullable Location getPosition(Team team, int positionNumber) {
        String base = "positions." + team.name().toLowerCase() + ".position_" + positionNumber;
        String worldName = positionsFile.config().getString(base + ".world");
        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        return new Location(
                world,
                positionsFile.config().getInt(base + ".x"),
                positionsFile.config().getInt(base + ".y"),
                positionsFile.config().getInt(base + ".z"),
                getFacing(team),
                0f
        );
    }

    public static final class PositionSelection {
        private final Location targetBlock;
        private @Nullable Team team;

        public PositionSelection(Location targetBlock) {
            this.targetBlock = targetBlock;
        }

        public Location getTargetBlock() {
            return targetBlock;
        }

        public @Nullable Team getTeam() {
            return team;
        }

        public void setTeam(@Nullable Team team) {
            this.team = team;
        }
    }
}
