package me.heroicstudios.heroicvolleyball.music;

import java.io.File;
import java.util.List;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public final class MusicManager {

    private static final Sound[] INSTRUMENTS = {
            Sound.BLOCK_NOTE_BLOCK_HARP,
            Sound.BLOCK_NOTE_BLOCK_BASS,
            Sound.BLOCK_NOTE_BLOCK_BASEDRUM,
            Sound.BLOCK_NOTE_BLOCK_SNARE,
            Sound.BLOCK_NOTE_BLOCK_HAT,
            Sound.BLOCK_NOTE_BLOCK_GUITAR,
            Sound.BLOCK_NOTE_BLOCK_FLUTE,
            Sound.BLOCK_NOTE_BLOCK_BELL,
            Sound.BLOCK_NOTE_BLOCK_CHIME,
            Sound.BLOCK_NOTE_BLOCK_XYLOPHONE,
            Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE,
            Sound.BLOCK_NOTE_BLOCK_COW_BELL,
            Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO,
            Sound.BLOCK_NOTE_BLOCK_BIT,
            Sound.BLOCK_NOTE_BLOCK_BANJO,
            Sound.BLOCK_NOTE_BLOCK_PLING
    };

    private final HeroicVolleyBall plugin;
    private final File folder;

    private boolean enabled = true;
    private String defaultSong = "haikyuu.nbs";
    private double volume = 1.0;

    private @Nullable BukkitTask task;
    private @Nullable NbsSong song;
    private int currentTick;
    private double accumulator;

    public MusicManager(HeroicVolleyBall plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "music");
        if (!folder.exists() && folder.mkdirs()) {
            plugin.getLogger().info("Created music folder: " + folder.getPath());
        }
        loadConfig();
    }

    public void loadConfig() {
        var config = plugin.getConfig();
        this.enabled = config.getBoolean("music.enabled", true);
        this.defaultSong = config.getString("music.song", "haikyuu.nbs");
        this.volume = Math.max(0.0, config.getDouble("music.volume", 1.0));
    }

    public File getFolder() {
        return folder;
    }

    public boolean isPlaying() {
        return task != null;
    }

    public void startDefault() {
        if (!enabled) {
            return;
        }
        start(defaultSong);
    }

    public boolean start(String fileName) {
        stop();

        File file = new File(folder, fileName);
        if (!file.isFile()) {
            plugin.getLogger().warning("Music file not found: " + file.getPath());
            return false;
        }

        NbsSong parsed;
        try {
            parsed = NbsParser.parse(file);
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to read NBS file \"" + fileName + "\": " + exception.getMessage());
            return false;
        }

        this.song = parsed;
        this.currentTick = 0;
        this.accumulator = 0.0;
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 1L);
        return true;
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        song = null;
    }

    public void shutdown() {
        stop();
    }

    private void tick() {
        NbsSong current = song;
        if (current == null) {
            stop();
            return;
        }

        accumulator += current.tempo() / 20.0;
        while (accumulator >= 1.0) {
            accumulator -= 1.0;
            playTick(current, currentTick);
            currentTick++;
            if (currentTick >= current.length()) {
                currentTick = Math.max(0, Math.min(current.loopStart(), current.length() - 1));
            }
        }
    }

    private void playTick(NbsSong current, int tick) {
        List<NbsNote> notes = current.notesAt(tick);
        if (notes == null) {
            return;
        }

        for (NbsNote note : notes) {
            Sound sound = instrumentSound(note.instrument());
            float pitch = pitchOf(note);
            float noteVolume = volumeOf(current, note);
            if (noteVolume <= 0.0f) {
                continue;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, SoundCategory.RECORDS, noteVolume, pitch);
            }
        }
    }

    private Sound instrumentSound(int instrument) {
        if (instrument >= 0 && instrument < INSTRUMENTS.length) {
            return INSTRUMENTS[instrument];
        }
        return Sound.BLOCK_NOTE_BLOCK_HARP;
    }

    private float pitchOf(NbsNote note) {
        double semitones = (note.key() - 45) / 12.0;
        double cents = note.pitch() / 1200.0;
        double value = Math.pow(2.0, semitones + cents);
        return (float) Math.max(0.5, Math.min(2.0, value));
    }

    private float volumeOf(NbsSong current, NbsNote note) {
        double value = (note.velocity() / 100.0) * current.layerVolume(note.layer()) * volume;
        return (float) Math.max(0.0, Math.min(1.0, value));
    }
}
