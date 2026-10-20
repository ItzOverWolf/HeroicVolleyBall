package me.heroicstudios.heroicvolleyball;

import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.ball.BallListener;
import me.heroicstudios.heroicvolleyball.command.RestartGameCommand;
import me.heroicstudios.heroicvolleyball.command.SetArenaCommand;
import me.heroicstudios.heroicvolleyball.command.SetBackrowCommand;
import me.heroicstudios.heroicvolleyball.command.SetBallSpawnCommand;
import me.heroicstudios.heroicvolleyball.command.SpawnBallCommand;
import me.heroicstudios.heroicvolleyball.command.DuelCommand;
import me.heroicstudios.heroicvolleyball.command.StartGameCommand;
import me.heroicstudios.heroicvolleyball.command.StopGameCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyArenaCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyGiveCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyJoinCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyLeaveCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyMusicCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyReloadCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyRootCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyTeamCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyWandCommand;
import me.heroicstudios.heroicvolleyball.game.ArenaPickerGui;
import me.heroicstudios.heroicvolleyball.duel.DuelManager;
import me.heroicstudios.heroicvolleyball.game.SessionManager;
import me.heroicstudios.heroicvolleyball.game.SetupGui;
import me.heroicstudios.heroicvolleyball.gui.GuiConfig;
import me.heroicstudios.heroicvolleyball.gui.GuiListener;
import me.heroicstudios.heroicvolleyball.gui.team.TeamGuiListener;
import me.heroicstudios.heroicvolleyball.hook.VolleyballPlaceholders;
import me.heroicstudios.heroicvolleyball.queue.QueueGui;
import me.heroicstudios.heroicvolleyball.queue.QueueListener;
import me.heroicstudios.heroicvolleyball.queue.QueueManager;
import me.heroicstudios.heroicvolleyball.util.ChatInputManager;
import me.heroicstudios.heroicvolleyball.listener.ArenaListener;
import me.heroicstudios.heroicvolleyball.listener.SessionListener;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.team.TeamManager;
import me.heroicstudios.heroicvolleyball.tool.ToolListener;
import me.heroicstudios.heroicvolleyball.tool.ToolManager;
import me.heroicstudios.heroicvolleyball.util.ConfigDefaults;
import me.heroicstudios.heroicvolleyball.wand.WandListener;
import me.heroicstudios.heroicvolleyball.wand.WandManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class HeroicVolleyBall extends JavaPlugin {

    private ArenaManager arenaManager;
    private WandManager wandManager;
    private GuiListener guiListener;
    private ToolManager toolManager;
    private SessionManager sessionManager;
    private TeamManager teamManager;
    private DuelManager duelManager;
    private QueueManager queueManager;
    private QueueGui queueGui;
    private GuiConfig guiConfig;
    private ChatInputManager chatInput;
    private SetupGui setupGui;
    private ArenaPickerGui arenaPickerGui;
    private Messages messages;
    private VolleyRootCommand rootCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        syncConfigDefaults();

        this.messages = new Messages(this);
        this.guiConfig = new GuiConfig(this);
        this.arenaManager = new ArenaManager(this);
        this.wandManager = new WandManager(this);
        this.guiListener = new GuiListener(arenaManager, messages);
        this.toolManager = new ToolManager(this);
        this.sessionManager = new SessionManager(this, arenaManager, toolManager);
        this.teamManager = new TeamManager(this);
        this.duelManager = new DuelManager(this, sessionManager);
        this.queueManager = new QueueManager(this, sessionManager, arenaManager, teamManager, messages);
        this.queueGui = new QueueGui(this, queueManager);
        this.chatInput = new ChatInputManager(this);
        this.chatInput.start();
        this.setupGui = new SetupGui(this, sessionManager);
        this.arenaPickerGui = new ArenaPickerGui(this, arenaManager, sessionManager, setupGui);

        // Sweep ball entities left in the world save by older plugin builds.
        sessionManager.purgeGhosts();

        getServer().getPluginManager().registerEvents(new WandListener(wandManager, messages), this);
        getServer().getPluginManager().registerEvents(new ArenaListener(arenaManager, messages), this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new ToolListener(toolManager), this);
        getServer().getPluginManager().registerEvents(new BallListener(sessionManager, toolManager), this);
        getServer().getPluginManager().registerEvents(new SessionListener(this, sessionManager), this);
        getServer().getPluginManager().registerEvents(new QueueListener(queueManager), this);
        getServer().getPluginManager().registerEvents(queueGui, this);
        getServer().getPluginManager().registerEvents(setupGui, this);
        getServer().getPluginManager().registerEvents(arenaPickerGui, this);
        getServer().getPluginManager().registerEvents(new TeamGuiListener(this, chatInput), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new VolleyballPlaceholders(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        PluginCommand volleyWandCommand = getCommand("volleywand");
        if (volleyWandCommand != null) {
            volleyWandCommand.setExecutor(new VolleyWandCommand(wandManager, messages));
        }

        SetArenaCommand setArenaCommand = new SetArenaCommand(arenaManager, wandManager, messages);
        PluginCommand setArena = getCommand("setarena");
        if (setArena != null) {
            setArena.setExecutor(setArenaCommand);
            setArena.setTabCompleter(setArenaCommand);
        }

        SetBackrowCommand setBackrowCommand = new SetBackrowCommand(arenaManager, wandManager, messages);
        PluginCommand setBackrow = getCommand("setbackrow");
        if (setBackrow != null) {
            setBackrow.setExecutor(setBackrowCommand);
            setBackrow.setTabCompleter(setBackrowCommand);
        }

        SetBallSpawnCommand setBallSpawnCommand = new SetBallSpawnCommand(arenaManager, messages);
        PluginCommand setBallSpawn = getCommand("setballspawnpoint");
        if (setBallSpawn != null) {
            setBallSpawn.setExecutor(setBallSpawnCommand);
            setBallSpawn.setTabCompleter(setBallSpawnCommand);
        }

        register("spawnball", new SpawnBallCommand(sessionManager, arenaManager, messages));
        register("startgame", new StartGameCommand(setupGui, arenaPickerGui, sessionManager, arenaManager, messages));
        register("stopgame", new StopGameCommand(sessionManager, arenaManager, messages));
        register("restartgame", new RestartGameCommand(sessionManager, arenaManager, messages));
        register("volleyreload", new VolleyReloadCommand(this));
        register("volleyarena", new VolleyArenaCommand(arenaManager, sessionManager, queueManager, messages));
        register("volleyteam", new VolleyTeamCommand(teamManager, sessionManager, queueManager, messages));
        register("teamduel", new DuelCommand(duelManager, teamManager, messages));
        register("volleyjoin", new VolleyJoinCommand(queueManager, arenaManager, queueGui, messages));
        register("volleyleave", new VolleyLeaveCommand(queueManager, sessionManager, messages));

        VolleyMusicCommand volleyMusicCommand = new VolleyMusicCommand(sessionManager, arenaManager, messages);
        PluginCommand volleyMusic = getCommand("volleymusic");
        if (volleyMusic != null) {
            volleyMusic.setExecutor(volleyMusicCommand);
            volleyMusic.setTabCompleter(volleyMusicCommand);
        }

        getServer().getScheduler().runTaskTimer(this, () -> {
            teamManager.tick();
            duelManager.tick();
            queueManager.tick();
        }, 40L, 20L);

        VolleyGiveCommand volleyGiveCommand = new VolleyGiveCommand(toolManager, messages);
        PluginCommand volleyGive = getCommand("volleygive");
        if (volleyGive != null) {
            volleyGive.setExecutor(volleyGiveCommand);
            volleyGive.setTabCompleter(volleyGiveCommand);
        }

        this.rootCommand = new VolleyRootCommand(this, messages);
        for (String name : java.util.List.of("volleyteam", "teamduel", "volleyjoin", "volleyleave",
                "volleywand", "volleyarena", "setarena", "setbackrow", "setballspawnpoint",
                "spawnball", "startgame", "stopgame", "restartgame", "volleygive",
                "volleymusic", "volleyreload")) {
            rootCommand.route(name, getCommand(name));
        }
        PluginCommand vb = getCommand("vb");
        if (vb != null) {
            vb.setExecutor(rootCommand);
            vb.setTabCompleter(rootCommand);
            rootCommand.setSelf(vb);
        }
        PluginCommand volleyHelp = getCommand("volleyhelp");
        if (volleyHelp != null) {
            volleyHelp.setExecutor(rootCommand);
            volleyHelp.setTabCompleter(rootCommand);
        }
    }

    public void syncConfigDefaults() {
        if (ConfigDefaults.merge(getResource("config.yml"), getConfig())) {
            saveConfig();
        }
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter completer) {
                command.setTabCompleter(completer);
            }
        }
    }

    @Override
    public void onDisable() {
        // Each step is isolated: one failing shutdown must never skip the ball
        // removal below it, or the ball would be written to the world save as
        // a ghost that can't be interacted with after the restart.
        if (queueManager != null) {
            isolateShutdown("queues", queueManager::shutdown);
        }
        if (sessionManager != null) {
            isolateShutdown("sessions", sessionManager::shutdown);
        }
        if (duelManager != null) {
            isolateShutdown("duels", duelManager::shutdown);
        }
        if (chatInput != null) {
            isolateShutdown("chat input", chatInput::stop);
        }
        if (teamManager != null) {
            isolateShutdown("teams", teamManager::shutdown);
        }
        if (arenaManager != null) {
            isolateShutdown("arena save", arenaManager::saveAll);
            isolateShutdown("arena io", arenaManager::shutdown);
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

    public WandManager getWandManager() {
        return wandManager;
    }

    public GuiListener getGuiListener() {
        return guiListener;
    }

    public ToolManager getToolManager() {
        return toolManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    public SetupGui getSetupGui() {
        return setupGui;
    }

    public Messages getMessages() {
        return messages;
    }

    public VolleyRootCommand getRootCommand() {
        return rootCommand;
    }
}
