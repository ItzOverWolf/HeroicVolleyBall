package me.heroicstudios.heroicvolleyball.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.music.MusicManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VolleyMusicCommand implements CommandExecutor, TabCompleter {

    private final MusicManager musicManager;
    private final Messages messages;

    public VolleyMusicCommand(MusicManager musicManager, Messages messages) {
        this.musicManager = musicManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            messages.send(sender, "music.usage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start", "play" -> {
                String song = args.length >= 2 ? args[1] : null;
                boolean ok = song == null ? startDefault() : musicManager.start(song);
                if (!ok) {
                    messages.send(sender, "music.start-failed");
                } else {
                    messages.send(sender, "music.now-playing");
                }
            }
            case "stop" -> {
                musicManager.stop();
                messages.send(sender, "music.stopped");
            }
            case "list" -> {
                File[] files = musicManager.getFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(".nbs"));
                if (files == null || files.length == 0) {
                    messages.send(sender, "music.no-files", "path", musicManager.getFolder().getPath());
                    return true;
                }
                messages.send(sender, "music.songs");
                for (File file : files) {
                    messages.sendRaw(sender, "music.song-entry", "song", file.getName());
                }
            }
            case "reload" -> {
                musicManager.loadConfig();
                messages.send(sender, "music.reloaded");
            }
            default -> messages.send(sender, "music.usage");
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
