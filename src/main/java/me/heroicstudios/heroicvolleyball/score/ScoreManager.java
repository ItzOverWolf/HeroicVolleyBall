package me.heroicstudios.heroicvolleyball.score;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.music.MusicManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;

public final class ScoreManager implements Listener {

    private final HeroicVolleyBall plugin;
    private final ArenaManager arenaManager;
    private final MusicManager musicManager;

    private int pointsPerSet = 25;
    private int setsToWin = 2;

    private int redPoints;
    private int bluePoints;
    private int redSets;
    private int blueSets;
    private boolean matchOver;

    private Scoreboard board;
    private Objective objective;
    private Score scoreLine;
    private Score redLine;
    private Score blueLine;

    public ScoreManager(HeroicVolleyBall plugin, ArenaManager arenaManager, MusicManager musicManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.musicManager = musicManager;
        loadConfig();
        setupBoard();
    }

    public void loadConfig() {
        this.pointsPerSet = Math.max(1, plugin.getConfig().getInt("game.points-per-set", 25));
        this.setsToWin = Math.max(1, plugin.getConfig().getInt("game.sets-to-win", 2));
    }

    private void setupBoard() {
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = board.registerNewObjective("heroicvolley", Criteria.DUMMY,
                Component.text("Volleyball", NamedTextColor.GOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        this.scoreLine = addLine("score", 3);
        this.redLine = addLine("red", 2);
        this.blueLine = addLine("blue", 1);

        redraw();
    }

    private Score addLine(String key, int order) {
        Score score = objective.getScore(key);
        score.setScore(order);
        score.numberFormat(NumberFormat.blank());
        return score;
    }

    public void redraw() {
        scoreLine.customName(Component.text("Score: " + redPoints + " - " + bluePoints, NamedTextColor.WHITE));
        redLine.customName(Component.text("Red: " + redSets, Team.RED.getColor()));
        blueLine.customName(Component.text("Blue: " + blueSets, Team.BLUE.getColor()));
    }

    public void apply(Player player) {
        player.setScoreboard(board);
    }

    public void applyAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }

    public void addPoint(Team team) {
        if (matchOver) {
            return;
        }

        if (team == Team.RED) {
            redPoints++;
        } else {
            bluePoints++;
        }

        Bukkit.broadcast(Component.text(team.getDisplayName() + " scores!  ", team.getColor())
                .append(Component.text("Score: " + redPoints + " - " + bluePoints, NamedTextColor.WHITE)));

        showTitle(
                Component.text(team.getDisplayName() + " scores!", team.getColor()),
                Component.text("Score: " + redPoints + " - " + bluePoints, NamedTextColor.WHITE));

        playTeamSound(team, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        playTeamSound(team.opposite(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

        checkSet(team);
        redraw();
    }

    private void checkSet(Team team) {
        int points = team == Team.RED ? redPoints : bluePoints;
        if (points < pointsPerSet) {
            return;
        }

        if (team == Team.RED) {
            redSets++;
        } else {
            blueSets++;
        }
        redPoints = 0;
        bluePoints = 0;

        Bukkit.broadcast(Component.text(team.getDisplayName() + " wins the set!  ", team.getColor())
                .append(Component.text("Sets: Red " + redSets + " - " + blueSets + " Blue", NamedTextColor.WHITE)));

        showTitle(
                Component.text(team.getDisplayName() + " wins the set!", team.getColor()),
                Component.text("Sets: Red " + redSets + " - " + blueSets + " Blue", NamedTextColor.WHITE));

        playTeamSound(team, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        playTeamSound(team.opposite(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);

        int sets = team == Team.RED ? redSets : blueSets;
        if (sets >= setsToWin) {
            matchOver = true;
            if (musicManager != null) {
                musicManager.stop();
            }
            Bukkit.broadcast(Component.text(team.getDisplayName() + " wins the game! ", team.getColor())
                    .append(Component.text("(Run /startgame to play again)", NamedTextColor.GRAY)));
            showTitle(
                    Component.text(team.getDisplayName() + " wins the game!", team.getColor()),
                    Component.text("Run /startgame to play again", NamedTextColor.GRAY));
            playTeamSound(team, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        }
    }

    private void showTitle(Component title, Component subtitle) {
        Title display = Title.title(title, subtitle);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(display);
        }
    }

    private void playTeamSound(Team team, Sound sound, float volume, float pitch) {
        for (Player player : playersOf(team)) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private List<Player> playersOf(Team team) {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (arenaManager.getArenaAt(player.getLocation()) == team) {
                players.add(player);
            }
        }
        return players;
    }

    public void resetMatch() {
        redPoints = 0;
        bluePoints = 0;
        redSets = 0;
        blueSets = 0;
        matchOver = false;
        redraw();
        applyAll();
    }

    public void shutdown() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboard() == board) {
                player.setScoreboard(main);
            }
        }
        if (objective != null) {
            objective.unregister();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        apply(event.getPlayer());
    }
}
