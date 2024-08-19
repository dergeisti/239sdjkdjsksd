package de.geisti.smashdashmash;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldManager implements Listener {

    private final JavaPlugin plugin;

    public WorldManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setWorldSettings();
        Bukkit.getPluginManager().registerEvents(this, plugin); // Register this class as a listener
    }

    // Setze die Welteinstellungen für alle Welten
    private void setWorldSettings() {
        Bukkit.getWorlds().forEach(world -> {
            // Setze die Spielregeln für die Welt
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); // Immer Tag
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);  // Kein Wetterwechsel
            world.setTime(1000); // Setze die Zeit auf Tag
            world.setStorm(false); // Kein Regen

            world.setGameRule(GameRule.DO_MOB_SPAWNING, false); // Keine Monster
            world.setGameRule(GameRule.SPAWN_RADIUS, 0); // Reduziere die Monster-Spawn-Rate

            world.setGameRule(GameRule.DO_INSOMNIA, false); // Keine Phantome
        });
    }

    // Verhindere das Spawnen von Monstern
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            event.setCancelled(true);
        }
    }

    // Verhindere Falldamage für alle Spieler
    @EventHandler
    public void onPlayerFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    // Verhindere Wetteränderungen (Regen, Gewitter)
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true); // Kein Regen oder Gewitter
        }
    }

    // Blockiere Zeitübersprünge, damit es immer Tag bleibt
    @EventHandler
    public void onTimeSkip(TimeSkipEvent event) {
        if (event.getSkipReason() == TimeSkipEvent.SkipReason.NIGHT_SKIP) {
            event.setCancelled(true);
        }
    }

    // Verhindere Hungerverlust für alle Spieler
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setCancelled(true); // Verhindert Hungerverlust
        if (event.getEntity() instanceof org.bukkit.entity.Player) {
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getEntity();
            player.setFoodLevel(20); // Setze das Nahrungslevel auf das Maximum
        }
    }
}
