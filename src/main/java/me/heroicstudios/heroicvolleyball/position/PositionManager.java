package me.heroicstudios.heroicvolleyball.position;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Team;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public final class PositionManager {

    private final HeroicVolleyBall plugin;
    private final Map<UUID, PositionSelection> selections = new HashMap<>();

    public PositionManager(HeroicVolleyBall plugin) {
        this.plugin = plugin;
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
        String path = "arenas." + arenaName + "." + team.name().toLowerCase() + ".position_" + positionNumber;
        plugin.getConfig().set(path + ".world", location.getWorld() != null ? location.getWorld().getName() : null);
        plugin.getConfig().set(path + ".x", location.getBlockX());
        plugin.getConfig().set(path + ".y", location.getBlockY());
        plugin.getConfig().set(path + ".z", location.getBlockZ());
        plugin.saveConfig();
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
