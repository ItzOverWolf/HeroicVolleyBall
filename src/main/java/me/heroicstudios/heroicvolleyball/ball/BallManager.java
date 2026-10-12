package me.heroicstudios.heroicvolleyball.ball;

import java.util.concurrent.ThreadLocalRandom;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import me.heroicstudios.heroicvolleyball.score.ScoreManager;
import me.heroicstudios.heroicvolleyball.tool.VolleyTier;
import me.heroicstudios.heroicvolleyball.tool.VolleyTool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
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

    private static final double BALL_RADIUS = 0.45;
    private static final double BALL_HEIGHT = 1.6;
    private static final double COLLISION_STEP = 0.1;

    private enum State {
        IDLE,
        RALLY
    }

    private final HeroicVolleyBall plugin;
    private final ArenaManager arenaManager;
    private final ScoreManager scoreManager;

    private @Nullable SulfurCube ball;
    private @Nullable Location current;
    private @Nullable BukkitTask task;
    private Vector motion = new Vector();

    private int maxTouches = 3;
    private double gravity = 0.045;
    private double airDrag = 0.99;
    private double velocityScale = 1.0;
    private double maxVelocity = 3.0;
    private double spikeDownward = 0.8;
    private double shieldDeflect = 0.9;
    private double bounceFactor = 0.3;
    private double settleThreshold = 0.12;
    private double shovelWooden = 0.6;
    private double shovelIron = 0.9;
    private double shovelDiamond = 1.2;
    private double spikeWooden = 1.0;
    private double spikeIron = 1.4;
    private double spikeDiamond = 1.8;

    private State state = State.IDLE;
    private int touches = 3;
    private boolean wasOnGround = true;
    private @Nullable Team possession;
    private @Nullable Team lastHitter;
    private int lastHitTick = -100;

    public BallManager(HeroicVolleyBall plugin, ArenaManager arenaManager, ScoreManager scoreManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.scoreManager = scoreManager;
        loadConfig();
    }

    public void loadConfig() {
        var config = plugin.getConfig();
        this.maxTouches = config.getInt("ball.max-touches", 3);
        this.gravity = config.getDouble("ball.gravity", 0.045);
        this.airDrag = config.getDouble("ball.air-drag", 0.99);
        this.velocityScale = config.getDouble("ball.velocity-scale", 1.0);
        this.maxVelocity = config.getDouble("ball.max-velocity", 3.0);
        this.spikeDownward = config.getDouble("ball.spike-downward", 0.8);
        this.shieldDeflect = config.getDouble("ball.shield-deflect", 0.9);
        this.bounceFactor = config.getDouble("ball.bounce", 0.3);
        this.shovelWooden = config.getDouble("ball.power.shovel.wooden", 0.6);
        this.shovelIron = config.getDouble("ball.power.shovel.iron", 0.9);
        this.shovelDiamond = config.getDouble("ball.power.shovel.diamond", 1.2);
        this.spikeWooden = config.getDouble("ball.power.spike.wooden", 1.0);
        this.spikeIron = config.getDouble("ball.power.spike.iron", 1.4);
        this.spikeDiamond = config.getDouble("ball.power.spike.diamond", 1.8);

        this.touches = Math.min(maxTouches, touches);
        updateName();
    }

    public @Nullable SulfurCube getBall() {
        return ball;
    }

    public boolean isBall(@Nullable Entity entity) {
        return ball != null && entity != null && entity.getUniqueId().equals(ball.getUniqueId());
    }

    public void spawnAtPlayer(Player player) {
        spawnBall(player.getLocation().add(0.0, 0.5, 0.0));
    }

    public boolean spawnAtStart() {
        return resetToServe(Team.RED) || resetToServe(Team.BLUE);
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
            cube.setSize(2);
            cube.setWander(false);
            cube.setAware(false);
            cube.setAI(false);
            cube.setGravity(false);
            cube.setPersistent(true);
            cube.setSilent(true);
            cube.setMaximumNoDamageTicks(0);
            cube.setNoDamageTicks(0);
            cube.setCustomNameVisible(true);
            cube.setEquipped(new ItemStack(Material.RED_WOOL));
        });

        this.current = location.clone();
        this.motion = new Vector();
        this.state = State.IDLE;
        this.touches = maxTouches;
        this.wasOnGround = true;
        this.lastHitTick = -100;
        this.possession = arenaManager.getArenaAt(location);
        this.lastHitter = null;
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
        lastHitTick = now;

        Team hitter = arenaManager.getArenaAt(player.getLocation());
        if (hitter != null) {
            lastHitter = hitter;
        }

        if (tool == VolleyTool.SHOVEL) {
            bump(player, tier);
        } else if (tool == VolleyTool.SWORD) {
            spike(player, tier);
        } else if (tool == VolleyTool.SHIELD) {
            block();
        }
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
        motion = new Vector(0.0, power, 0.0);
        wasOnGround = false;
        updateName();

        if (current != null) {
            current.getWorld().playSound(current, Sound.BLOCK_WOOL_PLACE, 1.0f, 1.2f);
        }
        player.showTitle(Title.title(
                Component.text("BUMP", NamedTextColor.GREEN),
                Component.text("Ball Up", NamedTextColor.GRAY)));
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

        Vector direction = ballLocation.toVector().subtract(eye.toVector());
        Vector horizontal = new Vector(direction.getX(), 0.0, direction.getZ());
        if (horizontal.lengthSquared() < 0.01) {
            Vector look = eye.getDirection();
            horizontal = new Vector(look.getX(), 0.0, look.getZ());
        }
        if (horizontal.lengthSquared() < 0.01) {
            horizontal = new Vector(0.0, 0.0, 1.0);
        }
        horizontal.normalize();

        double power = spikePower(tier) * velocityScale;
        Vector velocity = horizontal.multiply(power);

        boolean above = (eye.getY() - ballLocation.getY()) > 1.0;
        if (above) {
            velocity.setY(-spikeDownward * velocityScale);
        } else {
            velocity.setY(0.2 * velocityScale);
        }

        motion = velocity;
        wasOnGround = false;
        updateName();

        current.getWorld().playSound(current, Sound.BLOCK_WOOL_BREAK, 1.0f, 0.9f);
        player.showTitle(Title.title(
                Component.text("SPIKE", NamedTextColor.RED),
                Component.text("Ball Spike", NamedTextColor.GRAY)));
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

    private boolean collides(World world, Location location) {
        double minX = location.getX() - BALL_RADIUS;
        double maxX = location.getX() + BALL_RADIUS;
        double minY = location.getY();
        double maxY = location.getY() + BALL_HEIGHT;
        double minZ = location.getZ() - BALL_RADIUS;
        double maxZ = location.getZ() + BALL_RADIUS;

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
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (ball != null && ball.isValid()) {
                resetToServe(team);
            }
        });
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

        Component name;
        if (state == State.IDLE) {
            name = Component.text("Serve", NamedTextColor.GRAY);
        } else {
            NamedTextColor color = possession != null ? possession.getColor() : NamedTextColor.GRAY;
            name = Component.text("Touches: " + Math.max(touches, 0) + "/" + maxTouches, color);
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
