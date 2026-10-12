package me.heroicstudios.heroicvolleyball.command;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import me.heroicstudios.heroicvolleyball.wand.WandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final PositionManager positionManager;

    public SetArenaCommand(ArenaManager arenaManager, WandManager wandManager, PositionManager positionManager) {
        this.arenaManager = arenaManager;
        this.wandManager = wandManager;
        this.positionManager = positionManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            player.sendMessage(Component.text("Usage: /" + label + " <red|blue> [direction]", NamedTextColor.RED));
            player.sendMessage(Component.text("Direction: north, south, east, west (or n/s/e/w) plus diagonals. "
                    + "Omit it to face where you are looking now.", NamedTextColor.GRAY));
            return true;
        }

        Team team = Team.fromString(args[0]);
        if (team == null) {
            player.sendMessage(Component.text("Unknown team \"" + args[0] + "\". Use red or blue.", NamedTextColor.RED));
            return true;
        }

        float yaw;
        if (args.length == 2) {
            Float resolved = yawFor(args[1]);
            if (resolved == null) {
                player.sendMessage(Component.text("Unknown direction \"" + args[1] + "\". "
                        + "Use north, south, east, west or a diagonal.", NamedTextColor.RED));
                return true;
            }
            yaw = resolved;
        } else {
            yaw = player.getLocation().getYaw();
        }

        Location first = wandManager.getFirstPoint(player.getUniqueId());
        Location second = wandManager.getSecondPoint(player.getUniqueId());

        if (first == null || second == null) {
            player.sendMessage(Component.text("You must select two points with the Volleyball Wand (/volleywand) first.", NamedTextColor.RED));
            return true;
        }

        if (first.getWorld() == null || !first.getWorld().equals(second.getWorld())) {
            player.sendMessage(Component.text("Both points must be in the same world.", NamedTextColor.RED));
            return true;
        }

        Arena arena = new Arena(team, first.getWorld(), first, second);
        arenaManager.setArena(team, arena);
        positionManager.saveFacing(team, yaw);

        player.sendMessage(Component.text("The ", NamedTextColor.GRAY)
                .append(Component.text(team.getDisplayName(), team.getColor()))
                .append(Component.text(" team's arena has been set, facing " + describeYaw(yaw) + "!", NamedTextColor.GRAY)));
        return true;
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
            for (Team team : Team.values()) {
                String name = team.name().toLowerCase();
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
        return List.of();
    }
}
