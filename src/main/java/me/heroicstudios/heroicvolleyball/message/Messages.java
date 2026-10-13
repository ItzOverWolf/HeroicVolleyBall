package me.heroicstudios.heroicvolleyball.message;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.util.ConfigDefaults;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class Messages {

    private final HeroicVolleyBall plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private FileConfiguration config;

    public Messages(HeroicVolleyBall plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        if (ConfigDefaults.merge(plugin.getResource("messages.yml"), config)) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save messages.yml: " + e.getMessage());
            }
        }
    }

    public String raw(String key) {
        String value = config.getString(key);
        return value != null ? value : key;
    }

    public Component parse(String raw, String... placeholders) {
        String text = raw;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String replacement = placeholders[i + 1];
            text = text.replace("{" + placeholders[i] + "}", replacement == null ? "" : replacement);
        }
        try {
            return miniMessage.deserialize(text);
        } catch (RuntimeException exception) {
            return Component.text(text);
        }
    }

    public Component get(String key, String... placeholders) {
        return parse(raw(key), placeholders);
    }

    public List<Component> getLines(String key, String... placeholders) {
        List<String> lines = config.getStringList(key);
        if (!lines.isEmpty()) {
            List<Component> components = new ArrayList<>(lines.size());
            for (String line : lines) {
                components.add(parse(line, placeholders));
            }
            return components;
        }
        String single = config.getString(key);
        return single == null ? List.of() : List.of(parse(single, placeholders));
    }

    public Component prefix() {
        return parse(raw("prefix"));
    }

    public Component getPrefixed(String key, String... placeholders) {
        return prefix().append(get(key, placeholders));
    }

    public void send(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(getPrefixed(key, placeholders));
    }

    public void sendRaw(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    public void actionBar(Player player, String key, String... placeholders) {
        player.sendActionBar(get(key, placeholders));
    }

    public Title title(String key, String... placeholders) {
        return Title.title(get(key + ".title", placeholders), get(key + ".subtitle", placeholders));
    }

    public static Title noFade(Component title, Component subtitle) {
        return Title.title(title, subtitle,
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500)));
    }

    public static String teamDisplay(Team team) {
        String color = team == Team.RED ? "red" : "blue";
        return "<" + color + ">" + team.getDisplayName() + "</" + color + ">";
    }

    public static String colorName(Team team) {
        return team == Team.RED ? "red" : "blue";
    }
}
