package me.heroicstudios.heroicvolleyball;

import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.backrow.BackrowManager;
import me.heroicstudios.heroicvolleyball.ball.BallListener;
import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.command.PartyCommand;
import me.heroicstudios.heroicvolleyball.command.RestartGameCommand;
import me.heroicstudios.heroicvolleyball.command.SetArenaCommand;
import me.heroicstudios.heroicvolleyball.command.SetBackrowCommand;
import me.heroicstudios.heroicvolleyball.command.SetBallSpawnCommand;
import me.heroicstudios.heroicvolleyball.command.SpawnBallCommand;
import me.heroicstudios.heroicvolleyball.command.StartGameCommand;
import me.heroicstudios.heroicvolleyball.command.StopGameCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyGiveCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyMusicCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyReloadCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyWandCommand;
import me.heroicstudios.heroicvolleyball.game.GameManager;
import me.heroicstudios.heroicvolleyball.game.SetupGui;
import me.heroicstudios.heroicvolleyball.game.CoinFlipGui;
import me.heroicstudios.heroicvolleyball.gui.GuiListener;
import me.heroicstudios.heroicvolleyball.hook.VolleyballPlaceholders;
import me.heroicstudios.heroicvolleyball.listener.ArenaListener;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.music.MusicManager;
import me.heroicstudios.heroicvolleyball.party.PartyManager;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import me.heroicstudios.heroicvolleyball.spawn.BallSpawnManager;
import me.heroicstudios.heroicvolleyball.tool.ToolListener;
import me.heroicstudios.heroicvolleyball.tool.ToolManager;
import me.heroicstudios.heroicvolleyball.util.ConfigDefaults;
import me.heroicstudios.heroicvolleyball.wand.WandListener;
import me.heroicstudios.heroicvolleyball.wand.WandManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class HeroicVolleyBall extends JavaPlugin {

    private ArenaManager arenaManager;
    private WandManager wandManager;
    private PositionManager positionManager;
    private GuiListener guiListener;
    private ToolManager toolManager;
    private PartyManager partyManager;
    private GameManager gameManager;
    private CoinFlipGui coinFlipGui;
    private SetupGui setupGui;
    private BallManager ballManager;
    private ScoreManager scoreManager;
    private MusicManager musicManager;
    private BackrowManager backrowManager;
    private BallSpawnManager ballSpawnManager;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        syncConfigDefaults();

        this.messages = new Messages(this);
        this.arenaManager = new ArenaManager(this);
        this.backrowManager = new BackrowManager(this);
        this.ballSpawnManager = new BallSpawnManager(this);
        this.positionManager = new PositionManager(this);
        this.wandManager = new WandManager(this);
        this.guiListener = new GuiListener(positionManager, messages);
        this.toolManager = new ToolManager(this);
        this.partyManager = new PartyManager(this);
        this.coinFlipGui = new CoinFlipGui(this, messages);
        this.gameManager = new GameManager(this, arenaManager, positionManager, toolManager, partyManager, coinFlipGui);
        this.musicManager = new MusicManager(this);
        this.scoreManager = new ScoreManager(this, arenaManager, musicManager);
        this.ballManager = new BallManager(this, arenaManager, scoreManager, gameManager, toolManager);
        this.gameManager.bind(ballManager, scoreManager, musicManager);
        this.setupGui = new SetupGui(this, gameManager, partyManager, positionManager);

        // Sweep ghost balls left in the world save by older plugin builds.
        ballManager.purgeGhosts();

        getServer().getPluginManager().registerEvents(new WandListener(wandManager, messages), this);
        getServer().getPluginManager().registerEvents(new ArenaListener(arenaManager, messages), this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new ToolListener(toolManager), this);
        getServer().getPluginManager().registerEvents(new BallListener(ballManager, toolManager), this);
        getServer().getPluginManager().registerEvents(setupGui, this);
        getServer().getPluginManager().registerEvents(coinFlipGui, this);
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

        SetBackrowCommand setBackrowCommand = new SetBackrowCommand(backrowManager, wandManager, messages);
        PluginCommand setBackrow = getCommand("setbackrow");
        if (setBackrow != null) {
            setBackrow.setExecutor(setBackrowCommand);
            setBackrow.setTabCompleter(setBackrowCommand);
        }

        SetBallSpawnCommand setBallSpawnCommand = new SetBallSpawnCommand(ballSpawnManager, messages);
        PluginCommand setBallSpawn = getCommand("setballspawnpoint");
        if (setBallSpawn != null) {
            setBallSpawn.setExecutor(setBallSpawnCommand);
        }

        register("spawnball", new SpawnBallCommand(ballManager, messages));
        register("startgame", new StartGameCommand(partyManager, setupGui, ballManager, scoreManager, musicManager,
                arenaManager, positionManager, ballSpawnManager, messages));
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
        if (ConfigDefaults.merge(getResource("config.yml"), getConfig())) {
            saveConfig();
        }
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        }
    }

    @Override
    public void onDisable() {
        // Each step is isolated: one failing shutdown must never skip the ball
        // removal below it, or the ball would be written to the world save as
        // a ghost that can't be interacted with after the restart.
        if (gameManager != null) {
            isolateShutdown("game manager", gameManager::shutdown);
        }
        if (ballManager != null) {
            isolateShutdown("ball manager", ballManager::shutdown);
        }
        if (musicManager != null) {
            isolateShutdown("music manager", musicManager::shutdown);
        }
        if (scoreManager != null) {
            isolateShutdown("score manager", scoreManager::shutdown);
        }
        if (arenaManager != null) {
            isolateShutdown("arena save", arenaManager::save);
        }
    }

    private void isolateShutdown(String what, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            getLogger().severe("Error while shutting down " + what + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public BackrowManager getBackrowManager() {
        return backrowManager;
    }

    public BallSpawnManager getBallSpawnManager() {
        return ballSpawnManager;
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

    public SetupGui getSetupGui() {return setupGui;}

    public Messages getMessages() {
        return messages;
    }
}
