package me.heroicstudios.heroicvolleyball.command;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.spawn.BallSpawnManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SetBallSpawnCommand implements CommandExecutor, TabCompleter {

    private final BallSpawnManager ballSpawnManager;
    private final Messages messages;

    public SetBallSpawnCommand(BallSpawnManager ballSpawnManager, Messages messages) {
        this.ballSpawnManager = ballSpawnManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "general.players-only");
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            messages.send(player, "ballspawn.usage");
            return true;
        }

        Team team = Team.fromString(args[0]);
        if (team == null) {
            messages.send(player, "ballspawn.unknown-team", "input", args[0]);
            return true;
        }

        if (args.length == 2 && args[1].equalsIgnoreCase("clear")) {
            ballSpawnManager.clear(team);
            messages.send(player, "ballspawn.cleared", "team", Messages.teamDisplay(team));
            return true;
        }
        if (args.length == 2) {
            messages.send(player, "ballspawn.usage");
            return true;
        }

        ballSpawnManager.set(team, player.getLocation());
        messages.send(player, "ballspawn.set",
                "team", Messages.teamDisplay(team),
                "location", player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY()
                        + ", " + player.getLocation().getBlockZ());
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
        if (args.length == 2 && "clear".startsWith(args[1].toLowerCase())) {
            return List.of("clear");
        }
        return List.of();
    }
}