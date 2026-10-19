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
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.SulfurCube;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

public final class BallManager {

    private static final double COLLISION_STEP = 0.1;

    /**
     * Only used to recognise sulfur cube ghosts saved to disk by older builds
     * of this plugin during the startup sweep - the ball itself never uses the
     * cube anymore.
     */
    private static final boolean SULFUR_CUBE_SUPPORTED = classExists("org.bukkit.entity.SulfurCube");

    /**
     * Rendered height of a player head in an ItemDisplay at scale 1 (the head
     * block model is half a block tall). The display's scale factor is derived
     * from this so the visible head always matches the physics ball height.
     */
    private static final double HEAD_BASE_SIZE = 0.5;

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, BallManager.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

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

    private @Nullable LivingEntity ball;
    private @Nullable ItemDisplay ballDisplay;
    private @Nullable TextDisplay ballNameDisplay;
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
    private boolean swingFallback = true;
    private double swingReachBuffer = 0.5;
    private double swingSpeedTolerance = 1.0;
    private final Map<UUID, ItemStack> brokenShields = new HashMap<>();
    private double bounceFactor = 0.3;
    private double settleThreshold = 0.12;
    private int serveResetDelay = 10;
    private int pointCountdown = 3;
    private boolean touchPenalty = true;
    private boolean showName = true;
    private boolean pickupEnabled = true;
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
        this.swingFallback = config.getBoolean("ball.hit.swing-fallback", true);
        this.swingReachBuffer = config.getDouble("ball.hit.swing-reach-buffer", 0.5);
        this.swingSpeedTolerance = Math.max(0.0, config.getDouble("ball.hit.swing-speed-tolerance", 1.0));
        this.bounceFactor = config.getDouble("ball.bounce", 0.3);
        this.serveResetDelay = Math.max(0, config.getInt("ball.serve-reset-delay", 10));
        this.pointCountdown = Math.max(0, config.getInt("game.point-countdown", 3));
        this.touchPenalty = config.getBoolean("game.opponent-touch-penalty", true);
        this.showName = config.getBoolean("ball.show-name", true);
        this.pickupEnabled = config.getBoolean("ball.pickup-enabled", true);
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

