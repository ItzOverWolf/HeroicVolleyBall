package me.heroicstudios.heroicvolleyball.score;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Side;
import me.heroicstudios.heroicvolleyball.game.Countdown;
import me.heroicstudios.heroicvolleyball.game.GameSession;
import me.heroicstudios.heroicvolleyball.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;
import java.util.function.Consumer;

public final class ScoreManager {

    private final HeroicVolleyBall plugin;
    private final GameSession session;
    private Consumer<Side> onMatchEnd;

    private int pointsPerSet = 25;
    private int setsToWin = 2;
    private int roundCountdown = 5;
    private boolean scoreboardEnabled = true;
    private boolean showTitles = true;
    private boolean broadcastPoints = true;
    private boolean deuceEnabled = true;
    private int deuceMargin = 2;

    private int redPoints;
    private int bluePoints;
    private int redSets;
    private int blueSets;
    private boolean matchOver;
    private boolean roundTransition;
    private boolean deuceAnnounced;
    private int roundToken;

    private Scoreboard board;
    private Objective objective;
    private Score scoreLine;
    private Score redLine;
    private Score blueLine;

    public ScoreManager(HeroicVolleyBall plugin, GameSession session) {
        this.plugin = plugin;
        this.session = session;
        loadConfig();
        setupBoard();
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

    public boolean isRoundTransition() {
        return roundTransition;
    }

    /** Overrides the config sets-to-win for this match (used by team duels). */
    public void setSetsToWin(int setsToWin) {
        this.setsToWin = Math.max(1, setsToWin);
    }

    public void setOnMatchEnd(Consumer<Side> onMatchEnd) {
        this.onMatchEnd = onMatchEnd;
    }

    public void loadConfig() {
        this.pointsPerSet = Math.max(1, plugin.getConfig().getInt("game.points-per-set", 25));
        this.setsToWin = Math.max(1, plugin.getConfig().getInt("game.sets-to-win", 2));
        this.roundCountdown = Math.max(0, plugin.getConfig().getInt("game.round-countdown", 5));
        this.scoreboardEnabled = plugin.getConfig().getBoolean("scoreboard.enabled", true);
        this.showTitles = plugin.getConfig().getBoolean("game.titles", true);
        this.broadcastPoints = plugin.getConfig().getBoolean("game.broadcast-points", true);
        this.deuceEnabled = plugin.getConfig().getBoolean("game.deuce.enabled", true);
        this.deuceMargin = Math.max(1, plugin.getConfig().getInt("game.deuce.margin", 2));
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
        scoreLine.customName(Component.text("Score: ", NamedTextColor.WHITE)
                .append(colorComponent(Side.RED, String.valueOf(redPoints)))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(colorComponent(Side.BLUE, String.valueOf(bluePoints))));
        redLine.customName(Component.text(session.teamName(Side.RED) + ": ", session.teamColor(Side.RED))
                .append(colorComponent(Side.RED, String.valueOf(redSets))));
        blueLine.customName(Component.text(session.teamName(Side.BLUE) + ": ", session.teamColor(Side.BLUE))
                .append(colorComponent(Side.BLUE, String.valueOf(blueSets))));
    }

    /** Builds a component in the team colour, used by the scoreboard lines. */
    private Component colorComponent(Side side, String value) {
        TextColor color = session.teamColor(side);
        return color != null ? Component.text(value, color) : Component.text(value);
    }

    public void apply(Player player) {
        if (!scoreboardEnabled) {
            return;
        }
        player.setScoreboard(board);
    }

    public void applyAll() {
        for (Player player : session.players()) {
            apply(player);
        }
    }

    public boolean isBoard(Player player) {
        return player.getScoreboard() == board;
    }

    public void restoreBoard(Player player) {
        if (isBoard(player)) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void addPoint(Side side) {
        if (matchOver || roundTransition) {
            return;
        }

        if (side == Side.RED) {
            redPoints++;
        } else {
            bluePoints++;
        }

        Messages messages = plugin.getMessages();
        String teamTag = session.teamDisplay(side);
        String red = colorize(Side.RED, String.valueOf(redPoints));
        String blue = colorize(Side.BLUE, String.valueOf(bluePoints));

        if (broadcastPoints) {
            session.broadcast(messages.get("score.point", "team", teamTag, "red", red, "blue", blue));
        }
        if (showTitles) {
            showTitle(
                    messages.get("score.point-title", "team", teamTag, "red", red, "blue", blue),
                    messages.get("score.point-subtitle", "team", teamTag, "red", red, "blue", blue));
        }

        playTeamSound(side, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        playTeamSound(side.opposite(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

        announceDeuce();

        checkSet(side);
        if (!matchOver) {
            redraw();
        }
    }

    /** Announces the deuce once per deuce window so players know the set runs on. */
    private void announceDeuce() {
        if (!deuceEnabled || deuceAnnounced) {
            return;
        }
        if (!isDeuce()) {
            return;
        }
        deuceAnnounced = true;
        Messages messages = plugin.getMessages();
        String red = colorize(Side.RED, String.valueOf(redPoints));
        String blue = colorize(Side.BLUE, String.valueOf(bluePoints));
        if (broadcastPoints) {
            session.broadcast(messages.get("score.deuce", "red", red, "blue", blue,
                    "margin", String.valueOf(deuceMargin)));
        }
        if (showTitles) {
            showTitle(
                    messages.get("score.deuce-title", "red", red, "blue", blue,
                            "margin", String.valueOf(deuceMargin)),
                    messages.get("score.deuce-subtitle", "red", red, "blue", blue,
                            "margin", String.valueOf(deuceMargin)));
        }
    }

    /** Coloured "&lt;red&gt;12 - &lt;blue&gt;9" line used by the action bar and menus. */
    public String coloredScore() {
        return colorize(Side.RED, String.valueOf(redPoints)) + " <gray>-</gray> "
                + colorize(Side.BLUE, String.valueOf(bluePoints));
    }

    /** Wraps a value in the team colour so scores never read as plain white numbers. */
    private String colorize(Side side, String value) {
        TextColor color = session.teamColor(side);
        if (color == null) {
            return value;
        }
        return "<" + color.asHexString() + ">" + value;
    }

    /**
     * A set is won when a team reaches {@code points-per-set} and, with the
     * deuce rule enabled, holds a {@code deuce.margin} point lead. That means
     * 25-24 keeps the rally alive and the set ends on 26-24 or 27-25.
     */
    private boolean winsSet(Side side) {
        int points = side == Side.RED ? redPoints : bluePoints;
        int other = side == Side.RED ? bluePoints : redPoints;
        if (points < pointsPerSet) {
            return false;
        }
        if (!deuceEnabled) {
            return true;
        }
        return points - other >= deuceMargin;
    }

    /** True while the score sits in the deuce zone (both teams level-ish at the set target). */
    public boolean isDeuce() {
        if (!deuceEnabled) {
            return false;
        }
        int target = Math.max(1, pointsPerSet - 1);
        return redPoints >= target && bluePoints >= target;
    }

    private void checkSet(Side side) {
        if (!winsSet(side)) {
            return;
        }

        if (side == Side.RED) {
            redSets++;
        } else {
            blueSets++;
        }
        redPoints = 0;
        bluePoints = 0;
        deuceAnnounced = false;

        Messages messages = plugin.getMessages();
        String teamTag = session.teamDisplay(side);
        String redSetsText = colorize(Side.RED, String.valueOf(redSets));
        String blueSetsText = colorize(Side.BLUE, String.valueOf(blueSets));

        if (broadcastPoints) {
            session.broadcast(messages.get("score.set", "team", teamTag,
                    "red_sets", redSetsText, "blue_sets", blueSetsText));
        }
        if (showTitles) {
            showTitle(
                    messages.get("score.set-title", "team", teamTag,
                            "red_sets", redSetsText, "blue_sets", blueSetsText),
                    messages.get("score.set-subtitle", "team", teamTag,
                            "red_sets", redSetsText, "blue_sets", blueSetsText));
        }

        playTeamSound(side, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        playTeamSound(side.opposite(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);

        int sets = side == Side.RED ? redSets : blueSets;
        if (sets >= setsToWin) {
            matchOver = true;
            session.getMusic().stop();
            if (broadcastPoints) {
                session.broadcast(messages.get("score.win", "team", teamTag));
            }
            if (showTitles) {
                showTitle(
                        messages.get("score.win-title", "team", teamTag),
                        messages.get("score.win-subtitle", "team", teamTag));
            }
            playTeamSound(side, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            session.pauseBall();
            GameSession target = session;
            Consumer<Side> callback = onMatchEnd;
            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(side));
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> target.getManager().stop(target));
            }
        } else {
            startRoundBreak(side);
        }
    }

    private void startRoundBreak(Side nextServer) {
        roundTransition = true;
        int token = ++roundToken;
        int nextRound = redSets + blueSets + 1;
        if (roundCountdown > 0) {
            session.pauseBall();
        }
        Countdown.run(plugin, roundCountdown, session.players(),
                "game.round-countdown-title", "game.round-countdown-subtitle",
                () -> {
                    if (token != roundToken) {
                        return;
                    }
                    roundTransition = false;
                    Messages messages = plugin.getMessages();
                    if (broadcastPoints) {
                        session.broadcast(messages.get("game.round-start",
                                "round", String.valueOf(nextRound),
                                "team", session.teamDisplay(nextServer)));
                    }
                    session.startNextRound(nextServer);
                },
                "round", String.valueOf(nextRound));
    }

    private void showTitle(Component title, Component subtitle) {
        Title display = Messages.noFade(title, subtitle);
        for (Player player : session.players()) {
            player.showTitle(display);
        }
    }

    private void playTeamSound(Side side, Sound sound, float volume, float pitch) {
        java.util.List<org.bukkit.entity.Player> targets = session.playersOf(side);
        if (!targets.isEmpty()) {
            for (org.bukkit.entity.Player player : targets) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
            return;
        }
        // Fallback for solo matches - play to all participants
        if (session.hasSoloPlayers()) {
            for (org.bukkit.entity.Player player : session.players()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
            return;
        }
        // Last resort
        for (org.bukkit.entity.Player player : session.players()) {
            Side playerSide = session.getSide(player);
            if (playerSide == side) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }
    }

    public void resetMatch() {
        redPoints = 0;
        bluePoints = 0;
        redSets = 0;
        blueSets = 0;
        matchOver = false;
        roundTransition = false;
        deuceAnnounced = false;
        roundToken++;
        redraw();
        applyAll();
    }

    public void shutdown() {
        roundToken++;
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        List<Player> players = session.players();
        for (Player player : players) {
            if (player.getScoreboard() == board) {
                player.setScoreboard(main);
            }
        }
        if (objective != null) {
            objective.unregister();
            objective = null;
        }
    }
}
