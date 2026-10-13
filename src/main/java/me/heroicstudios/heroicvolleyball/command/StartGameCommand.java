package me.heroicstudios.heroicvolleyball.command;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.game.SetupGui;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.music.MusicManager;
import me.heroicstudios.heroicvolleyball.party.Party;
import me.heroicstudios.heroicvolleyball.party.PartyManager;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import me.heroicstudios.heroicvolleyball.spawn.BallSpawnManager;
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
    private final ArenaManager arenaManager;
    private final PositionManager positionManager;
    private final BallSpawnManager ballSpawnManager;
    private final Messages messages;

    public StartGameCommand(PartyManager partyManager, SetupGui setupGui, BallManager ballManager,
                            ScoreManager scoreManager, MusicManager musicManager, ArenaManager arenaManager,
                            PositionManager positionManager, BallSpawnManager ballSpawnManager, Messages messages) {
        this.partyManager = partyManager;
        this.setupGui = setupGui;
        this.ballManager = ballManager;
        this.scoreManager = scoreManager;
        this.musicManager = musicManager;
        this.arenaManager = arenaManager;
        this.positionManager = positionManager;
        this.ballSpawnManager = ballSpawnManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isSetupComplete(sender)) {
            return true;
        }

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

    private boolean isSetupComplete(CommandSender sender) {
        List<String[]> missing = new ArrayList<>();

        for (Team team : Team.values()) {
            if (!arenaManager.isSet(team)) {
                missing.add(new String[]{"game.setup-missing-arena",
                        "team", Messages.teamDisplay(team),
                        "color", team.name().toLowerCase()});
            }
            if (!positionManager.hasAllPositions(team)) {
                missing.add(new String[]{"game.setup-missing-positions",
                        "team", Messages.teamDisplay(team)});
            }
            if (!ballSpawnManager.isSet(team)) {
                missing.add(new String[]{"game.setup-missing-spawnpoint",
                        "team", Messages.teamDisplay(team),
                        "color", team.name().toLowerCase()});
            }
        }

        if (missing.isEmpty()) {
            return true;
        }

        messages.send(sender, "game.setup-missing");
        for (String[] entry : missing) {
            String key = entry[0];
            String[] placeholders = new String[entry.length - 1];
            System.arraycopy(entry, 1, placeholders, 0, placeholders.length);
            messages.send(sender, key, placeholders);
        }
        return false;
    }
}
