package me.heroicstudios.heroicvolleyball.command;

import java.util.ArrayList;
import java.util.List;

import me.heroicstudios.heroicvolleyball.tool.ToolManager;
import me.heroicstudios.heroicvolleyball.tool.VolleyTier;
import me.heroicstudios.heroicvolleyball.tool.VolleyTool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VolleyGiveCommand implements CommandExecutor, TabCompleter {

    private final ToolManager toolManager;

    public VolleyGiveCommand(ToolManager toolManager) {
        this.toolManager = toolManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /" + label + " <shovel|spear|shield> [tier]", NamedTextColor.RED));
            return true;
        }

        VolleyTool tool = VolleyTool.fromString(args[0]);
        if (tool == null) {
            player.sendMessage(Component.text("Unknown tool \"" + args[0] + "\". Use shovel, spear or shield.", NamedTextColor.RED));
            return true;
        }

        VolleyTier tier = VolleyTier.WOODEN;
        if (args.length >= 2) {
            VolleyTier parsed = VolleyTier.fromString(args[1]);
            if (parsed == null) {
                player.sendMessage(Component.text("Unknown tier \"" + args[1] + "\". Use wooden, iron or diamond.", NamedTextColor.RED));
                return true;
            }
            tier = parsed;
        }

        player.getInventory().addItem(toolManager.create(tool, tier));
        player.sendMessage(Component.text("You received the ", NamedTextColor.GREEN)
                .append(Component.text(tier.getDisplayName() + " " + tool.name().toLowerCase(), tier.getColor()))
                .append(Component.text(".", NamedTextColor.GREEN)));
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
