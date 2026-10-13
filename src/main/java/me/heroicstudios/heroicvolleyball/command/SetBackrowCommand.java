package me.heroicstudios.heroicvolleyball.command;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.backrow.BackrowManager;
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

    private final BackrowManager backrowManager;
    private final WandManager wandManager;
    private final Messages messages;

    public SetBackrowCommand(BackrowManager backrowManager, WandManager wandManager, Messages messages) {
        this.backrowManager = backrowManager;
        this.wandManager = wandManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "general.players-only");
            return true;
        }

        if (args.length != 1) {
            messages.send(player, "backrow.usage");
            messages.send(player, "arena.usage-hint");
            return true;
        }

        Team team = Team.fromString(args[0]);
        if (team == null) {
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

        backrowManager.setBackrow(team, first, second);

        messages.send(player, "backrow.set",
                "team", Messages.teamDisplay(team),
                "location", first.getBlockX() + ", " + first.getBlockY() + ", " + first.getBlockZ()
                        + " -> " + second.getBlockX() + ", " + second.getBlockY() + ", " + second.getBlockZ());
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