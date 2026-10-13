package me.heroicstudios.heroicvolleyball.spawn;

import java.util.EnumMap;
import java.util.Map;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.util.YamlFile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public final class BallSpawnManager {

    private final HeroicVolleyBall plugin;
    private final YamlFile spawnFile;

    public BallSpawnManager(HeroicVolleyBall plugin) {
        this.plugin = plugin;
        this.spawnFile = new YamlFile(plugin, "ballspawn.yml");
        migrateLegacy();
    }

    private void migrateLegacy() {
        if (!spawnFile.config().contains("ball-spawn.world", true)) {
            return;
        }
        for (Team team : Team.values()) {
            if (!isSet(team)) {
                set(team, legacy());
            }
        }
        spawnFile.config().set("ball-spawn", null);
        spawnFile.save();
    }

    private @Nullable Location legacy() {
        World world = Bukkit.getWorld(spawnFile.config().getString("ball-spawn.world"));
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                spawnFile.config().getDouble("ball-spawn.x"),
                spawnFile.config().getDouble("ball-spawn.y"),
                spawnFile.config().getDouble("ball-spawn.z"),
                (float) spawnFile.config().getDouble("ball-spawn.yaw", 0.0D),
                (float) spawnFile.config().getDouble("ball-spawn.pitch", 0.0D)
        );
    }

    public void set(Team team, Location location) {
        String path = "ball-spawn." + team.name().toLowerCase();
        World world = location.getWorld();
        spawnFile.config().set(path + ".world", world != null ? world.getName() : null);
        spawnFile.config().set(path + ".x", location.getX());
        spawnFile.config().set(path + ".y", location.getY());
        spawnFile.config().set(path + ".z", location.getZ());
        spawnFile.config().set(path + ".yaw", location.getYaw());
        spawnFile.config().set(path + ".pitch", location.getPitch());
        spawnFile.save();
    }

    public void clear(Team team) {
        spawnFile.config().set("ball-spawn." + team.name().toLowerCase(), null);
        spawnFile.save();
    }

    public boolean isSet(Team team) {
        return spawnFile.config().contains("ball-spawn." + team.name().toLowerCase() + ".world", true);
    }

    public boolean isAnySet() {
        for (Team team : Team.values()) {
            if (isSet(team)) {
                return true;
            }
        }
        return false;
    }

    public @Nullable Location get(Team team) {
        if (!isSet(team)) {
            return null;
        }
        String path = "ball-spawn." + team.name().toLowerCase();
        World world = Bukkit.getWorld(spawnFile.config().getString(path + ".world"));
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                spawnFile.config().getDouble(path + ".x"),
                spawnFile.config().getDouble(path + ".y"),
                spawnFile.config().getDouble(path + ".z"),
                (float) spawnFile.config().getDouble(path + ".yaw", 0.0D),
                (float) spawnFile.config().getDouble(path + ".pitch", 0.0D)
        );
    }
}