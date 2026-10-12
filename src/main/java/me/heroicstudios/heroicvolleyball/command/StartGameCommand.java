package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.game.SetupGui;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.music.MusicManager;
import me.heroicstudios.heroicvolleyball.party.Party;
import me.heroicstudios.heroicvolleyball.party.PartyManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
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
    private final Messages messages;

    public StartGameCommand(PartyManager partyManager, SetupGui setupGui, BallManager ballManager,
                            ScoreManager scoreManager, MusicManager musicManager, Messages messages) {
        this.partyManager = partyManager;
        this.setupGui = setupGui;
        this.ballManager = ballManager;
        this.scoreManager = scoreManager;
        this.musicManager = musicManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            Party party = partyManager.getParty(player);
            if (party != null) {
                if (party.size() < 2 || party.size() % 2 != 0) {
                    messages.send(sender, "game.party-needs-even");
                    return true;
                }
                setupGui.open(player);
                messages.send(sender, "game.party-choose");
                return true;
            }

            setupGui.openSolo(player);
            messages.send(sender, "game.solo-choose");
            return true;
        }

        scoreManager.resetMatch();
        if (!ballManager.spawnAtStart()) {
            messages.send(sender, "game.console-need-position");
            return true;
        }
        musicManager.startDefault();
        messages.send(sender, "game.console-start");
        return true;
    }
}
