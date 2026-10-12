package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class StartGameCommand implements CommandExecutor {

    private final BallManager ballManager;
    private final ScoreManager scoreManager;

    public StartGameCommand(BallManager ballManager, ScoreManager scoreManager) {
        this.ballManager = ballManager;
        this.scoreManager = scoreManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        scoreManager.resetMatch();

        if (!ballManager.spawnAtStart()) {
            sender.sendMessage(Component.text("Position 6 has not been set for either team yet.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("The volleyball has been placed at Position 6. Use the shovel to serve!", NamedTextColor.GREEN));
        return true;
    }
}
