package me.heroicstudios.heroicvolleyball.score;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.game.GameManager;
import me.heroicstudios.heroicvolleyball.message.Messages;
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
    private GameManager gameManager;

    private int pointsPerSet = 25;
    private int setsToWin = 2;
    private boolean scoreboardEnabled = true;
    private boolean showTitles = true;
    private boolean broadcastPoints = true;

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

    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public int getRedPoints() {
        return redPoints;
    }

    public int getBluePoints() {
        return bluePoints;
    }

    public int getRedSets() {
        return redSets;
    }

    public int getBlueSets() {
        return blueSets;
    }

    public boolean isMatchOver() {
        return matchOver;
    }

    public void loadConfig() {
        this.pointsPerSet = Math.max(1, plugin.getConfig().getInt("game.points-per-set", 25));
        this.setsToWin = Math.max(1, plugin.getConfig().getInt("game.sets-to-win", 2));
        this.scoreboardEnabled = plugin.getConfig().getBoolean("scoreboard.enabled", true);
        this.showTitles = plugin.getConfig().getBoolean("game.titles", true);
        this.broadcastPoints = plugin.getConfig().getBoolean("game.broadcast-points", true);
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
        if (!scoreboardEnabled) {
            return;
        }
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

        Messages messages = plugin.getMessages();
        String teamTag = Messages.teamDisplay(team);
        String red = String.valueOf(redPoints);
        String blue = String.valueOf(bluePoints);

        if (broadcastPoints) {
            Bukkit.broadcast(messages.get("score.point", "team", teamTag, "red", red, "blue", blue));
        }
        if (showTitles) {
            showTitle(
                    messages.get("score.point-title", "team", teamTag, "red", red, "blue", blue),
                    messages.get("score.point-subtitle", "team", teamTag, "red", red, "blue", blue));
        }

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

        Messages messages = plugin.getMessages();
        String teamTag = Messages.teamDisplay(team);
        String redSetsText = String.valueOf(redSets);
        String blueSetsText = String.valueOf(blueSets);

        if (broadcastPoints) {
            Bukkit.broadcast(messages.get("score.set", "team", teamTag,
                    "red_sets", redSetsText, "blue_sets", blueSetsText));
        }
        if (showTitles) {
            showTitle(
                    messages.get("score.set-title", "team", teamTag,
                            "red_sets", redSetsText, "blue_sets", blueSetsText),
                    messages.get("score.set-subtitle", "team", teamTag,
                            "red_sets", redSetsText, "blue_sets", blueSetsText));
        }

        playTeamSound(team, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        playTeamSound(team.opposite(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);

        int sets = team == Team.RED ? redSets : blueSets;
        if (sets >= setsToWin) {
            matchOver = true;
            if (musicManager != null) {
                musicManager.stop();
            }
            if (broadcastPoints) {
                Bukkit.broadcast(messages.get("score.win", "team", teamTag));
            }
            if (showTitles) {
                showTitle(
                        messages.get("score.win-title", "team", teamTag),
                        messages.get("score.win-subtitle", "team", teamTag));
            }
            playTeamSound(team, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            if (gameManager != null) {
                gameManager.end();
            }
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
        if (gameManager != null && gameManager.isActive()) {
            return gameManager.playersOf(team);
        }
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
