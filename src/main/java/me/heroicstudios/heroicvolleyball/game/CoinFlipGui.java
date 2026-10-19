package me.heroicstudios.heroicvolleyball.game;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Side;
import me.heroicstudios.heroicvolleyball.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class CoinFlipGui implements Listener {

    private static final int CENTER = 13;

    private static final int[] DELAYS = {2, 2, 2, 2, 2, 3, 3, 4, 5, 6, 8, 10};

    private static final Material[] GLASS = {
            Material.WHITE_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE,
            Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE,
            Material.PINK_STAINED_GLASS_PANE,
            Material.CYAN_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE
    };

    private final HeroicVolleyBall plugin;
    private final Messages messages;
    private final GameSession session;
    private final Inventory inventory;
    private final Holder holder = new Holder();
    private final List<Player> viewers = new ArrayList<>();

    private boolean flipping;
    private int generation;

    public CoinFlipGui(HeroicVolleyBall plugin, Messages messages, GameSession session) {
        this.plugin = plugin;
        this.messages = messages;
        this.session = session;
        this.inventory = Bukkit.createInventory(holder, 27, Component.text("Coin Flip", NamedTextColor.GOLD));
        holder.inventory = inventory;
    }

    public boolean isFlipping() {
        return flipping;
    }

    public void flip(Collection<Player> players, Consumer<Side> onResult) {
        if (flipping) {
            return;
        }
        flipping = true;
        int gen = ++generation;
        viewers.clear();
        viewers.addAll(players);

        Side result = ThreadLocalRandom.current().nextBoolean() ? Side.RED : Side.BLUE;
        fillFrame();
        inventory.setItem(CENTER, wool(Side.RED));

        for (Player player : viewers) {
            if (player != null && player.isOnline()) {
                player.openInventory(inventory);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }
        }
        animate(0, result, gen, onResult);
    }

    public void cancel() {
        generation++;
        flipping = false;
        closeViewers();
    }

    private void animate(int frame, Side result, int gen, Consumer<Side> onResult) {
        if (gen != generation) {
            return;
        }
        if (frame >= DELAYS.length) {
            reveal(result, gen, onResult);
            return;
        }

        Material material = GLASS[ThreadLocalRandom.current().nextInt(GLASS.length)];
        inventory.setItem(CENTER, glass(material));

        for (Player player : viewers) {
            if (player != null && player.isOnline()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
            }
        }

        int delay = DELAYS[frame];
        Bukkit.getScheduler().runTaskLater(plugin, () -> animate(frame + 1, result, gen, onResult), delay);
    }

    private void reveal(Side result, int gen, Consumer<Side> onResult) {
        if (gen != generation) {
            return;
        }

        inventory.setItem(CENTER, wool(result));

        Component title = messages.get("game.coin-flip-title");
        Component subtitle = messages.get("game.coin-flip-result", "team", session.teamDisplay(result));
        for (Player player : viewers) {
            if (player != null && player.isOnline()) {
                player.showTitle(Messages.noFade(title, subtitle));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
            }
        }
        session.broadcast(messages.getPrefixed("game.coin-flip-broadcast", "team", session.teamDisplay(result)));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (gen != generation) {
                return;
            }
            closeViewers();
            flipping = false;
            onResult.accept(result);
        }, 40L);
    }

    private void closeViewers() {
        for (Player player : viewers) {
            if (player != null && player.isOnline()
                    && player.getOpenInventory().getTopInventory().getHolder() == holder) {
                player.closeInventory();
            }
        }
        viewers.clear();
    }

    private void fillFrame() {
        ItemStack pane = glass(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, pane);
        }
    }

    private ItemStack glass(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack wool(Side side) {
        ItemStack item = new ItemStack(side.getWool());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Serves first", session.getArena().teamColor(side))
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private static final class Holder implements InventoryHolder {

        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