    public @Nullable LivingEntity getBall() {
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
        if (entity == null) {
            return false;
        }
        if (ball != null && entity.getUniqueId().equals(ball.getUniqueId())) {
            return true;
        }
        // Old clients attack the ItemDisplay twin (they cannot see the cube).
        return ballDisplay != null && entity.getUniqueId().equals(ballDisplay.getUniqueId());
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
        Team team = possession;
        if (team == null && ball != null && ball.isValid()) {
            team = arenaManager.getArenaAt(ball.getLocation());
        }
        if (team != null && plugin.getBackrowManager() != null) {
            Arena backrow = plugin.getBackrowManager().getBackrow(team);
            if (backrow != null && !backrow.contains(player.getLocation())) {
                plugin.getMessages().actionBar(player, "ball.pickup-backrow");
                return false;
            }
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
            meta.displayName(plugin.getMessages().get("ball.name"));
            meta.lore(plugin.getMessages().getLines("ball.lore"));
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
        Arena region = placementRegion(pickupTeam);
        if (region == null || !region.contains(clicked.getLocation())) {
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

    private @Nullable Arena placementRegion(Team team) {
        if (plugin.getBackrowManager() != null) {
            Arena backrow = plugin.getBackrowManager().getBackrow(team);
            if (backrow != null) {
                return backrow;
            }
        }
        return arenaManager.getArena(team);
    }

    public void spawnAtPlayer(Player player) {
        Location spawn = null;
        Team team = arenaManager.getArenaAt(player.getLocation());
        if (team != null && plugin.getBallSpawnManager() != null) {
            spawn = plugin.getBallSpawnManager().get(team);
        }
        if (spawn == null) {
            spawn = player.getLocation().add(0.0, 0.5, 0.0);
        }
        spawnBall(spawn);
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
        Location base = null;
        if (plugin.getBallSpawnManager() != null && plugin.getBallSpawnManager().isSet(team)) {
            base = plugin.getBallSpawnManager().get(team);
        }
        if (base == null && plugin.getPositionManager() != null) {
            base = plugin.getPositionManager().getPosition(team, 6);
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

        // The ball is an invisible slime used purely as the physics/click
        // hitbox - it resizes with ball.size, exists on every supported
        // version, and can never render wrong because it never renders at all.
        // The floating player head ItemDisplay above it is the ball everyone
        // sees on every client version.
        this.ball = world.spawn(location, Slime.class, CreatureSpawnEvent.SpawnReason.CUSTOM, slime -> {
            slime.setSize(Math.max(1, size));
            slime.setAware(false);
            slime.setAI(false);
            slime.setGravity(false);
            // Non-persistent: never written to the world save, so a crash
            // or force-stop can never leave a ghost ball behind.
            slime.setPersistent(false);
            slime.setSilent(true);
            slime.setMaximumNoDamageTicks(0);
            slime.setNoDamageTicks(0);
            slime.setInvisible(true);
            slime.getPersistentDataContainer().set(ballKey, PersistentDataType.BOOLEAN, true);
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

    /**
     * Server-side fallback for clicks the vanilla attack packet never delivers
     * as a hit: a fast-falling ball sits at different positions on the client
     * and the server, so the attack can fail both the crosshair and the reach
     * check even though the player clearly clicked the ball they saw. Called
     * from the arm swing event; BallListener deduplicates it against a real
     * attack in the same tick. Same tool rules, cooldowns and penalties apply
     * because the hit still flows through onPlayerHit.
     */
    public boolean tryReachHit(Player player) {
        if (!swingFallback || ball == null || !ball.isValid() || ball.isDead()
                || current == null || awaitingServe) {
            return false;
        }

        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection();
        if (look.lengthSquared() < 1.0E-6) {
            return false;
        }
        look = look.normalize();

        Vector toBall = current.toVector()
                .add(new Vector(0.0, ballHeight() / 2.0, 0.0))
                .subtract(eye.toVector());
        double along = toBall.dot(look);
        if (along <= 0.0) {
            return false;
        }

        // Crosshair must pass within the ball's surface plus however far the
        // ball dropped since the client last saw it.
        double tolerance = Math.max(ballRadius(), ballHeight() / 2.0)
                + Math.min(motion.length() * swingSpeedTolerance, 4.0);
        if (toBall.crossProduct(look).length() > tolerance) {
            return false;
        }
        // Distance from the eye to the ball's surface must fit the player's
        // interaction range (includes the game's tools.reach-bonus).
        if (toBall.length() - tolerance > interactionReach(player)) {
            return false;
        }

        // No swinging through walls or the net: a block clearly in front of
        // the ball blocks the fallback. The one block slack keeps the floor
        // the ball rests on from counting as cover when aiming downwards.
        RayTraceResult hit = eye.getWorld().rayTraceBlocks(eye, look, along,
                FluidCollisionMode.NEVER, true);
        if (hit != null && hit.getHitPosition() != null
                && hit.getHitPosition().distance(eye.toVector()) < along - 1.0) {
            return false;
        }
        return true;
    }

    private double interactionReach(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        double range = instance != null ? instance.getValue() : 3.0;
        return Math.max(3.0, range) + swingReachBuffer;
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
            Location displayAt = displayLocation();
            if (displayAt != null) {
                ballDisplay.teleport(displayAt);
            }
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

        // The slime underneath is always an invisible physics hitbox; the
        // floating head display is the ball everyone sees on every client
        // version - there is no per-client visibility handling anymore.
        ball.setInvisible(true);
        showDisplay(ballItem(tintedBallMaterial()));
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

    /**
     * Where the visible head belongs. A display is centred on its own
     * position, so it is lifted by half its rendered height - with the scale
     * below that is half the physics ball height - which puts the head's
     * bottom exactly on the ground line (instead of half the ball clipping
     * below the floor) and its top right under the name hologram.
     */
    private @Nullable Location displayLocation() {
        Location base = current != null ? current : (ball != null ? ball.getLocation() : null);
        if (base == null) {
            return null;
        }
        return base.clone().add(0.0, ballHeight() / 2.0, 0.0);
    }

    /**
     * Scales the player head so its rendered height equals the physics ball
     * height - this is what makes ball.size affect the visible ball, not just
     * the invisible hitbox.
     */
    private Transformation headTransformation() {
        float scale = (float) (ballHeight() / HEAD_BASE_SIZE);
        return new Transformation(new Vector3f(), new Quaternionf(),
                new Vector3f(scale, scale, scale), new Quaternionf());
    }

    private void showDisplay(ItemStack item) {
        if (ball == null || !ball.isValid()) {
            return;
        }
        if (ballDisplay != null && ballDisplay.isValid()) {
            ballDisplay.setItemStack(item);
            ballDisplay.setTransformation(headTransformation());
            return;
        }
        Location spawnAt = displayLocation();
        if (spawnAt == null) {
            return;
        }
        ballDisplay = ball.getWorld().spawn(spawnAt, ItemDisplay.class,
                CreatureSpawnEvent.SpawnReason.CUSTOM, display -> {
                    display.setItemStack(item);
                    display.setPersistent(false);
                    display.setInvulnerable(true);
                    display.setGravity(false);
                    display.setSilent(true);
                    display.setBillboard(Display.Billboard.FIXED);
                    display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    display.setCustomNameVisible(false);
                    display.getPersistentDataContainer().set(ballKey, PersistentDataType.BOOLEAN, true);
                    display.setTransformation(headTransformation());
                    // Glide over one tick so the twin tracks the ball smoothly.
                    display.setTeleportDuration(1);
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
            removeNameDisplay();
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

        // The ball mob is always invisible and clients never render a name tag
        // on an invisible entity, so the name always floats on the TextDisplay
        // hologram - it shows for every player on every version.
        ball.setCustomNameVisible(false);
        showNameDisplay(name);
    }

    private @Nullable Location nameDisplayLocation() {
        Location base = current != null ? current : (ball != null ? ball.getLocation() : null);
        if (base == null) {
            return null;
        }
        return base.clone().add(0.0, ballHeight() + 0.5, 0.0);
    }

    private void showNameDisplay(Component name) {
        Location location = nameDisplayLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }
        if (ballNameDisplay != null && ballNameDisplay.isValid()) {
            ballNameDisplay.text(name);
            ballNameDisplay.teleport(location);
            return;
        }
        removeNameDisplay();
        ballNameDisplay = location.getWorld().spawn(location, TextDisplay.class,
                CreatureSpawnEvent.SpawnReason.CUSTOM, display -> {
                    display.text(name);
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setAlignment(TextDisplay.TextAlignment.CENTER);
                    display.setPersistent(false);
                    display.setInvulnerable(true);
                    display.setGravity(false);
                    display.setSilent(true);
                    display.setShadowed(false);
                    display.setSeeThrough(false);
                    display.setDefaultBackground(false);
                    display.setBackgroundColor(Color.fromARGB(0));
                    display.getPersistentDataContainer().set(ballKey, PersistentDataType.BOOLEAN, true);
                    // 1 = the client glides to each new position over one tick,
                    // matching how it interpolates the ball entity itself.
                    // 0 would snap to the raw server position and look laggy.
                    display.setTeleportDuration(1);
                    display.setCustomNameVisible(false);
                });
    }

    private void removeNameDisplay() {
        if (ballNameDisplay != null) {
            ballNameDisplay.remove();
            ballNameDisplay = null;
        }
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
        removeNameDisplay();
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
        removeNameDisplay();
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
        removeNameDisplay();
        resetShieldCooldown();
        if (ball != null) {
            ball.remove();
            ball = null;
        }
        current = null;
        motion = new Vector();
    }

    /**
     * Removes leftover ball entities from the world on startup. Ball entities
     * are non-persistent now so they can never be saved again, but ghosts
     * written to disk by older builds (force-stops before onDisable ran) still
     * need to be swept: tagged entities first, then the legacy signature of an
     * AI-disabled invisible/named ball mob.
     */
    public void purgeGhosts() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isGhostBall(entity)) {
                    entity.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Removed " + removed + " leftover volleyball ball entity(ies) from the world.");
        }
    }

    boolean isGhostBall(Entity entity) {
        if (ball != null && entity.getUniqueId().equals(ball.getUniqueId())) {
            return false;
        }
        if (entity.getPersistentDataContainer().has(ballKey, PersistentDataType.BOOLEAN)) {
            return true;
        }
        // Signature of ghosts saved by builds before ball entities were tagged
        // and made non-persistent: a ball mob with AI switched off that is
        // either invisible or carried the touch counter as its custom name.
        if (!(entity instanceof Slime) && !(SULFUR_CUBE_SUPPORTED && entity instanceof SulfurCube)) {
            return false;
        }
        Mob mob = (Mob) entity;
        return !mob.isAware() && (mob.isInvisible() || mob.customName() != null);
    }
}
