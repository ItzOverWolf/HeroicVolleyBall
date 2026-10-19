package me.heroicstudios.heroicvolleyball.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.ArenaSide;
import me.heroicstudios.heroicvolleyball.arena.Side;
import me.heroicstudios.heroicvolleyball.message.Messages;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

public final class ArenaListener implements Listener {

    private final ArenaManager arenaManager;
    private final Messages messages;
    private final Map<UUID, ArenaSide> currentArenas = new HashMap<>();

    public ArenaListener(ArenaManager arenaManager, Messages messages) {
        this.arenaManager = arenaManager;
        this.messages = messages;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        ArenaSide current = locate(to);
        ArenaSide previous = currentArenas.get(uuid);

        if (same(current, previous)) {
            return;
        }

        if (current == null) {
            currentArenas.remove(uuid);
            messages.actionBar(player, "arena.leave");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        } else {
            currentArenas.put(uuid, current);
            messages.actionBar(player, "arena.enter",
                    "team", Messages.teamDisplay(current.arena(), current.side()),
                    "arena", current.arena().getName());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
        }
    }

    private @Nullable ArenaSide locate(Location location) {
        ArenaSide court = arenaManager.courtAt(location);
        if (court != null) {
            return court;
        }
        Arena arena = arenaManager.arenaAt(location);
        if (arena == null) {
            return null;
        }
        Side side = arena.getBackrowSide(location);
        return side != null ? new ArenaSide(arena, side) : null;
    }

    private boolean same(@Nullable ArenaSide first, @Nullable ArenaSide second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.arena().getName().equals(second.arena().getName()) && first.side() == second.side();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        currentArenas.remove(event.getPlayer().getUniqueId());
    }
}
