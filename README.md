# HeroicVolleyBall 🏐

A Haikyuu-inspired Minecraft volleyball minigame! Play solo, join public matches, build teams, challenge other teams, and experience fast rallies with custom volleyball tools, scoring, rotations, arenas, and music.

## Features

- **Volleyball gameplay** with a custom physics-based ball, configurable gravity, bounce, speed, and hit detection.
- **Three tools:** Up, Spike, and Block, each with distinct uses and adjustable power.
- **Match rules:** configurable set points, best-of sets, deuce, touch limits, and opponent-side touch penalties.
- **Automatic player positioning and rotation** during team matches.
- **Solo practice**, Practice Mode.
- **Create Multiple arenas**, each with its own court regions, positions, and ball spawn points.
- **In-game menus and guides** for players and administrators.
- **Scoreboard, titles, chat announcements, potion effects, and Note Block Studio music.**
- **PlaceholderAPI support** for scoreboards and other server displays.
- **Highly configurable:** customize gameplay settings, GUI titles/items/lore, and chat messages in the plugin's YAML files.

## How to play

1. Open the main menu with `/vb menu` or read the in-game guide with `/vb guide`.
2. Join a public match with `/vb join`, or create/manage a team and challenge another team.
3. Choose your starting court position when prompted.
4. Use your volleyball tools to keep the ball in play and send it over the net.
5. A team can touch the ball up to the configured touch limit (3 by default). If the ball lands or a rule is broken, the other team may score.
6. The default set target is **25 points**, with a **2-point lead required at deuce**. The default match is best-of-three sets (first team to win 2 sets).

### Tools

- **Up (shovel):** Hit the ball upward to set it up for another touch. Shovel tier controls its power.
- **Spike (sword):** Attack the ball. Hitting from above drives it down; level or lower hits change its trajectory. Sword tier controls its power.
- **Block (shield):** Deflect an incoming ball like a wall, or hold right-click while underneath the ball to catch/pop it upward. The shield has a cooldown and does not consume a team touch.
- **Change tool tier:** Hold sneak and right-click while holding an Up or Spike tool to cycle Wooden → Iron → Diamond. The Shield does not cycle tiers this way.

## Commands

Player commands (the main command is `/vb`; `/volley` is an alias):

| Command | Description |
|---|---|
| `/vb` or `/vb help [page]` | Show command help. |
| `/vb menu` | Open the main menu. |
| `/vb guide` | Open the gameplay guide. |
| `/vb join [arena]` | Join the public queue, optionally for an arena. |
| `/vb leave` | Leave the queue or current match. |
| `/vb team <subcommand>` | Manage your team; see team commands below. |
| `/vb duel <team> <sets>` | Challenge another team to a duel. |

Team commands (`/volleyteam`, aliases `/vteam`, `/vt`, `/team`; also available under `/vb team`):

- `/volleyteam create <name>` — create a team.
- `/volleyteam invite <player>` — invite a player.
- `/volleyteam accept` / `/volleyteam deny` — respond to an invite.
- `/volleyteam leave` — leave your team.
- `/volleyteam kick <player>` — remove a member (leader).
- `/volleyteam transfer <player>` — transfer leadership.
- `/volleyteam disband` — disband the team (leader).
- `/volleyteam rename <name>` — rename your team.
- `/volleyteam color <hex>` — change team colour.
- `/volleyteam info [team]` — view team information.
- `/volleyteam top` — show the team leaderboard.
- `/volleyteam chat` — toggle team chat.
- `/volleyteam queue <arena>` / `/volleyteam unqueue` — queue or remove your team from an arena queue.

Administrative commands (most require operator permissions):

| Command | Description |
|---|---|
| `/vb admin panel` | Open the admin GUI. |
| `/vb admin guide` | Open the admin setup guide. |
| `/volleywand` or `/vb admin wand` | Get the selection wand. |
| `/volleyarena <create\|delete\|clone\|rename\|list\|info\|enable\|disable\|set\|sign> ...` | Manage arenas, queue settings, and join signs. |
| `/setarena <red\|blue> [direction] [arena]` | Set the selected court region for a side. |
| `/setbackrow <red\|blue> [arena]` | Set a side's back-row region. |
| `/setballspawnpoint <red\|blue> [clear] [arena]` | Set or clear a side's ball spawn point. |
| `/spawnball [arena]` | Spawn the ball at your location. |
| `/startgame [arena]` | Start a game in an arena. |
| `/stopgame [arena]` | Stop a game and reset its score. |
| `/restartgame [arena]` | Restart a game and reset its score. |
| `/volleygive <shovel\|sword\|shield> [wooden\|iron\|diamond]` | Give a volleyball tool. |
| `/volleymusic <start\|stop\|list\|reload> [song]` | Manage match music. |
| `/volleyreload` | Reload plugin configuration. |
| `/vb admin debug <on\|off\|toggle\|status\|reload>` | Manage debug logging. |
| `/teamduel <team> <sets>` or `/vduel` | Challenge a team to a duel. |

