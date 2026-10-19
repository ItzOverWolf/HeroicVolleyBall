package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.game.GameSession;
import me.heroicstudios.heroicvolleyball.game.SessionManager;
import me.heroicstudios.heroicvolleyball.message.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpawnBallCommand implements CommandExecutor {

    private final SessionManager sessionManager;
    private final ArenaManager arenaManager;
    private final Messages messages;

    public SpawnBallCommand(SessionManager sessionManager, ArenaManager arenaManager, Messages messages) {
        this.sessionManager = sessionManager;
        this.arenaManager = arenaManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "general.players-only");
            return true;
        }

        Arena arena = resolve(player, args.length >= 1 ? args[0] : null);
        if (arena == null) {
            messages.send(player, "arena.none");
            return true;
        }

        if (!arena.isEnabled()) {
            messages.send(player, "game.arena-disabled", "arena", arena.getName());
            return true;
        }

        if (sessionManager.hasLiveGame(arena)) {
            messages.send(player, "game.already-running", "arena", arena.getName());
            return true;
        }

        GameSession session = sessionManager.getOrCreate(arena);
        session.getBall().spawnAtPlayer(player);
        messages.send(player, "ball.spawned", "arena", arena.getName());
        return true;
    }

    private @Nullable Arena resolve(Player player, @Nullable String name) {
        if (name != null && !name.isBlank()) {
            return arenaManager.get(name);
        }
        Arena arena = arenaManager.arenaAt(player.getLocation());
        return arena != null ? arena : arenaManager.getDefault();
    }
}
