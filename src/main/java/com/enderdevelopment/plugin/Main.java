package com.enderdevelopment.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Main extends JavaPlugin implements Listener {
    private final Map<UUID, UUID> stalkers = new HashMap<>();
    private final Map<UUID, Integer> stareTicks = new HashMap<>();
    private BukkitTask task;
    private boolean automatic = true;
    private long nextSpawnCheck;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("herobrine").setExecutor(this);
        task = Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 5L);
        getLogger().info("HerobrineStalker enabled.");
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        for (UUID npc : stalkers.values()) {
            Entity entity = Bukkit.getEntity(npc);
            if (entity != null) entity.remove();
        }
        stalkers.clear();
        stareTicks.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            UUID npcId = stalkers.get(playerId);
            Entity entity = npcId == null ? null : Bukkit.getEntity(npcId);
            if (entity == null || !entity.isValid()) {
                stalkers.remove(playerId);
                stareTicks.remove(playerId);
                if (automatic && now >= nextSpawnCheck && Math.random() < 0.12) {
                    spawnNear(player);
                    nextSpawnCheck = now + 15000L;
                }
                continue;
            }

            if (isLookingAt(player, entity)) {
                int ticks = stareTicks.merge(playerId, 5, Integer::sum);
                if (ticks >= 100) {
                    entity.remove();
                    stalkers.remove(playerId);
                    stareTicks.remove(playerId);
                    player.sendMessage(Component.text("Herobrine vanished into the darkness.", NamedTextColor.GRAY));
                    continue;
                }
            } else {
                stareTicks.remove(playerId);
            }

            stalkToward(player, (LivingEntity) entity);
        }
    }

    private boolean isLookingAt(Player player, Entity target) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        RayTraceResult entityHit = player.getWorld().rayTraceEntities(eye, direction, 48.0, 0.0,
                candidate -> candidate.getUniqueId().equals(target.getUniqueId()));
        if (entityHit == null || entityHit.getHitEntity() == null) return false;

        RayTraceResult blockHit = player.getWorld().rayTraceBlocks(eye, direction, 48.0,
                org.bukkit.FluidCollisionMode.NEVER, true);
        return blockHit == null || blockHit.getHitPosition().distanceSquared(eye.toVector())
                > entityHit.getHitPosition().distanceSquared(eye.toVector());
    }

    private void stalkToward(Player player, LivingEntity npc) {
        Location playerLocation = player.getLocation();
        Location npcLocation = npc.getLocation();
        if (npcLocation.getWorld() != playerLocation.getWorld()) return;
        double distance = npcLocation.distance(playerLocation);
        if (distance > 22.0) {
            Location destination = findSpawnLocation(player, 10.0 + Math.random() * 5.0);
            if (destination != null) npc.teleport(destination);
            return;
        }
        if (distance > 5.5) {
            Vector movement = playerLocation.toVector().subtract(npcLocation.toVector()).normalize().multiply(0.32);
            Location next = npcLocation.clone().add(movement);
            next.setDirection(playerLocation.toVector().subtract(next.toVector()));
            npc.teleport(next);
        } else {
            Location facing = npcLocation.clone();
            facing.setDirection(playerLocation.toVector().subtract(npcLocation.toVector()));
            npc.teleport(facing);
        }
    }

    private void spawnNear(Player player) {
        if (stalkers.containsKey(player.getUniqueId())) return;
        Location location = findSpawnLocation(player, 9.0 + Math.random() * 7.0);
        if (location == null) return;
        World world = player.getWorld();
        Villager npc = (Villager) world.spawnEntity(location, EntityType.VILLAGER);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setCollidable(false);
        npc.setAdult();
        npc.setProfession(Villager.Profession.NONE);
        npc.customName(Component.text("Herobrine", NamedTextColor.WHITE));
        npc.setCustomNameVisible(false);
        npc.setAware(false);
        location.setDirection(player.getLocation().toVector().subtract(location.toVector()));
        npc.teleport(location);
        stalkers.put(player.getUniqueId(), npc.getUniqueId());
    }

    private Location findSpawnLocation(Player player, double radius) {
        Location origin = player.getLocation();
        World world = player.getWorld();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = Math.random() * Math.PI * 2.0;
            int x = origin.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int z = origin.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z) + 1;
            if (y >= world.getMinHeight() && y + 1 < world.getMaxHeight()
                    && world.getBlockAt(x, y, z).isPassable()
                    && world.getBlockAt(x, y + 1, z).isPassable()
                    && world.getBlockAt(x, y - 1, z).getType().isSolid()) {
                Location result = new Location(world, x + 0.5, y, z + 0.5);
                if (result.distanceSquared(origin) >= 36.0) return result;
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("herobrine.use")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /herobrine <on|off|spawn>", NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "on" -> {
                automatic = true;
                sender.sendMessage(Component.text("Random Herobrine stalking enabled.", NamedTextColor.GREEN));
            }
            case "off" -> {
                automatic = false;
                for (UUID npc : stalkers.values()) {
                    Entity entity = Bukkit.getEntity(npc);
                    if (entity != null) entity.remove();
                }
                stalkers.clear();
                stareTicks.clear();
                sender.sendMessage(Component.text("Herobrine stalking disabled.", NamedTextColor.GREEN));
            }
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
                    return true;
                }
                if (stalkers.containsKey(player.getUniqueId())) {
                    sender.sendMessage(Component.text("Herobrine is already stalking you.", NamedTextColor.YELLOW));
                } else {
                    spawnNear(player);
                    sender.sendMessage(Component.text("Herobrine is nearby.", NamedTextColor.GRAY));
                }
            }
            default -> sender.sendMessage(Component.text("Usage: /herobrine <on|off|spawn>", NamedTextColor.YELLOW));
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID npcId = stalkers.remove(event.getPlayer().getUniqueId());
        stareTicks.remove(event.getPlayer().getUniqueId());
        if (npcId != null) {
            Entity entity = Bukkit.getEntity(npcId);
            if (entity != null) entity.remove();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        stareTicks.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (stalkers.containsValue(event.getEntity().getUniqueId())) event.setCancelled(true);
    }
}
