package me.heroicstudios.heroicvolleyball.ball;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Arena;
import me.heroicstudios.heroicvolleyball.arena.Region;
import me.heroicstudios.heroicvolleyball.arena.Side;
import me.heroicstudios.heroicvolleyball.game.Countdown;
import me.heroicstudios.heroicvolleyball.game.GameSession;
import me.heroicstudios.heroicvolleyball.message.Messages;
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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
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

    /** Below this speed a grounded ball is treated as at rest and its physics are skipped. */
    private static final double REST_EPSILON = 1.0E-4;

    /** Ticks the ball visually pulses after a hit. */
    private static final int HIT_PULSE_TICKS = 4;

    private static final double TWO_PI = Math.PI * 2.0;

    /** How far down the shadow particle ring looks for the floor. */
    private static final double SHADOW_RAY_LENGTH = 12.0;

    /**
     * Rendered height of a player head in an ItemDisplay at scale 1 (the head
     * block model is half a block tall). The display's scale factor is derived
     * from this so the visible head always matches the physics ball height.
     */
    private static final double HEAD_BASE_SIZE = 0.5;

    private enum State {
        IDLE,
        RALLY
    }

    private final HeroicVolleyBall plugin;
    private final GameSession session;
    private final Arena arena;
    private final ScoreManager scoreManager;
    private final ToolManager toolManager;
    private final NamespacedKey ballKey;

    private @Nullable Entity ball;
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
    private double bounceFactor = 0.3;
    private double settleThreshold = 0.12;
    private int serveResetDelay = 10;
    private int pointCountdown = 3;
    private boolean touchPenalty = true;
    private boolean showName = true;
    private String nameBackground = "#E6141A20";
    private @Nullable String appliedNameBackground;
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
    private @Nullable Side pickupTeam;
    private @Nullable UUID pickupHolder;
    private @Nullable Side possession;
    private @Nullable Side lastHitter;
    private int lastHitTick = -100;

    private int stuckTimeoutTicks = 30 * 20;
    private int outOfBoundsTicks;
    private int idleRallyTicks;
    private boolean spinEnabled = true;
    private boolean shadowEnabled = true;
    private boolean trailEnabled = true;
    private double trailSpeed = 0.6;
    private boolean hitPulseEnabled = true;
    private boolean scaledHitSound = true;
    private float spinAngle;
    private int pulseTicks;
    private float appliedSpinAngle = Float.NaN;
    private int appliedPulse = -1;
    private @Nullable Component lastNameText;
    private @Nullable Location lastNamePos;

    public BallManager(HeroicVolleyBall plugin, GameSession session, ToolManager toolManager) {
        this.plugin = plugin;
        this.session = session;
        this.arena = session.getArena();
        this.scoreManager = session.getScore();
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
        this.settleThreshold = config.getDouble("ball.settle-threshold", 0.12);
        this.stuckTimeoutTicks = Math.max(0, config.getInt("ball.stuck-timeout", 30)) * 20;
        this.spinEnabled = config.getBoolean("ball.visual.spin", true);
        this.shadowEnabled = config.getBoolean("ball.visual.shadow", true);
        this.trailEnabled = config.getBoolean("ball.visual.trail", true);
        this.trailSpeed = Math.max(0.0, config.getDouble("ball.visual.trail-speed", 0.6));
        this.hitPulseEnabled = config.getBoolean("ball.visual.hit-pulse", true);
        this.scaledHitSound = config.getBoolean("ball.visual.scaled-hit-sound", true);
        this.serveResetDelay = Math.max(0, config.getInt("ball.serve-reset-delay", 10));
        this.pointCountdown = Math.max(0, config.getInt("game.point-countdown", 3));
        this.touchPenalty = config.getBoolean("game.opponent-touch-penalty", true);
        this.showName = config.getBoolean("ball.show-name", true);
        this.nameBackground = config.getString("ball.name-background", "#E6141A20");
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
        lastNameText = null;
        lastNamePos = null;
        applyBallAppearance();
        updateName();
    }

    public @Nullable Entity getBall() {
        return ball;
    }

    public int getTouches() {
        return Math.max(touches, 0);
    }

    public int getMaxTouches() {
        return maxTouches;
    }

    public @Nullable Side getPossession() {
        return possession;
    }

    public String getStateName() {
        return state == State.RALLY ? "Rally" : "Serve";
    }

    public boolean isBall(@Nullable Entity entity) {
        return entity != null && ball != null && entity.getUniqueId().equals(ball.getUniqueId());
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
        Side side = possession;
        if (side == null && ball != null && ball.isValid()) {
            side = arena.getCourtSide(ball.getLocation());
        }
        if (side != null) {
            Region backrow = arena.getBackrow(side);
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

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
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
        Region region = placementRegion(pickupTeam);
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

    private @Nullable Region placementRegion(Side side) {
        Region backrow = arena.getBackrow(side);
        if (backrow != null) {
            return backrow;
        }
        return arena.getCourt(side);
    }

    public void spawnAtPlayer(Player player) {
        Location spawn = null;
        Side side = arena.getCourtSide(player.getLocation());
        if (side != null) {
            spawn = arena.getBallSpawn(side);
        }
        if (spawn == null) {
            spawn = player.getLocation().add(0.0, 0.5, 0.0);
        }
        spawnBall(spawn);
    }

    public boolean spawnAtStart() {
        return spawnAtStartTeam() != null;
    }

    public @Nullable Side spawnAtStartTeam() {
        if (resetToServe(Side.RED)) {
            return Side.RED;
        }
        if (resetToServe(Side.BLUE)) {
            return Side.BLUE;
        }
        return null;
    }

    public boolean resetToServe(Side side) {
        Location base = arena.getBallSpawn(side);
        if (base == null) {
            base = arena.getPosition(side, 6);
        }
        if (base == null) {
            base = arena.centerFloor(side);
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
        takeBackHeldBall();

        World world = location.getWorld();
        if (world == null) {
            return;
        }

        // The ball is an Interaction entity: a click-only hitbox with no
        // rendered model, no AI and no physics of its own. Its width/height
        // resize with ball.size and its box matches the physics AABB used by
        // collides() exactly (bottom on the ball's y, horizontally centred).
        // The floating player head ItemDisplay above it is the ball everyone
        // sees on every client version.
        this.ball = world.spawn(location, Interaction.class, CreatureSpawnEvent.SpawnReason.CUSTOM, interaction -> {
            interaction.setInteractionWidth((float) (ballRadius() * 2.0));
            interaction.setInteractionHeight((float) ballHeight());
            // Swinging at the ball plays the normal arm-swing feedback.
            interaction.setResponsive(true);
            // Non-persistent: never written to the world save, so a crash
            // or force-stop can never leave a ghost ball behind.
            interaction.setPersistent(false);
            interaction.getPersistentDataContainer().set(ballKey, PersistentDataType.BOOLEAN, true);
        });

        this.current = location.clone();
        this.motion = new Vector();
        this.state = State.IDLE;
        this.touches = maxTouches;
        this.wasOnGround = false;
        this.awaitingServe = false;
        this.placed = false;
        this.pickupTeam = null;
        this.pickupHolder = null;
        this.lastHitTick = -100;
        this.spinAngle = 0.0f;
        this.pulseTicks = 0;
        this.outOfBoundsTicks = 0;
        this.idleRallyTicks = 0;
        this.possession = arena.getCourtSide(location);
        this.lastHitter = null;
        resetShieldCooldown();
        applyBallAppearance();
        updateName();
        startTask();
    }

    public void takeBackHeldBall() {
        Player holder = pickupHolder != null ? Bukkit.getPlayer(pickupHolder) : null;
        takeBackHeldBall(holder);
    }

    /**
     * Removes any picked-up ball item from the given player's inventory and
     * forgets the held ball. Used when the holder quits or dies so the ball
     * item can never be kept across a session or dropped on death.
     */
    public void takeBackHeldBall(@Nullable Player holder) {
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

        Side playerTeam = resolveTeam(player);

        Side ballSide = possession;
        boolean penaltyActive = touchPenalty && session.isActive();
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

        if (tool == VolleyTool.SHIELD
                && (shieldCooldownTicks > 0 || player.hasCooldown(Material.SHIELD))) {
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

    private @Nullable Side resolveTeam(Player player) {
        Side side = session.getSide(player);
        if (side != null) {
            return side;
        }
        return arena.getCourtSide(player.getLocation());
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
        pulseTicks = HIT_PULSE_TICKS;
        updateName();

        playHitSound(current, Sound.BLOCK_WOOL_PLACE, 1.0f, 1.2f);
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
        pulseTicks = HIT_PULSE_TICKS;
        updateName();

        playHitSound(current, Sound.BLOCK_WOOL_BREAK, 1.0f, 0.9f);
        plugin.getMessages().actionBar(player, "ball.spike");
    }

    private void tick() {
        if (ball == null || !ball.isValid() || ball.isDead() || current == null) {
            cleanup();
            return;
        }

        World world = current.getWorld();
        if (world == null) {
            cleanup();
            return;
        }
        // Freeze the whole simulation while the ball's chunk is unloaded so we
        // never touch or generate blocks and never teleport in the dark.
        if (!world.isChunkLoaded(current.getBlockX() >> 4, current.getBlockZ() >> 4)) {
            return;
        }

        if (shieldCooldownTicks > 0) {
            shieldCooldownTicks--;
        }

        tryShieldCatch();

        // A grounded ball that has settled needs no integration or block
        // scans; only wake it back up when something moves it (a hit resets
        // wasOnGround/motion) or a new possession change is pending.
        if (!resting()) {
            integrate();
            updatePossession();
            step();
        }

        if (stuckTimeoutTicks > 0) {
            if (arena.getCourtSide(current) == null) {
                outOfBoundsTicks++;
            } else {
                outOfBoundsTicks = 0;
            }
            if (state == State.RALLY && resting()) {
                idleRallyTicks++;
            } else {
                idleRallyTicks = 0;
            }
            if (outOfBoundsTicks > stuckTimeoutTicks) {
                failsafeReset("out of bounds");
                return;
            }
            if (idleRallyTicks > stuckTimeoutTicks) {
                failsafeReset("idle during rally");
                return;
            }
        } else {
            outOfBoundsTicks = 0;
            idleRallyTicks = 0;
        }

        updateVisuals();
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
                Side scorer = resolveScorer();
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

    private boolean resting() {
        return wasOnGround && motion.lengthSquared() < REST_EPSILON;
    }

    /**
     * Advances the purely cosmetic ball state (spin, hit pulse) and the
     * client-side particles each tick. Runs even while the ball is resting so
     * a hit pulse still decays and the display transform stays correct.
     */
    private void updateVisuals() {
        if (spinEnabled) {
            double speed = motion.length();
            if (speed > 1.0E-4) {
                spinAngle = (float) ((spinAngle + speed * 0.5) % TWO_PI);
            }
        }
        if (pulseTicks > 0) {
            pulseTicks--;
        }
        if (ballDisplay != null && ballDisplay.isValid()
                && (spinAngle != appliedSpinAngle || pulseTicks != appliedPulse)) {
            ballDisplay.setTransformation(headTransformation());
            appliedSpinAngle = spinAngle;
            appliedPulse = pulseTicks;
        }
        spawnShadow();
        spawnTrail();
    }

    /**
     * A small ring of dark dust on the floor under the ball so players can tell
     * where it will land, found by tracing straight down from the ball.
     */
    private void spawnShadow() {
        if (!shadowEnabled || current == null) {
            return;
        }
        World world = current.getWorld();
        if (world == null) {
            return;
        }
        RayTraceResult hit = world.rayTraceBlocks(current, new Vector(0.0, -1.0, 0.0),
                SHADOW_RAY_LENGTH, FluidCollisionMode.NEVER, true);
        if (hit == null || hit.getHitPosition() == null) {
            return;
        }
        Vector ground = hit.getHitPosition();
        double radius = ballRadius();
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(25, 25, 30),
                (float) Math.max(0.4, radius));
        for (int i = 0; i < 10; i++) {
            double angle = TWO_PI / 10.0 * i;
            world.spawnParticle(Particle.DUST,
                    ground.getX() + Math.cos(angle) * radius,
                    ground.getY() + 0.05,
                    ground.getZ() + Math.sin(angle) * radius,
                    1, 0.0, 0.0, 0.0, 0.0, dust);
        }
    }

    private void spawnTrail() {
        if (!trailEnabled || current == null || motion.length() < trailSpeed) {
            return;
        }
        World world = current.getWorld();
        if (world == null) {
            return;
        }
        Location center = displayLocation();
        if (center == null) {
            return;
        }
        world.spawnParticle(Particle.END_ROD, center, 1, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Plays a hit sound whose volume and pitch scale with how hard the ball was
     * struck, so a soft bump and a full-power spike do not sound identical.
     */
    private void playHitSound(@Nullable Location at, Sound sound, float volume, float pitch) {
        if (at == null || at.getWorld() == null) {
            return;
        }
        float vol = volume;
        float pit = pitch;
        if (scaledHitSound) {
            double norm = Math.min(1.0, motion.length() / Math.max(0.001, maxVelocity));
            vol = (float) (volume * (0.6 + 0.5 * norm));
            pit = (float) (pitch * (0.85 + 0.45 * norm));
        }
        at.getWorld().playSound(at, sound, vol, pit);
    }

    /**
     * Last-resort recovery when the ball never returns to play (launched out of
     * bounds or wedged during a rally). Hands the serve to the side that last
     * had the ball so the game keeps going.
     */
    private void failsafeReset(String reason) {
        Side team = possession != null ? possession : (lastHitter != null ? lastHitter : Side.RED);
        outOfBoundsTicks = 0;
        idleRallyTicks = 0;
        plugin.getLogger().warning("Volleyball ball was stuck (" + reason + ") - resetting the serve.");
        resetRally();
        if (resetToServe(team) && session.isActive()) {
            session.teleportToPositions(team);
        }
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

        // Never touch blocks in an unloaded chunk - getBlockAt would generate
        // them. A ball straddling a chunk border is treated as free until its
        // neighbours finish loading.
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    return false;
                }
            }
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
        pulseTicks = HIT_PULSE_TICKS;
        startShieldCooldown(player);
        updateName();

        playHitSound(current, Sound.BLOCK_WOOL_BREAK, 1.0f, 0.9f);
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
        World world = current.getWorld();
        if (world == null) {
            return;
        }
        // Only players close enough to reach the ball can catch it.
        for (Player player : world.getNearbyPlayers(current, shieldCatchReach + 1.0)) {
            if (!session.contains(player)) {
                continue;
            }
            if (!player.isBlocking() || player.hasCooldown(Material.SHIELD)) {
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
        pulseTicks = HIT_PULSE_TICKS;
        if (state == State.IDLE) {
            state = State.RALLY;
        }
        startShieldCooldown(player);
        updateName();

        playHitSound(current, Sound.BLOCK_WOOL_BREAK, 1.0f, 1.3f);
        plugin.getMessages().actionBar(player, "ball.shield-catch");
    }

    private void startShieldCooldown(Player player) {
        if (shieldCooldown <= 0) {
            shieldCooldownTicks = 0;
            return;
        }
        shieldCooldownTicks = shieldCooldown;
        if (player != null) {
            // Native shield-disable animation/cooldown: the client greys the
            // shield out until it recharges, exactly like a vanilla axe hit.
            player.setCooldown(Material.SHIELD, shieldCooldown);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
        }
    }

    private void resetShieldCooldown() {
        shieldCooldownTicks = 0;
        for (Player player : session.players()) {
            if (player.hasCooldown(Material.SHIELD)) {
                player.setCooldown(Material.SHIELD, 0);
            }
        }
    }

    private void updatePossession() {
        Side currentSide = arena.getCourtSide(current);
        if (currentSide == possession) {
            return;
        }
        possession = currentSide;
        touches = maxTouches;
        applyBallAppearance();
    }

    private void applyBallAppearance() {
        if (ball == null || !ball.isValid()) {
            return;
        }

        // The Interaction hitbox is never rendered; the floating head display
        // is the ball everyone sees on every client version.
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
     * the invisible hitbox - and applies the current spin/hit-pulse rotation.
     */
    private Transformation headTransformation() {
        float scale = (float) (ballHeight() / HEAD_BASE_SIZE);
        float pulse = hitPulseEnabled ? hitPulseScale() : 1.0f;
        float s = scale * pulse;
        Quaternionf rotation = new Quaternionf();
        if (spinEnabled) {
            double horizontal = Math.hypot(motion.getX(), motion.getZ());
            if (horizontal > 1.0E-4) {
                // Roll about the axis perpendicular to the horizontal travel
                // direction so the head turns the way it is moving.
                Vector3f axis = new Vector3f((float) motion.getZ(), 0.0f, (float) -motion.getX()).normalize();
                rotation.rotationAxis(spinAngle, axis);
            }
        }
        return new Transformation(new Vector3f(), rotation,
                new Vector3f(s, s, s), new Quaternionf());
    }

    private float hitPulseScale() {
        return 1.0f + 0.15f * pulseTicks / HIT_PULSE_TICKS;
    }

    private void showDisplay(ItemStack item) {
        if (ball == null || !ball.isValid()) {
            return;
        }
        if (ballDisplay != null && ballDisplay.isValid()) {
            ballDisplay.setItemStack(item);
            ballDisplay.setTransformation(headTransformation());
            appliedSpinAngle = spinAngle;
            appliedPulse = pulseTicks;
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
                    display.getPersistentDataContainer().set(ballKey, PersistentDataType.BOOLEAN, true);
                    display.setTransformation(headTransformation());
                    // Glide over one tick so the visible head tracks the ball smoothly.
                    display.setTeleportDuration(1);
                });
        appliedSpinAngle = spinAngle;
        appliedPulse = pulseTicks;
    }

    private void removeDisplay() {
        if (ballDisplay != null) {
            ballDisplay.remove();
            ballDisplay = null;
        }
        appliedSpinAngle = Float.NaN;
        appliedPulse = -1;
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

    private @Nullable Side resolveScorer() {
        if (lastHitter == null) {
            return null;
        }

        Side landingSide = arena.getCourtSide(current);
        boolean landedOnOpponent = landingSide != null && landingSide != lastHitter;
        return landedOnOpponent ? lastHitter : lastHitter.opposite();
    }

    private boolean scorePoint(@Nullable Side scorer) {
        if (scorer == null || awaitingServe) {
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

    private void scheduleServeReset(Side team) {
        if (serveResetDelay <= 0) {
            beginServeCountdown(team);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> beginServeCountdown(team), serveResetDelay);
        }
    }

    private void beginServeCountdown(Side team) {
        if (pointCountdown <= 0) {
            performServeReset(team);
            return;
        }
        Countdown.run(plugin, pointCountdown, session.players(),
                "game.point-countdown-title", "game.point-countdown-subtitle",
                () -> performServeReset(team), "team", session.teamDisplay(team));
    }

    private void performServeReset(Side team) {
        if (state == State.RALLY) {
            return;
        }
        resetToServe(team);
        session.teleportToPositions(team);
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
            removeNameDisplay();
            return;
        }

        Messages messages = plugin.getMessages();
        Component name;
        if (state == State.IDLE) {
            name = messages.get("ball.name-serve");
        } else {
            String color = possession != null ? session.teamColor(possession).asHexString() : "gray";
            name = messages.get("ball.name-touches",
                    "color", color,
                    "touches", String.valueOf(Math.max(touches, 0)),
                    "max", String.valueOf(maxTouches));
        }

        // The name always floats on the TextDisplay hologram so it shows for
        // every player on every client version.
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
            if (!nameBackground.equals(appliedNameBackground)) {
                applyNameBackground(ballNameDisplay);
            }
            // Only push a packet when something actually changed - a resting
            // ball otherwise re-sends text/position every single tick.
            if (lastNameText == null || !lastNameText.equals(name)) {
                ballNameDisplay.text(name);
                lastNameText = name;
            }
            if (lastNamePos == null || !lastNamePos.equals(location)) {
                ballNameDisplay.teleport(location);
                lastNamePos = location.clone();
            }
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
                    applyNameBackground(display);
                    display.getPersistentDataContainer().set(ballKey, PersistentDataType.BOOLEAN, true);
                    // 1 = the client glides to each new position over one tick,
                    // matching how it interpolates the ball entity itself.
                    // 0 would snap to the raw server position and look laggy.
                    display.setTeleportDuration(1);
                });
        lastNameText = name;
        lastNamePos = location.clone();
    }

    private void applyNameBackground(TextDisplay display) {
        Color background = nameBackgroundColor();
        display.setDefaultBackground(false);
        display.setBackgroundColor(background != null ? background : Color.fromARGB(0));
        appliedNameBackground = nameBackground;
    }

    /**
     * Parses ball.name-background: "#AARRGGBB" or "#RRGGBB" (alpha defaults to
     * 90%), or "none" for no plate at all. Invalid values warn once per value
     * and fall back to the default dark plate.
     */
    private @Nullable Color nameBackgroundColor() {
        if (nameBackground == null) {
            return null;
        }
        String hex = nameBackground.trim();
        if (hex.isEmpty() || hex.equalsIgnoreCase("none")) {
            return null;
        }
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            long argb = Long.parseUnsignedLong(hex, 16);
            if (hex.length() == 6) {
                argb |= 0xE6000000L;
            }
            return Color.fromARGB((int) argb);
        } catch (NumberFormatException e) {
            if (!nameBackground.equals(appliedNameBackground)) {
                plugin.getLogger().warning("Invalid ball.name-background '" + nameBackground
                        + "' - expected #RRGGBB, #AARRGGBB or none. Falling back to the default.");
            }
            return Color.fromARGB(0xE6141A20);
        }
    }

    private void removeNameDisplay() {
        if (ballNameDisplay != null) {
            ballNameDisplay.remove();
            ballNameDisplay = null;
        }
        appliedNameBackground = null;
        lastNameText = null;
        lastNamePos = null;
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
        pulseTicks = 0;
        outOfBoundsTicks = 0;
        idleRallyTicks = 0;
    }

    private void cleanup() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeBall();
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeBall();
    }
}
