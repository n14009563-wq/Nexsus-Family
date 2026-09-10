# NexusFamily v0.1.0

Marry another player, have a kid together (a real baby villager who grows up), gear them out,
and talk to them. No animations, no romance content of any kind -- this is a social/family
system built entirely around commands, an optional ring item, and a villager.

## Marriage

Two ways to get married, both leading to the exact same state, so they never disagree:

- **Commands**: `/marry propose <player>`, then they run `/marry accept` (or `/marry deny`).
  Proposals expire after `marriage.proposal-timeout-seconds` (default 60s) if not answered.
- **Wedding Ring**: right-click another player while holding one. If they had already rung
  *you* first (their own pending proposal to you is still open), your right-click is treated
  as accepting it instead of sending a second one -- so two players tapping each other with
  rings, in either order, just get married. The ring isn't consumed; it's reusable.

Either player can end it with `/marry divorce` at any time -- no confirmation, immediate. You
can only be married to one person at a time; divorce before proposing to someone new.
`/marry status` shows who you're married to and for how long. Admins can hand out rings
directly with `/marry ring <player> [amount]`, and the ring is also craftable by default
(shapeless: a gold ingot + a diamond, any arrangement) -- turn that off with
`marriage.ring.recipe.enabled: false`.

Marriage state is stored directly on each player's own data (via their
PersistentDataContainer), the same trick NexusWarbeasts uses for its mobs -- no separate
database, works whether the spouse is online or not, survives restarts automatically.

## Kids

While a married couple are both online and standing within `kids.proximity-radius` blocks of
each other, the plugin rolls `kids.chance-percent` on every `kids.check-interval-minutes`
sweep to have them a kid -- fully passive, no command, no item, nothing to click. A couple
stops being offered new kids once they hit `kids.max-kids-per-couple`.

A kid is a real Villager, spawned as a baby with a random name from `kids.default-names`
(change it any time with `/family rename <name>` while looking at them). It grows into an
adult after `kids.growth-minutes` (default 20, matching vanilla's own baby-to-adult time) --
handled by the plugin's own timer rather than vanilla's, so it's exact and configurable, and
it's re-armed automatically if the plugin restarts mid-growth.

Right-clicking a kid always stops vanilla's own trading behavior (kids aren't real villager
traders here) and instead gets you a quick, config-driven greeting -- a different pool of
lines if you're one of its parents versus a stranger. **Shift**-right-click instead (if you're
a parent or an admin) to open the management menu:

- **Inventory** -- a real 27-slot storage GUI. Put armor, tools, food, whatever you want in
  it. One thing worth knowing up front: vanilla villagers don't visually wear or hold
  equipment -- there's no vanilla renderer for it on this mob. The items are genuinely stored
  and safe (and could power future features), they just won't appear on the kid's body. This
  was a deliberate tradeoff so the kid stays a real, growing, ordinary-looking villager instead
  of a fake NPC skin that would need vanilla growth/behavior rebuilt from scratch.
- **Info** -- shows both parents' names and whether the kid has grown up yet.

## The "basic AI" talk feature

Two separate things, both mentioned in the original ask:

- **Give them things to say**: `/family addline <line>` (while looking at a kid, parent/admin
  only) adds a line to that specific kid's own pool. Right now those personal lines aren't
  wired into a specific trigger yet in v0.1 beyond being stored -- flagged below as the most
  obvious next addition (e.g. having the kid say one at random alongside its regular greeting).
- **Talk back**: `/family talk <message>` (anyone, while looking at a kid) runs your message
  through plain keyword matching against `dialogue.responses` in the config -- if every keyword
  in an entry appears anywhere in your message, one of its configured replies is picked at
  random; otherwise you get a random line from `dialogue.fallback`. This is **not** a real AI
  model -- no network call, no API key, no per-message cost, entirely config-driven and
  editable by you. You picked this over hooking up a real AI API, which would need your own key
  and cost money per message.

## Commands

- `/marry propose|accept|deny|divorce|status|ring` -- see Marriage above.
- `/family talk <message>` -- anyone, talks to the kid you're looking at.
- `/family addline <line>` -- parent/admin, adds a personal line to the kid you're looking at.
- `/family rename <name>` -- parent/admin, renames the kid you're looking at.
- `/family info` -- shows a kid's parents and growth status (same as the Info button).
- `/nexusfamily reload` -- admin, reloads config.yml and the ring's material/name/lore.

## Config (`config.yml`)

- `marriage.proposal-timeout-seconds`, `marriage.ring.*` (material/name/lore/recipe) -- see
  Marriage above.
- `kids.check-interval-minutes`, `chance-percent`, `proximity-radius`, `max-kids-per-couple`,
  `growth-minutes`, `default-names` -- see Kids above.
- `kids.management-open-to-everyone` -- if true, anyone (not just parents/admins) can
  shift-right-click a kid to open its management menu. Off by default.
- `dialogue.greetings`, `parent-greetings`, `responses`, `fallback` -- see the talk feature
  above.
- `messages.*` -- all the feedback/broadcast text, with `{player1}`/`{player2}`/`{target}`/
  `{proposer}`/`{name}`/`{old}`/`{new}`/`{seconds}` placeholders where applicable.

## Setup

1. Build with `mvn clean package` (needs network access to `repo.papermc.io` and Maven
   Central -- see the verification note below, this build environment doesn't have that).
   The pom is pinned to `1.21.4-R0.1-SNAPSHOT`; bump that version string first if you're on a
   newer Minecraft release.
2. Drop the jar into `plugins/` and restart.
3. `config.yml` and `kids.yml` (a small internal index, not meant to be hand-edited) generate
   under `plugins/NexusFamily/` on first run.

## How this was verified (please read)

Same situation as NexusGate, the NexusNPC fixes, and NexusWarbeasts: I don't have network
access to Maven Central or PaperMC's repository in this environment, so I couldn't run the
real `mvn compile`/`mvn package` against the actual Paper API jars.

What I did instead: hand-wrote a stand-in Java library reproducing the exact method and class
signatures this plugin actually calls -- `Villager`/`Ageable` (for the baby-to-adult growth),
`PersistentDataContainer`, `Inventory`/`InventoryHolder` (for the two GUIs), `OfflinePlayer`
(for marriage data that has to survive a spouse logging off), `BukkitObjectOutputStream` (for
saving a kid's inventory contents), and so on -- compiled that stub with plain `javac`, then
compiled all 18 of this plugin's source files against it. That came back completely clean --
zero errors, zero warnings even with `-Xlint:all` on.

That confirms the code is internally consistent -- no typos, no wrong types, no calls to
methods that don't exist by the signature I wrote -- but it is **not** a guarantee it matches
the real Bukkit/Paper jars exactly, since I couldn't fetch those to check against. If
`mvn package` throws anything when you actually build it, send me the error output (or the
project) and I'll fix it directly, same as I did with NexusNPC.

## What's deliberately not in v0.1.0

A few things I scoped out to keep the first version buildable and correct, rather than guess
at riskier APIs:

- **No in-game text-entry GUI** (an anvil-style rename prompt) -- renaming is a command
  (`/family rename <name>`) instead, to avoid a less-stable, more version-sensitive part of
  the API.
- **A kid's personal lines aren't triggered automatically yet** -- `/family addline` stores
  them, but nothing currently makes the kid say one on its own (e.g. randomly alongside its
  greeting). Easy to wire in if you want that behavior specifically.
- **No appearance customization** based on parents (skin, profession, etc.) -- kids spawn as a
  plain default villager.

Happy to add any of these, or go further on the dialogue system (more keyword rules, moods,
per-kid personality), whenever you want the next pass.
