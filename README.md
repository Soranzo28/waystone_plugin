# Waystones

A Paper plugin that adds player-configured waystone networks to Minecraft, inspired by the classic [Waystones mod](https://www.curseforge.com/minecraft/mc-mods/waystones).

Craft a special Lodestone, place and name it, discover other Waystones, and connect each origin to a destination. Once a connection is configured, standing on the origin Waystone teleports the player after a configurable delay.

## Preview

<table>
  <tr>
    <td align="center"><b>Place</b><br><img width="460" height="270" src="https://github.com/user-attachments/assets/52517f1b-2b86-4c4d-92d3-19424804268a" /></td>
    <td align="center"><b>Naming</b><br><img width="460" height="270" src="https://github.com/user-attachments/assets/b5de5c34-2eef-4eb4-b225-f83c712cc90b" /></td>
  </tr>
  <tr>
    <td align="center"><b>Discovering</b><br><img width="460" height="270" src="https://github.com/user-attachments/assets/23867891-15a1-483e-94bb-d42fff0f40f8" /></td>
    <td align="center"><b>Teleport</b><br><img width="460" height="270" src="https://github.com/user-attachments/assets/defa329b-dd8e-4c6e-b907-805e332158ee" /></td>
  </tr>
  <tr>
    <td align="center"><b>GUI</b><br><img width="460" height="270" src="https://github.com/user-attachments/assets/c1559bfb-5855-405a-a953-51494f6f6e7a" /></td>
    <td align="center"><b>GUI - Select</b><br><img width="460" height="270" src="https://github.com/user-attachments/assets/c9a4ca48-e0a9-4825-b7c1-941c191fb099" /></td>
  </tr>
</table>

## Features

- Custom craftable Waystone item based on a Lodestone
- Sign-based naming and activation
- Per-player discovery lists
- Per-player origin-to-destination connections
- Paginated destination selection GUI
- Configurable teleport delay and cooldown
- Optional XP level costs based on distance
- Optional cross-dimension travel with a separate fixed cost
- Global Waystones managed by administrators
- Owner protection for blocks and attached signs
- Safe custom-item drops when Waystones are broken or destroyed by explosions
- Administrative GUI with dimension and owner filters
- Administrative commands for listing, deleting, giving, reloading, and managing Waystones
- Sounds, particles, action bars, and titles for player feedback

## Requirements

- [Paper](https://papermc.io/) `26.1.2`
- Java `25`
- Maven, only when building from source

## Installation

1. Build the plugin or download a release JAR.
2. Place the JAR in the server's `plugins/` directory.
3. Start or restart the Paper server.
4. Optionally edit `plugins/Waystones/config.yml`.
5. Run `/waystone reload` after changing the configuration, or restart the server.

The default configuration is ready to use.

## Building

Clone the repository and run:

```bash
mvn clean package
```

The generated plugin JAR will be placed in `target/`.

## Crafting

The recipe produces a glowing item named `Waystone`:

```text
[ ] [E] [ ]
[ ] [L] [ ]
[ ] [A] [ ]
```

| Symbol | Ingredient |
| --- | --- |
| `E` | Ender Pearl |
| `L` | Lodestone |
| `A` | Amethyst Shard |

A normal Lodestone does not become a Waystone. The placed item must contain the plugin's custom persistent-data marker.

## Player Guide

### 1. Place a Waystone

Place the custom Waystone item. It is registered at that block location with:

- The placing player as its owner
- The temporary name `nameless`
- An inactive state

The player needs the `waystones.place` permission.

### 2. Name and activate it

Attach a wall sign directly to the Waystone and write a name on any line. The first non-empty line is trimmed and used as the Waystone name.

Only the owner or a player with `waystones.break` can name or rename it. Breaking the attached sign marks the Waystone as inactive. Attaching and editing another wall sign activates it again.

### 3. Discover it

Right-click an active Waystone with the main hand. The first interaction adds that location to the player's personal discovery list and plays the discovery effects.

The player needs the `waystones.use` permission. Holding a sign skips the interaction so the sign can be attached normally.

### 4. Choose a destination

Right-click an already discovered, active Waystone to open the destination GUI.

The GUI contains:

- The current origin at the top
- Up to 36 discovered Waystones per page
- Coordinates, dimension, distance, and XP cost
- A glowing item for the currently selected destination
- Previous and next page controls when required

Selecting an entry stores a personal connection from the current origin to that destination. Different players can assign different destinations to the same origin.

### 5. Teleport

Stand on a Waystone that has a destination configured for your player. After the configured delay, the plugin:

1. Checks the cross-dimension setting.
2. Calculates and validates the XP level cost.
3. Removes the required levels.
4. Teleports the player one block above the destination.
5. Starts the teleport cooldown.

The teleport task checks online players every 10 server ticks.

## Teleport Costs

XP costs use whole experience levels, not raw experience points.

For destinations in the same world:

```text
cost = floor(sqrt(distance / teleport-cost-scale))
```

With the default scale of `100`:

| Distance | Cost |
| ---: | ---: |
| 99 blocks or less | 0 levels |
| 100 blocks | 1 level |
| 400 blocks | 2 levels |
| 900 blocks | 3 levels |

Cross-dimension travel uses the fixed `cross-dimension-cost` value instead of the distance formula.

## Ownership and Destruction

- Owners can break their own Waystones and attached wall signs.
- Players with `waystones.break` can break or rename another player's Waystones.
- Other players are prevented from doing so.
- Breaking a registered Waystone suppresses the normal Lodestone drop and drops the custom Waystone item.
- Explosions also remove the registration and drop the custom item.
- Removing a Waystone deletes it from all discovery lists and removes connections that use it as either origin or destination.

## Global Waystones

Administrators can mark an active Waystone as global with the command or admin GUI.

When a Waystone becomes global, it is added to the discovery lists of players already stored in `waystone.yml`. The global flag is persisted and displayed in administrative views.

Removing the global flag does not remove an existing discovery from a player's list.

## Admin Commands

All `/waystone` subcommands require `waystones.admin`.

| Command | Description |
| --- | --- |
| `/waystone list` | Opens a written book containing all Waystones. The console receives a text list instead. |
| `/waystone delete <name> [index]` | Deletes a Waystone by name. If names are duplicated, the command displays indexes to select the correct location. |
| `/waystone reload` | Reloads `config.yml` and `waystone.yml` into memory. |
| `/waystone global add` | Marks the Waystone the player is looking at, within 10 blocks, as global. |
| `/waystone global remove` | Removes the global flag from the targeted Waystone. |
| `/waystone admin` | Opens the administrative Waystone GUI. |
| `/waystone give <player>` | Gives the custom Waystone item to an online player. |

Tab completion is available for subcommands, online player names, global actions, and Waystone names.

## Admin GUI

Run `/waystone admin` to browse all registered Waystones.

The interface supports:

- Pagination with up to 45 Waystones per page
- Filtering by Overworld, Nether, or The End
- Filtering by Waystone owner
- Teleporting directly to a Waystone
- Opening a written book with its location, owner, state, and global flag
- Deleting the Waystone and its block
- Adding or removing the global flag

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `waystones.use` | Everyone | Discover Waystones, open the destination GUI, and teleport. |
| `waystones.place` | Everyone | Place the custom Waystone item. |
| `waystones.break` | Operators | Break, deactivate, or rename another player's Waystone. |
| `waystones.admin` | Operators | Use every `/waystone` administrative command. |

## Configuration

Default `config.yml`:

```yaml
# Time in ticks the player must stand on a waystone before teleporting
standing-delay: 10

# Cooldown in seconds before the player can teleport again
teleport-cooldown: 3

# Allow teleporting between different dimensions
cross-dimension: true

# Charge XP levels for teleporting
teleport-cost-enabled: true

# Same-world cost: floor(sqrt(distance / teleport-cost-scale))
teleport-cost-scale: 100

# Fixed XP level cost for cross-dimension teleporting
cross-dimension-cost: 5
```

| Option | Default | Description |
| --- | ---: | --- |
| `standing-delay` | `10` | Number of ticks represented by the required standing delay. One tick is 50 ms. |
| `teleport-cooldown` | `3` | Seconds before the player can teleport again. |
| `cross-dimension` | `true` | Enables travel between different worlds or dimensions. |
| `teleport-cost-enabled` | `true` | Enables XP level costs. |
| `teleport-cost-scale` | `100` | Controls the same-world distance cost. Higher values make travel cheaper. |
| `cross-dimension-cost` | `5` | Fixed number of levels charged when changing worlds. |

## Data Storage

Runtime data is stored in:

```text
plugins/Waystones/waystone.yml
```

The file contains:

- Waystone location, name, active state, owner UUID, and global flag
- Player UUIDs and their discovered Waystone locations
- Each player's origin-to-destination connections

Locations are serialized as:

```text
world_name,x,y,z
```

Waystones and player data are loaded into memory when the plugin starts or `/waystone reload` is executed. Teleport cooldowns are kept only in memory and are reset by a server restart.

## Project Structure

```text
src/main/java/dev/soranzo/
|-- Waystone.java
|-- WaystoneConstants.java
|-- WaystoneManager.java
|-- WaystoneRecipe.java
|-- WaystoneTeleportTask.java
|-- commands/
|-- dto/
|-- gui/
`-- listeners/
```

- `Waystone` initializes the plugin and registers its services.
- `WaystoneManager` manages YAML persistence and in-memory state.
- `WaystoneRegisterListener` handles placement, signs, discovery, destruction, and the player GUI.
- `WaystoneTeleportTask` performs automatic teleports.
- `WaystoneCommand` and the admin GUI classes provide management tools.

## Author

Created by [Soranzo](https://github.com/Soranzo28).
