package me.heroicstudios.heroicvolleyball.ball;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.game.Countdown;
import me.heroicstudios.heroicvolleyball.game.GameManager;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import me.heroicstudios.heroicvolleyball.tool.ToolManager;
import me.heroicstudios.heroicvolleyball.tool.VolleyTier;
import me.heroicstudios.heroicvolleyball.tool.VolleyTool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.SulfurCube;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public final class BallManager {

    private static final double COLLISION_STEP = 0.1;

    private enum State {
        IDLE,
        RALLY
    }

    private final HeroicVolleyBall plugin;
    private final ArenaManager arenaManager;
    private final ScoreManager scoreManager;
    private final GameManager gameManager;
    private final ToolManager toolManager;
    private final NamespacedKey ballKey;

    private @Nullable SulfurCube ball;
    private @Nullable ItemDisplay ballDisplay;
    private @Nullable Location current;
    private @Nullable BukkitTask task;
    private Vector motion = new Vector();

    private int size = 2;
    private int maxTouches = 3;
    private double gravity = 0.045;
    private double airDrag = 0.99;
    private double velocityScale = 1.0;
    private double maxVelocity = 3.0;
    private double spikeTopDown = 1.5;
    private double spikeStraight = 0.2;
    private double spikeUp = 0.7;
    private double spikeTopHorizontal = 0.25;
    private double spikeVerticalThreshold = 1.0;
    private double shieldDeflect = 0.9;
    private double shieldForward = 0.6;
    private double shieldCatchPower = 0.8;
    private double shieldCatchReach = 2.5;
    private double shieldCatchHeight = 3.0;
    private int shieldCooldown = 100;
    private int shieldCooldownTicks;
    private final Map<UUID, ItemStack> brokenShields = new HashMap<>();
    private double bounceFactor = 0.3;
    private double settleThreshold = 0.12;
    private int serveResetDelay = 10;
    private int pointCountdown = 3;
    private boolean touchPenalty = true;
    private boolean showName = true;
    private boolean pickupEnabled = true;
    private boolean sulfurCube = true;
    private Material ballMaterial = Material.GRAY_WOOL;
    private String ballTexture = "";
    private double shovelWooden = 0.6;
    private double shovelIron = 0.9;
    private double shovelDiamond = 1.2;
    private double shovelForward = 0.25;
    private double spikeWooden = 1.0;
    private double spikeIron = 1.4;
    private double spikeDiamond = 1.8;

    private State state = State.IDLE;
    private int touches = 3;
    private boolean wasOnGround = true;
    private boolean awaitingServe;
    private boolean placed;
    private @Nullable Team pickupTeam;
    private @Nullable UUID pickupHolder;
    private @Nullable Team possession;
    private @Nullable Team lastHitter;
    private int lastHitTick = -100;

    public BallManager(HeroicVolleyBall plugin, ArenaManager arenaManager, ScoreManager scoreManager,
            GameManager gameManager, ToolManager toolManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.scoreManager = scoreManager;
        this.gameManager = gameManager;
        this.toolManager = toolManager;
        this.ballKey = new NamespacedKey(plugin, "ball");
        loadConfig();
    }

    public void loadConfig() {
        var config = plugin.getConfig();
        this.size = Math.max(1, config.getInt("ball.size", 2));
        this.maxTouches = config.getInt("ball.max-touches", 3);
        this.gravity = config.getDouble("ball.gravity", 0.045);
        this.airDrag = config.getDouble("ball.air-drag", 0.99);
        this.velocityScale = config.getDouble("ball.velocity-scale", 1.0);
        this.maxVelocity = config.getDouble("ball.max-velocity", 3.0);
        this.spikeTopDown = config.getDouble("ball.spike.top-down", 1.5);
        this.spikeStraight = config.getDouble("ball.spike.straight", 0.2);
        this.spikeUp = config.getDouble("ball.spike.up", 0.7);
        this.spikeTopHorizontal = config.getDouble("ball.spike.top-horizontal", 0.25);
        this.spikeVerticalThreshold = config.getDouble("ball.spike.vertical-threshold", 1.0);
        this.shieldDeflect = config.getDouble("ball.shield.deflect", config.getDouble("ball.shield-deflect", 0.9));
        this.shieldForward = config.getDouble("ball.shield.forward", 0.6);
        this.shieldCatchPower = config.getDouble("ball.shield.catch-power", 0.8);
        this.shieldCatchReach = config.getDouble("ball.shield.catch-reach", 2.5);
        this.shieldCatchHeight = config.getDouble("ball.shield.catch-height", 3.0);
        this.shieldCooldown = Math.max(0, config.getInt("ball.shield.cooldown", 100));
        this.bounceFactor = config.getDouble("ball.bounce", 0.3);
        this.serveResetDelay = Math.max(0, config.getInt("ball.serve-reset-delay", 10));
        this.pointCountdown = Math.max(0, config.getInt("game.point-countdown", 3));
        this.touchPenalty = config.getBoolean("game.opponent-touch-penalty", true);
        this.showName = config.getBoolean("ball.show-name", true);
        this.pickupEnabled = config.getBoolean("ball.pickup-enabled", true);
        this.sulfurCube = config.getBoolean("ball.sulfur-cube", true);
        this.ballMaterial = parseBallMaterial(config.getString("ball.material", "GRAY_WOOL"));
        this.ballTexture = config.getString("ball.texture", "");
        this.shovelWooden = config.getDouble("ball.power.shovel.wooden", 0.6);
        this.shovelIron = config.getDouble("ball.power.shovel.iron", 0.9);
        this.shovelDiamond = config.getDouble("ball.power.shovel.diamond", 1.2);
        this.shovelForward = config.getDouble("ball.shovel-forward", 0.25);
        this.spikeWooden = config.getDouble("ball.power.spike.wooden", 1.0);
        this.spikeIron = config.getDouble("ball.power.spike.iron", 1.4);
        this.spikeDiamond = config.getDouble("ball.power.spike.diamond", 1.8);

        this.touches = Math.min(maxTouches, touches);
        applyBallAppearance();
        updateName();
    }

    public @Nullable SulfurCube getBall() {
        return ball;
    }

    public int getTouches() {
        return Math.max(touches, 0);
    }

    public int getMaxTouches() {
        return maxTouches;
    }

    public @Nullable Team getPossession() {
        return possession;
    }

    public String getStateName() {
        return state == State.RALLY ? "Rally" : "Serve";
    }

    public boolean isBall(@Nullable Entity entity) {
        return ball != null && entity != null && entity.getUniqueId().equals(ball.getUniqueId());
    }

    public boolean canPickupBall() {
        return pickupEnabled && !awaitingServe && !placed
                && state == State.IDLE && ball != null && ball.isValid();
    }

    public boolean isBallItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(ballKey, PersistentDataType.BOOLEAN);
    }

    public void sendPickupDenied(Player player) {
        plugin.getMessages().actionBar(player, "ball.pickup-denied");
    }

    public boolean pickupBall(Player player) {
        if (!canPickupBall()) {
            return false;
        }
        if (possession != null) {
            pickupTeam = possession;
        }
        pickupHolder = player.getUniqueId();
        Location where = current != null ? current : ball.getLocation();
        where.getWorld().playSound(where, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);

        ItemStack item = ballItem(tintedBallMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(ballKey, PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
        }

        removeBall();

        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
        }
        plugin.getMessages().actionBar(player, "ball.pickup");
        return true;
    }

    public boolean placeBall(Player player, Block clicked) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isBallItem(item)) {
            return false;
        }
        if (pickupTeam == null) {
            return false;
        }
        Arena arena = arenaManager.getArena(pickupTeam);
        if (arena == null || !arena.contains(clicked.getLocation())) {
            plugin.getMessages().actionBar(player, "ball.place-outside");
            return false;
        }

        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() > 0) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        spawnBall(clicked.getLocation().add(0.5, 1.0, 0.5));
        placed = true;
        pickupTeam = null;
        pickupHolder = null;
        plugin.getMessages().actionBar(player, "ball.place");
        return true;
    }

    public void spawnAtPlayer(Player player) {
        spawnBall(player.getLocation().add(0.0, 0.5, 0.0));
    }

    public boolean spawnAtStart() {
        return spawnAtStartTeam() != null;
    }

    public @Nullable Team spawnAtStartTeam() {
        if (resetToServe(Team.RED)) {
            return Team.RED;
        }
        if (resetToServe(Team.BLUE)) {
            return Team.BLUE;
        }
        return null;
    }

    public boolean resetToServe(Team team) {
        PositionManager positionManager = plugin.getPositionManager();
        Location base = null;
        if (positionManager != null) {
            base = positionManager.getPosition(team, 6);
        }
        if (base == null) {
            base = arenaCenterFloor(team);
        }
        if (base == null) {
            return false;
        }

        World world = base.getWorld();
        if (world == null) {
            return false;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location spawn = new Location(
                world,
                base.getBlockX() + 0.5 + random.nextInt(-1, 2),
                base.getBlockY() + 1.0,
                base.getBlockZ() + 0.5 + random.nextInt(-1, 2)
        );
        spawnBall(spawn);
        return true;
    }

    private @Nullable Location arenaCenterFloor(Team team) {
        Arena arena = arenaManager.getArena(team);
        if (arena == null) {
            return null;
        }
        World world = Bukkit.getWorld(arena.getWorld());
        if (world == null) {
            return null;
        }
        int x = (arena.getMinX() + arena.getMaxX()) / 2;
        int z = (arena.getMinZ() + arena.getMaxZ()) / 2;
        int floorY = arena.getMinY();
        for (int y = arena.getMaxY(); y >= arena.getMinY() - 1; y--) {
            if (!world.getBlockAt(x, y, z).isPassable()) {
                floorY = y + 1;
                break;
            }
        }
        return new Location(world, x + 0.5, floorY, z + 0.5);
    }

    public void spawnBall(Location location) {
        removeBall();
        takeBackHeldBall();

        World world = location.getWorld();
        if (world == null) {
            return;
        }

        this.ball = world.spawn(location, SulfurCube.class, CreatureSpawnEvent.SpawnReason.CUSTOM, cube -> {
            cube.setSize(size);
            cube.setWander(false);
            cube.setAware(false);
            cube.setAI(false);
            cube.setGravity(false);
            cube.setPersistent(true);
            cube.setSilent(true);
            cube.setMaximumNoDamageTicks(0);
            cube.setNoDamageTicks(0);
            cube.setCustomNameVisible(true);
            cube.setInvisible(!sulfurCube);
            cube.setEquipped(ballItem(ballMaterial));
        });

        this.current = location.clone();
        this.motion = new Vector();
        this.state = State.IDLE;
        this.touches = maxTouches;
        this.wasOnGround = true;
        this.awaitingServe = false;
        this.placed = false;
        this.pickupTeam = null;
        this.pickupHolder = null;
        this.lastHitTick = -100;
        this.possession = arenaManager.getArenaAt(location);
        this.lastHitter = null;
        resetShieldCooldown();
        applyBallAppearance();
        updateName();
        startTask();
    }

    private void takeBackHeldBall() {
        if (pickupHolder == null) {
            return;
        }
        Player holder = Bukkit.getPlayer(pickupHolder);
        if (holder != null) {
            ItemStack[] contents = holder.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                if (isBallItem(contents[i])) {
                    contents[i] = null;
                }
            }
            holder.getInventory().setContents(contents);
        }
        pickupHolder = null;
    }

    public void onPlayerHit(Player player, VolleyTool tool, VolleyTier tier) {
        if (ball == null || !ball.isValid()) {
            return;
        }
        int now = ball.getTicksLived();
        if (now - lastHitTick < 4) {
            return;
        }
        if (awaitingServe) {
            return;
        }

        Team playerTeam = resolveTeam(player);

        Team ballSide = possession;
        boolean penaltyActive = touchPenalty && gameManager != null && gameManager.isActive();
        if (penaltyActive && ballSide != null && playerTeam != null && ballSide != playerTeam) {
            lastHitTick = now;
            if (scorePoint(ballSide)) {
                if (current != null) {
                    current.getWorld().playSound(current, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                plugin.getMessages().actionBar(player, "ball.penalty");
            }
            return;
        }

        if (tool == VolleyTool.SHIELD && shieldCooldownTicks > 0) {
            plugin.getMessages().actionBar(player, "ball.shield-cooldown");
            return;
        }
        if (tool == VolleyTool.SHIELD && isBelowBall(player)) {
            plugin.getMessages().actionBar(player, "ball.shield-bottom");
            return;
        }

        lastHitTick = now;
        if (playerTeam != null) {
            lastHitter = playerTeam;
        }

        if (tool == VolleyTool.SHOVEL) {
            bump(player, tier);
        } else if (tool == VolleyTool.SWORD) {
            spike(player, tier);
        } else if (tool == VolleyTool.SHIELD) {
            block(player);
        }
    }

    private Team resolveTeam(Player player) {
        if (gameManager != null) {
            Team team = gameManager.getTeam(player);
            if (team != null) {
                return team;
            }
        }
        return arenaManager.getArenaAt(player.getLocation());
    }

    private Vector horizontalDirection(Player player) {
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection();
        Vector horizontal = new Vector(look.getX(), 0.0, look.getZ());
        if (horizontal.lengthSquared() < 0.01) {
            Location ballLocation = current != null ? current : ball.getLocation();
            Vector direction = ballLocation.toVector().subtract(eye.toVector());
            horizontal = new Vector(direction.getX(), 0.0, direction.getZ());
        }
        if (horizontal.lengthSquared() < 0.01) {
            horizontal = new Vector(0.0, 0.0, 1.0);
        }
        return horizontal.normalize();
    }

    private void bump(Player player, VolleyTier tier) {
        if (state == State.RALLY && touches <= 0) {
            return;
        }
        if (state == State.IDLE) {
            state = State.RALLY;
        } else {
            touches--;
        }

        double power = shovelPower(tier) * velocityScale;
        Vector forward = horizontalDirection(player);
        double horizontal = power * shovelForward;
        motion = new Vector(forward.getX() * horizontal, power, forward.getZ() * horizontal);
        wasOnGround = false;
        updateName();

        if (current != null) {
            current.getWorld().playSound(current, Sound.BLOCK_WOOL_PLACE, 1.0f, 1.2f);
        }
        plugin.getMessages().actionBar(player, "ball.bump");
    }

    private void spike(Player player, VolleyTier tier) {
        if (state == State.RALLY && touches <= 0) {
            return;
        }
        if (state == State.IDLE) {
            state = State.RALLY;
        } else {
            touches--;
        }

        Location ballLocation = current != null ? current : ball.getLocation();
        Location eye = player.getEyeLocation();

        Vector horizontal = horizontalDirection(player);
        double power = spikePower(tier) * velocityScale;
        double vertical = eye.getY() - ballLocation.getY();

        Vector velocity;
        if (vertical > spikeVerticalThreshold) {
            // Hit from above: drive the ball straight down, fast.
            velocity = horizontal.multiply(power * spikeTopHorizontal);
            velocity.setY(-spikeTopDown * velocityScale);
        } else if (vertical < -spikeVerticalThreshold) {
            // Hit from below: bounce the ball up and forward where the player is looking.
            velocity = horizontal.multiply(power);
            velocity.setY(spikeUp * velocityScale);
        } else {
            // Level hit: send it straight forward, gravity takes it down.
            velocity = horizontal.multiply(power);
            velocity.setY(spikeStraight * velocityScale);
        }

        motion = velocity;
        wasOnGround = false;
        updateName();

        current.getWorld().playSound(current, Sound.BLOCK_WOOL_BREAK, 1.0f, 0.9f);
        plugin.getMessages().actionBar(player, "ball.spike");
    }

    private void tick() {
        if (ball == null || !ball.isValid() || ball.isDead() || current == null) {
            cleanup();
            return;
        }

        if (shieldCooldownTicks > 0) {
            shieldCooldownTicks--;
            updateBrokenShields();
            if (shieldCooldownTicks <= 0) {
                restoreBrokenShields();
            }
        }
        tryShieldCatch();
        integrate();
        updatePossession();
        step();
        updateName();
    }

    private void integrate() {
        motion.setY(motion.getY() - gravity);
        motion.multiply(airDrag);
    }

    private void step() {
        World world = current.getWorld();
        if (world == null) {
            return;
        }

        clamp(motion);

        moveHorizontal(world, motion.getX(), 0);
        moveHorizontal(world, motion.getZ(), 2);

        boolean hitFloor = false;
        double vy = motion.getY();
        if (vy != 0.0) {
            int steps = substeps(vy);
            double increment = vy / steps;
            for (int i = 0; i < steps; i++) {
                Location candidate = current.clone();
                candidate.setY(candidate.getY() + increment);
                if (collides(world, candidate)) {
                    if (vy < 0.0) {
                        hitFloor = true;
                        current.setY(Math.floor(candidate.getY()) + 1.0);
                    }
                    double bounce = -vy * bounceFactor;
                    motion.setY(Math.abs(bounce) < settleThreshold ? 0.0 : bounce);
                    break;
                }
                current = candidate;
            }
        }

        boolean onGround = hitFloor || grounded(world);
        if (onGround) {
            motion.setX(motion.getX() * 0.5);
            motion.setZ(motion.getZ() * 0.5);

            if (state == State.RALLY && !wasOnGround) {
                Team scorer = resolveScorer();
                if (!scorePoint(scorer)) {
                    resetRally();
                }
            }
        }

        wasOnGround = onGround;
        ball.teleport(current);
        if (ballDisplay != null && ballDisplay.isValid()) {
            ballDisplay.teleport(current);
        }
    }

    private void moveHorizontal(World world, double amount, int axis) {
        if (amount == 0.0) {
            return;
        }
        int steps = substeps(amount);
        double increment = amount / steps;
        for (int i = 0; i < steps; i++) {
            Location candidate = current.clone();
            if (axis == 0) {
                candidate.setX(candidate.getX() + increment);
            } else {
                candidate.setZ(candidate.getZ() + increment);
            }
            if (collides(world, candidate)) {
                double bounce = -amount * bounceFactor;
                if (axis == 0) {
                    motion.setX(bounce);
                } else {
                    motion.setZ(bounce);
                }
                return;
            }
            current = candidate;
        }
    }

    private int substeps(double amount) {
        return Math.max(1, (int) Math.ceil(Math.abs(amount) / COLLISION_STEP));
    }

    private boolean grounded(World world) {
        Location below = current.clone();
        below.setY(below.getY() - 0.05);
        return collides(world, below);
    }

    private double ballRadius() {
        return 0.45 * (size / 2.0);
    }

    private double ballHeight() {
        return 1.6 * (size / 2.0);
    }

    private boolean collides(World world, Location location) {
        double radius = ballRadius();
        double height = ballHeight();
        double minX = location.getX() - radius;
        double maxX = location.getX() + radius;
        double minY = location.getY();
        double maxY = location.getY() + height;
        double minZ = location.getZ() - radius;
        double maxZ = location.getZ() + radius;

        int x0 = (int) Math.floor(minX);
        int x1 = (int) Math.floor(maxX);
        int y0 = (int) Math.floor(minY);
        int y1 = (int) Math.floor(maxY);
        int z0 = (int) Math.floor(minZ);
        int z1 = (int) Math.floor(maxZ);

        if (!world.isChunkLoaded(x0 >> 4, z0 >> 4) || !world.isChunkLoaded(x1 >> 4, z1 >> 4)) {
            return false;
        }

        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (!world.getBlockAt(x, y, z).isPassable()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void block(Player player) {
        Vector forward = horizontalDirection(player);
        motion.setX(forward.getX() * shieldForward * velocityScale);
        motion.setZ(forward.getZ() * shieldForward * velocityScale);
        motion.setY(-shieldDeflect * velocityScale);
        wasOnGround = false;
        startShieldCooldown(player);
        updateName();

        if (current != null) {
            current.getWorld().playSound(current, Sound.BLOCK_WOOL_BREAK, 1.0f, 0.9f);
        }
        plugin.getMessages().actionBar(player, "ball.shield-block");
    }

    private boolean isBelowBall(Player player) {
        Location ballLocation = current != null ? current : ball.getLocation();
        return player.getEyeLocation().getY() - ballLocation.getY() < -spikeVerticalThreshold;
    }

    private void tryShieldCatch() {
        if (ball == null || !ball.isValid() || current == null || awaitingServe || shieldCooldownTicks > 0) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isBlocking()) {
                continue;
            }
            ItemStack main = player.getInventory().getItemInMainHand();
            if (toolManager.getTool(main) != VolleyTool.SHIELD) {
                continue;
            }
            if (shieldCatchable(player)) {
                catchBall(player);
                return;
            }
        }
    }

    private boolean shieldCatchable(Player player) {
        Location eye = player.getEyeLocation();
        double dy = current.getY() - eye.getY();
        if (dy < 0.3 || dy > shieldCatchHeight) {
            return false;
        }
        double reach = Math.hypot(current.getX() - eye.getX(), current.getZ() - eye.getZ());
        return reach <= shieldCatchReach;
    }

    private void catchBall(Player player) {
        motion.setX(motion.getX() * 0.3);
        motion.setZ(motion.getZ() * 0.3);
        motion.setY(shieldCatchPower * velocityScale);
        wasOnGround = false;
        if (state == State.IDLE) {
            state = State.RALLY;
        }
        startShieldCooldown(player);
        updateName();

        if (current != null) {
            current.getWorld().playSound(current, Sound.BLOCK_WOOL_BREAK, 1.0f, 1.3f);
        }
        plugin.getMessages().actionBar(player, "ball.shield-catch");
    }

    private void startShieldCooldown(Player player) {
        if (shieldCooldown <= 0) {
            shieldCooldownTicks = 0;
            return;
        }
        shieldCooldownTicks = shieldCooldown;
        if (player != null) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
            breakShield(player);
        }
    }

    private void updateBrokenShields() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (brokenShields.containsKey(player.getUniqueId())) {
                continue;
            }
            breakShield(player);
        }
    }

    private void breakShield(Player player) {
        if (brokenShields.containsKey(player.getUniqueId())) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (toolManager.getTool(item) != VolleyTool.SHIELD) {
            return;
        }
        brokenShields.put(player.getUniqueId(), item.clone());
        player.getInventory().setItemInMainHand(crackedShield(item));
    }

    private ItemStack crackedShield(ItemStack item) {
        ItemStack cracked = item.clone();
        ItemMeta meta = cracked.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setUnbreakable(false);
            damageable.setDamage(Math.max(0, cracked.getType().getMaxDurability() - 2));
        }
        cracked.setItemMeta(meta);
        return cracked;
    }

    private void restoreBrokenShields() {
        for (Map.Entry<UUID, ItemStack> entry : brokenShields.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            ItemStack original = entry.getValue();
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null && toolManager.getTool(contents[i]) == VolleyTool.SHIELD) {
                    player.getInventory().setItem(i, original.clone());
                }
            }
        }
        brokenShields.clear();
    }

    private void resetShieldCooldown() {
        shieldCooldownTicks = 0;
        restoreBrokenShields();
    }

    private void updatePossession() {
        Team currentTeam = arenaManager.getArenaAt(current);
        if (currentTeam == possession) {
            return;
        }
        possession = currentTeam;
        touches = maxTouches;
        applyBallAppearance();
    }

    private void applyBallAppearance() {
        if (ball == null || !ball.isValid()) {
            return;
        }

        ItemStack item = ballItem(tintedBallMaterial());

        if (sulfurCube) {
            ball.setInvisible(false);
            ball.setEquipped(item);
            removeDisplay();
            return;
        }

        ball.setInvisible(true);
        ball.setEquipped(new ItemStack(Material.AIR));
        showDisplay(item);
    }

    private Material tintedBallMaterial() {
        Material material = ballMaterial;
        if (material.name().endsWith("_WOOL")) {
            material = switch (possession) {
                case RED -> Material.RED_WOOL;
                case BLUE -> Material.BLUE_WOOL;
                case null -> Material.GRAY_WOOL;
            };
        }
        return material;
    }

    private void showDisplay(ItemStack item) {
        if (ball == null || !ball.isValid()) {
            return;
        }
        if (ballDisplay != null && ballDisplay.isValid()) {
            ballDisplay.setItemStack(item);
            return;
        }
        ballDisplay = ball.getWorld().spawn(ball.getLocation(), ItemDisplay.class,
                CreatureSpawnEvent.SpawnReason.CUSTOM, display -> {
                    display.setItemStack(item);
                    display.setPersistent(true);
                    display.setInvulnerable(true);
                    display.setGravity(false);
                    display.setSilent(true);
                    display.setBillboard(Display.Billboard.FIXED);
                    display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    display.setCustomNameVisible(false);
                    display.setTeleportDuration(0);
                });
    }

    private void removeDisplay() {
        if (ballDisplay != null) {
            ballDisplay.remove();
            ballDisplay = null;
        }
    }

    private ItemStack ballItem(Material material) {
        ItemStack item = new ItemStack(material);
        if (material != Material.PLAYER_HEAD || ballTexture.isEmpty()) {
            return item;
        }
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) {
            return item;
        }
        PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(ballTexture.getBytes()), "Ball");
        profile.setProperty(new ProfileProperty("textures", ballTexture));
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
        return item;
    }

    private Material parseBallMaterial(String name) {
        if (name == null) {
            return Material.GRAY_WOOL;
        }
        Material material = Material.matchMaterial(name);
        return material != null && material.isItem() ? material : Material.GRAY_WOOL;
    }

    private void resetRally() {
        state = State.IDLE;
        touches = maxTouches;
        placed = false;
    }

    private @Nullable Team resolveScorer() {
        if (lastHitter == null) {
            return null;
        }

        Team landingTeam = arenaManager.getArenaAt(current);
        boolean landedOnOpponent = landingTeam != null && landingTeam != lastHitter;
        return landedOnOpponent ? lastHitter : lastHitter.opposite();
    }

    private boolean scorePoint(@Nullable Team scorer) {
        if (scorer == null || awaitingServe || scoreManager == null) {
            return false;
        }
        awaitingServe = true;
        scoreManager.addPoint(scorer);
        resetRally();
        if (!scoreManager.isMatchOver() && !scoreManager.isRoundTransition()) {
            scheduleServeReset(scorer);
        }
        return true;
    }

    private void scheduleServeReset(Team team) {
        if (serveResetDelay <= 0) {
            beginServeCountdown(team);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> beginServeCountdown(team), serveResetDelay);
        }
    }

    private void beginServeCountdown(Team team) {
        if (pointCountdown <= 0) {
            performServeReset(team);
            return;
        }
        Countdown.run(plugin, pointCountdown, "game.point-countdown-title", "game.point-countdown-subtitle",
                () -> performServeReset(team), "team", Messages.teamDisplay(team));
    }

    private void performServeReset(Team team) {
        if (state == State.RALLY) {
            return;
        }
        resetToServe(team);
        if (gameManager != null) {
            gameManager.teleportToPositions(team);
        }
    }

    private void clamp(Vector vector) {
        vector.setX(clampComponent(vector.getX()));
        vector.setY(clampComponent(vector.getY()));
        vector.setZ(clampComponent(vector.getZ()));
    }

    private double clampComponent(double value) {
        if (value > maxVelocity) {
            return maxVelocity;
        }
        if (value < -maxVelocity) {
            return -maxVelocity;
        }
        return value;
    }

    private void updateName() {
        if (ball == null || !ball.isValid()) {
            return;
        }

        if (!showName) {
            ball.setCustomNameVisible(false);
            return;
        }

        Messages messages = plugin.getMessages();
        Component name;
        if (state == State.IDLE) {
            name = messages.get("ball.name-serve");
        } else {
            String color = possession != null ? Messages.colorName(possession) : "gray";
            name = messages.get("ball.name-touches",
                    "color", color,
                    "touches", String.valueOf(Math.max(touches, 0)),
                    "max", String.valueOf(maxTouches));
        }
        ball.customName(name);
        ball.setCustomNameVisible(true);
    }

    private double shovelPower(VolleyTier tier) {
        return switch (tier) {
            case WOODEN -> shovelWooden;
            case IRON -> shovelIron;
            case DIAMOND -> shovelDiamond;
        };
    }

    private double spikePower(VolleyTier tier) {
        return switch (tier) {
            case WOODEN -> spikeWooden;
            case IRON -> spikeIron;
            case DIAMOND -> spikeDiamond;
        };
    }

    private void startTask() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void removeBall() {
        removeDisplay();
        resetShieldCooldown();
        if (ball != null) {
            ball.remove();
            ball = null;
        }
        current = null;
        motion = new Vector();
    }

    private void cleanup() {
        removeDisplay();
        resetShieldCooldown();
        ball = null;
        current = null;
        motion = new Vector();
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeDisplay();
        resetShieldCooldown();
        if (ball != null) {
            ball.remove();
            ball = null;
        }
        current = null;
        motion = new Vector();
    }
}
