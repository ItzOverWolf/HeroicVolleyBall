package me.heroicstudios.heroicvolleyball.arena;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ArenaManager {

    private final HeroicVolleyBall plugin;
    private final File folder;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public ArenaManager(HeroicVolleyBall plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "arenas");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create arena folder: " + folder.getPath());
        }
        migrateLegacy();
        loadAll();
    }

    public void loadAll() {
        arenas.clear();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            Arena arena = load(file);
            if (arena != null) {
                arenas.put(arena.getName(), arena);
            }
        }
    }

    public Collection<Arena> all() {
        return List.copyOf(arenas.values());
    }

    public Collection<String> names() {
        return List.copyOf(arenas.keySet());
    }

    public @Nullable Arena get(String rawName) {
        if (rawName == null) {
            return null;
        }
        Arena exact = arenas.get(rawName);
        if (exact != null) {
            return exact;
        }
        String name = sanitize(rawName);
        return name != null ? arenas.get(name) : null;
    }

    public boolean exists(String rawName) {
        return get(rawName) != null;
    }

    public String defaultName() {
        String configured = sanitize(plugin.getConfig().getString("arena.default", "default"));
        if (configured != null && arenas.containsKey(configured)) {
            return configured;
        }
        if (arenas.containsKey("default")) {
            return "default";
        }
        return arenas.isEmpty() ? "default" : arenas.keySet().iterator().next();
    }

    public @Nullable Arena getDefault() {
        return arenas.get(defaultName());
    }

    public @Nullable Arena resolve(@Nullable String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return getDefault();
        }
        return get(rawName);
    }

    public @Nullable Arena resolve(@Nullable Location location) {
        if (location == null) {
            return getDefault();
        }
        Arena arena = arenaAt(location);
        return arena != null ? arena : getDefault();
    }

    public @Nullable Arena create(String rawName) {
        String name = sanitize(rawName);
        if (name == null || arenas.containsKey(name)) {
            return null;
        }
        Arena arena = new Arena(name);
        arenas.put(name, arena);
        save(arena);
        return arena;
    }

    public Arena createIfMissing(String rawName) {
        Arena arena = get(rawName);
        if (arena != null) {
            return arena;
        }
        Arena created = create(rawName);
        return created != null ? created : get(defaultName());
    }

    public boolean delete(String rawName) {
        Arena arena = get(rawName);
        if (arena == null) {
            return false;
        }
        arenas.remove(arena.getName());
        File file = fileFor(arena.getName());
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete arena file: " + file.getPath());
        }
        return true;
    }

    public void save(Arena arena) {
        Path target = fileFor(arena.getName()).toPath();
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        YamlConfiguration config = toYaml(arena);
        try {
            Files.createDirectories(target.getParent());
            config.save(temp.toFile());
            move(temp, target);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save arena " + arena.getName() + ": " + exception.getMessage());
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
            }
        }
    }

    public void saveAll() {
        for (Arena arena : arenas.values()) {
            save(arena);
        }
    }

    public @Nullable Arena arenaAt(Location location) {
        for (Arena arena : arenas.values()) {
            if (arena.isEnabled() && arena.contains(location)) {
                return arena;
            }
        }
        return null;
    }

    public @Nullable ArenaSide courtAt(Location location) {
        for (Arena arena : arenas.values()) {
            if (!arena.isEnabled()) {
                continue;
            }
            Side side = arena.getCourtSide(location);
            if (side != null) {
                return new ArenaSide(arena, side);
            }
        }
        return null;
    }

    public @Nullable ArenaSide sideOf(Arena arena, Location location) {
        Side side = arena != null ? arena.getCourtSide(location) : null;
        return side != null ? new ArenaSide(arena, side) : null;
    }

    public static @Nullable String sanitize(String rawName) {
        if (rawName == null) {
            return null;
        }
        String name = rawName.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "_")
                .replaceAll("^_+|_+$", "");
        if (name.isEmpty()) {
            return null;
        }
        return name.length() > 32 ? name.substring(0, 32) : name;
    }

    private File fileFor(String name) {
        return new File(folder, name + ".yml");
    }

    private @Nullable Arena load(File file) {
        if (!file.isFile()) {
            return null;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String fileName = file.getName();
        String name = sanitize(config.getString("name", fileName.substring(0, fileName.length() - 4)));
        if (name == null) {
            return null;
        }
        Arena arena = new Arena(name);
        arena.setEnabled(config.getBoolean("enabled", true));
        for (Side side : Side.values()) {
            Region court = readRegion(config, "courts." + side.getId());
            if (court != null) {
                arena.setCourt(side, court);
            }
            Region backrow = readRegion(config, "backrows." + side.getId());
            if (backrow != null) {
                arena.setBackrow(side, backrow);
            }
            Location spawn = readLocation(config, "ball-spawn." + side.getId());
            if (spawn != null) {
                arena.setBallSpawn(side, spawn);
            }
            arena.setFacing(side, (float) config.getDouble("facing." + side.getId(), 0.0));
            for (int number = 1; number <= 6; number++) {
                Location position = readLocation(config, "positions." + side.getId() + "." + number);
                if (position != null) {
                    arena.setPosition(side, number, position);
                }
            }
        }
        return arena;
    }

    private YamlConfiguration toYaml(Arena arena) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", arena.getName());
        config.set("enabled", arena.isEnabled());
        for (Side side : Side.values()) {
            String id = side.getId();
            writeRegion(config, "courts." + id, arena.getCourt(side));
            writeRegion(config, "backrows." + id, arena.getBackrow(side));
            Location spawn = arena.getBallSpawn(side);
            if (spawn != null) {
                config.set("ball-spawn." + id, spawn);
            }
            config.set("facing." + id, arena.getFacing(side));
            for (int number = 1; number <= 6; number++) {
                Location position = arena.getPosition(side, number);
                if (position != null) {
                    config.set("positions." + id + "." + number, position);
                }
            }
        }
        return config;
    }

    private void writeRegion(YamlConfiguration config, String path, @Nullable Region region) {
        if (region == null) {
            return;
        }
        config.set(path + ".world", region.getWorld());
        config.set(path + ".min.x", region.getMinX());
        config.set(path + ".min.y", region.getMinY());
        config.set(path + ".min.z", region.getMinZ());
        config.set(path + ".max.x", region.getMaxX());
        config.set(path + ".max.y", region.getMaxY());
        config.set(path + ".max.z", region.getMaxZ());
    }

    private @Nullable Region readRegion(YamlConfiguration config, String path) {
        String world = config.getString(path + ".world");
        if (world == null) {
            return null;
        }
        return new Region(
                world,
                config.getInt(path + ".min.x"), config.getInt(path + ".min.y"), config.getInt(path + ".min.z"),
                config.getInt(path + ".max.x"), config.getInt(path + ".max.y"), config.getInt(path + ".max.z")
        );
    }

    private @Nullable Location readLocation(YamlConfiguration config, String path) {
        if (!config.isConfigurationSection(path)) {
            return null;
        }
        Location location = config.getLocation(path);
        if (location == null || location.getWorld() == null) {
            String worldName = config.getString(path + ".world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            if (world == null) {
                return null;
            }
            location = new Location(world,
                    config.getDouble(path + ".x"), config.getDouble(path + ".y"), config.getDouble(path + ".z"),
                    (float) config.getDouble(path + ".yaw", 0.0), (float) config.getDouble(path + ".pitch", 0.0));
        }
        return location;
    }

    private void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void migrateLegacy() {
        File[] existing = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (existing != null && existing.length > 0) {
            return;
        }

        File dataFolder = plugin.getDataFolder();
        File legacyArenas = new File(dataFolder, "arenas.yml");
        File legacyPositions = new File(dataFolder, "positions.yml");
        File legacyBackrow = new File(dataFolder, "backrow.yml");
        File legacyBallSpawn = new File(dataFolder, "ballspawn.yml");
        if (!legacyArenas.isFile() && !legacyPositions.isFile()
                && !legacyBackrow.isFile() && !legacyBallSpawn.isFile()) {
            return;
        }

        Arena arena = new Arena("default");
        boolean found = false;

        if (legacyArenas.isFile()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyArenas);
            for (Side side : Side.values()) {
                Region region = readRegion(config, "arenas." + side.getId());
                if (region != null) {
                    arena.setCourt(side, region);
                    found = true;
                }
            }
        }
        if (legacyPositions.isFile()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyPositions);
            for (Side side : Side.values()) {
                String id = side.getId();
                if (config.contains("facing." + id)) {
                    arena.setFacing(side, (float) config.getDouble("facing." + id));
                }
                for (int number = 1; number <= 6; number++) {
                    Location position = readLocation(config, "positions." + id + ".position_" + number);
                    if (position != null) {
                        arena.setPosition(side, number, position);
                        found = true;
                    }
                }
            }
        }
        if (legacyBackrow.isFile()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyBackrow);
            for (Side side : Side.values()) {
                Region region = readRegion(config, "backrow." + side.getId());
                if (region != null) {
                    arena.setBackrow(side, region);
                    found = true;
                }
            }
        }
        if (legacyBallSpawn.isFile()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyBallSpawn);
            for (Side side : Side.values()) {
                Location spawn = readLocation(config, "ball-spawn." + side.getId());
                if (spawn != null) {
                    arena.setBallSpawn(side, spawn);
                    found = true;
                }
            }
        }

        if (found) {
            save(arena);
            plugin.getLogger().info("Migrated the existing court setup into arenas/default.yml.");
        }
        moveToLegacy(legacyArenas);
        moveToLegacy(legacyPositions);
        moveToLegacy(legacyBackrow);
        moveToLegacy(legacyBallSpawn);
    }

    private void moveToLegacy(File file) {
        if (!file.isFile()) {
            return;
        }
        File target = new File(file.getParentFile(), file.getName() + ".legacy");
        try {
            Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not archive " + file.getName() + ": " + exception.getMessage());
        }
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (Side side : Side.values()) {
            ids.add(side.getId());
        }
        return ids;
    }
}
