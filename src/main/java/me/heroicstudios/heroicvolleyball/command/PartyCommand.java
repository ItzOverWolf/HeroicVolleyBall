package me.heroicstudios.heroicvolleyball.command;

import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.party.Party;
import me.heroicstudios.heroicvolleyball.party.PartyManager;
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
    private final Messages messages;

    public PartyCommand(PartyManager partyManager, Messages messages) {
        this.partyManager = partyManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "party.players-only");
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
            messages.send(player, "party.already-in");
            return;
        }
        Party party = partyManager.create(player);
        messages.send(player, "party.created");
        partyManager.sendMessage(party, messages.get("party.created-broadcast", "player", player.getName()));
    }

    private void invite(Player player, String[] args) {
        Party party = partyManager.getParty(player);
        if (party == null) {
            messages.send(player, "party.not-in");
            return;
        }
        if (!party.isLeader(player.getUniqueId())) {
            messages.send(player, "party.leader-only-invite");
            return;
        }
        if (args.length < 1) {
            messages.send(player, "party.invite-usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(player, "party.player-not-found");
            return;
        }
        if (target.equals(player)) {
            messages.send(player, "party.self-invite");
            return;
        }
        if (partyManager.isInParty(target)) {
            messages.send(player, "party.target-in-party", "player", target.getName());
            return;
        }
        if (party.size() >= partyManager.getMaxSize()) {
            messages.send(player, "party.full", "max", String.valueOf(partyManager.getMaxSize()));
            return;
        }

        partyManager.invite(player, target);
        partyManager.sendMessage(party, messages.get("party.joined-broadcast",
                "player", target.getName(),
                "size", String.valueOf(party.size()),
                "max", String.valueOf(partyManager.getMaxSize())));
    }

    private void leave(Player player) {
        Party party = partyManager.leave(player);
        if (party == null) {
            messages.send(player, "party.not-in");
            return;
        }
        messages.send(player, "party.left");
        partyManager.sendMessage(party, messages.get("party.left-broadcast", "player", player.getName()));
    }

    private void disband(Player player) {
        Party party = partyManager.getParty(player);
        if (party == null) {
            messages.send(player, "party.not-in");
            return;
        }
        if (!party.isLeader(player.getUniqueId())) {
            messages.send(player, "party.not-leader-disband");
            return;
        }
        partyManager.sendMessage(party, messages.get("party.disbanded"));
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
