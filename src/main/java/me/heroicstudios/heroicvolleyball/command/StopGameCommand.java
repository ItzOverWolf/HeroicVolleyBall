package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.game.GameManager;
import me.heroicstudios.heroicvolleyball.music.MusicManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class StopGameCommand implements CommandExecutor {

    private final BallManager ballManager;
    private final ScoreManager scoreManager;
    private final MusicManager musicManager;
    private final GameManager gameManager;

    public StopGameCommand(BallManager ballManager, ScoreManager scoreManager, MusicManager musicManager, GameManager gameManager) {
        this.ballManager = ballManager;
        this.scoreManager = scoreManager;
        this.musicManager = musicManager;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ballManager.removeBall();
        musicManager.stop();
        scoreManager.resetMatch();
        gameManager.end();
        sender.sendMessage(Component.text("Game stopped. The ball and music have been removed and the score reset.", NamedTextColor.YELLOW));
        return true;
    }
}