Other command aliases: `/volleyjoin` (`/vjoin`), `/volleyleave` (`/vleave`, `/vquit`), and `/volleyhelp` (`/vhelp`).

## PlaceholderAPI placeholders

Requires [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi). Use these with the `%hvb_<placeholder>%` format.

| Placeholder | Description |
|---|---|
| `%hvb_red_points%` | Red team's current set points. |
| `%hvb_red_score%` | Alias for red points. |
| `%hvb_blue_points%` | Blue team's current set points. |
| `%hvb_blue_score%` | Alias for blue points. |
| `%hvb_red_sets%` | Sets won by red. |
| `%hvb_blue_sets%` | Sets won by blue. |
| `%hvb_score%` | Current points as `red - blue`. |
| `%hvb_sets%` | Sets as `red - blue`. |
| `%hvb_match_over%` | Whether the match has ended (`true`/`false`). |
| `%hvb_active%` | Whether any match is active. |
| `%hvb_players%` | Total players in plugin sessions. |
| `%hvb_sessions%` | Number of active session objects. |
| `%hvb_arena%` | Arena name for the resolved session. |
| `%hvb_touches%` | Number of touches in the current rally. |
| `%hvb_max_touches%` | Maximum allowed touches per possession. |
| `%hvb_state%` | Ball state, or `Idle` when no session is resolved. |
| `%hvb_possession%` | Team currently in possession, or `None`. |
| `%hvb_team%` | The requesting player's team in the resolved session. |
| `%hvb_position%` | The requesting player's court position (1–6), or `0` if none. |

If no session can be resolved, score and touch placeholders return their documented zero/default values. Session-based placeholders may resolve the player's own session, the configured default arena, or another active session.

## Configuration

The plugin creates its files under `plugins/HeroicVolleyBall/`:

- **`config.yml`** — ball physics and appearance, tool power, touch limits, scoring and deuce rules, effects, scoreboards, team limits, duel and queue timers, music, arena defaults, session behavior, and debug logging.
- **`messages.yml`** — player-facing chat messages, command help text, and game announcements.
- **`gui.yml`** — menu titles, item materials, names, lore, guide pages, and GUI presentation.
- **`arenas/<name>.yml`** — each arena's court regions, back rows, ball spawn points, player positions, and facing directions.
- **`teams.yml`** — saved team data.
- **`music/`** — Note Block Studio `.nbs` music files.

Most gameplay settings can be changed in `config.yml` and reloaded with `/volleyreload`. Edit `messages.yml` to customize chat text and `gui.yml` to customize the menus. Back up your files before making large changes, and keep YAML indentation valid.

## Requirements

- Minecraft server running **Paper 1.21 or newer** (the project is built against a modern Paper API; test against your target server version).
- Java 21 or newer.
- PlaceholderAPI is optional and only required for the placeholders above.

## Permissions

- `heroicvolleyball.hub` — main command/menu (default: everyone).
- `heroicvolleyball.help` — help (default: everyone).
- `heroicvolleyball.guide` — player guide (default: everyone).
- `heroicvolleyball.team` — team commands (default: everyone).
- `heroicvolleyball.team.duel` — team duels (default: everyone).
- `heroicvolleyball.queue` — join/leave public queue (default: everyone).
- `heroicvolleyball.admin` — admin panel and setup commands (default: operators; includes setup permissions).
- `heroicvolleyball.volleywand`, `heroicvolleyball.volleyarena`, `heroicvolleyball.setarena`, `heroicvolleyball.setbackrow`, `heroicvolleyball.setballspawnpoint`, `heroicvolleyball.spawnball`, `heroicvolleyball.startgame`, `heroicvolleyball.stopgame`, `heroicvolleyball.restartgame`, `heroicvolleyball.volleygive`, `heroicvolleyball.volleyreload`, `heroicvolleyball.volleymusic`, `heroicvolleyball.queue.admin`, `heroicvolleyball.queue.signs` — individual admin/setup permissions (default: operators).

---
