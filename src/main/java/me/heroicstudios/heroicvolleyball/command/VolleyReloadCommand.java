package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.game.GameSession;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class VolleyReloadCommand implements CommandExecutor {

    private final HeroicVolleyBall plugin;

    public VolleyReloadCommand(HeroicVolleyBall plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        plugin.reloadConfig();
        plugin.syncConfigDefaults();
        plugin.getMessages().reload();
        plugin.getGuiConfig().reload();
        plugin.getArenaManager().loadAll();
        plugin.getQueueManager().reload();

        for (GameSession session : plugin.getSessionManager().sessions()) {
            session.getBall().loadConfig();
            session.getScore().loadConfig();
            session.getScore().applyAll();
            session.getMusic().loadConfig();
        }

        plugin.getMessages().send(sender, "general.reloaded");
        return true;
    }
}
