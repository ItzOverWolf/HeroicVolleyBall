package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.wand.WandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class VolleyWandCommand implements CommandExecutor {

    private final WandManager wandManager;

    public VolleyWandCommand(WandManager wandManager) {
        this.wandManager = wandManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        player.getInventory().addItem(wandManager.createWand());
        player.sendMessage(Component.text("You received the Volleyball Wand.", NamedTextColor.GREEN)
                .append(Component.text(" Left-click for point 1, right-click for point 2.", NamedTextColor.GRAY)));
        return true;
    }
}
