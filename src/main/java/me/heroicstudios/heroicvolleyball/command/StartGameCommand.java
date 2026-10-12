package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.game.SetupGui;
import me.heroicstudios.heroicvolleyball.music.MusicManager;
import me.heroicstudios.heroicvolleyball.party.Party;
import me.heroicstudios.heroicvolleyball.party.PartyManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StartGameCommand implements CommandExecutor {

    private final PartyManager partyManager;
    private final SetupGui setupGui;
    private final BallManager ballManager;
    private final ScoreManager scoreManager;
    private final MusicManager musicManager;

    public StartGameCommand(PartyManager partyManager, SetupGui setupGui, BallManager ballManager,
                            ScoreManager scoreManager, MusicManager musicManager) {
        this.partyManager = partyManager;
        this.setupGui = setupGui;
        this.ballManager = ballManager;
        this.scoreManager = scoreManager;
        this.musicManager = musicManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            Party party = partyManager.getParty(player);
            if (party != null) {
                if (party.size() < 2 || party.size() % 2 != 0) {
                    sender.sendMessage(Component.text("A party needs an even number of players (2, 4, 6...) to start.", NamedTextColor.RED));
                    return true;
                }
                setupGui.open(player);
                sender.sendMessage(Component.text("Choose your team and position to begin.", NamedTextColor.GREEN));
                return true;
            }
        }

        scoreManager.resetMatch();
        if (!ballManager.spawnAtStart()) {
            sender.sendMessage(Component.text("Position 6 has not been set for either team yet.", NamedTextColor.RED));
            return true;
        }
        musicManager.startDefault();
        sender.sendMessage(Component.text("The volleyball has been placed at Position 6. Use the shovel to serve!", NamedTextColor.GREEN));
        return true;
    }
}
