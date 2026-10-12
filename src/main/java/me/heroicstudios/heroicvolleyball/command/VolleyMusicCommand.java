package me.heroicstudios.heroicvolleyball.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.music.MusicManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VolleyMusicCommand implements CommandExecutor, TabCompleter {

    private final MusicManager musicManager;

    public VolleyMusicCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /" + label + " <start|stop|list|reload> [song]", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start", "play" -> {
                String song = args.length >= 2 ? args[1] : null;
                boolean ok = song == null ? startDefault() : musicManager.start(song);
                if (!ok) {
                    sender.sendMessage(Component.text("Could not start music. Check the music folder.", NamedTextColor.RED));
                } else {
                    sender.sendMessage(Component.text("Now playing.", NamedTextColor.GREEN));
                }
            }
            case "stop" -> {
                musicManager.stop();
                sender.sendMessage(Component.text("Music stopped.", NamedTextColor.YELLOW));
            }
            case "list" -> {
                File[] files = musicManager.getFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(".nbs"));
                if (files == null || files.length == 0) {
                    sender.sendMessage(Component.text("No .nbs files found in " + musicManager.getFolder().getPath(), NamedTextColor.GRAY));
                    return true;
                }
                sender.sendMessage(Component.text("Songs:", NamedTextColor.GOLD));
                for (File file : files) {
                    sender.sendMessage(Component.text(" - " + file.getName(), NamedTextColor.GRAY));
                }
            }
            case "reload" -> {
                musicManager.loadConfig();
                sender.sendMessage(Component.text("Music configuration reloaded.", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /" + label + " <start|stop|list|reload> [song]", NamedTextColor.RED));
        }
        return true;
    }

    private boolean startDefault() {
        musicManager.startDefault();
        return musicManager.isPlaying();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            for (String option : List.of("start", "stop", "list", "reload")) {
                if (option.startsWith(args[0].toLowerCase())) {
                    options.add(option);
                }
            }
            return options;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("play"))) {
            File[] files = musicManager.getFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(".nbs"));
            if (files == null) {
                return List.of();
            }
            List<String> options = new ArrayList<>();
            for (File file : files) {
                if (file.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    options.add(file.getName());
                }
            }
            return options;
        }
        return List.of();
    }
}
