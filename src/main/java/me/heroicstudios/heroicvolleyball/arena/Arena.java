package me.heroicstudios.heroicvolleyball.arena;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Arena {

    private final String name;
    private boolean enabled = true;
    private final EnumMap<Side, Region> courts = new EnumMap<>(Side.class);
    private final EnumMap<Side, Region> backrows = new EnumMap<>(Side.class);
    private final EnumMap<Side, Location> ballSpawns = new EnumMap<>(Side.class);
    private final EnumMap<Side, Float> facings = new EnumMap<>(Side.class);
    private final EnumMap<Side, Map<Integer, Location>> positions = new EnumMap<>(Side.class);
    private final EnumMap<Side, String> teamNames = new EnumMap<>(Side.class);
    private final EnumMap<Side, TextColor> teamColors = new EnumMap<>(Side.class);
    private Location lobby;
    private Location spectatorSpawn;
    private boolean queueEnabled;
    private int minPlayers = 2;
    private int maxPlayers = 12;
    private final List<Location> joinSigns = new ArrayList<>();

    public Arena(String name) {
        this.name = name;
        for (Side side : Side.values()) {
            positions.put(side, new HashMap<>());
            facings.put(side, 0.0f);
        }
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public @Nullable Region getCourt(Side side) {
        return courts.get(side);
    }

    public void setCourt(Side side, Region region) {
        if (region == null) {
            courts.remove(side);
        } else {
            courts.put(side, region);
        }
    }

    public @Nullable Region getBackrow(Side side) {
        return backrows.get(side);
    }

    public void setBackrow(Side side, Region region) {
        if (region == null) {
            backrows.remove(side);
        } else {
            backrows.put(side, region);
        }
    }

    public @Nullable Location getBallSpawn(Side side) {
        return ballSpawns.get(side);
    }

    public void setBallSpawn(Side side, Location location) {
        if (location == null || location.getWorld() == null) {
            ballSpawns.remove(side);
        } else {
            ballSpawns.put(side, location.clone());
        }
    }

    public float getFacing(Side side) {
        return facings.getOrDefault(side, 0.0f);
    }

    public void setFacing(Side side, float yaw) {
        facings.put(side, yaw);
    }

    public @Nullable Location getPosition(Side side, int number) {
        if (number < 1 || number > 6) {
            return null;
        }
        Location location = positions.get(side).get(number);
        return location != null ? location.clone() : null;
    }

    public void setPosition(Side side, int number, Location location) {
        if (number < 1 || number > 6) {
            return;
        }
        if (location == null || location.getWorld() == null) {
            positions.get(side).remove(number);
        } else {
            positions.get(side).put(number, location.clone());
        }
    }

    public boolean hasAllPositions(Side side) {
        for (int number = 1; number <= 6; number++) {
            if (!positions.get(side).containsKey(number)) {
                return false;
            }
        }
        return true;
    }

    public String teamName(Side side) {
        return teamNames.getOrDefault(side, side.getDisplayName());
    }

    public void setTeamName(Side side, @Nullable String name) {
        if (name == null || name.isBlank()) {
            teamNames.remove(side);
        } else {
            teamNames.put(side, name);
        }
    }

    public TextColor teamColor(Side side) {
        return teamColors.getOrDefault(side, side.getColor());
    }

    public void setTeamColor(Side side, @Nullable TextColor color) {
        if (color == null) {
            teamColors.remove(side);
        } else {
            teamColors.put(side, color);
        }
    }

    public String teamColorHex(Side side) {
        return teamColor(side).asHexString();
    }

    public String teamDisplay(Side side) {
        String hex = teamColorHex(side);
        return "<" + hex + ">" + teamName(side) + "</" + hex + ">";
    }

    public @Nullable Location getLobby() {
        return lobby != null ? lobby.clone() : null;
    }

    public void setLobby(@Nullable Location location) {
        this.lobby = location != null && location.getWorld() != null ? location.clone() : null;
    }

    public @Nullable Location getSpectatorSpawn() {
        return spectatorSpawn != null ? spectatorSpawn.clone() : null;
    }

    public void setSpectatorSpawn(@Nullable Location location) {
        this.spectatorSpawn = location != null && location.getWorld() != null ? location.clone() : null;
    }

    public boolean isQueueEnabled() {
        return queueEnabled;
    }

    public void setQueueEnabled(boolean queueEnabled) {
        this.queueEnabled = queueEnabled;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = Math.max(2, Math.min(12, minPlayers));
        if (maxPlayers < this.minPlayers) {
            maxPlayers = this.minPlayers;
        }
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = Math.max(2, Math.min(12, maxPlayers));
        if (minPlayers > this.maxPlayers) {
            minPlayers = this.maxPlayers;
        }
    }

    /** No side may ever field more than six players. */
    public int getMaxPerSide() {
        return Math.min(6, Math.max(1, (maxPlayers + 1) / 2));
    }

    public List<Location> getJoinSigns() {
        List<Location> copy = new ArrayList<>(joinSigns.size());
        for (Location location : joinSigns) {
            copy.add(location.clone());
        }
        return copy;
    }

    public void setJoinSigns(List<Location> locations) {
        joinSigns.clear();
        if (locations != null) {
            for (Location location : locations) {
                addJoinSign(location);
            }
        }
    }

    public boolean addJoinSign(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Location normalized = location.toBlockLocation();
        for (Location existing : joinSigns) {
            if (sameBlock(existing, normalized)) {
                return false;
            }
        }
        joinSigns.add(normalized);
        return true;
    }

    public boolean removeJoinSign(Location location) {
        if (location == null) {
            return false;
        }
        return joinSigns.removeIf(existing -> sameBlock(existing, location));
    }

    public boolean isJoinSign(Location location) {
        if (location == null) {
            return false;
        }
        for (Location sign : joinSigns) {
            if (sameBlock(sign, location)) {
                return true;
            }
        }
        return false;
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() != null && second.getWorld() != null
                && first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    public @Nullable Side getCourtSide(Location location) {
        for (Side side : Side.values()) {
            Region region = courts.get(side);
            if (region != null && region.contains(location)) {
                return side;
            }
        }
        return null;
    }

    public @Nullable Side getBackrowSide(Location location) {
        for (Side side : Side.values()) {
            Region region = backrows.get(side);
            if (region != null && region.contains(location)) {
                return side;
            }
        }
        return null;
    }

    public boolean contains(Location location) {
        return getCourtSide(location) != null || getBackrowSide(location) != null;
    }

    public Side defaultSide() {
        if (courts.containsKey(Side.RED)) {
            return Side.RED;
        }
        if (courts.containsKey(Side.BLUE)) {
            return Side.BLUE;
        }
        return Side.RED;
    }

    public @Nullable Side firstServeSide() {
        if (getBallSpawn(Side.RED) != null || getPosition(Side.RED, 6) != null) {
            return Side.RED;
        }
        if (getBallSpawn(Side.BLUE) != null || getPosition(Side.BLUE, 6) != null) {
            return Side.BLUE;
        }
        return null;
    }

    public @Nullable Location centerFloor(Side side) {
        Region region = courts.get(side);
        if (region == null) {
            return null;
        }
        World world = Bukkit.getWorld(region.getWorld());
        if (world == null) {
            return null;
        }
        int x = region.centerX();
        int z = region.centerZ();
        int floorY = region.getMinY();
        for (int y = region.getMaxY(); y >= region.getMinY() - 1; y--) {
            if (!world.getBlockAt(x, y, z).isPassable()) {
                floorY = y + 1;
                break;
            }
        }
        return new Location(world, x + 0.5, floorY, z + 0.5);
    }

    public int positionCount(Side side) {
        return positions.get(side).size();
    }

    public String readyState() {
        return isComplete() ? "ready" : "incomplete";
    }

    public boolean isComplete() {
        for (Side side : Side.values()) {
            if (courts.get(side) == null || !hasAllPositions(side) || ballSpawns.get(side) == null) {
                return false;
            }
        }
        return true;
    }

    public Arena copy(String newName) {
        Arena copy = new Arena(newName);
        copy.enabled = this.enabled;
        for (Side side : Side.values()) {
            Region court = courts.get(side);
            if (court != null) {
                copy.setCourt(side, court);
            }
            Region backrow = backrows.get(side);
            if (backrow != null) {
                copy.setBackrow(side, backrow);
            }
            Location spawn = ballSpawns.get(side);
            if (spawn != null) {
                copy.setBallSpawn(side, spawn);
            }
            copy.setFacing(side, getFacing(side));
            for (Map.Entry<Integer, Location> entry : positions.get(side).entrySet()) {
                copy.setPosition(side, entry.getKey(), entry.getValue());
            }
            copy.setTeamName(side, teamNames.get(side));
            copy.setTeamColor(side, teamColors.get(side));
        }
        copy.setLobby(lobby);
        copy.setSpectatorSpawn(spectatorSpawn);
        copy.setQueueEnabled(queueEnabled);
        copy.setMaxPlayers(maxPlayers);
        copy.setMinPlayers(minPlayers);
        copy.setJoinSigns(joinSigns);
        return copy;
    }
}
