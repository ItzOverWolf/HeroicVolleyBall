package me.heroicstudios.heroicvolleyball.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Merges the bundled default config keys into an existing (possibly older)
 * on-disk config file without touching values the user has already set.
 *
 * To add a new toggle/setting later: put a default value into the bundled
 * config.yml (or messages.yml) and call this on load. Every existing install
 * then gains the new key automatically, while keeping all of their own values
 * and their separate coordinate files intact.
 */
public final class ConfigDefaults {

    private ConfigDefaults() {
    }

    public static boolean merge(InputStream defaults, FileConfiguration target) {
        if (defaults == null) {
            return false;
        }
        YamlConfiguration source = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaults, StandardCharsets.UTF_8));
        boolean changed = false;
        for (String key : source.getKeys(true)) {
            if (source.isConfigurationSection(key) || target.contains(key)) {
                continue;
            }
            target.set(key, source.get(key));
            changed = true;
        }
        return changed;
    }
}