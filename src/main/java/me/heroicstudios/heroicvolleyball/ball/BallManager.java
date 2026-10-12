package me.heroicstudios.heroicvolleyball.ball;

import java.util.concurrent.ThreadLocalRandom;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.game.GameManager;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import me.heroicstudios.heroicvolleyball.tool.VolleyTier;
import me.heroicstudios.heroicvolleyball.tool.VolleyTool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SulfurCube;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
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

    private @Nullable SulfurCube ball;
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
    private double bounceFactor = 0.3;
    private double settleThreshold = 0.12;
    private int serveResetDelay = 10;
    private boolean touchPenalty = true;
    private boolean showName = true;
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
    private @Nullable Team possession;
    private @Nullable Team lastHitter;
    private int lastHitTick = -100;

    public BallManager(HeroicVolleyBall plugin, ArenaManager arenaManager, ScoreManager scoreManager, GameManager gameManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.scoreManager = scoreManager;
        this.gameManager = gameManager;
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
        this.shieldDeflect = config.getDouble("ball.shield-deflect", 0.9);
        this.bounceFactor = config.getDouble("ball.bounce", 0.3);
        this.serveResetDelay = Math.max(0, config.getInt("ball.serve-reset-delay", 10));
        this.touchPenalty = config.getBoolean("game.opponent-touch-penalty", true);
        this.showName = config.getBoolean("ball.show-name", true);
        this.shovelWooden = config.getDouble("ball.power.shovel.wooden", 0.6);
        this.shovelIron = config.getDouble("ball.power.shovel.iron", 0.9);
        this.shovelDiamond = config.getDouble("ball.power.shovel.diamond", 1.2);
        this.shovelForward = config.getDouble("ball.shovel-forward", 0.25);
        this.spikeWooden = config.getDouble("ball.power.spike.wooden", 1.0);
        this.spikeIron = config.getDouble("ball.power.spike.iron", 1.4);
        this.spikeDiamond = config.getDouble("ball.power.spike.diamond", 1.8);

        this.touches = Math.min(maxTouches, touches);
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
            cube.setEquipped(new ItemStack(Material.GRAY_WOOL));
        });

        this.current = location.clone();
        this.motion = new Vector();
        this.state = State.IDLE;
        this.touches = maxTouches;
        this.wasOnGround = true;
        this.lastHitTick = -100;
        this.possession = arenaManager.getArenaAt(location);
        this.lastHitter = null;
        applyBallColor();
        updateName();
        startTask();
    }

    public void onPlayerHit(Player player, VolleyTool tool, VolleyTier tier) {
        if (ball == null || !ball.isValid()) {
            return;
        }
        int now = ball.getTicksLived();
        if (now - lastHitTick < 4) {
            return;
        }

        Team playerTeam = resolveTeam(player);

        Team ballSide = possession;
        boolean penaltyActive = touchPenalty && gameManager != null && gameManager.isActive();
        if (penaltyActive && ballSide != null && playerTeam != null && ballSide != playerTeam) {
            lastHitTick = now;
            scoreManager.addPoint(ballSide);
            resetRally();
            scheduleServeReset(ballSide);
            if (current != null) {
                current.getWorld().playSound(current, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            plugin.getMessages().actionBar(player, "ball.penalty");
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
            block();
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
                Team scorer = awardPoint();
                resetRally();
                if (scorer != null) {
                    scheduleServeReset(scorer);
                }
            }
        }

        wasOnGround = onGround;
        ball.teleport(current);
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

    private void block() {
        motion.setX(motion.getX() * 0.2);
        motion.setZ(motion.getZ() * 0.2);
        motion.setY(-shieldDeflect * velocityScale);
        wasOnGround = false;
        updateName();
    }

    private void updatePossession() {
        Team currentTeam = arenaManager.getArenaAt(current);
        if (currentTeam == possession) {
            return;
        }
        possession = currentTeam;
        touches = maxTouches;
        applyBallColor();
    }

    private void applyBallColor() {
        if (ball == null || !ball.isValid()) {
            return;
        }
        Material material = switch (possession) {
            case RED -> Material.RED_WOOL;
            case BLUE -> Material.BLUE_WOOL;
            case null -> Material.GRAY_WOOL;
        };
        ball.setEquipped(new ItemStack(material));
    }

    private void resetRally() {
        state = State.IDLE;
        touches = maxTouches;
    }

    private @Nullable Team awardPoint() {
        if (scoreManager == null || lastHitter == null) {
            return null;
        }

        Team landingTeam = arenaManager.getArenaAt(current);
        boolean landedOnOpponent = landingTeam != null && landingTeam != lastHitter;
        Team scorer = landedOnOpponent ? lastHitter : lastHitter.opposite();
        scoreManager.addPoint(scorer);
        return scorer;
    }

    private void scheduleServeReset(Team team) {
        if (serveResetDelay <= 0) {
            Bukkit.getScheduler().runTask(plugin, () -> performServeReset(team));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> performServeReset(team), serveResetDelay);
        }
    }

    private void performServeReset(Team team) {
        if (state == State.RALLY) {
            return;
        }
        if (ball != null && ball.isValid()) {
            resetToServe(team);
        }
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
        if (ball != null) {
            ball.remove();
            ball = null;
        }
        current = null;
        motion = new Vector();
    }

    private void cleanup() {
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
        if (ball != null) {
            ball.remove();
            ball = null;
        }
        current = null;
        motion = new Vector();
    }
}
