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
| `/spawnball` | Spawns the volleyball at your location. | `heroicvolleyball.spawnball` |
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
5. Run `/startgame`. Party members pick a team and position; the ball serves from
   Position 6 and players are teleported into place.

### Tools

- **Shovel (Up)** – bumps the ball up and slightly forward.
- **Sword (Spike)** – spikes the ball forward; hitting from above drives it down.
- **Shield (Block)** – deflects the ball downward.
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

`config.yml` highlights:

- `ball.*` – size, `max-touches`, `gravity`, `air-drag`, `velocity-scale`,
  `max-velocity`, spike speeds, `shovel-forward`, `shield-deflect`, `bounce`,
  `serve-reset-delay`, `show-name`, and per-tier `power`.
- `game.points-per-set`, `game.sets-to-win`, `game.opponent-touch-penalty`.
- `game.effects.{enabled,jump-boost-level,speed-level}` – infinite potion effects
  while a game runs (set a level to `0` to disable it).
- `game.titles`, `game.broadcast-points` – on-screen titles and chat announcements.
- `scoreboard.enabled` – toggles the sidebar scoreboard.
- `party.max-size` – maximum party size (must be even to start).
- `tools.reach-bonus` – extra interaction range while a game is running.
- `music.{enabled,song,volume}` – Note Block Studio track in `plugins/HeroicVolleyBall/music/`.

`messages.yml` uses [MiniMessage](https://docs.advntr.dev/minimessage/format.html)
formatting. Placeholders are written as `{name}` (for example `{team}`, `{position}`,
`{location}`). The `prefix` key is prepended to command feedback.
