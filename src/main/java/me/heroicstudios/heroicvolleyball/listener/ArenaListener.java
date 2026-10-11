package me.heroicstudios.heroicvolleyball.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

public final class ArenaListener implements Listener {

    private final ArenaManager arenaManager;
    private final Map<UUID, Team> currentArenas = new HashMap<>();

    public ArenaListener(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
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

        Team current = arenaManager.getArenaAt(to);
        Team previous = currentArenas.get(uuid);

        if (current == previous) {
            return;
        }

        if (current == null) {
            currentArenas.remove(uuid);
            player.sendMessage(Component.text("You're out of the arena!", NamedTextColor.YELLOW));
        } else {
            currentArenas.put(uuid, current);
            player.sendMessage(Component.text("You're in the ", NamedTextColor.GRAY)
                    .append(Component.text(current.getDisplayName(), current.getColor()))
                    .append(Component.text(" team's arena", NamedTextColor.GRAY)));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        currentArenas.remove(event.getPlayer().getUniqueId());
    }
}
