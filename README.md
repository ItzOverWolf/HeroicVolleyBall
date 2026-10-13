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
| `/setarena <red\|blue> [direction]` | Sets the selected region as a team's arena and the direction that team faces. | `heroicvolleyball.setarena` |
| `/setbackrow <red\|blue>` | Sets the selected region as a team's back row. The ball can only be picked up and placed there. | `heroicvolleyball.setbackrow` |
| `/setballspawnpoint <red\|blue> [clear]` | Saves your current location as the team's ball spawn point (where the ball serves from). | `heroicvolleyball.setballspawnpoint` |
| `/spawnball` | Spawns the volleyball, preferring the saved spawn point. | `heroicvolleyball.spawnball` |
| `/startgame` | Players: opens the team/position picker. Console: serves the ball at Position 6. | `heroicvolleyball.startgame` |
| `/stopgame` | Stops the game, removes the ball/music and resets the score. | `heroicvolleyball.stopgame` |
| `/restartgame` | Resets the score and respawns the ball at Position 6. | `heroicvolleyball.restartgame` |
| `/volleygive <shovel\|sword\|shield> [wooden\|iron\|diamond]` | Gives a volleyball tool. | `heroicvolleyball.volleygive` |
| `/volleymusic <start\|stop\|list\|reload> [song]` | Controls Note Block Studio music. | `heroicvolleyball.volleymusic` |
| `/volleyreload` | Reloads `config.yml` and `messages.yml`. | `heroicvolleyball.volleyreload` |
| `/partycreate` | Creates a party. | `heroicvolleyball.party` |
| `/partyinvite <player>` | Invites a player (leader only). | `heroicvolleyball.party` |
| `/partyleave` | Leaves your party. | `heroicvolleyball.party` |
| `/partydisband` | Disbands the party (leader only). | `heroicvolleyball.party` |

### Setting up a court

1. Give yourself the wand: `/volleywand`.
2. Left-click one corner and right-click the opposite corner of a team's court half.
3. Save it: `/setarena red` (add a direction, e.g. `/setarena red north`, or omit it
   to face wherever you are looking). Repeat for `blue`.
4. Sneak + right-click a block with the wand to open the menu, then pick the team and
   click the spot for each position `1`–`6`. Position `6` is the serve spot.
5. Optional: select the back row of each team with the wand and save it with
   `/setbackrow <red|blue>`. When a team has a back row set, its ball can only be
   picked up and placed inside it (otherwise the whole arena is allowed).
6. Optional: stand where each team's ball should serve from and run
   `/setballspawnpoint <red|blue>` (clear it with `[clear]`). `/spawnball` and serve
   placements prefer it.
7. Run `/startgame`. Party members pick a team and position; the ball serves from
   Position 6 and players are teleported into place.

`/startgame` refuses to start until the setup is complete and lists whatever is
missing: team arenas (`/setarena`), all six positions per team (wand menu) and the
ball spawn point (`/setballspawnpoint`).

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

All permissions default to `op` except `heroicvolleyball.party`, which defaults to
`true` for everyone. Permission nodes are the same as the command names listed above.

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
| `%hvb_active%` | Whether a party game is running. |
| `%hvb_players%` | Number of players in the current game. |
| `%hvb_touches%` / `%hvb_max_touches%` | Current and maximum touches this possession. |
| `%hvb_state%` | Ball state (`Idle`, `Rally`, ...). |
| `%hvb_possession%` | Team that last touched the ball, or `None`. |
| `%hvb_team%` | The requesting player's team, or `None`. |
| `%hvb_position%` | The requesting player's position, or `0`. |

## Configuration

Your data is split across three files, so you can never lose your court by
editing or regenerating a config:

- `config.yml` – plugin settings only. New settings added in later versions are
  inserted into existing files automatically on startup and on `/volleyreload`
  (existing values are never overwritten), so you never need to delete it.
- `arenas.yml` – the two court regions set with `/setarena`.
- `positions.yml` – positions `1`–`6` per team plus the facing direction.
- `backrow.yml` – the optional back-row regions set with `/setbackrow`.
- `ballspawn.yml` – the ball spawn point set with `/setballspawnpoint`.

`config.yml` highlights:

- `ball.*` – size, `max-touches`, `gravity`, `air-drag`, `velocity-scale`,
  `max-velocity`, spike speeds, `shovel-forward`, `shield-deflect`, `bounce`,
  `serve-reset-delay`, `show-name`, and per-tier `power`.
- `ball.shield.*` – deflect/forward speeds on a middle/top shield hit, the upward
  power and reach/height of the bottom shield catch, and the shield cooldown in
  ticks after any shield use.
- `ball.sulfur-cube` – `true` keeps the vanilla sulfur cube with the item inside
  it; `false` hides the cube and shows only the configured item floating as the
  ball. Changeable live with `/volleyreload`.
- `ball.cube-invisible` – hides just the cube model (item, physics and touches
  untouched) for experimenting.
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
- `party.max-size` – maximum party size (must be even to start).
- `tools.reach-bonus` – extra interaction range while a game is running.
- `music.{enabled,song,volume}` – Note Block Studio track in `plugins/HeroicVolleyBall/music/`.

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
