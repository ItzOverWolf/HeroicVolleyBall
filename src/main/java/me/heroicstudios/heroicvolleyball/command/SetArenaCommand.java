package me.heroicstudios.heroicvolleyball.command;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Team;
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

    public SetArenaCommand(ArenaManager arenaManager, WandManager wandManager) {
        this.arenaManager = arenaManager;
        this.wandManager = wandManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /" + label + " <red|blue>", NamedTextColor.RED));
            return true;
        }

        Team team = Team.fromString(args[0]);
        if (team == null) {
            player.sendMessage(Component.text("Unknown team \"" + args[0] + "\". Use red or blue.", NamedTextColor.RED));
            return true;
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

        player.sendMessage(Component.text("The ", NamedTextColor.GRAY)
                .append(Component.text(team.getDisplayName(), team.getColor()))
                .append(Component.text(" team's arena has been set!", NamedTextColor.GRAY)));
        return true;
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
        return List.of();
    }
}
