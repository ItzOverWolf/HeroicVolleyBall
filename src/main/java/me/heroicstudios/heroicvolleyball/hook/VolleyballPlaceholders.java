package me.heroicstudios.heroicvolleyball.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.Side;
import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.game.GameSession;
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
        GameSession session = resolve(player);
        ScoreManager score = session != null ? session.getScore() : null;
        BallManager ball = session != null ? session.getBall() : null;

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
                return String.valueOf(isAnyActive());
            case "players":
                return String.valueOf(totalPlayers());
            case "sessions":
                return String.valueOf(plugin.getSessionManager().sessions().size());
            case "arena":
                return session != null ? session.getArena().getName() : "None";
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
                return teamOf(player, session);
            case "position":
                return positionOf(player, session);
            default:
                return null;
        }
    }

    private @Nullable GameSession resolve(OfflinePlayer player) {
        if (player instanceof Player online) {
            GameSession session = plugin.getSessionManager().get(online);
            if (session != null) {
                return session;
            }
        }
        Arena arena = plugin.getArenaManager().getDefault();
        if (arena != null) {
            GameSession session = plugin.getSessionManager().get(arena);
            if (session != null) {
                return session;
            }
        }
        for (GameSession candidate : plugin.getSessionManager().sessions()) {
            if (candidate.isActive()) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isAnyActive() {
        for (GameSession session : plugin.getSessionManager().sessions()) {
            if (session.isActive()) {
                return true;
            }
        }
        return false;
    }

    private int totalPlayers() {
        int total = 0;
        for (GameSession session : plugin.getSessionManager().sessions()) {
            total += session.getPlayerCount();
        }
        return total;
    }

    private String teamOf(OfflinePlayer player, @Nullable GameSession session) {
        if (session == null || !(player instanceof Player online)) {
            return "None";
        }
        Side side = session.getSide(online);
        return side != null ? side.getDisplayName() : "None";
    }

    private String positionOf(OfflinePlayer player, @Nullable GameSession session) {
        if (session == null || !(player instanceof Player online)) {
            return "0";
        }
        Integer position = session.getPosition(online);
        return position != null ? String.valueOf(position) : "0";
    }
}
