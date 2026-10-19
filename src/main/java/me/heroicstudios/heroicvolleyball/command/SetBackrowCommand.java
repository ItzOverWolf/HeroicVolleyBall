package me.heroicstudios.heroicvolleyball.command;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Region;
import me.heroicstudios.heroicvolleyball.arena.Side;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.wand.WandManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SetBackrowCommand implements CommandExecutor, TabCompleter {

    private final ArenaManager arenaManager;
    private final WandManager wandManager;
    private final Messages messages;

    public SetBackrowCommand(ArenaManager arenaManager, WandManager wandManager, Messages messages) {
        this.arenaManager = arenaManager;
        this.wandManager = wandManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "general.players-only");
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            messages.send(player, "backrow.usage");
            messages.send(player, "arena.usage-hint");
            return true;
        }

        Side side = Side.fromString(args[0]);
        if (side == null) {
            messages.send(player, "backrow.unknown-team", "input", args[0]);
            return true;
        }

        Location first = wandManager.getFirstPoint(player.getUniqueId());
        Location second = wandManager.getSecondPoint(player.getUniqueId());

        if (first == null || second == null) {
            messages.send(player, "arena.need-points");
            return true;
        }

        if (first.getWorld() == null || !first.getWorld().equals(second.getWorld())) {
            messages.send(player, "arena.world-mismatch");
            return true;
        }

        Arena arena = resolveArena(player, args.length == 2 ? args[1] : null);
        if (arena == null) {
            messages.send(player, "arena.none");
            return true;
        }

        arena.setBackrow(side, Region.of(first, second));
        arenaManager.save(arena);

        messages.send(player, "backrow.set",
                "team", Messages.teamDisplay(side),
                "arena", arena.getName(),
                "location", first.getBlockX() + ", " + first.getBlockY() + ", " + first.getBlockZ()
                        + " -> " + second.getBlockX() + ", " + second.getBlockY() + ", " + second.getBlockZ());
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
            return Filter.names(arenaManager.names(), args[1]);
        }
        return List.of();
    }
}
