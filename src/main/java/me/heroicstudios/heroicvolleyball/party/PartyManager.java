package me.heroicstudios.heroicvolleyball.party;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PartyManager {

    private final HeroicVolleyBall plugin;
    private final Map<UUID, Party> partyByMember = new HashMap<>();
    private final Set<Party> parties = new HashSet<>();

    public PartyManager(HeroicVolleyBall plugin) {
        this.plugin = plugin;
    }

    public int getMaxSize() {
        return Math.max(2, plugin.getConfig().getInt("party.max-size", 12));
    }

    public @Nullable Party getParty(Player player) {
        return partyByMember.get(player.getUniqueId());
    }

    public @Nullable Party getParty(UUID uuid) {
        return partyByMember.get(uuid);
    }

    public boolean isInParty(Player player) {
        return partyByMember.containsKey(player.getUniqueId());
    }

    public Party create(Player leader) {
        leave(leader);
        Party party = new Party(leader.getUniqueId());
        parties.add(party);
        partyByMember.put(leader.getUniqueId(), party);
        return party;
    }

    public boolean invite(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            return false;
        }
        if (party.isMember(target.getUniqueId())) {
            return false;
        }
        if (party.size() >= getMaxSize()) {
            return false;
        }
        party.add(target.getUniqueId());
        partyByMember.put(target.getUniqueId(), party);
        return true;
    }

    public @Nullable Party leave(Player player) {
        Party party = partyByMember.remove(player.getUniqueId());
        if (party == null) {
            return null;
        }
        party.remove(player.getUniqueId());
        if (party.isEmpty()) {
            parties.remove(party);
            return null;
        }
        return party;
    }

    public void disband(Player leader) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            return;
        }
        for (UUID uuid : party.getMembers()) {
            partyByMember.remove(uuid);
        }
        parties.remove(party);
    }

    public void sendMessage(Party party, net.kyori.adventure.text.Component message) {
        for (UUID uuid : party.getMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }
}
