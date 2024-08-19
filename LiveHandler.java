package de.geisti.smashdashmash.Handler;

import de.geisti.smashdashmash.GameStateManager.GameState;
import de.geisti.smashdashmash.GameStateManager.GameStateManager;
import de.geisti.smashdashmash.GameStateManager.Waiting;
import de.geisti.smashdashmash.Main;
import de.geisti.smashdashmash.RegionManager.Region;
import de.geisti.smashdashmash.RegionManager.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LiveHandler implements Listener {

    private final JavaPlugin plugin;
    private final RegionManager regionManager;
    private GameStateManager gameStateManager;
    private final Map<UUID, Integer> playerLives = new HashMap<>();
    private final Map<UUID, UUID> lastHitBy = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private int initialLives;
    private FileConfiguration settingsConfig;
    private File settingsFile;

    public LiveHandler(JavaPlugin plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        loadConfig();
    }

    public void setGameStateManager(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    public Main getPlugin() {
        return (Main) plugin;
    }

    private void loadConfig() {
        settingsFile = new File(plugin.getDataFolder(), "settings.yml");
        if (!settingsFile.exists()) {
            settingsFile.getParentFile().mkdirs();
            plugin.saveResource("settings.yml", false);
        }
        settingsConfig = YamlConfiguration.loadConfiguration(settingsFile);
        initialLives = settingsConfig.getInt("initialLives", 3);
        settingsConfig.set("initialLives", initialLives);
        saveConfig();
    }

    private void saveConfig() {
        try {
            settingsConfig.save(settingsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getInitialLives() {
        return initialLives;
    }

    public void setPlayerLives(Player player, int lives) {
        playerLives.put(player.getUniqueId(), lives);
        updatePlayerHealth(player);
    }

    public int getPlayerLives(Player player) {
        return playerLives.getOrDefault(player.getUniqueId(), initialLives);
    }

    public void updatePlayerHealth(Player player) {
        int lives = getPlayerLives(player);
        if (lives > 0) {
            player.setMaxHealth(lives * 2); // 1 Herz = 2 Gesundheitspunkte
            player.setHealth(player.getMaxHealth());
        } else {
            player.setMaxHealth(1); // Verhindert den Fehler durch 0
            player.setHealth(player.getMaxHealth());
            gameStateManager.setGameState(player, GameState.DEATH);
            teleportToRandomSpawnInCurrentRegion(player);
            player.sendMessage(ChatColor.RED + "Du hast alle Herzen verloren und wurdest in den Zustand DEATH versetzt.");
        }
    }

    public void handleGameStateChange(Player player, GameState newState) {
        if (newState == GameState.INGAME) {
            setPlayerLives(player, initialLives);
            teleportToRandomSpawnInCurrentRegion(player);
        }
    }

    private void teleportToRandomSpawnInCurrentRegion(Player player) {
        for (Region region : regionManager.getRegions().values()) {
            if (region.isInRegion(player.getLocation())) {
                regionManager.teleportToRandomSpawnInRegion(player, region);
                break;
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player damagedPlayer = (Player) event.getEntity();
        Entity damagerEntity = event.getDamager();
        Player damager = null;

        if (damagerEntity instanceof Player) {
            damager = (Player) damagerEntity;
            // Prevent actual damage in PvP
            event.setCancelled(true);

            // Handle XP gain based on hit direction
            double angle = damagedPlayer.getLocation().getDirection().angle(damager.getLocation().getDirection());
            if (angle < Math.PI / 2) { // Front hit
                damagedPlayer.giveExp(1); // 1 full XP
            } else { // Back hit
                damagedPlayer.giveExp(2); // 2 full XP
            }

            // Save last hit info
            lastHitBy.put(damagedPlayer.getUniqueId(), damager.getUniqueId());
            lastHitTime.put(damagedPlayer.getUniqueId(), System.currentTimeMillis());

            // Check XP overflow
            if (damagedPlayer.getTotalExperience() >= 500) {
                handlePlayerDamage(damagedPlayer, damager);
            }
        } else if (damagerEntity instanceof Arrow || damagerEntity instanceof LivingEntity) {
            // Arrow or mob hit
            event.setCancelled(true); // Prevent actual damage
            damagedPlayer.giveExp(5); // Give 5 XP for damage from arrows or mobs
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        GameState state = gameStateManager.getGameState(player);

        // Prevent falldamage if player is in a region or at all
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }

        if (state == GameState.INGAME) {
            handlePlayerDamage(player, null);
        }
    }

    private void handlePlayerDamage(Player player, Player damager) {
        int lives = getPlayerLives(player);

        if (lives > 0) {
            setPlayerLives(player, lives - 1);
            player.sendMessage(ChatColor.RED + "Du hast ein Herz verloren!");

            if (lives - 1 == 0) {
                gameStateManager.setGameState(player, GameState.DEATH);
                teleportToRandomSpawnInCurrentRegion(player);
                player.sendMessage(ChatColor.RED + "Du hast alle Herzen verloren und wurdest in den Zustand DEATH versetzt.");
                if (damager != null) {
                    Bukkit.broadcastMessage(ChatColor.RED + player.getName() + " wurde von " + damager.getName() + " besiegt.");
                }
            }
        }
    }

    public void handlePlayerLeaveRegion(Player player, Region region) {
        GameState state = gameStateManager.getGameState(player);

        if (state == GameState.INGAME) {
            handlePlayerDamage(player, null);

            if (lastHitBy.containsKey(player.getUniqueId())) {
                long lastHitTimeStamp = lastHitTime.get(player.getUniqueId());
                if (System.currentTimeMillis() - lastHitTimeStamp <= 3000) { // 3 Sekunden
                    Player killer = Bukkit.getPlayer(lastHitBy.get(player.getUniqueId()));
                    if (killer != null) {
                        Bukkit.broadcastMessage(ChatColor.RED + player.getName() + " wurde von " + killer.getName() + " getötet.");
                    }
                }
            }
        }
    }
}
