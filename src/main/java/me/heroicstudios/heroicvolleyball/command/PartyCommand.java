package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.party.Party;
import me.heroicstudios.heroicvolleyball.party.PartyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PartyCommand implements CommandExecutor, TabCompleter {

    private final PartyManager partyManager;

    public PartyCommand(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use party commands.", NamedTextColor.RED));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "partycreate" -> create(player);
            case "partyinvite" -> invite(player, args);
            case "partyleave" -> leave(player);
            case "partydisband" -> disband(player);
            default -> {
                return true;
            }
        }
        return true;
    }

    private void create(Player player) {
        if (partyManager.isInParty(player)) {
            player.sendMessage(Component.text("You are already in a party. Use /partyleave first.", NamedTextColor.RED));
            return;
        }
        Party party = partyManager.create(player);
        player.sendMessage(Component.text("Party created! Invite players with /partyinvite <player>.", NamedTextColor.GREEN));
        partyManager.sendMessage(party, Component.text(player.getName() + " created a party.", NamedTextColor.GRAY));
    }

    private void invite(Player player, String[] args) {
        Party party = partyManager.getParty(player);
        if (party == null) {
            player.sendMessage(Component.text("You are not in a party. Use /partycreate first.", NamedTextColor.RED));
            return;
        }
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(Component.text("Only the party leader can invite players.", NamedTextColor.RED));
            return;
        }
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /partyinvite <player>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(Component.text("You are already in the party.", NamedTextColor.RED));
            return;
        }
        if (partyManager.isInParty(target)) {
            player.sendMessage(Component.text(target.getName() + " is already in a party.", NamedTextColor.RED));
            return;
        }
        if (party.size() >= partyManager.getMaxSize()) {
            player.sendMessage(Component.text("Your party is full (" + partyManager.getMaxSize() + ").", NamedTextColor.RED));
            return;
        }

        partyManager.invite(player, target);
        partyManager.sendMessage(party, Component.text(target.getName() + " joined the party. (" + party.size() + "/" + partyManager.getMaxSize() + ")", NamedTextColor.GREEN));
    }

    private void leave(Player player) {
        Party party = partyManager.leave(player);
        if (party == null) {
            player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("You left the party.", NamedTextColor.YELLOW));
        partyManager.sendMessage(party, Component.text(player.getName() + " left the party.", NamedTextColor.GRAY));
    }

    private void disband(Player player) {
        Party party = partyManager.getParty(player);
        if (party == null) {
            player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
            return;
        }
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(Component.text("Only the party leader can disband the party.", NamedTextColor.RED));
            return;
        }
        partyManager.sendMessage(party, Component.text("The party has been disbanded.", NamedTextColor.YELLOW));
        partyManager.disband(player);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("partyinvite") && args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    names.add(online.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
