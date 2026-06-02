# Waystones

A Paper plugin that brings a waystone system to vanilla Minecraft — inspired by the classic [Waystones mod](https://www.curseforge.com/minecraft/mc-mods/waystones). Place a Lodestone, name it with a sign, and teleport between discovered locations.

---

## Preview

> _Add your GIFs and screenshots here_

| Discovering a waystone | Setting a destination | Teleporting |
|---|---|---|
| ![discover]() | ![destination]() | ![teleport]() |

---

## Features

- **Craft** a special Waystone item using a custom recipe
- **Place** it anywhere in the world to register a new waystone
- **Name** it by attaching a wall sign — the waystone activates
- **Discover** waystones by right-clicking them for the first time
- **Set destinations** through a per-player GUI with pagination
- **Teleport** by standing on a waystone you own a connection for
- **Per-player connections** — each player configures their own network independently
- **Explosion-safe** — waystones drop as items when destroyed by explosions
- Visual and audio feedback throughout all interactions

---

## Requirements

- [Paper](https://papermc.io) `26.1.2`
- Java `25`

---

## Installation

1. Download the `.jar` from the [releases page](#)
2. Drop it in your server's `plugins/` folder
3. Restart the server

No configuration needed — the plugin works out of the box.

---

## How to Use

### 1. Craft a Waystone

```
[ ]  [E]  [ ]
[ ]  [L]  [ ]
[ ]  [A]  [ ]
```

| Symbol | Item |
|---|---|
| `E` | Ender Pearl |
| `L` | Lodestone |
| `A` | Amethyst Shard |

---

### 2. Place it

Put the Waystone block on the ground. It starts inactive and unnamed.

---

### 3. Name it

Attach a **wall sign** to the Waystone and write its name on the first line. The waystone activates as soon as you finish writing.

> Breaking the sign deactivates the waystone. Replacing it with a new sign reactivates it.

---

### 4. Discover it

**Right-click** an active waystone you've never visited before to discover it. You'll only see the teleport GUI for waystones you've already discovered.

---

### 5. Set a destination

Right-click a discovered waystone to open your personal GUI. Select any other discovered waystone to set it as your destination for that origin point.

- 🟡 **Gold** — the waystone you're currently at
- 🟢 **Green** — your currently configured destination
- ⚫ **Gray** — other available waystones

---

### 6. Teleport

Stand on a waystone that has a destination configured. You'll be teleported automatically after a short moment.

---

## Data

Player discoveries and connections are stored in `plugins/Waystones/waystone.yml`. Each player's network is independent — discovering or connecting waystones only affects your own data.

---

## Author

Made by [soranzo](https://github.com/soranzo)
