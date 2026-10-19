# HeroicVolleyBall

A Haikyuu-inspired volleyball plugin for Paper. Build a court, place six positions
per team, and rally a sulfur-cube ball using shovels, swords and shields.

## Requirements

- Paper `1.21.3+` (API `26.3`)
- Java 25
- Optional: [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for the `%hvb_*%` placeholders

## Installation

1. Drop the plugin jar into your `plugins/` folder.
2. Start the server once to generate `config.yml` and `messages.yml`.
3. Edit `plugins/HeroicVolleyBall/config.yml` (ball physics, match rules) and
   `messages.yml` (all chat, titles and action bars).

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/volleywand` | Gives the selection wand (left-click point 1, right-click point 2, sneak + right-click to open the position menu). | `heroicvolleyball.volleywand` |
| `/setarena <red\|blue> [direction] [arena]` | Sets the selected region as a team's court and the direction that team faces, in the named arena. | `heroicvolleyball.setarena` |
| `/setbackrow <red\|blue> [arena]` | Sets the selected region as a team's back row. The ball can only be picked up and placed there. | `heroicvolleyball.setbackrow` |
| `/setballspawnpoint <red\|blue> [clear] [arena]` | Saves your current location as the team's ball spawn point (where the ball serves from). | `heroicvolleyball.setballspawnpoint` |
| `/spawnball [arena]` | Spawns the volleyball, preferring the saved spawn point. | `heroicvolleyball.spawnball` |
| `/startgame [arena]` | Starts a match on a free arena, or opens the arena picker when several are free. Console: serves the ball at Position 6. | `heroicvolleyball.startgame` |
| `/stopgame [arena]` | Stops the game, removes the ball/music and resets the score. | `heroicvolleyball.stopgame` |
| `/restartgame [arena]` | Resets the score and respawns the ball at Position 6. | `heroicvolleyball.restartgame` |
| `/volleyarena <create\|delete\|clone\|rename\|list\|info\|enable\|disable> [name] [name]` | Manages arenas: create, clone, rename, inspect, toggle or delete a court setup. | `heroicvolleyball.volleyarena` |
| `/volleygive <shovel\|sword\|shield> [wooden\|iron\|diamond]` | Gives a volleyball tool. | `heroicvolleyball.volleygive` |
| `/volleymusic <start\|stop\|list\|reload> [song]` | Controls Note Block Studio music. | `heroicvolleyball.volleymusic` |
| `/volleyreload` | Reloads `config.yml` and `messages.yml`. | `heroicvolleyball.volleyreload` |
| `/volleyteam <create\|invite\|accept\|deny\|leave\|kick\|transfer\|disband\|rename\|color\|info\|top\|chat> ...` | Manages persistent teams (aliases `/vteam`, `/vt`). | `heroicvolleyball.team` |
| `/teamduel <team> <sets>` | Challenges another team to a best-of `1`/`3`/`5` duel (alias `/vduel`). | `heroicvolleyball.team.duel` |

### Teams

Teams are persistent and stored in `plugins/HeroicVolleyBall/teams.yml` behind a
`TeamRepository` (a SQL backend can be dropped in later). A team has a unique name
(3–16 letters, digits or underscores, case-insensitive, reserved words blocked), a
hex colour, an optional tag, a leader, members (each with a preferred role and join
date), created-at, aggregate stats and invite settings.

- `/volleyteam create <name>` – found a team (you become leader).
- `/volleyteam invite <player>` – leader only; the invite expires after
  `team.invite-expiry-seconds` and is accepted/declined with clickable chat buttons
  or `/volleyteam accept` / `/volleyteam deny`.
- `/volleyteam leave`, `kick <player>`, `transfer <player>`, `disband`,
  `rename <name>`, `color <hex|name>`, `info [team]`, `top`, `chat <message>`.
- Roles: `SETTER`, `OUTSIDE_HITTER`, `MIDDLE_BLOCKER`, `OPPOSITE`, `LIBERO`,
  `DEFENSIVE_SPECIALIST`, `ANY`; each maps to a preferred court slot for lineups.
- `/volleyteam info` opens the team screen: the leader's head up top, member heads
  (online first) below, and leader-only rows for name, colour, tag, invites and
  disband. Members click their own head to change their role; the leader can kick,
  transfer leadership or change another member's role.
- Team colour can be picked from presets or typed as a custom `#RRGGBB` in chat
  (type `cancel` to abort, timed out after `team.input-timeout-seconds`).

### Team duels

- `/teamduel <team> <sets>` – only the leader can challenge; the target leader gets
  clickable Accept/Decline buttons that expire after `duel.challenge-expiry-seconds`.
  A team that is already in a match or has a pending challenge cannot be challenged.
- On accept, both leaders pick their starting lineup from their online members
  (same number each side) and press Ready; a random free arena is reserved.
- During a duel, a disconnected player pauses the match for up to
  `duel.disconnect-grace-seconds` (configurable) for a rejoin, after which their team
  forfeits; a team with no players left always forfeits.
- Results are recorded in both teams' stats and shown on a results screen.

### Setting up a court

Each court is an **arena** stored in its own file (`arenas/<name>.yml`), so a
finished setup can be copied between servers or run several at once. Commands that
change a court accept an optional `[arena]` argument; when omitted they use the
arena you are standing in, then the default arena (`arena.default`).

1. Create the arena: `/volleyarena create main`.
2. Give yourself the wand: `/volleywand`.
3. Left-click one corner and right-click the opposite corner of a team's court half.
4. Save it: `/setarena red main` (add a direction, e.g. `/setarena red north main`, or
   omit the direction to face wherever you are looking). Repeat for `blue`.
5. Sneak + right-click a block with the wand to open the menu, then pick the team and
   click the spot for each position `1`–`6`. Position `6` is the serve spot.
6. Optional: select the back row of each team with the wand and save it with
   `/setbackrow <red|blue>`. When a team has a back row set, its ball can only be
   picked up and placed inside it (otherwise the whole court is allowed).
7. Optional: stand where each team's ball should serve from and run
   `/setballspawnpoint <red|blue>` (clear it with `[clear]`). `/spawnball` and serve
   placements prefer it.
8. Check progress with `/volleyarena info main`, then run `/startgame main`. Players
   pick a position; the ball serves from Position 6 and players are teleported into
   place. When more than one arena is free, `/startgame` opens an arena picker that
   shows each arena's state (free, in use, disabled or not ready).

`/startgame` refuses to start until the arena is complete and lists whatever is
missing: team courts (`/setarena`), all six positions per team (wand menu) and the
ball spawn point (`/setballspawnpoint`). Use `/volleyarena list` to see every arena
and whether it is ready. A disabled arena cannot be started, and an arena that
already has a live match refuses a second start; a player who is already in a game
cannot join another one. Several arenas can run matches in parallel, up to
`session.max-concurrent` (`-1` = unlimited).

Copy a finished court to make a second one with `/volleyarena clone <source> <new>`,
or move it with `/volleyarena rename <old> <new>` (this stops any match in it first).

### Tools

- **Shovel (Up)** – bumps the ball up and slightly forward.
- **Sword (Spike)** – spikes the ball forward; hitting from above drives it down.
- **Shield (Block)** – left-click the middle/top of the ball to knock it forward and
  down. Hold right-click (shield raised) underneath the ball to catch it and pop it
  straight up like a free ball. Shield hits never count as a team touch, and after a
  deflect/catch the shield recharges for a few seconds before it can be used again.
- Sneak + right-click a shovel or sword to cycle its power tier (wooden, iron, diamond).

A team gets three touches per possession. Hitting the ball while it is over the
opponent's half scores them a point (configurable). The neutral gray ball is free
to touch.

## Permissions

All permissions default to `op` except `heroicvolleyball.team` and
`heroicvolleyball.team.duel`, which default to `true` for everyone. Permission nodes
are the same as the command names listed above.

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
| `%hvb_players%` | Number of players across all running games. |
| `%hvb_sessions%` | Number of loaded arena sessions. |
| `%hvb_arena%` | The requesting player's arena name, the default arena, or `None`. |
| `%hvb_touches%` / `%hvb_max_touches%` | Current and maximum touches this possession. |
| `%hvb_state%` | Ball state (`Idle`, `Rally`, ...). |
| `%hvb_possession%` | Team that last touched the ball, or `None`. |
| `%hvb_team%` | The requesting player's team, or `None`. |
| `%hvb_position%` | The requesting player's position, or `0`. |

## Configuration

Your data is split so you can never lose a court by editing or regenerating a
config:

- `config.yml` – plugin settings only. New settings added in later versions are
  inserted into existing files automatically on startup and on `/volleyreload`
  (existing values are never overwritten), so you never need to delete it.
- `gui.yml` – every menu's title, size, filler glass, button slots, item
  materials and names/lore (with `&` colour codes and `{}` placeholders).
  `/volleyreload` re-reads it. Menu titles are legacy `&`-coded (e.g.
  `&8Team Info`). Available placeholders are listed at the bottom of the file.
- `arenas/<name>.yml` – one file per arena, holding its courts, back rows, ball
  spawn points and positions `1`–`6` per team, each team's facing direction,
  per-team display name/colour (`teams.<red|blue>.name` / `.color`) and the
  optional `lobby` and `spectator-spawn` locations. Manage them with
  `/volleyarena`.
- `teams.yml` – persistent teams (identity, members, roles, stats, invite
  settings). Managed with `/volleyteam`.
- Older single-arena installs (`arenas.yml`, `positions.yml`, `backrow.yml`,
  `ballspawn.yml`) are migrated into `arenas/default.yml` on first start and the
  originals are renamed to `*.legacy`.

`config.yml` highlights:

- `ball.*` – size, `max-touches`, `gravity`, `air-drag`, `velocity-scale`,
  `max-velocity`, spike speeds, `shovel-forward`, `bounce`, `serve-reset-delay`,
  `show-name`, and per-tier `power`.
- `ball.shield.*` – deflect/forward speeds on a middle/top shield hit, the upward
  power and reach/height of the bottom shield catch, and the shield cooldown in
  ticks after any shield use.
- `ball.material` – the ball's appearance (and the item you pick up). Any
  block/item material name works (`GRAY_WOOL`, `SLIME_BALL`, `DIAMOND`,
  `PLAYER_HEAD`, ...). Wools tint red/blue by which court the ball is over.
- `ball.texture` – base64 skin value used when `ball.material` is `PLAYER_HEAD`,
  so you can make the ball any custom head (leave `""` for a plain head).
- `ball.pickup-enabled` – when `true`, a player can right-click the resting ball
  (`Serve` state) with an empty hand to pick it up into their inventory and then
  right-click any block in the ball's own court to place it back. Only allowed
  before the round starts and once per serve; the ball cannot be picked up again
  once placed or while the rally is live.

### Ball pickup & placement

1. Ball is resting (not yet hit) → right-click it with an empty hand to pick it up.
2. Right-click a block **inside the ball's own team court** to place it back there.
3. Start the round as normal (first shovel bumps it). Placing is one-time per serve.
- `game.points-per-set`, `game.sets-to-win`, `game.opponent-touch-penalty`,
  `game.point-countdown`, `game.round-countdown`.
- `game.effects.{enabled,jump-boost-level,speed-level}` – infinite potion effects
  while a game runs (set a level to `0` to disable it).
- `game.titles`, `game.broadcast-points` – on-screen titles and chat announcements.
- `scoreboard.enabled` – toggles the sidebar scoreboard.
- `scoreboard.viewers` – when `true`, players standing in an arena but not playing
  that match see the live sidebar as spectators (removed when they leave or the
  match ends).
- `team.{max-roster,invite-expiry-seconds,default-color,input-timeout-seconds}` –
  roster cap (hard-capped at 12), invite lifetime, default team colour and chat
  input timeout.
- `duel.{max-sets,challenge-expiry-seconds,disconnect-grace-seconds}` – largest
  best-of value, challenge lifetime and disconnect grace before a forfeit.
- `tools.reach-bonus` – extra interaction range while a game is running.
- `music.{enabled,song,volume}` – Note Block Studio track in `plugins/HeroicVolleyBall/music/`.
- `arena.default` – the arena used by commands and placeholders when none is named.
- `session.{restore-on-world-change,restore-on-leave}` – when to give a player back
  the inventory and potion effects saved when they joined a game.
- `session.max-concurrent` – maximum matches running at once across all arenas
  (`-1` = unlimited).

`messages.yml` uses [MiniMessage](https://docs.advntr.dev/minimessage/format.html)
formatting. Placeholders are written as `{name}` (for example `{team}`, `{position}`,
`{location}`). The `prefix` key is prepended to command feedback. Like `config.yml`,
new message keys are merged into existing files automatically on startup/`/volleyreload`.

### How to add a new setting (the safe way)

To add e.g. a `game.max-scoring-errors` toggle later without making players reset
their config:

1. Add the key + default to `src/main/resources/config.yml` (or `messages.yml`),
   e.g. `game.max-scoring-errors: 7`.
2. That's it. `HeroicVolleyBall.syncConfigDefaults()` (config) and
   `Messages.reload()` now both call `ConfigDefaults.merge(...)`, which copies any
   missing key from the bundled file into the player's existing file on load.
3. Read it anywhere with `plugin.getConfig().getInt("game.max-scoring-errors", 7)`.

Never delete a player's `config.yml` to push a default again – the merge handles it.
