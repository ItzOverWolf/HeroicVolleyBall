package me.heroicstudios.heroicvolleyball.command;

import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.game.GameSession;
import me.heroicstudios.heroicvolleyball.game.SessionManager;
import me.heroicstudios.heroicvolleyball.message.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RestartGameCommand implements CommandExecutor, TabCompleter {

    private final SessionManager sessionManager;
    private final ArenaManager arenaManager;
    private final Messages messages;

    public RestartGameCommand(SessionManager sessionManager, ArenaManager arenaManager, Messages messages) {
        this.sessionManager = sessionManager;
        this.arenaManager = arenaManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Arena arena = resolve(sender, args.length >= 1 ? args[0] : null);
        if (arena == null) {
            messages.send(sender, "arena.none");
            return true;
        }
        if (!arena.isEnabled()) {
            messages.send(sender, "game.arena-disabled", "arena", arena.getName());
            return true;
        }
        GameSession session = sessionManager.get(arena);
        if (session == null) {
            messages.send(sender, "game.not-running", "arena", arena.getName());
            return true;
        }
        if (!session.restart()) {
            messages.send(sender, "game.console-need-position");
            return true;
        }
        messages.send(sender, "game.restarted", "arena", arena.getName());
        return true;
    }

    private @Nullable Arena resolve(CommandSender sender, @Nullable String name) {
        if (name != null && !name.isBlank()) {
            return arenaManager.get(name);
        }
        if (sender instanceof Player player) {
            Arena arena = arenaManager.arenaAt(player.getLocation());
            if (arena != null) {
                return arena;
            }
        }
        return arenaManager.getDefault();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Filter.names(arenaManager.names(), args[0]);
        }
        return List.of();
    }
}
