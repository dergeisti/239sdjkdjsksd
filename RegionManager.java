package de.geisti.smashdashmash.RegionManager;

import de.geisti.smashdashmash.GameStateManager.GameState;
import de.geisti.smashdashmash.GameStateManager.GameStateManager;
import de.geisti.smashdashmash.Handler.LiveHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RegionManager implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final GameStateManager gameStateManager;
    private final LiveHandler liveHandler;
    private Location pos1;
    private Location pos2;
    private Location voidLocation;
    private final Map<String, Region> regions = new HashMap<>();
    private FileConfiguration regionConfig;
    private File regionFile;


    public RegionManager(JavaPlugin plugin, GameStateManager gameStateManager, LiveHandler liveHandler) {
        this.plugin = plugin;
        this.gameStateManager = gameStateManager;
        this.liveHandler = liveHandler;
    }

    public void loadConfig() {
        regionFile = new File(plugin.getDataFolder(), "region.yml");
        if (!regionFile.exists()) {
            regionFile.getParentFile().mkdirs();
            plugin.saveResource("region.yml", false);
        }
        regionConfig = YamlConfiguration.loadConfiguration(regionFile);
        loadRegionsFromConfig();
        voidLocation = loadLocationFromConfig("voidLocation");
    }

    private Location loadLocationFromConfig(String path) {
        String worldName = regionConfig.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = regionConfig.getDouble(path + ".x");
        double y = regionConfig.getDouble(path + ".y");
        double z = regionConfig.getDouble(path + ".z");
        float yaw = (float) regionConfig.getDouble(path + ".yaw");
        float pitch = (float) regionConfig.getDouble(path + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    private void saveLocationToConfig(String path, Location loc) {
        if (loc != null) {
            regionConfig.set(path + ".world", loc.getWorld().getName());
            regionConfig.set(path + ".x", loc.getX());
            regionConfig.set(path + ".y", loc.getY());
            regionConfig.set(path + ".z", loc.getZ());
            regionConfig.set(path + ".yaw", loc.getYaw());
            regionConfig.set(path + ".pitch", loc.getPitch());
        }
    }

    private void loadRegionsFromConfig() {
        if (regionConfig.isConfigurationSection("regions")) {
            for (String regionName : regionConfig.getConfigurationSection("regions").getKeys(false)) {
                Location pos1 = loadLocationFromConfig("regions." + regionName + ".pos1");
                Location pos2 = loadLocationFromConfig("regions." + regionName + ".pos2");

                if (pos1 != null && pos2 != null) {
                    Region region = new Region(regionName, pos1, pos2);
                    if (regionConfig.isConfigurationSection("regions." + regionName + ".spawns")) {
                        for (String index : regionConfig.getConfigurationSection("regions." + regionName + ".spawns").getKeys(false)) {
                            Location spawn = loadLocationFromConfig("regions." + regionName + ".spawns." + index);
                            region.setSpawn(Integer.parseInt(index), spawn);
                        }
                    }
                    regions.put(regionName, region);
                } else {
                    plugin.getLogger().warning("Region " + regionName + " konnte nicht geladen werden, da eine oder beide Positionen null sind.");
                }
            }
        }
    }

    private void saveRegionToConfig(Region region) {
        String path = "regions." + region.getName();
        saveLocationToConfig(path + ".pos1", region.getPos1());
        saveLocationToConfig(path + ".pos2", region.getPos2());
        for (int i = 1; i <= 100; i++) {
            Location spawn = region.getSpawn(i);
            if (spawn != null) {
                saveLocationToConfig(path + ".spawns." + i, spawn);
            }
        }
        saveConfigFile();
    }

    private void saveConfigFile() {
        try {
            regionConfig.save(regionFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Region> getRegions() {
        return regions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            switch (command.getName().toLowerCase()) {
                case "pos1":
                    pos1 = player.getLocation();
                    player.sendMessage(ChatColor.GREEN + "Position 1 gesetzt.");
                    return true;

                case "pos2":
                    pos2 = player.getLocation();
                    player.sendMessage(ChatColor.GREEN + "Position 2 gesetzt.");
                    return true;

                case "saveregion":
                    if (args.length != 1) {
                        player.sendMessage(ChatColor.RED + "Bitte benutze /saveregion <Regionname> oder füge eine neue Region hinzu.");
                        return true;
                    }
                    if (pos1 == null || pos2 == null) {
                        player.sendMessage(ChatColor.RED + "Bitte setze beide Positionen zuerst mit /pos1 und /pos2.");
                        return true;
                    }
                    String regionName = args[0];
                    Region region = new Region(regionName, pos1, pos2);
                    regions.put(regionName, region);
                    saveRegionToConfig(region);
                    player.sendMessage(ChatColor.GREEN + "Region " + regionName + " gespeichert.");
                    return true;

                case "regions":
                    player.sendMessage(ChatColor.GREEN + "Existierende Regionen:");
                    for (String name : regions.keySet()) {
                        player.sendMessage(ChatColor.YELLOW + "- " + name);
                    }
                    return true;

                case "setspawn":
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.RED + "Bitte benutze /setspawn <Regionname> <1-100>");
                        return true;
                    }
                    regionName = args[0];
                    int spawnIndex;
                    try {
                        spawnIndex = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Ungültiger Spawn-Index.");
                        return true;
                    }
                    region = regions.get(regionName);
                    if (region == null) {
                        player.sendMessage(ChatColor.RED + "Region nicht gefunden.");
                        return true;
                    }
                    region.setSpawn(spawnIndex, player.getLocation());
                    saveRegionToConfig(region);
                    player.sendMessage(ChatColor.GREEN + "Spawnpunkt " + spawnIndex + " in der Region " + regionName + " gesetzt.");
                    return true;

                case "region":
                    if (args.length != 3 || !args[0].equalsIgnoreCase("tp")) {
                        player.sendMessage(ChatColor.RED + "Bitte benutze /region tp <Regionname> <1-100>");
                        return true;
                    }
                    regionName = args[1];
                    try {
                        spawnIndex = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Ungültiger Spawn-Index.");
                        return true;
                    }
                    region = regions.get(regionName);
                    if (region == null) {
                        player.sendMessage(ChatColor.RED + "Region nicht gefunden.");
                        return true;
                    }
                    Location spawn = region.getSpawn(spawnIndex);
                    if (spawn == null) {
                        player.sendMessage(ChatColor.RED + "Kein Spawnpunkt an diesem Index gefunden.");
                        return true;
                    }
                    player.teleport(spawn);
                    player.sendMessage(ChatColor.GREEN + "Teleportiert zu " + regionName + " Spawn " + spawnIndex + ".");
                    return true;

                case "setvoid":
                    voidLocation = player.getLocation();
                    saveLocationToConfig("voidLocation", voidLocation);
                    saveConfigFile();
                    player.sendMessage(ChatColor.GREEN + "Void Location für 'waiting' Region gesetzt.");
                    return true;

                // Neuer Befehl: delspawn
                case "delspawn":
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.RED + "Bitte benutze /delspawn <Regionname> <1-100>");
                        return true;
                    }
                    regionName = args[0];
                    try {
                        spawnIndex = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Ungültiger Spawn-Index.");
                        return true;
                    }
                    region = regions.get(regionName);
                    if (region == null) {
                        player.sendMessage(ChatColor.RED + "Region nicht gefunden.");
                        return true;
                    }
                    if (region.getSpawn(spawnIndex) == null) {
                        player.sendMessage(ChatColor.RED + "Kein Spawnpunkt an diesem Index vorhanden.");
                        return true;
                    }
                    region.setSpawn(spawnIndex, null);
                    saveRegionToConfig(region);
                    player.sendMessage(ChatColor.GREEN + "Spawnpunkt " + spawnIndex + " in der Region " + regionName + " gelöscht.");
                    return true;

                // Neuer Befehl: delregion
                case "delregion":
                    if (args.length != 1) {
                        player.sendMessage(ChatColor.RED + "Bitte benutze /delregion <Regionname>");
                        return true;
                    }
                    regionName = args[0];
                    if (!regions.containsKey(regionName)) {
                        player.sendMessage(ChatColor.RED + "Region nicht gefunden.");
                        return true;
                    }
                    regions.remove(regionName);
                    regionConfig.set("regions." + regionName, null);
                    saveConfigFile();
                    player.sendMessage(ChatColor.GREEN + "Region " + regionName + " gelöscht.");
                    return true;

                default:
                    return false;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GameState initialState = gameStateManager.isAnyPlayerInNonWaitingState() ? GameState.SPECTATOR : GameState.WAITING;
        gameStateManager.setGameState(player, initialState);

        if (initialState == GameState.INGAME) {
            liveHandler.setPlayerLives(player, liveHandler.getInitialLives());
        }

        Region waitingRegion = regions.get("waiting");
        if (waitingRegion != null) {
            Location spawn = waitingRegion.getSpawn(1); // Beispiel: Standardspawn
            if (spawn != null) {
                player.teleport(spawn);
                player.sendMessage(ChatColor.GREEN + "Willkommen! Du wurdest zum 'waiting'-Region-Spawn teleportiert.");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        Region currentRegion = null;
        for (Region region : regions.values()) {
            if (region.isInRegion(from)) {
                currentRegion = region;
                break;
            }
        }

        if (currentRegion != null && !currentRegion.isInRegion(to)) {
            liveHandler.handlePlayerLeaveRegion(player, currentRegion);
            teleportToRandomSpawnInRegion(player, currentRegion);
        }

        if (voidLocation != null && voidLocation.getWorld().equals(to.getWorld()) && voidLocation.distance(to) < 1) {
            teleportToWaitingSpawn(player);
        }
    }

    public void teleportToRandomSpawnInRegion(Player player, Region region) {
        List<Location> availableSpawns = new ArrayList<>();

        // Sammle alle verfügbaren Spawnpunkte in der Region
        for (int i = 1; i <= 100; i++) {
            Location spawn = region.getSpawn(i);
            if (spawn != null && isSpawnPointFree(spawn)) {
                availableSpawns.add(spawn);
            }
        }

        // Wenn es verfügbare Spawnpunkte gibt, wähle einen zufällig aus
        if (!availableSpawns.isEmpty()) {
            Random random = new Random();
            Location randomSpawn = availableSpawns.get(random.nextInt(availableSpawns.size()));
            player.teleport(randomSpawn);
            player.sendMessage(ChatColor.GREEN + "Du hast die Region verlassen und wurdest zu einem freien Spawnpunkt in der Region " + region.getName() + " teleportiert.");
        } else {
            player.sendMessage(ChatColor.RED + "Es konnte kein freier Spawnpunkt in der Region " + region.getName() + " gefunden werden.");
        }
    }

    private boolean isSpawnPointFree(Location location) {
        // Überprüfen, ob sich ein anderer Spieler an diesem Spawnpunkt befindet
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getLocation().distance(location) < 1) {
                return false;
            }
        }
        return true;
    }

    private void teleportToWaitingSpawn(Player player) {
        Region waitingRegion = regions.get("waiting");
        if (waitingRegion != null) {
            Location spawn = waitingRegion.getSpawn(1); // Beispiel: Standardspawn
            if (spawn != null) {
                player.teleport(spawn);
                player.sendMessage(ChatColor.GREEN + "Du hast die Void Location betreten und wurdest zum 'waiting'-Region-Spawn teleportiert.");
            }
        }
    }
}
