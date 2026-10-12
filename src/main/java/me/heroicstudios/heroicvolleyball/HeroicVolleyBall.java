package me.heroicstudios.heroicvolleyball;

import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.ball.BallListener;
import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.command.SetArenaCommand;
import me.heroicstudios.heroicvolleyball.command.SpawnBallCommand;
import me.heroicstudios.heroicvolleyball.command.StartGameCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyGiveCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyMusicCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyReloadCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyWandCommand;
import me.heroicstudios.heroicvolleyball.gui.GuiListener;
import me.heroicstudios.heroicvolleyball.listener.ArenaListener;
import me.heroicstudios.heroicvolleyball.music.MusicManager;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import me.heroicstudios.heroicvolleyball.tool.ToolListener;
import me.heroicstudios.heroicvolleyball.tool.ToolManager;
import me.heroicstudios.heroicvolleyball.wand.WandListener;
import me.heroicstudios.heroicvolleyball.wand.WandManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class HeroicVolleyBall extends JavaPlugin {

    private ArenaManager arenaManager;
    private WandManager wandManager;
    private PositionManager positionManager;
    private GuiListener guiListener;
    private ToolManager toolManager;
    private BallManager ballManager;
    private ScoreManager scoreManager;
    private MusicManager musicManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.arenaManager = new ArenaManager(this);
        this.positionManager = new PositionManager(this);
        this.wandManager = new WandManager(this);
        this.guiListener = new GuiListener(positionManager);
        this.toolManager = new ToolManager(this);
        this.musicManager = new MusicManager(this);
        this.scoreManager = new ScoreManager(this, arenaManager, musicManager);
        this.ballManager = new BallManager(this, arenaManager, scoreManager);

        getServer().getPluginManager().registerEvents(new WandListener(wandManager), this);
        getServer().getPluginManager().registerEvents(new ArenaListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new ToolListener(toolManager), this);
        getServer().getPluginManager().registerEvents(new BallListener(ballManager, toolManager), this);
        getServer().getPluginManager().registerEvents(scoreManager, this);
        scoreManager.applyAll();

        PluginCommand volleyWandCommand = getCommand("volleywand");
        if (volleyWandCommand != null) {
            volleyWandCommand.setExecutor(new VolleyWandCommand(wandManager));
        }

        SetArenaCommand setArenaCommand = new SetArenaCommand(arenaManager, wandManager);
        PluginCommand setArena = getCommand("setarena");
        if (setArena != null) {
            setArena.setExecutor(setArenaCommand);
            setArena.setTabCompleter(setArenaCommand);
        }

        register("spawnball", new SpawnBallCommand(ballManager));
        register("startgame", new StartGameCommand(ballManager, scoreManager, musicManager));
        register("volleyreload", new VolleyReloadCommand(this));

        VolleyMusicCommand volleyMusicCommand = new VolleyMusicCommand(musicManager);
        PluginCommand volleyMusic = getCommand("volleymusic");
        if (volleyMusic != null) {
            volleyMusic.setExecutor(volleyMusicCommand);
            volleyMusic.setTabCompleter(volleyMusicCommand);
        }

        VolleyGiveCommand volleyGiveCommand = new VolleyGiveCommand(toolManager);
        PluginCommand volleyGive = getCommand("volleygive");
        if (volleyGive != null) {
            volleyGive.setExecutor(volleyGiveCommand);
            volleyGive.setTabCompleter(volleyGiveCommand);
        }
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        }
    }

    @Override
    public void onDisable() {
        if (ballManager != null) {
            ballManager.shutdown();
        }
        if (musicManager != null) {
            musicManager.shutdown();
        }
        if (scoreManager != null) {
            scoreManager.shutdown();
        }
        if (arenaManager != null) {
            arenaManager.save();
        }
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public WandManager getWandManager() {
        return wandManager;
    }

    public PositionManager getPositionManager() {
        return positionManager;
    }

    public GuiListener getGuiListener() {
        return guiListener;
    }

    public ToolManager getToolManager() {
        return toolManager;
    }

    public BallManager getBallManager() {
        return ballManager;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }
}
