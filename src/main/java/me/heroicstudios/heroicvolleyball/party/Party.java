package me.heroicstudios.heroicvolleyball.party;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Party {

    private final UUID leader;
    private final LinkedHashSet<UUID> members = new LinkedHashSet<>();

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }

    public int size() {
        return members.size();
    }

    public void add(UUID uuid) {
        members.add(uuid);
    }

    public void remove(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }
}
