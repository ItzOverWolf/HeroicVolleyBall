package me.heroicstudios.heroicvolleyball.game;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.ball.BallManager;
import me.heroicstudios.heroicvolleyball.music.MusicManager;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import me.heroicstudios.heroicvolleyball.party.PartyManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import me.heroicstudios.heroicvolleyball.tool.ToolManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GameManager {

    private final HeroicVolleyBall plugin;
    private final ArenaManager arenaManager;
    private final PositionManager positionManager;
    private final ToolManager toolManager;
    private final PartyManager partyManager;
    private final NamespacedKey reachKey;

    private BallManager ballManager;
    private ScoreManager scoreManager;
    private MusicManager musicManager;

    private final Map<UUID, Team> teams = new HashMap<>();
    private final Map<UUID, Integer> positions = new HashMap<>();
    private final Map<UUID, Integer> soloPositions = new HashMap<>();
    private final Map<UUID, ItemStack[]> inventories = new HashMap<>();
    private final Map<UUID, Collection<PotionEffect>> effects = new HashMap<>();

    private boolean active;

    public GameManager(HeroicVolleyBall plugin, ArenaManager arenaManager, PositionManager positionManager,
                       ToolManager toolManager, PartyManager partyManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.positionManager = positionManager;
        this.toolManager = toolManager;
        this.partyManager = partyManager;
        this.reachKey = new NamespacedKey(plugin, "volley_reach");
    }

    public void bind(BallManager ballManager, ScoreManager scoreManager, MusicManager musicManager) {
        this.ballManager = ballManager;
        this.scoreManager = scoreManager;
        this.musicManager = musicManager;
        if (scoreManager != null) {
            scoreManager.setGameManager(this);
        }
    }

    public boolean isActive() {
        return active;
    }

    public int getPlayerCount() {
        return teams.size() + soloPositions.size();
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public @Nullable Team getTeam(Player player) {
        return teams.get(player.getUniqueId());
    }

    public @Nullable Team getTeam(UUID uuid) {
        return teams.get(uuid);
    }

    public @Nullable Integer getPosition(Player player) {
        return positions.get(player.getUniqueId());
    }

    public @Nullable Team resolveTeam(Player player) {
        Team team = teams.get(player.getUniqueId());
        return team != null ? team : arenaManager.getArenaAt(player.getLocation());
    }

    public List<Player> playersOf(Team team) {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : teams.keySet()) {
            if (teams.get(uuid) == team) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    public void start(Map<UUID, Team> assignedTeams, Map<UUID, Integer> assignedPositions) {
        end();

        teams.putAll(assignedTeams);
        positions.putAll(assignedPositions);
        active = true;

        for (UUID uuid : teams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            snapshot(player);
            toolManager.giveKit(player);
            applyGameEffects(player);
            applyReach(player);
        }

        if (scoreManager != null) {
            scoreManager.resetMatch();
        }
        Team serving = null;
        if (ballManager != null) {
            serving = ballManager.spawnAtStartTeam();
        }
        teleportToPositions(serving);
        if (musicManager != null) {
            musicManager.startDefault();
        }
    }

    public void startSolo(Player player, int position) {
        end();

        soloPositions.put(player.getUniqueId(), position);
        snapshot(player);
        toolManager.giveKit(player);
        applyGameEffects(player);
        applyReach(player);

        if (scoreManager != null) {
            scoreManager.resetMatch();
        }
        Team serving = null;
        if (ballManager != null) {
            serving = ballManager.spawnAtStartTeam();
        }
        teleportToPositions(serving);
        if (musicManager != null) {
            musicManager.startDefault();
        }
    }

    public boolean hasSoloPlayers() {
        return !soloPositions.isEmpty();
    }

    public void end() {
        for (UUID uuid : new ArrayList<>(inventories.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                restore(player);
            }
        }
        teams.clear();
        positions.clear();
        soloPositions.clear();
        inventories.clear();
        effects.clear();
        active = false;
    }

    private void applyGameEffects(Player player) {
        if (!plugin.getConfig().getBoolean("game.effects.enabled", true)) {
            return;
        }
        applyEffect(player, PotionEffectType.JUMP_BOOST, plugin.getConfig().getInt("game.effects.jump-boost-level", 4));
        applyEffect(player, PotionEffectType.SPEED, plugin.getConfig().getInt("game.effects.speed-level", 4));
    }

    private void applyReach(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (instance == null) {
            return;
        }
        instance.removeModifier(reachKey);
        double bonus = plugin.getConfig().getDouble("tools.reach-bonus", 2.0);
        if (bonus != 0.0) {
            instance.addTransientModifier(new AttributeModifier(reachKey, bonus, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private void removeReach(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (instance != null) {
            instance.removeModifier(reachKey);
        }
    }

    private void applyEffect(Player player, PotionEffectType type, int level) {
        if (level <= 0) {
            return;
        }
        player.addPotionEffect(new PotionEffect(type, PotionEffect.INFINITE_DURATION, level - 1, false, false, false));
    }

    private void snapshot(Player player) {
        UUID uuid = player.getUniqueId();
        inventories.put(uuid, player.getInventory().getContents().clone());
        effects.put(uuid, new ArrayList<>(player.getActivePotionEffects()));

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void restore(Player player) {
        ItemStack[] contents = inventories.get(player.getUniqueId());
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        if (contents != null) {
            player.getInventory().setContents(contents);
        }

        removeReach(player);

        Collection<PotionEffect> saved = effects.get(player.getUniqueId());
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        if (saved != null) {
            for (PotionEffect effect : saved) {
                player.addPotionEffect(effect);
            }
        }
    }

    public void teleportToPositions(@Nullable Team serving) {
        for (Map.Entry<UUID, Team> entry : teams.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            Integer position = positions.get(entry.getKey());
            if (position == null) {
                continue;
            }
            Location location = positionManager.getPosition(entry.getValue(), position);
            if (location != null) {
                player.teleport(location);
            }
        }

        if (serving != null) {
            for (Map.Entry<UUID, Integer> entry : soloPositions.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null) {
                    continue;
                }
                Location location = positionManager.getPosition(serving, entry.getValue());
                if (location != null) {
                    player.teleport(location);
                }
            }
        }
    }

    public void broadcast(Component message) {
        for (UUID uuid : teams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        end();
    }
}
