package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.ball.BallManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class StartGameCommand implements CommandExecutor {

    private final BallManager ballManager;

    public StartGameCommand(BallManager ballManager) {
        this.ballManager = ballManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!ballManager.spawnAtStart()) {
            sender.sendMessage(Component.text("Position 6 has not been set for either team yet.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("The volleyball has been placed at Position 6. Use the shovel to serve!", NamedTextColor.GREEN));
        return true;
    }
}
