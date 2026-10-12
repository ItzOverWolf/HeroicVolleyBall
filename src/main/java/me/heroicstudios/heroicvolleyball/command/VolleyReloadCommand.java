package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        plugin.getArenaManager().load();
        plugin.getBallManager().loadConfig();
        plugin.getScoreManager().loadConfig();
        sender.sendMessage(Component.text("HeroicVolleyBall configuration reloaded.", NamedTextColor.GREEN));
        return true;
    }
}
