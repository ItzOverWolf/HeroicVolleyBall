package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.wand.WandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class VolleyWandCommand implements CommandExecutor {

    private final WandManager wandManager;
    private final Messages messages;

    public VolleyWandCommand(WandManager wandManager, Messages messages) {
        this.wandManager = wandManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "general.players-only");
            return true;
        }

        player.getInventory().addItem(wandManager.createWand());
        messages.send(player, "wand.received");
        return true;
    }
}
