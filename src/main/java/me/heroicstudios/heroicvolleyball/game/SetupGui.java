package me.heroicstudios.heroicvolleyball.game;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.arena.Team;
import me.heroicstudios.heroicvolleyball.gui.PositionLayout;
import me.heroicstudios.heroicvolleyball.message.Messages;
import me.heroicstudios.heroicvolleyball.party.Party;
import me.heroicstudios.heroicvolleyball.party.PartyManager;
import me.heroicstudios.heroicvolleyball.position.PositionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SetupGui implements Listener {

    private final HeroicVolleyBall plugin;
    private final GameManager gameManager;
    private final PartyManager partyManager;
    private final PositionManager positionManager;

    public SetupGui(HeroicVolleyBall plugin, GameManager gameManager, PartyManager partyManager, PositionManager positionManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.partyManager = partyManager;
        this.positionManager = positionManager;
    }

    public void open(Player leader) {
        Party party = partyManager.getParty(leader);
        if (party == null) {
            return;
        }

        Setup setup = new Setup();
        for (UUID uuid : party.getMembers()) {
            setup.players.add(uuid);
        }
        setup.maxPerTeam = setup.players.size() / 2;

        for (UUID uuid : setup.players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                openTeamMenu(setup, player);
            }
        }
    }

    public void openSolo(Player player) {
        Holder holder = new Holder(null, Stage.SOLO, player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, PositionLayout.SIZE, Component.text("Choose your position"));
        holder.inventory = inventory;
        renderSolo(holder);
        player.openInventory(inventory);
    }

    private void renderSolo(Holder holder) {
        Inventory inventory = holder.inventory;
        PositionLayout.fillFrame(inventory);
        for (int number = 1; number <= 6; number++) {
            ItemStack item = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("Position " + number, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            List<Component> lore = new ArrayList<>();
            lore.add(loreLine("You will stand here on whichever team is up", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
            inventory.setItem(PositionLayout.slot(number), item);
        }
    }

    private void openTeamMenu(Setup setup, Player player) {
        Holder holder = new Holder(setup, Stage.TEAM, player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, PositionLayout.SIZE, Component.text("Choose your team"));
        holder.inventory = inventory;
        renderTeam(holder);
        player.openInventory(inventory);
    }

    private void openPositionMenu(Setup setup, Player player, Team team) {
        Holder holder = new Holder(setup, Stage.POSITION, player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, PositionLayout.SIZE, Component.text("Choose your position"));
        holder.inventory = inventory;
        renderPosition(holder, team);
        player.openInventory(inventory);
    }

    private void renderTeam(Holder holder) {
        Inventory inventory = holder.inventory;
        PositionLayout.fillFrame(inventory);
        Setup setup = holder.setup;
        inventory.setItem(11, teamItem(Team.RED, setup));
        inventory.setItem(15, teamItem(Team.BLUE, setup));
        inventory.setItem(4, infoItem(setup));
    }

    private void renderPosition(Holder holder, Team team) {
        Inventory inventory = holder.inventory;
        PositionLayout.fillFrame(inventory);
        Setup setup = holder.setup;
        for (int number = 1; number <= 6; number++) {
            inventory.setItem(PositionLayout.slot(number), positionItem(setup, team, number));
        }
        inventory.setItem(4, infoItem(setup));
    }

    private ItemStack teamItem(Team team, Setup setup) {
        Material material = team == Team.RED ? Material.RED_WOOL : Material.BLUE_WOOL;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(team.getDisplayName() + " Team", team.getColor())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        int count = 0;
        for (Map.Entry<UUID, Team> entry : setup.teams.entrySet()) {
            if (entry.getValue() == team) {
                lore.add(loreLine(" - " + nameOf(entry.getKey()), team.getColor()));
                count++;
            }
        }
        if (count == 0) {
            lore.add(loreLine("No players yet", NamedTextColor.DARK_GRAY));
        }
        lore.add(loreLine(count + "/" + setup.maxPerTeam + " slots filled", NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack positionItem(Setup setup, Team team, int number) {
        Material material = team == Team.RED ? Material.RED_WOOL : Material.BLUE_WOOL;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Position " + number, team.getColor())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        Location location = positionManager.getPosition(team, number);
        if (location != null) {
            lore.add(loreLine("Set at " + location.getBlockX() + ", " + location.getBlockY() + ", "
                    + location.getBlockZ(), NamedTextColor.GRAY));
        } else {
            lore.add(loreLine("Not set yet", NamedTextColor.DARK_GRAY));
        }

        UUID occupant = occupantOf(setup, team, number);
        if (occupant != null) {
            lore.add(loreLine("Taken by " + nameOf(occupant), NamedTextColor.RED));
        } else {
            lore.add(loreLine("Click to choose", NamedTextColor.GREEN));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack infoItem(Setup setup) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Game setup", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(loreLine("Players: " + setup.players.size(), NamedTextColor.WHITE));
        lore.add(loreLine("Max per team: " + setup.maxPerTeam, NamedTextColor.GRAY));
        lore.add(loreLine("Pick a team, then a position.", NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Component loreLine(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private @Nullable UUID occupantOf(Setup setup, Team team, int number) {
        for (Map.Entry<UUID, Integer> entry : setup.positions.entrySet()) {
            if (entry.getValue() == number && setup.teams.get(entry.getKey()) == team) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String nameOf(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != top) {
            return;
        }

        if (holder.stage == Stage.SOLO) {
            int number = PositionLayout.position(event.getSlot());
            if (number < 1 || number > 6) {
                return;
            }
            player.closeInventory();
            gameManager.startSolo(player, number);
            plugin.getMessages().send(player, "game.solo-started", "position", String.valueOf(number));
            return;
        }

        Setup setup = holder.setup;
        if (holder.stage == Stage.TEAM) {
            Team team = teamAt(event.getSlot());
            if (team == null) {
                return;
            }
            if (setup.teams.containsKey(player.getUniqueId())) {
                return;
            }
            if (countTeam(setup, team) >= setup.maxPerTeam) {
                plugin.getMessages().send(player, "game.team-full");
                return;
            }
            setup.teams.put(player.getUniqueId(), team);
            refreshTeamMenus(setup);
            openPositionMenu(setup, player, team);
        } else {
            Team team = setup.teams.get(player.getUniqueId());
            if (team == null) {
                return;
            }
            if (setup.positions.containsKey(player.getUniqueId())) {
                return;
            }
            int number = PositionLayout.position(event.getSlot());
            if (number < 1 || number > 6) {
                return;
            }
            if (occupantOf(setup, team, number) != null) {
                plugin.getMessages().send(player, "game.position-taken");
                return;
            }
            setup.positions.put(player.getUniqueId(), number);
            player.closeInventory();
            plugin.getMessages().send(player, "game.chosen", "position", String.valueOf(number));
            maybeStart(setup);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        if (holder.stage != Stage.POSITION) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Setup setup = holder.setup;
        Team team = setup.teams.get(player.getUniqueId());
        if (team == null || setup.positions.containsKey(player.getUniqueId())) {
            return;
        }
        if (setup.teams.size() != setup.players.size() || setup.positions.size() != setup.players.size()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && !setup.positions.containsKey(player.getUniqueId())) {
                    openPositionMenu(setup, player, team);
                }
            });
        }
    }

    private void maybeStart(Setup setup) {
        if (setup.teams.size() != setup.players.size() || setup.positions.size() != setup.players.size()) {
            return;
        }

        Map<UUID, Team> teams = new LinkedHashMap<>(setup.teams);
        Map<UUID, Integer> positions = new LinkedHashMap<>(setup.positions);

        for (UUID uuid : setup.players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.closeInventory();
            }
        }

        gameManager.start(teams, positions);

        for (UUID uuid : setup.players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getMessages().send(player, "game.assigned");
            }
            Team team = teams.get(uuid);
            if (team != null) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    String teamTag = Messages.teamDisplay(team);
                    p.showTitle(net.kyori.adventure.title.Title.title(
                            plugin.getMessages().get("game.assigned-title", "team", teamTag),
                            plugin.getMessages().get("game.assigned-subtitle", "team", teamTag)));
                }
            }
        }
    }

    private void refreshTeamMenus(Setup setup) {
        for (UUID uuid : setup.players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            Inventory open = player.getOpenInventory().getTopInventory();
            if (open.getHolder() instanceof Holder holder
                    && holder.setup == setup
                    && holder.stage == Stage.TEAM) {
                renderTeam(holder);
                player.updateInventory();
            }
        }
    }

    private int countTeam(Setup setup, Team team) {
        int count = 0;
        for (Team value : setup.teams.values()) {
            if (value == team) {
                count++;
            }
        }
        return count;
    }

    private @Nullable Team teamAt(int slot) {
        if (slot == 11) {
            return Team.RED;
        }
        if (slot == 15) {
            return Team.BLUE;
        }
        return null;
    }

    private enum Stage {
        TEAM,
        POSITION,
        SOLO
    }

    private static final class Setup {
        private final Set<UUID> players = new java.util.LinkedHashSet<>();
        private final Map<UUID, Team> teams = new LinkedHashMap<>();
        private final Map<UUID, Integer> positions = new LinkedHashMap<>();
        private int maxPerTeam = 1;
    }

    private static final class Holder implements InventoryHolder {
        private final @Nullable Setup setup;
        private final Stage stage;
        @SuppressWarnings("unused")
        private final UUID viewer;
        private Inventory inventory;

        private Holder(@Nullable Setup setup, Stage stage, UUID viewer) {
            this.setup = setup;
            this.stage = stage;
            this.viewer = viewer;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
