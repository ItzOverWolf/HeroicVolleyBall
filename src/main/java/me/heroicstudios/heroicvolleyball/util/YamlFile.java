package me.heroicstudios.heroicvolleyball.util;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class YamlFile {

    private final HeroicVolleyBall plugin;
    private final File file;
    private YamlConfiguration config;

    public YamlFile(HeroicVolleyBall plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration config() {
        return config;
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}