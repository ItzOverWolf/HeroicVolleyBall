package me.heroicstudios.heroicvolleyball.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.game.GameManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VolleyballPlaceholders extends PlaceholderExpansion {

    private final HeroicVolleyBall plugin;

    public VolleyballPlaceholders(HeroicVolleyBall plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hvb";
    }

    @Override
    public @NotNull String getAuthor() {
        return "HeroicStudios";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        ScoreManager score = plugin.getScoreManager();
        GameManager game = plugin.getGameManager();
        BallManager ball = plugin.getBallManager();

        switch (params.toLowerCase()) {
            case "red_points", "red_score":
                return score != null ? String.valueOf(score.getRedPoints()) : "0";
            case "blue_points", "blue_score":
                return score != null ? String.valueOf(score.getBluePoints()) : "0";
            case "red_sets":
                return score != null ? String.valueOf(score.getRedSets()) : "0";
            case "blue_sets":
                return score != null ? String.valueOf(score.getBlueSets()) : "0";
            case "score":
                return score != null ? score.getRedPoints() + " - " + score.getBluePoints() : "0 - 0";
            case "sets":
                return score != null ? score.getRedSets() + " - " + score.getBlueSets() : "0 - 0";
            case "match_over":
                return score != null ? String.valueOf(score.isMatchOver()) : "false";
            case "active":
                return game != null ? String.valueOf(game.isActive()) : "false";
            case "players":
                return game != null ? String.valueOf(game.getPlayerCount()) : "0";
            case "touches":
                return ball != null ? String.valueOf(ball.getTouches()) : "0";
            case "max_touches":
                return ball != null ? String.valueOf(ball.getMaxTouches()) : "0";
            case "state":
                return ball != null ? ball.getStateName() : "Idle";
            case "possession":
                return ball != null && ball.getPossession() != null
                        ? ball.getPossession().getDisplayName() : "None";
            case "team":
                return teamOf(player, game);
            case "position":
                return positionOf(player, game);
            default:
                return null;
        }
    }

    private String teamOf(OfflinePlayer player, GameManager game) {
        Team team = teamOf(game, player);
        return team != null ? team.getDisplayName() : "None";
    }

    private String positionOf(OfflinePlayer player, GameManager game) {
        if (game == null || !(player instanceof Player online)) {
            return "0";
        }
        Integer position = game.getPosition(online);
        return position != null ? String.valueOf(position) : "0";
    }

    private @Nullable Team teamOf(GameManager game, OfflinePlayer player) {
        if (game == null || !(player instanceof Player online)) {
            return null;
        }
        return game.getTeam(online);
    }
}
