package me.heroicstudios.heroicvolleyball.command;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Side;
import me.heroicstudios.heroicvolleyball.message.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SetBallSpawnCommand implements CommandExecutor, TabCompleter {

    private final ArenaManager arenaManager;
    private final Messages messages;

    public SetBallSpawnCommand(ArenaManager arenaManager, Messages messages) {
        this.arenaManager = arenaManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "general.players-only");
            return true;
        }

        if (args.length < 1 || args.length > 3) {
            messages.send(player, "ballspawn.usage");
            return true;
        }

        Side side = Side.fromString(args[0]);
        if (side == null) {
            messages.send(player, "ballspawn.unknown-team", "input", args[0]);
            return true;
        }

        boolean clear = args.length == 2 && args[1].equalsIgnoreCase("clear");
        if (args.length == 2 && !clear) {
            messages.send(player, "ballspawn.usage");
            return true;
        }

        Arena arena = resolveArena(player, args.length == 3 ? args[2] : null);
        if (arena == null) {
            messages.send(player, "arena.none");
            return true;
        }

        if (clear) {
            arena.setBallSpawn(side, null);
            arenaManager.save(arena);
            messages.send(player, "ballspawn.cleared",
                    "team", Messages.teamDisplay(arena, side), "arena", arena.getName());
            return true;
        }

        arena.setBallSpawn(side, player.getLocation());
        arenaManager.save(arena);
        messages.send(player, "ballspawn.set",
                "team", Messages.teamDisplay(arena, side),
                "arena", arena.getName(),
                "location", player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY()
                        + ", " + player.getLocation().getBlockZ());
        return true;
    }

    private @Nullable Arena resolveArena(Player player, @Nullable String name) {
        if (name != null && !name.isBlank()) {
            return arenaManager.createIfMissing(name);
        }
        Arena arena = arenaManager.arenaAt(player.getLocation());
        return arena != null ? arena : arenaManager.getDefault();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Side side : Side.values()) {
                String name = side.getId();
                if (name.startsWith(prefix)) {
                    options.add(name);
                }
            }
            return options;
        }
        if (args.length == 2) {
            List<String> options = new ArrayList<>();
            if ("clear".startsWith(args[1].toLowerCase())) {
                options.add("clear");
            }
            options.addAll(Filter.names(arenaManager.names(), args[1]));
            return options;
        }
        if (args.length == 3) {
            return Filter.names(arenaManager.names(), args[2]);
        }
        return List.of();
    }
}
