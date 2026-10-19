package me.heroicstudios.heroicvolleyball.command;

import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.game.SessionManager;
import me.heroicstudios.heroicvolleyball.game.SetupGui;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.party.Party;
import me.heroicstudios.heroicvolleyball.party.PartyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StartGameCommand implements CommandExecutor, TabCompleter {

    private final PartyManager partyManager;
    private final SetupGui setupGui;
    private final SessionManager sessionManager;
    private final ArenaManager arenaManager;
    private final Messages messages;

    public StartGameCommand(PartyManager partyManager, SetupGui setupGui, SessionManager sessionManager,
                            ArenaManager arenaManager, Messages messages) {
        this.partyManager = partyManager;
        this.setupGui = setupGui;
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

        if (sender instanceof Player player) {
            if (!arena.isComplete()) {
                messages.send(player, "game.setup-missing");
                messages.send(player, "arena.still-incomplete", "arena", arena.getName());
                return true;
            }
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

        if (!arena.isComplete()) {
            messages.send(sender, "game.setup-missing");
            messages.send(sender, "arena.still-incomplete", "arena", arena.getName());
            return true;
        }

        if (!sessionManager.startConsole(arena)) {
            messages.send(sender, "game.console-need-position");
            return true;
        }
        messages.send(sender, "game.console-start", "arena", arena.getName());
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
