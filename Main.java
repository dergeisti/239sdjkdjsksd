package de.geisti.smashdashmash;

import java.util.List;
import de.geisti.smashdashmash.GameStateManager.*;
import de.geisti.smashdashmash.Handler.LiveHandler;
import de.geisti.smashdashmash.RegionManager.RegionManager;
import de.geisti.smashdashmash.Setup.Setup;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;


public class Main extends JavaPlugin {

    private RegionManager regionManager;
    private GameStateManager gameStateManager;
    private LiveHandler liveHandler;
    private WorldManager worldManager;
    private Waiting waiting;
    private DEATHSPEC deathSpec;  // Neue DEATHSPEC-Instanz

    @Override
    public void onEnable() {
        // Zuerst den RegionManager initialisieren
        regionManager = new RegionManager(this, null, null); // Zuerst ohne GameStateManager und LiveHandler

        // Jetzt den LiveHandler und GameStateManager initialisieren
        liveHandler = new LiveHandler(this, regionManager);
        gameStateManager = new GameStateManager(liveHandler);

        // Waiting-Klasse initialisieren
        waiting = new Waiting(this, gameStateManager);

        // DEATHSPEC-Klasse initialisieren
        deathSpec = new DEATHSPEC(this, gameStateManager, regionManager);

        // Jetzt den RegionManager mit den korrekten Instanzen initialisieren
        regionManager = new RegionManager(this, gameStateManager, liveHandler);

        // LiveHandler bekommt den nun vollständig initialisierten GameStateManager
        liveHandler.setGameStateManager(gameStateManager);

        // WorldManager initialisieren
        worldManager = new WorldManager(this);

        registerCommands();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            regionManager.loadConfig();
            getLogger().info("Regionen wurden geladen.");
        }, 20L); // 1 Sekunde Verzögerung, um sicherzustellen, dass Welten geladen sind

        Bukkit.getPluginManager().registerEvents(regionManager, this);
        Bukkit.getPluginManager().registerEvents(liveHandler, this); // Register LiveHandler
        Bukkit.getPluginManager().registerEvents(worldManager, this); // Register WorldManager
        Bukkit.getPluginManager().registerEvents(deathSpec, this); // Register DEATHSPEC

        getLogger().info("RegionSystem Plugin aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("RegionSystem Plugin deaktiviert.");
    }

    private void registerCommands() {
        getCommand("pos1").setExecutor(regionManager);
        getCommand("pos2").setExecutor(regionManager);
        getCommand("saveregion").setExecutor(regionManager);
        getCommand("regions").setExecutor(regionManager);
        getCommand("setspawn").setExecutor(regionManager);
        getCommand("region").setExecutor(regionManager);
        getCommand("setvoid").setExecutor(regionManager);
        getCommand("delregion").setExecutor(regionManager);
        getCommand("delspawn").setExecutor(regionManager);
        getCommand("gamestate").setExecutor(new GameStateCommand(gameStateManager));
        getCommand("setgamestate").setExecutor(new GameStateCommand(gameStateManager));
    }

    public Waiting getWaiting() {
        return waiting;
    }

    public DEATHSPEC getDeathSpec() {
        return deathSpec;
    }
}
