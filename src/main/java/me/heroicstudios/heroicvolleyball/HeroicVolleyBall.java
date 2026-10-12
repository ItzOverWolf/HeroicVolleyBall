package me.heroicstudios.heroicvolleyball;

import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.ball.BallListener;
import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.command.PartyCommand;
import me.heroicstudios.heroicvolleyball.command.RestartGameCommand;
import me.heroicstudios.heroicvolleyball.command.SetArenaCommand;
import me.heroicstudios.heroicvolleyball.command.SpawnBallCommand;
import me.heroicstudios.heroicvolleyball.command.StartGameCommand;
import me.heroicstudios.heroicvolleyball.command.StopGameCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyGiveCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyMusicCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyReloadCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyWandCommand;
import me.heroicstudios.heroicvolleyball.game.GameManager;
import me.heroicstudios.heroicvolleyball.game.SetupGui;
import me.heroicstudios.heroicvolleyball.gui.GuiListener;
import me.heroicstudios.heroicvolleyball.hook.VolleyballPlaceholders;
import me.heroicstudios.heroicvolleyball.listener.ArenaListener;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.music.MusicManager;
import me.heroicstudios.heroicvolleyball.party.PartyManager;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import me.heroicstudios.heroicvolleyball.tool.ToolListener;
import me.heroicstudios.heroicvolleyball.tool.ToolManager;
import me.heroicstudios.heroicvolleyball.wand.WandListener;
import me.heroicstudios.heroicvolleyball.wand.WandManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class HeroicVolleyBall extends JavaPlugin {

    private ArenaManager arenaManager;
    private WandManager wandManager;
    private PositionManager positionManager;
    private GuiListener guiListener;
    private ToolManager toolManager;
    private PartyManager partyManager;
    private GameManager gameManager;
    private SetupGui setupGui;
    private BallManager ballManager;
    private ScoreManager scoreManager;
    private MusicManager musicManager;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        syncConfigDefaults();

        this.messages = new Messages(this);
        this.arenaManager = new ArenaManager(this);
        this.positionManager = new PositionManager(this);
        this.wandManager = new WandManager(this);
        this.guiListener = new GuiListener(positionManager, messages);
        this.toolManager = new ToolManager(this);
        this.partyManager = new PartyManager(this);
        this.gameManager = new GameManager(this, arenaManager, positionManager, toolManager, partyManager);
        this.musicManager = new MusicManager(this);
        this.scoreManager = new ScoreManager(this, arenaManager, musicManager);
        this.ballManager = new BallManager(this, arenaManager, scoreManager, gameManager);
        this.gameManager.bind(ballManager, scoreManager, musicManager);
        this.setupGui = new SetupGui(this, gameManager, partyManager, positionManager);

        getServer().getPluginManager().registerEvents(new WandListener(wandManager, messages), this);
        getServer().getPluginManager().registerEvents(new ArenaListener(arenaManager, messages), this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new ToolListener(toolManager), this);
        getServer().getPluginManager().registerEvents(new BallListener(ballManager, toolManager), this);
        getServer().getPluginManager().registerEvents(setupGui, this);
        getServer().getPluginManager().registerEvents(scoreManager, this);
        scoreManager.applyAll();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new VolleyballPlaceholders(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        PluginCommand volleyWandCommand = getCommand("volleywand");
        if (volleyWandCommand != null) {
            volleyWandCommand.setExecutor(new VolleyWandCommand(wandManager, messages));
        }

        SetArenaCommand setArenaCommand = new SetArenaCommand(arenaManager, wandManager, positionManager, messages);
        PluginCommand setArena = getCommand("setarena");
        if (setArena != null) {
            setArena.setExecutor(setArenaCommand);
            setArena.setTabCompleter(setArenaCommand);
        }

        register("spawnball", new SpawnBallCommand(ballManager, messages));
        register("startgame", new StartGameCommand(partyManager, setupGui, ballManager, scoreManager, musicManager, messages));
        register("stopgame", new StopGameCommand(ballManager, scoreManager, musicManager, gameManager, messages));
        register("restartgame", new RestartGameCommand(ballManager, scoreManager, musicManager, gameManager, messages));
        register("volleyreload", new VolleyReloadCommand(this));

        PartyCommand partyCommand = new PartyCommand(partyManager, messages);
        for (String name : new String[]{"partycreate", "partyinvite", "partyleave", "partydisband"}) {
            PluginCommand party = getCommand(name);
            if (party != null) {
                party.setExecutor(partyCommand);
                party.setTabCompleter(partyCommand);
            }
        }

        VolleyMusicCommand volleyMusicCommand = new VolleyMusicCommand(musicManager, messages);
        PluginCommand volleyMusic = getCommand("volleymusic");
        if (volleyMusic != null) {
            volleyMusic.setExecutor(volleyMusicCommand);
            volleyMusic.setTabCompleter(volleyMusicCommand);
        }

        VolleyGiveCommand volleyGiveCommand = new VolleyGiveCommand(toolManager, messages);
        PluginCommand volleyGive = getCommand("volleygive");
        if (volleyGive != null) {
            volleyGive.setExecutor(volleyGiveCommand);
            volleyGive.setTabCompleter(volleyGiveCommand);
        }
    }

    public void syncConfigDefaults() {
        try (InputStream in = getResource("config.yml")) {
            if (in == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            FileConfiguration config = getConfig();
            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key) || config.contains(key)) {
                    continue;
                }
                config.set(key, defaults.get(key));
                changed = true;
            }
            if (changed) {
                saveConfig();
            }
        } catch (IOException e) {
            getLogger().warning("Could not sync missing config defaults: " + e.getMessage());
        }
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        }
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
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

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public SetupGui getSetupGui() {
        return setupGui;
    }

    public Messages getMessages() {
        return messages;
    }
}
