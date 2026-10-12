package me.heroicstudios.heroicvolleyball.command;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.tool.ToolManager;
import me.heroicstudios.heroicvolleyball.tool.VolleyTier;
import me.heroicstudios.heroicvolleyball.tool.VolleyTool;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VolleyGiveCommand implements CommandExecutor, TabCompleter {

    private final ToolManager toolManager;
    private final Messages messages;

    public VolleyGiveCommand(ToolManager toolManager, Messages messages) {
        this.toolManager = toolManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "general.players-only");
            return true;
        }

        if (args.length < 1) {
            messages.send(player, "give.usage");
            return true;
        }

        VolleyTool tool = VolleyTool.fromString(args[0]);
        if (tool == null) {
            messages.send(player, "give.unknown-tool", "input", args[0]);
            return true;
        }

        VolleyTier tier = VolleyTier.WOODEN;
        if (args.length >= 2) {
            VolleyTier parsed = VolleyTier.fromString(args[1]);
            if (parsed == null) {
                messages.send(player, "give.unknown-tier", "input", args[1]);
                return true;
            }
            tier = parsed;
        }

        player.getInventory().addItem(toolManager.create(tool, tier));
        messages.send(player, "give.received", "tool", tier.getDisplayName() + " " + tool.name().toLowerCase());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(VolleyTool.values(), args[0]);
        }
        if (args.length == 2) {
            return filter(VolleyTier.values(), args[1]);
        }
        return List.of();
    }

    private List<String> filter(Enum<?>[] values, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> options = new ArrayList<>();
        for (Enum<?> value : values) {
            String name = value.name().toLowerCase();
            if (name.startsWith(lower)) {
                options.add(name);
            }
        }
        return options;
    }
}
