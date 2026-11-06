# Herobrine Stalker Minecraft Plugin

Herobrine Stalker is a Minecraft plugin for Paper servers that spawns a stalking NPC near players, follows them, and makes it disappear when they keep it directly in view for five seconds.

## Install

1. Build the project with Maven and Java 21 using `mvn package`.
2. Copy the resulting `target/plugin.jar` into your server’s `plugins` folder.
3. Restart the Paper server.

## Use

- The plugin automatically checks online players for random spawns. It can spawn one stalker per player when a safe nearby location is available.
- Use `/herobrine spawn` in-game to request a stalker near yourself.
- Use `/herobrine off` to disable automatic stalking and remove existing stalkers.
- Use `/herobrine on` to enable automatic stalking again.
- The stalker disappears after you look directly at it, unobstructed, continuously for five seconds. Looking away resets the timer.

The NPC is implemented as a silent, invulnerable villager with its name hidden; it does not use a custom Herobrine model or skin. Commands require the `herobrine.use` permission, granted to operators by default.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/c434fba4-3d83-44c9-bea3-8a9b7153029c?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=c434fba4-3d83-44c9-bea3-8a9b7153029c
