package me.heroicstudios.heroicvolleyball;

import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.command.SetArenaCommand;
import me.heroicstudios.heroicvolleyball.command.VolleyWandCommand;
import me.heroicstudios.heroicvolleyball.gui.GuiListener;
import me.heroicstudios.heroicvolleyball.listener.ArenaListener;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import me.heroicstudios.heroicvolleyball.wand.WandListener;
import me.heroicstudios.heroicvolleyball.wand.WandManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class HeroicVolleyBall extends JavaPlugin {

    private ArenaManager arenaManager;
    private WandManager wandManager;
    private PositionManager positionManager;
    private GuiListener guiListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.arenaManager = new ArenaManager(this);
        this.positionManager = new PositionManager(this);
        this.wandManager = new WandManager(this);
        this.guiListener = new GuiListener(positionManager);

        getServer().getPluginManager().registerEvents(new WandListener(wandManager), this);
        getServer().getPluginManager().registerEvents(new ArenaListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(guiListener, this);

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
    }

    @Override
    public void onDisable() {
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
}
