# OrchidPlugins

An all-in-one [Paper](https://papermc.io) plugin (1.21.11, Java 21) that bundles the classic "small script" fun plugins into a single jar: player/dice rollers, chat-reaction giveaways, the death game, server announcements & restarts, random bounties — plus a full moderation suite and an admin-abuse command.

Built by consolidating five Skript scripts into native Paper API code.

---

## Features

- **Roll Dice** — `/rolldice` (alias `/roll`): animated random-player roller with sound effects and a decelerating reveal.
- **Chat Giveaway** — `/react` starts a giveaway with a custom phrase (or a random one), players type the phrase in chat to enter, `/endreact` rolls a winner with the same animated sequence.
- **Polls** — `/poll` starts a chat-answered poll. Players vote by typing a choice **number or its text** in chat (votes can be switched). Choices come three ways:
  - **No choices** → defaults to `YES` / `NO` (e.g. `/poll Should I reset?`)
  - **`[brackets]` in the question** → extracted as choices: `/poll Best mob? [Creeper] [Zombie]`
  - **`|` pipes**: `/poll Best mob? | Creeper | Zombie`
  
  Polls auto-end after `poll.seconds`, or early with `/endpoll`; results and winners are broadcast with a title. Every poll broadcast is branded with the styled `server-name` (full Minecraft formatting via legacy codes, e.g. `&l&bCLOUDREND SMP`).
- **Death Game** — `/stopmoving` (or `/deathgame start`): a red-light-green-light freeze game. Anyone who moves triggers a player vote: `/vote die` or `/vote gay` (gay = glowing, die = lightning kill). Optional mode flags:
  - `/stopmoving -die` — a mover **dies instantly, no vote**.
  - `/stopmoving -gay` — a mover is **marked gay instantly**, and their glow only appears when another player is within `glow-distance` blocks, so far-away raiders can't spot their base.
  
  `/deathgame stop` or `/stopmoving stop` cancels the game (also clears any gay glow). Countdown, vote length, and glow distance are configurable.
- **Announcements** — `/sannounce <message>`: full-screen title + action bar + sound to all players.
- **Restart Countdown** — `/srestart [time|cancel]`: parse times like `1h`, `3m`, `45s`, `1h30m`, or raw seconds (default from config). Milestone countdown titles, then kicks everyone and dispatches `restart`.
- **Bounties** — `/rollbounty` (alias `/bountyroll`): random player gets a configurable random bounty; killers claim it on death. `/checkbounty` to view. Payouts use **Vault** if available, otherwise an internal per-player balance.
- **Moderation** —
  - `/warn <player> [duration] [reason]` – persistent warnings (optional expiry)
  - `/unwarn <player> [count] [reason]` – remove warnings, **offline players get a one-time message on their next join**
  - `/ban <player> [duration] [reason]` – IGN/UUID ban with optional duration
  - `/banip <player> [duration] [reason]` – IP ban with optional duration
  - `/unban <name>`
  - `/suspendstaff <player> [duration] [reason]` / `/unsuspendstaff <player>` – LuckPerms-based staff suspension (saves & restores groups; auto-restores when a duration expires)
  - Durations parse Discord-style and may appear anywhere in the command: `30s`, `45m`, `1h`, `7d`, `1d6h`, `2w`, `0`/`permanent`.
- **Admin Abuse** — `/adminabuse` (alias `/aa`) attribute modifier commands. **Private by default** (only you + console see it); add `-b` (or `broadcast`) anywhere to broadcast publicly, `-s` to force-silence, configurable default. **Every execution is recorded in an SQLite audit DB** (executor, target, action, value, time) and can be browsed in-game.

| Subcommand (alias) | Attribute | Default |
|---|---|---|
| `bigcharacter`/`char` | generic.scale | 2.0 |
| `reach` | block + entity interaction range | 10 |
| `speed` | movement speed | 2x |
| `jump` | jump strength | +1.0 |
| `health`/`hp` | max health | 100 |
| `reset`/`r` | (clears all orchid modifiers) | — |

  Usage: `/aa <subcommand> [player] [value] [-b|-s]` — target defaults to the sender.

- **Discord Webhooks** — optional webhook notifications for moderation, bounties, and admin abuse. **Each event has its own URL** (e.g. different channels) and toggle, plus a shared `default-url`. Posts are sent off the main thread via the JDK HTTP client (no extra deps).
- **Plugin Info** — `/orchidplugins help` (`/orchidp`, `/orchid`) lists every command, `/orchidplugins abuselog [player] [limit]` browses the `/aa` audit DB, `/orchidplugins webhook-test` sends a sample embed, `/orchidplugins version` shows the installed version.

---

## Requirements

- **Paper 1.21.11** (or a recent 1.21 fork)
- **Java 21**

Optional (feature toggles automatically at runtime):

- **[Vault](https://www.spigotmc.org/resources/vault.34315/)** — bounty payouts hook into your economy (EssentialsX, CMI, etc.). Without it, bounties still pay out to an internal balance.
- **[LuckPerms](https://luckperms.net/)** — `/suspendstaff` requires it. Without it the command warns but does nothing.

---

## Installation

1. Grab the channel jar you want (`OrchidPlugins-<ver>-stable.jar` for production, `-neuro.jar` for testing) from `output/` or [Releases](../../releases).
2. Drop it into your server's `plugins/` folder.
3. Restart (or reload) the server.

Data files are created in `plugins/OrchidPlugins/`:

- `config.yml` — settings and message prefixes (see Configuration below)
- `data.db` — SQLite audit database of `/adminabuse` executions
- `moderation-data.json` — warnings (with expiry), queued offline unwarn messages, and last-known IPs (used for `/banip`)
- `suspensions.json` — saved staff groups while suspended

---

## Commands & Permissions

| Command | Description | Permission | Default |
|---|---|---|---|
| `/rolldice` (`/roll`) | random player roller | `orchid.roll` | everyone |
| `/react [phrase]` | start giveaway | `orchid.react` | op |
| `/endreact` | roll giveaway winner | `orchid.react.end` | op |
| `/poll <question> [\| or [choices]...]` | start a chat-answered poll | `orchid.poll` | op |
| `/endpoll` | end poll & show results | `orchid.poll.end` | op |
| `/deathgame start [-die\|-gay]\|stop` | freeze game control | `orchid.deathgame` | op |
| `/stopmoving [-die\|-gay\|stop]` | start/stop the freeze game | `orchid.deathgame` | op |
| `/vote die\|gay` | death game vote | — | everyone |
| `/sannounce <message>` | full-screen announcement | `orchid.announce` | op |
| `/srestart [time\|cancel]` | restart countdown | `orchid.restart` | op |
| `/rollbounty` | place random bounty | — | everyone |
| `/checkbounty [player]` | view bounty/balance | — | everyone |
| `/warn <player> [duration] [reason]` | warn a player | `orchid.moderation.warn` | op |
| `/unwarn <player> [count] [reason]` | remove warnings | `orchid.moderation.unwarn` | op |
| `/ban <player> [duration] [reason]` | IGN ban | `orchid.moderation.ban` | op |
| `/banip <player> [duration] [reason]` | IP ban | `orchid.moderation.banip` | op |
| `/unban <name>` | pardon player | `orchid.moderation.unban` | op |
| `/suspendstaff <player> [duration] [reason]` | suspend staff via LuckPerms | `orchid.moderation.suspendstaff` | op |
| `/unsuspendstaff <player>` | restore staff | `orchid.moderation.suspendstaff` | op |
| `/adminabuse <sub> [player] [value] [-b\|-s]` (`/aa`) | attribute abuse commands | `orchid.adminabuse` | op |
| `/orchidplugins abuselog [player] [limit]` (`/orchidp`, `/orchid`) | browse the /aa audit DB | `orchid.adminabuse.log` | op |
| `/orchidplugins webhook-test` | send a test Discord embed | `orchid.discord.test` | op |
| `/orchidplugins help\|version` (`/orchidp`, `/orchid`) | plugin info | — | everyone |

There is also an `orchid.*` wildcard permission that grants everything.

---

## Configuration

`config.yml` is generated on first start (you can regenerate it by deleting the file and restarting). Settings:

```yaml
# Server name shown (with full Minecraft formatting) on poll broadcasts.
# Legacy color/format codes are translated to MiniMessage in-game.
server-name: "&l&bCLOUDREND SMP"

# Poll settings
poll:
  seconds: 30

# /srestart default when no time is given
restart:
  default-time: "60s"

# Death game settings
deathgame:
  countdown-seconds: 5
  vote-seconds: 10
  # -gay tag glow only appears when another player is this close (blocks).
  # Keeps victims' bases hidden from far-away raiders.
  glow-distance: 24

# /rollbounty amounts
bounty:
  min-amount: 500
  max-amount: 5000

# Whether /adminabuse is public by default.
# false = silent (executor + console only); -b forces public, -s forces silent.
admin-abuse:
  broadcast-by-default: false

# Whether moderation actions broadcast publicly.
# false = private (executor + console only).
moderation-broadcast:
  warn: true
  ban: true
  banip: true
  unban: true
  unwarn: false
  suspendstaff: true
  unsuspendstaff: true

# Message prefixes (MiniMessage format)
prefixes:
  giveaway: "<light_purple><bold>[GIVEAWAY]</bold></light_purple> <yellow>"
  deathgame: "<red><bold>[DEATH GAME]</bold></red> <yellow>"
  bounty: "<dark_red><bold>[BOUNTY]</bold></dark_red> "
  warn: "<red>[WARN] "
  ban: "<red>[BAN] "
  ipban: "<dark_red>[IP-BAN] "
  unban: "<green>[UNBAN] "
  unwarn: "<green>[UNWARN] "
  suspend: "<red><bold>[SUSPEND]</bold></red> "
  abuse: "<light_purple>[ABUSE] "
  server: "<yellow><bold>[SERVER]</bold></yellow> "
  announce: "<gold>\uD83D\uDCE2 ANNOUNCEMENT"

# SQLite audit database (plugins/OrchidPlugins/<filename>).
# Stores every /adminabuse execution - executor, target, action, value, time.
database:
  filename: "data.db"

# Discord webhooks - each event can use its own URL (e.g. different channels).
# An event sends only if the master switch AND its own toggle are on.
# If an event has an empty url, discord-webhook.default-url is used.
discord-webhook:
  enabled: false
  default-url: ""
  events:
    warn:
      enabled: true
      url: ""
    ban:
      enabled: true
      url: ""
    banip:
      enabled: true
      url: ""
    unban:
      enabled: true
      url: ""
    unwarn:
      enabled: true
      url: ""
    suspendstaff:
      enabled: true
      url: ""
    unsuspendstaff:
      enabled: true
      url: ""
    bounty:
      enabled: true
      url: ""
    admin-abuse:
      enabled: true
      url: ""
```

**Webhooks:** with `discord-webhook.enabled: true`, fill in a `default-url` (or per-event `url`s — `admin-abuse` gets both silent and public `/aa` executions, great for staff oversight). Discord embeds are color-coded and posted off the main thread via the JDK HTTP client; failures are logged to console. Test with `/orchidplugins webhook-test`.

Attribute modifiers are added under the `orchid:` namespace, so re-running a command replaces the previous value and `/aa reset` removes them cleanly. Player attribute changes may reset on death/relog depending on the server's attribute persistence; `/aa <sub>` can be re-applied at any time.

---

## Building from source

Requires JDK 21 and Maven.

Project layout:
- `code/` = plugin source (`pom.xml`, `src/`, this README)
- `output/` = built channel jars

From the project root, use the build script (or run `mvn` directly inside `code/`):

```bash
./build.sh            # both channels
./build.sh neuro      # -> output/OrchidPlugins-<ver>-neuro.jar   (testing channel)
./build.sh stable     # -> output/OrchidPlugins-<ver>-stable.jar  (promoted channel)
```

The channel version is baked into each jar's `plugin.yml`, so `/orchidplugins version` reports the channel the server is running. Bump `<orchid.version>` in `code/pom.xml` when starting a new round; promote a tested build by running `./build.sh stable`.

The shade plugin bundles and relocates Gson (`com.orchidplugins.lib.gson`) and bundles SQLite (`org.xerial:sqlite-jdbc`, natives for all platforms + JDBC service entry) so the database works out of the box. Vault and LuckPerms are optional `provided` dependencies — the plugin soft-depends on them.

---

## License

Released under the [MIT License](LICENSE). Feel free to use, modify, and distribute — attribution is appreciated but not required.