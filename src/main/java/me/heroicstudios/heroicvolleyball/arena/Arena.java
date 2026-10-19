package me.heroicstudios.heroicvolleyball.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class Arena {

    private final String name;
    private boolean enabled = true;
    private final EnumMap<Side, Region> courts = new EnumMap<>(Side.class);
    private final EnumMap<Side, Region> backrows = new EnumMap<>(Side.class);
    private final EnumMap<Side, Location> ballSpawns = new EnumMap<>(Side.class);
    private final EnumMap<Side, Float> facings = new EnumMap<>(Side.class);
    private final EnumMap<Side, Map<Integer, Location>> positions = new EnumMap<>(Side.class);

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
        }
        return copy;
    }
}
