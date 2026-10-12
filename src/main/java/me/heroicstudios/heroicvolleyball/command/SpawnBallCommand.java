package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.ball.BallManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SpawnBallCommand implements CommandExecutor {

    private final BallManager ballManager;

    public SpawnBallCommand(BallManager ballManager) {
        this.ballManager = ballManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        ballManager.spawnAtPlayer(player);
        player.sendMessage(Component.text("The volleyball has been spawned at your location.", NamedTextColor.GREEN));
        return true;
    }
}
