# HeroicVolleyBall

A Haikyuu-inspired volleyball plugin for Paper. Build a court, place six positions
per team, and rally a sulfur-cube ball using shovels, swords and shields. Matches
can be started directly, played as team duels, or run as a public queue with
lobby signs, countdowns and automatic side balancing.

## Requirements

- Paper `1.21+` (built against Paper API `26.3`)
- Java 21 or newer at runtime
- JDK 25 to build from source (Paper API 26.3 requires it; the jar itself
  targets Java 21 bytecode)
- Optional: [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
  for the `%hvb_*%` placeholders

## Installation

1. Drop the plugin jar into your `plugins/` folder.
2. Start the server once to generate `config.yml`, `messages.yml`, `gui.yml` and
   the `arenas/` folder.
3. Edit `plugins/HeroicVolleyBall/config.yml` for physics, match and queue rules,
   `messages.yml` for every player-facing text, and `gui.yml` for menu layouts.
4. Reload with `/vb admin reload` (or `/volleyreload`) after editing (a restart also works).

## Features

- Arena-based courts with per-team regions, back rows, ball spawn points and six
  player positions, stored one file per arena.
- Bump, spike, block and catch mechanics with configurable tool tiers, physics,
  hit detection and visual polish.
- Sets, points, three-touch possessions, opponent-touch penalties, titles,
  sounds and a live sidebar scoreboard.
- Persistent teams with roles, colours, stats, invites, a team screen and chat.
- Leader-driven team duels with lineups, results screens and team stats.
- Public queue mode: join signs, lobby spawn, live sign status, countdown,
  side balancing, team queueing, leave item and reconnect grace.
- Every message and menu is configurable; new defaults are merged into existing
  files without overwriting your values.

## Quick start

1. `/vb admin arena create main`
2. `/vb admin wand`, then select each team's court half and run
   `/vb admin setarena red main` and `/vb admin setarena blue main`.
3. Sneak + right-click with the wand to set positions `1`–`6` per team.
4. Optional: `/vb admin setbackrow <red|blue> main` and
   `/vb admin setballspawnpoint <red|blue> main`.
5. `/vb admin arena info main` until it reports ready, then
   `/vb admin start main`.

All of this is also reachable from the admin panel: `/vb admin panel` (or
`/vb menu` → Admin Panel for staff).

For a public queue on that arena, see [Public queue mode](#public-queue-mode).

## Commands

### Main command (`/vb`, alias `/volley`)

| Command | Description |
| --- | --- |
| `/vb` or `/vb help [page]` | Permission-filtered help menu (also `/volleyhelp`). |
| `/vb menu` | Opens the player hub: guide, team, queue, duel, info and (for staff) admin. |
| `/vb guide` | Opens the paged gameplay guide. |
| `/vb team ...` | Same as `/volleyteam` (create, invite, roles, queueing, chat...). |
| `/vb duel <team> <1\|3\|5>` | Same as `/teamduel`. |
| `/vb join [arena]` | Same as `/volleyjoin`. |
| `/vb leave` | Same as `/volleyleave`. |
| `/vb admin panel` | Opens the admin control panel (arena list, tools, reload). |
| `/vb admin guide` | Opens the paged setup guide for staff. |
| `/vb admin wand\|arena\|setarena\|setbackrow\|setballspawnpoint\|spawnball\|start\|stop\|restart\|give\|music\|reload ...` | Forwards to the matching setup command below. |

The legacy commands below remain registered and behave exactly like their `/vb`
equivalents, so existing scripts, signs and documentation keep working.

### Game and arena

| Command | Description | Permission |
| --- | --- | --- |
| `/volleywand` | Gives the selection wand (left-click point 1, right-click point 2, sneak + right-click to open the position menu). | `heroicvolleyball.volleywand` |
| `/setarena <red\|blue> [direction] [arena]` | Sets the selected region as a team's court and the direction that team faces. | `heroicvolleyball.setarena` |
| `/setbackrow <red\|blue> [arena]` | Sets the selected region as a team's back row. The ball can only be picked up and placed there. | `heroicvolleyball.setbackrow` |
| `/setballspawnpoint <red\|blue> [clear] [arena]` | Saves your current location as the team's serve origin. | `heroicvolleyball.setballspawnpoint` |
| `/spawnball [arena]` | Spawns the volleyball, preferring the saved spawn point. | `heroicvolleyball.spawnball` |
| `/startgame [arena]` | Starts a match on a free arena, or opens the arena picker when several are free. Console: serves at Position 6. | `heroicvolleyball.startgame` |
| `/stopgame [arena]` | Stops the game, removes the ball and music and resets the score. | `heroicvolleyball.stopgame` |
| `/restartgame [arena]` | Resets the score and respawns the ball. | `heroicvolleyball.restartgame` |
| `/volleyarena <create\|delete\|clone\|rename\|list\|info\|enable\|disable\|set\|sign> ...` | Manages arenas and queue settings. | `heroicvolleyball.volleyarena` |
| `/volleygive <shovel\|sword\|shield> [wooden\|iron\|diamond]` | Gives a volleyball tool. | `heroicvolleyball.volleygive` |
| `/volleymusic <start\|stop\|list\|reload> [song]` | Controls Note Block Studio music. | `heroicvolleyball.volleymusic` |
| `/volleyreload` | Reloads `config.yml`, `messages.yml`, `gui.yml` and the arenas. | `heroicvolleyball.volleyreload` |

### Public queue

| Command | Description | Permission |
| --- | --- | --- |
| `/volleyjoin [arena]` (alias `/vjoin`) | Joins the queue for an arena, or opens a picker when none is named. | `heroicvolleyball.queue` |
| `/volleyleave` (aliases `/vleave`, `/vquit`) | Leaves the queue or your current match. | `heroicvolleyball.queue` |
| `/volleyarena set lobby <arena>` | Sets the waiting lobby to your current location. | `heroicvolleyball.queue.admin` |
| `/volleyarena set min <arena> <2-12>` | Sets the minimum players needed to start. | `heroicvolleyball.queue.admin` |
| `/volleyarena set max <arena> <2-12>` | Sets the maximum players for the arena. | `heroicvolleyball.queue.admin` |
| `/volleyarena set queue <arena> <true\|false>` | Enables or disables queue mode for the arena. | `heroicvolleyball.queue.admin` |
| `/volleyarena sign <add\|remove> <arena>` | Links or unlinks the sign you are looking at (within 6 blocks). | `heroicvolleyball.queue.signs` |
| `/volleyteam queue [arena]` | Queues your whole online team (leader only). | `heroicvolleyball.team` |
| `/volleyteam unqueue` | Removes your team from its queue (leader only). | `heroicvolleyball.team` |

### Teams and duels

| Command | Description | Permission |
| --- | --- | --- |
| `/volleyteam <create\|invite\|accept\|deny\|leave\|kick\|transfer\|disband\|rename\|color\|info\|top\|chat\|queue\|unqueue> ...` (aliases `/vteam`, `/vt`) | Manages persistent teams. | `heroicvolleyball.team` |
| `/teamduel <team> <sets>` (alias `/vduel`) | Challenges another team to a best-of `1`, `3` or `5` duel. | `heroicvolleyball.team.duel` |

## Public queue mode

Queue mode turns an arena into a lobby for pick-up matches. Each queue arena gets
its own waiting room and signs; players do not need to be in a team.

### Setting up a queue arena

1. Finish the arena normally and check `/vb admin arena info <arena>`.
2. Stand in the lobby and run `/vb admin arena set lobby <arena>`.
3. Set the player limits:
   `/vb admin arena set min <arena> 2` and `/vb admin arena set max <arena> 12`.
   The maximum is `12`; each side can hold at most `6` players.
4. Look at a sign within 6 blocks and run
   `/vb admin arena sign add <arena>`. Repeat for every join sign.
5. Enable the queue: `/vb admin arena set queue <arena> true`.
6. Join with `/vb join <arena>` or by right-clicking a join sign.

`/volleyarena set queue <arena> false` stops new joins. Use
`/volleyarena sign remove <arena>` while looking at a sign to unlink it. Breaking
a linked sign also removes it from the arena automatically.

### Queue behaviour

- Joining teleports you to the arena lobby and keeps your inventory. A snapshot is
  saved so it can be restored when you leave or the match ends.
- Waiting players receive a leave item (`queue.leave-item`, default `BARRIER`).
  Right-click it or run `/volleyleave` to leave.
- Once `min` players are waiting, a countdown starts
  (`queue.countdown-seconds`, default 20). It is broadcast in chat and shown on
  the action bar.
- When the arena reaches `max` players, the countdown is shortened once to
  `queue.full-countdown-seconds` (default 5).
- If the count drops below `min`, the countdown is cancelled and the arena keeps
  waiting.
- Sides are balanced automatically. Team members stay together on one side.
- With `queue.allow-uneven-sides: false` (default), an odd player count benches
  the most recent solo joiner; that player stays in the lobby and remains queued
  for the next match. If no solo player can be benched (for example an odd number
  of team players), the countdown is cancelled until the numbers change.
- At match end the arena returns to waiting. Players who competed are restored
  and teleported back to where they queued from; any benched player stays queued.
- A queued arena with an active waiting room counts as occupied, so
  `/startgame`, arena pickers and duel arena selection skip it. Waiting rooms
  also count toward `session.max-concurrent`.

### Team queueing

- Only the team leader can run `/volleyteam queue [arena]`. If no arena is named,
  the arena at the leader's location or the default arena is used.
- The whole online roster is queued together and placed on the same side.
- A team must fit within the arena's per-side cap and the arena's total capacity.
- At most two queued teams can share an arena; they are placed on opposite sides.
  Solo players fill the remaining slots.
- `/volleyteam unqueue` removes the team from its queue.
- A queued team cannot be challenged to a duel, and queued players are left out
  of duel lineups.

### Join signs

Sign lines come from `messages.yml`:

- `queue.sign-line-1` – arena name
- `queue.sign-line-2` – `players/max`
- `queue.sign-line-3` – `Waiting`, `Starting` or `In game`
- `queue.sign-line-4` – click hint

Signs refresh when players join or leave, when the countdown changes, when a
match starts or ends, and when their chunk loads. Right-clicking a sign joins the
queue; clicks are rate-limited to one every 0.5 seconds per player. Signs on a
disabled or incomplete arena show as waiting but reject joins with an explanatory
message.

### Disconnects

If a player disconnects during a running queue match, the match waits
`queue.disconnect-grace-seconds` (default 60) for them to return. Reconnecting
within that window puts them straight back into their side with their game kit.
If their side is still empty when the timer expires, the other side wins.

Queue state is in memory only; restarting the server ends active waiting rooms.
Saved player inventories are still restored on the next login.

## Arena setup

Each court is an **arena** stored in its own file (`arenas/<name>.yml`), so a
finished setup can be copied between servers or run several at once. Commands that
change a court accept an optional `[arena]` argument; when omitted they use the
arena you are standing in, then the default arena (`arena.default`).

1. Create the arena: `/volleyarena create main`.
2. Give yourself the wand: `/volleywand`.
3. Left-click one corner and right-click the opposite corner of a team's court half.
4. Save it: `/setarena red main` (add a direction, e.g.
   `/setarena red north main`, or omit the direction to face where you look).
   Repeat for `blue`.
5. Sneak + right-click a block with the wand to open the position menu, then pick
   the team and click the spot for each position `1`–`6`. Position `6` is the
   serve spot.
6. Optional: select the back row of each team with the wand and save it with
   `/setbackrow <red|blue>`. When a team has a back row set, its ball can only be
   picked up and placed inside it (otherwise the whole court is allowed).
7. Optional: stand where each team's ball should serve from and run
   `/setballspawnpoint <red|blue>` (clear it with `[clear]`).
8. Check progress with `/volleyarena info main`, then run `/startgame main`.

`/startgame` refuses to start until the arena is complete and lists whatever is
missing: team courts (`/setarena`), all six positions per team and the ball spawn
point (`/setballspawnpoint`). Use `/volleyarena list` to see every arena and
whether it is ready. A disabled arena cannot be started, and an arena that already
has a live match or waiting room refuses a second start; a player who is already
in a game or queue cannot join another one. Several arenas can run matches in
parallel, up to `session.max-concurrent` (`-1` = unlimited).

Copy a finished court with `/volleyarena clone <source> <new>`, or move it with
`/volleyarena rename <old> <new>` (this stops any match in it first).

## Teams

Teams are persistent and stored in `plugins/HeroicVolleyBall/teams.yml` behind a
`TeamRepository` (a SQL backend can be added later). A team has a unique name
(3–16 letters, digits or underscores, case-insensitive, reserved words blocked), a
hex colour, an optional tag, a leader, members (each with a preferred role and join
date), created-at, aggregate stats and invite settings.

- `/volleyteam create <name>` – found a team (you become leader).
- `/volleyteam invite <player>` – leader only; the invite expires after
  `team.invite-expiry-seconds` and is accepted or declined with clickable chat
  buttons or `/volleyteam accept` / `/volleyteam deny`.
- `/volleyteam leave`, `kick <player>`, `transfer <player>`, `disband`,
  `rename <name>`, `color <hex|name>`, `info [team]`, `top`, `chat <message>`.
- Roles: `SETTER`, `OUTSIDE_HITTER`, `MIDDLE_BLOCKER`, `OPPOSITE`, `LIBERO`,
  `DEFENSIVE_SPECIALIST`, `ANY`; each maps to a preferred court slot for lineups.
- `/volleyteam info` opens the team screen: the leader's head at the top, member
  heads below, and leader-only rows for name, colour, tag, invites and disband.
  Members click their own head to change their role; the leader can kick,
  transfer leadership or change another member's role.
- Team colour can be picked from presets or typed as a custom `#RRGGBB` in chat
  (type `cancel` to abort, timed out after `team.input-timeout-seconds`).

## Team duels

- `/teamduel <team> <sets>` – only the leader can challenge; the target leader
  gets clickable Accept/Decline buttons that expire after
  `duel.challenge-expiry-seconds`. A team that is already in a match, waiting in
  a queue or has a pending challenge cannot be challenged.
- On accept, both leaders pick their starting lineup from their online members
  (the same number each side) and press Ready; a random free arena is reserved.
- During a duel, a disconnected player pauses the match for up to
  `duel.disconnect-grace-seconds` for a rejoin, after which their team forfeits;
  a team with no players left always forfeits.
- Results are recorded in both teams' stats and shown on a results screen.

## Gameplay

- **Shovel (Up)** – bumps the ball up and slightly forward.
- **Sword (Spike)** – spikes the ball forward; hitting from above drives it down.
- **Shield (Block)** – left-click the middle or top of the ball to knock it
  forward and down. Hold right-click (shield raised) underneath the ball to catch
  it and pop it straight up like a free ball. Shield hits never count as a team
  touch, and after a deflect or catch the shield recharges before it can be used
  again.
- Sneak + right-click a shovel or sword to cycle its power tier (wooden, iron,
  diamond).
- A team gets three touches per possession. Hitting the ball while it is over the
  opponent's half scores them a point (configurable). The neutral gray ball is
  free to touch.
- While a rally has not started, the resting ball can be picked up with an empty
  hand and placed back inside its own court (once per serve, when
  `ball.pickup-enabled` is true).

## Placeholders (PlaceholderAPI)

Identifier: `hvb`

| Placeholder | Description |
| --- | --- |
| `%hvb_red_points%` / `%hvb_red_score%` | Red team's points. |
| `%hvb_blue_points%` / `%hvb_blue_score%` | Blue team's points. |
| `%hvb_red_sets%` / `%hvb_blue_sets%` | Sets won by each team. |
| `%hvb_score%` | Points as `red - blue`. |
| `%hvb_sets%` | Sets as `red - blue`. |
| `%hvb_match_over%` | Whether the match has been won. |
| `%hvb_active%` | Whether any arena has a game running. |
| `%hvb_players%` | Number of players across all active games. |
| `%hvb_sessions%` | Number of loaded arena sessions. |
| `%hvb_arena%` | The requesting player's arena name, the default arena, or `None`. |
| `%hvb_touches%` / `%hvb_max_touches%` | Current and maximum touches this possession. |
| `%hvb_state%` | Ball state (`Idle`, `Rally`, ...). |
| `%hvb_possession%` | Team that last touched the ball, or `None`. |
| `%hvb_team%` | The requesting player's team, or `None`. |
| `%hvb_position%` | The requesting player's position, or `0`. |

## Configuration

Data is split so editing or regenerating one file cannot destroy courts or teams:

- `config.yml` – plugin settings. Missing keys from newer versions are inserted
  automatically on startup and `/volleyreload`; your values are never overwritten.
- `messages.yml` – every player-facing message (MiniMessage). Also merged on
  startup and `/volleyreload`. Placeholders use `{name}` such as `{team}`,
  `{arena}`, `{players}` or `{seconds}`.
- `gui.yml` – menu titles, sizes, filler glass, button slots and item
  materials/names/lore. All text uses Adventure MiniMessage, e.g. `<#f08e26>`,
  `<bold>`, `<gray>` and `<gradient:#f08e26:#ff9f40>`, plus `{}` placeholders.
  The theme is `#F08E26` with `BLACK_STAINED_GLASS_PANE` filler. `/vb admin
  reload` re-reads it.
- `arenas/<name>.yml` – courts, back rows, ball spawn points, positions `1`–`6`,
  facing directions, per-team display name/colour, optional `lobby` and
  `spectator-spawn` locations, and queue settings (`queue.enabled`,
  `queue.min-players`, `queue.max-players`, `queue.join-signs`).
- `teams.yml` – persistent teams (identity, members, roles, stats, invite
  settings).
- `snapshots/` – temporary inventory/effect snapshots for players who join a game
  or queue. Restored and deleted when the player leaves or the match ends.
- Older single-arena installs (`arenas.yml`, `positions.yml`, `backrow.yml`,
  `ballspawn.yml`) are migrated into `arenas/default.yml` on first start and the
  originals are renamed to `*.legacy`.

### `config.yml` reference

- `ball.*` – size, `max-touches`, `gravity`, `air-drag`, `velocity-scale`,
  `max-velocity`, spike speeds, `shovel-forward`, `bounce`, `settle-threshold`,
  `serve-reset-delay`, `stuck-timeout`, `show-name`, `name-background`,
  `pickup-enabled`, `material`, `texture`, `visual.*` and per-tier `power`.
- `ball.shield.*` – deflect/forward speeds, catch power, reach/height and shield
  cooldown.
- `ball.hit.*` – swing fallback toggle, reach buffer and speed tolerance.
- `game.*` – `points-per-set`, `sets-to-win`, `opponent-touch-penalty`,
  `point-countdown`, `round-countdown`, `titles`, `broadcast-points`, and
  `effects.{enabled,jump-boost-level,speed-level}`.
- `scoreboard.*` – `enabled` and `viewers` (spectators standing in an arena).
- `team.*` – `max-roster`, `invite-expiry-seconds`, `default-color`,
  `input-timeout-seconds`.
- `duel.*` – `max-sets`, `challenge-expiry-seconds`, `disconnect-grace-seconds`.
- `tools.reach-bonus` – extra interaction range during a game.
- `music.*` – `enabled`, `song` and `volume` for Note Block Studio files in
  `plugins/HeroicVolleyBall/music/`.
- `arena.default` – arena used when a command omits the name.
- `session.restore-on-world-change` – restore a player's inventory/effects when
  they change worlds.
- `session.restore-on-leave` – restore them when they walk out of the court.
- `session.max-concurrent` – maximum occupied sessions across all arenas
  (`-1` = unlimited). Queue waiting rooms count as occupied.
- `queue.countdown-seconds` – countdown once the minimum is reached (default 20).
- `queue.full-countdown-seconds` – shortened countdown when the arena is full
  (default 5).
- `queue.allow-uneven-sides` – allow odd player counts instead of benching the
  latest solo joiner (default false).
- `queue.leave-item` – material given to waiting players (default `BARRIER`).
- `queue.scoreboard` – show the waiting-lobby sidebar (default true).
- `queue.disconnect-grace-seconds` – reconnect window during a queue match
  (default 60).

Example queue section:

```yaml
queue:
  countdown-seconds: 20
  full-countdown-seconds: 5
  allow-uneven-sides: false
  leave-item: BARRIER
  scoreboard: true
  disconnect-grace-seconds: 60
```

### `messages.yml`

All chat, titles, action bars, team/duel text, queue statuses and sign lines live
here. The `prefix` key is prepended to command feedback. MiniMessage formatting is
used, for example:

```yaml
queue:
  joined: "<green>You joined the queue for <white>{arena}</white> <gray>({players}/{max})."
  countdown-actionbar: "<gold>Starting in {seconds}s <gray>({players}/{max})"
```

### `gui.yml`

Every menu title, size, filler glass, button slot and item is configurable, and
the same black stained-glass pane is used as filler everywhere.

Menus included: `hub` (`/vb menu`), `guide` and `guide-topic` (player guide with
category cards for Basics, Queue and Teams), `admin-guide`/`admin-guide-topic`
(setup guide), `admin-panel`, `admin-arena-list`, `admin-arena`, `admin-tools`,
`team-info`, `role-picker`, `team-color`, `member-actions`, `confirm`, `lineup`,
`duel-result`, `arena-picker` and `queue-picker`.

The team screen uses a 6-row layout: guide button at slot `0`, the leader's real
head at slot `4`, up to five member heads at slots `10`–`14` (paged with arrows
at `9`/`17` when the roster is larger), a stats card at `22`, then invite, colour,
leave, name, tag, invites and disband across the bottom rows. Lore uses the
`<#f08e26>Description` / ` <#f08e26>| <gray>...` style and the summary item exposes
`{team}`, `{members}`, `{max}`, `{wins}`, `{losses}`, `{sets}` and `{points}`.
`/vb join` uses the `queue-picker` menu when more than one queue arena is
available.

Guide content lives in the `guide-pages` and `admin-guide-pages` lists at the
bottom of the file; each entry has `title`, `icon`, `heading` and `text`.
Card targets are set with `target:` on the `tutorial`/`queue`/`team` items.

### Adding a new default safely

1. Add the key and default to the bundled `config.yml` or `messages.yml`.
2. That is all. `ConfigDefaults.merge(...)` copies missing keys into existing
   server files on load. Never ask server owners to delete their config.

## Permissions

All permissions default to `op` except `heroicvolleyball.hub`,
`heroicvolleyball.help`, `heroicvolleyball.guide`, `heroicvolleyball.team`,
`heroicvolleyball.team.duel` and `heroicvolleyball.queue`, which default to
`true`. `heroicvolleyball.admin` is a parent node that grants every setup
permission below.

| Permission | Purpose |
| --- | --- |
| `heroicvolleyball.hub` | `/vb`, `/vb menu` and the hub |
| `heroicvolleyball.help` | `/vb help`, `/volleyhelp` |
| `heroicvolleyball.guide` | Player guide (`/vb guide`, guide buttons) |
| `heroicvolleyball.admin` | Admin panel, admin guide and all setup commands |
| `heroicvolleyball.volleywand` | `/volleywand` (alias `/vb admin wand`) |
| `heroicvolleyball.setarena` | `/setarena` |
| `heroicvolleyball.setbackrow` | `/setbackrow` |
| `heroicvolleyball.setballspawnpoint` | `/setballspawnpoint` |
| `heroicvolleyball.spawnball` | `/spawnball` |
| `heroicvolleyball.startgame` | `/startgame` |
| `heroicvolleyball.stopgame` | `/stopgame` |
| `heroicvolleyball.restartgame` | `/restartgame` |
| `heroicvolleyball.volleygive` | `/volleygive` |
| `heroicvolleyball.volleymusic` | `/volleymusic` |
| `heroicvolleyball.volleyreload` | `/volleyreload` |
| `heroicvolleyball.volleyarena` | `/volleyarena` |
| `heroicvolleyball.team` | `/volleyteam` |
| `heroicvolleyball.team.duel` | `/teamduel` |
| `heroicvolleyball.queue` | `/volleyjoin`, `/volleyleave` |
| `heroicvolleyball.queue.admin` | Queue limits, lobby and enable/disable subcommands |
| `heroicvolleyball.queue.signs` | Adding and removing join signs |

Buttons inside the admin panel and arena screens are shown locked with a grey dye
when the viewer lacks the matching granular permission.

`queue.admin` and `queue.signs` are checked inside `/volleyarena set` and
`/volleyarena sign`; those commands also require `heroicvolleyball.volleyarena`.

## Files and data

```
plugins/HeroicVolleyBall/
├── config.yml
├── messages.yml
├── gui.yml
├── teams.yml
├── arenas/
│   ├── default.yml
│   └── <arena>.yml
├── snapshots/
└── music/
```

## Building from source

The build requires a JDK 25 toolchain because Paper API 26.3 is compiled for Java
25, while the plugin output targets Java 21 bytecode:

```bash
./gradlew build
```

The jar is written to `build/libs/HeroicVolleyBall-1.0.jar`.

## Notes and limitations

- Queue state is not persisted across server restarts.
- Queue waiting rooms reserve a session, so a low `session.max-concurrent` also
  limits how many arenas can host a queue at once.
- A queue arena with players waiting is skipped by `/startgame` and duel arena
  selection until its waiting room is empty or the match ends.
- If an odd count cannot be balanced by benching a solo player, the countdown is
  cancelled rather than starting uneven sides (unless
  `queue.allow-uneven-sides: true`).
