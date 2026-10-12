package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.game.GameManager;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.music.MusicManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class RestartGameCommand implements CommandExecutor {

    private final BallManager ballManager;
    private final ScoreManager scoreManager;
    private final MusicManager musicManager;
    private final GameManager gameManager;
    private final Messages messages;

    public RestartGameCommand(BallManager ballManager, ScoreManager scoreManager, MusicManager musicManager,
                              GameManager gameManager, Messages messages) {
        this.ballManager = ballManager;
        this.scoreManager = scoreManager;
        this.musicManager = musicManager;
        this.gameManager = gameManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ballManager.removeBall();
        musicManager.stop();
        scoreManager.resetMatch();
        gameManager.end();

        if (!ballManager.spawnAtStart()) {
            messages.send(sender, "game.console-need-position");
            return true;
        }

        musicManager.startDefault();
        messages.send(sender, "game.restarted");
        return true;
    }
}
