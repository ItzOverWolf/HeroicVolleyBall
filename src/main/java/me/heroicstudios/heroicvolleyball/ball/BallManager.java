package me.heroicstudios.heroicvolleyball.ball;

import java.util.concurrent.ThreadLocalRandom;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.ArenaManager;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import me.heroicstudios.heroicvolleyball.tool.VolleyTier;
import me.heroicstudios.heroicvolleyball.tool.VolleyTool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SulfurCube;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public final class BallManager {

    private enum State {
        IDLE,
        RALLY
    }

    private final HeroicVolleyBall plugin;
    private final ArenaManager arenaManager;

    private @Nullable SulfurCube ball;
    private @Nullable BukkitTask task;
    private Vector motion = new Vector();

    private int maxTouches = 3;
    private double gravity = 0.045;
    private double airDrag = 0.99;
    private double velocityScale = 1.0;
    private double maxVelocity = 3.0;
    private double spikeDownward = 0.8;
    private double shieldDeflect = 0.9;
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
    private int lastHitTick = -100;

    public BallManager(HeroicVolleyBall plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
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
        PositionManager positionManager = plugin.getPositionManager();
        Location base = null;
        if (positionManager != null) {
            base = positionManager.getPosition(Team.RED, 6);
            if (base == null) {
                base = positionManager.getPosition(Team.BLUE, 6);
            }
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
            disableDrag(cube);
        });

        this.motion = new Vector();
        this.state = State.IDLE;
        this.touches = maxTouches;
        this.wasOnGround = true;
        this.lastHitTick = -100;
        this.possession = arenaManager.getArenaAt(location);
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

        if (tool == VolleyTool.SHOVEL) {
            bump(tier);
        } else if (tool == VolleyTool.SPEAR) {
            spike(player, tier);
        }
    }

    private void bump(VolleyTier tier) {
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
        ball.setVelocity(motion);
        wasOnGround = true;
        updateName();
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

        Location ballLocation = ball.getLocation();
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
        ball.setVelocity(motion);
        wasOnGround = true;
        updateName();
    }

    private void tick() {
        if (ball == null || !ball.isValid() || ball.isDead()) {
            cleanup();
            return;
        }

        applyMotion();
        handleShield();
        updatePossession();
        handleGround();
        updateName();
    }

    private void applyMotion() {
        motion.setY(motion.getY() - gravity);
        motion.multiply(airDrag);

        if (ball.isOnGround()) {
            if (motion.getY() < 0.0) {
                motion.setY(0.0);
            }
            motion.setX(motion.getX() * 0.5);
            motion.setZ(motion.getZ() * 0.5);
        }

        clamp(motion);
        ball.setVelocity(motion);
    }

    private void clamp(Vector vector) {
        double x = clampComponent(vector.getX());
        double y = clampComponent(vector.getY());
        double z = clampComponent(vector.getZ());
        vector.setX(x);
        vector.setY(y);
        vector.setZ(z);
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

    private void handleShield() {
        Location location = ball.getLocation();
        for (Player player : ball.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) > 4.0) {
                continue;
            }
            if (!player.isBlocking()) {
                continue;
            }
            motion.setX(motion.getX() * 0.2);
            motion.setZ(motion.getZ() * 0.2);
            motion.setY(-shieldDeflect * velocityScale);
            ball.setVelocity(motion);
            wasOnGround = true;
            break;
        }
    }

    private void updatePossession() {
        Team current = arenaManager.getArenaAt(ball.getLocation());
        if (current == possession) {
            return;
        }
        possession = current;
        touches = maxTouches;
    }

    private void handleGround() {
        boolean onGround = ball.isOnGround();
        if (onGround && !wasOnGround && state == State.RALLY) {
            resetRally();
        }
        wasOnGround = onGround;
    }

    private void resetRally() {
        state = State.IDLE;
        touches = maxTouches;
        motion = new Vector();
        if (ball != null) {
            ball.setVelocity(motion);
        }
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

    private void disableDrag(SulfurCube cube) {
        AttributeInstance air = cube.getAttribute(Attribute.AIR_DRAG_MODIFIER);
        if (air != null) {
            air.setBaseValue(-1.0);
        }
        AttributeInstance friction = cube.getAttribute(Attribute.FRICTION_MODIFIER);
        if (friction != null) {
            friction.setBaseValue(-1.0);
        }
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
        motion = new Vector();
    }

    private void cleanup() {
        ball = null;
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
    }
}
