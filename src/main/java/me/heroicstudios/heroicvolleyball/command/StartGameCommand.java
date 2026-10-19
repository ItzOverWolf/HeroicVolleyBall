package me.heroicstudios.heroicvolleyball.command;

import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.game.ArenaPickerGui;
import me.heroicstudios.heroicvolleyball.game.SessionManager;
import me.heroicstudios.heroicvolleyball.game.SetupGui;
import me.heroicstudios.heroicvolleyball.message.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StartGameCommand implements CommandExecutor, TabCompleter {

    private final SetupGui setupGui;
    private final ArenaPickerGui pickerGui;
    private final SessionManager sessionManager;
    private final ArenaManager arenaManager;
    private final Messages messages;

    public StartGameCommand(SetupGui setupGui, ArenaPickerGui pickerGui, SessionManager sessionManager,
                            ArenaManager arenaManager, Messages messages) {
        this.setupGui = setupGui;
        this.pickerGui = pickerGui;
        this.sessionManager = sessionManager;
        this.arenaManager = arenaManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && !args[0].isBlank()) {
            Arena arena = arenaManager.get(args[0]);
            if (arena == null) {
                messages.send(sender, "arena.not-found", "arena", args[0]);
                return true;
            }
            start(sender, arena);
            return true;
        }

        List<Arena> free = sessionManager.freeArenas();
        if (free.isEmpty()) {
            if (sessionManager.atCapacity()) {
                messages.send(sender, "session.capacity");
            } else {
                messages.send(sender, "session.no-free");
            }
            return true;
        }
        if (free.size() > 1 && sender instanceof Player player) {
            pickerGui.open(player);
            return true;
        }
        start(sender, free.get(0));
        return true;
    }

    private void start(CommandSender sender, Arena arena) {
        if (!arena.isEnabled()) {
            messages.send(sender, "game.arena-disabled", "arena", arena.getName());
            return;
        }
        if (sessionManager.isOccupied(arena)) {
            messages.send(sender, "game.already-running", "arena", arena.getName());
            return;
        }
        if (!arena.isComplete()) {
            messages.send(sender, "game.setup-missing");
            messages.send(sender, "arena.still-incomplete", "arena", arena.getName());
            return;
        }
        if (sessionManager.atCapacity()) {
            messages.send(sender, "session.capacity");
            return;
        }

        if (sender instanceof Player player) {
            if (sessionManager.get(player) != null) {
                messages.send(player, "game.already-in-game");
                return;
            }
            setupGui.openSolo(player, arena);
            messages.send(sender, "game.solo-choose");
            return;
        }

        if (!sessionManager.startConsole(arena)) {
            messages.send(sender, "game.console-need-position");
            return;
        }
        messages.send(sender, "game.console-start", "arena", arena.getName());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Filter.names(arenaManager.names(), args[0]);
        }
        return List.of();
    }
}
