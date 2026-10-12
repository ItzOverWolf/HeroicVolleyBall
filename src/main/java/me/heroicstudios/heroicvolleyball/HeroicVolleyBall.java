package me.heroicstudios.heroicvolleyball;

import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.ball.BallListener;
import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.command.SetArenaCommand;
import me.heroicstudios.heroicvolleyball.command.SpawnBallCommand;
import me.heroicstudios.heroicvolleyball.command.StartGameCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyGiveCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyReloadCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyWandCommand;
import me.heroicstudios.heroicvolleyball.gui.GuiListener;
import me.heroicstudios.heroicvolleyball.listener.ArenaListener;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.arenaManager = new ArenaManager(this);
        this.positionManager = new PositionManager(this);
        this.wandManager = new WandManager(this);
        this.guiListener = new GuiListener(positionManager);
        this.toolManager = new ToolManager(this);
        this.ballManager = new BallManager(this, arenaManager);

        getServer().getPluginManager().registerEvents(new WandListener(wandManager), this);
        getServer().getPluginManager().registerEvents(new ArenaListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new ToolListener(toolManager), this);
        getServer().getPluginManager().registerEvents(new BallListener(ballManager, toolManager), this);

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
        register("startgame", new StartGameCommand(ballManager));
        register("volleyreload", new VolleyReloadCommand(this));

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
}
