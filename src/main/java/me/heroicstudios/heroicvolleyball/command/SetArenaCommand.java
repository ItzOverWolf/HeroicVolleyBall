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

public final class SetArenaCommand implements CommandExecutor, TabCompleter {

    private final ArenaManager arenaManager;
    private final WandManager wandManager;
    private final Messages messages;

    public SetArenaCommand(ArenaManager arenaManager, WandManager wandManager, Messages messages) {
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

        if (args.length < 1 || args.length > 3) {
            messages.send(player, "arena.usage");
            messages.send(player, "arena.usage-hint");
            return true;
        }

        Side side = Side.fromString(args[0]);
        if (side == null) {
            messages.send(player, "arena.unknown-team", "input", args[0]);
            return true;
        }

        float yaw;
        if (args.length >= 2) {
            Float resolved = yawFor(args[1]);
            if (resolved == null) {
                messages.send(player, "arena.unknown-direction", "input", args[1]);
                return true;
            }
            yaw = resolved;
        } else {
            yaw = player.getLocation().getYaw();
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

        Arena arena = resolveArena(player, args.length == 3 ? args[2] : null);
        if (arena == null) {
            messages.send(player, "arena.none");
            return true;
        }

        arena.setCourt(side, Region.of(first, second));
        arena.setFacing(side, yaw);
        arenaManager.save(arena);

        messages.send(player, "arena.set",
                "team", Messages.teamDisplay(side),
                "arena", arena.getName(),
                "facing", describeYaw(yaw));

        if (!arena.isComplete()) {
            messages.send(player, "arena.still-incomplete", "arena", arena.getName());
        } else {
            messages.send(player, "arena.complete", "arena", arena.getName());
        }
        return true;
    }

    private @Nullable Arena resolveArena(Player player, @Nullable String name) {
        if (name != null && !name.isBlank()) {
            return arenaManager.createIfMissing(name);
        }
        Arena arena = arenaManager.arenaAt(player.getLocation());
        return arena != null ? arena : arenaManager.getDefault();
    }

    private @Nullable Float yawFor(String input) {
        return switch (input.toLowerCase()) {
            case "s", "south" -> 0f;
            case "w", "west" -> 90f;
            case "n", "north" -> 180f;
            case "e", "east" -> -90f;
            case "sw", "southwest", "south-west" -> 45f;
            case "nw", "northwest", "north-west" -> 135f;
            case "ne", "northeast", "north-east" -> -135f;
            case "se", "southeast", "south-east" -> -45f;
            default -> null;
        };
    }

    private String describeYaw(float yaw) {
        float normalized = ((yaw % 360f) + 360f) % 360f;
        int index = Math.round(normalized / 45f) % 8;
        return switch (index) {
            case 0 -> "south";
            case 1 -> "south-west";
            case 2 -> "west";
            case 3 -> "north-west";
            case 4 -> "north";
            case 5 -> "north-east";
            case 6 -> "east";
            default -> "south-east";
        };
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
            String prefix = args[1].toLowerCase();
            for (String direction : List.of("north", "south", "east", "west",
                    "north-east", "north-west", "south-east", "south-west")) {
                if (direction.startsWith(prefix)) {
                    options.add(direction);
                }
            }
            return options;
        }
        if (args.length == 3) {
            return Filter.names(arenaManager.names(), args[2]);
        }
        return List.of();
    }
}
