package de.geisti.smashdashmash.Setup;

import de.geisti.smashdashmash.GameStateManager.GameState;
import de.geisti.smashdashmash.GameStateManager.GameStateManager;
import de.geisti.smashdashmash.RegionManager.Region;
import de.geisti.smashdashmash.RegionManager.RegionManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Setup implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final GameStateManager gameStateManager;
    private final RegionManager regionManager;
    private final Set<UUID> buildModePlayers = new HashSet<>();
    private final Set<UUID> setupModePlayers = new HashSet<>();

    public Setup(JavaPlugin plugin, GameStateManager gameStateManager, RegionManager regionManager) {
        this.plugin = plugin;
        this.gameStateManager = gameStateManager;
        this.regionManager = regionManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (command.getName().equalsIgnoreCase("build")) {
            if (buildModePlayers.contains(playerId)) {
                deactivateBuildMode(player);
            } else {
                activateBuildMode(player);
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("setup")) {
            if (setupModePlayers.contains(playerId)) {
                deactivateSetupMode(player);
            } else {
                activateSetupMode(player);
            }
            return true;
        }

        return false;
    }

    private void activateBuildMode(Player player) {
        buildModePlayers.add(player.getUniqueId());
        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().clear();
        giveTeleportItem(player);
        player.sendMessage(ChatColor.GREEN + "Build-Modus aktiviert.");
    }

    private void deactivateBuildMode(Player player) {
        buildModePlayers.remove(player.getUniqueId());
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.sendMessage(ChatColor.RED + "Build-Modus deaktiviert.");
    }

    private void activateSetupMode(Player player) {
        setupModePlayers.add(player.getUniqueId());
        player.setGameMode(GameMode.CREATIVE);
        player.sendMessage(ChatColor.GREEN + "Setup-Modus aktiviert. Du kannst nun Regionen verlassen, ohne teleportiert zu werden.");
    }

    private void deactivateSetupMode(Player player) {
        setupModePlayers.remove(player.getUniqueId());
        player.setGameMode(GameMode.SURVIVAL);
        player.sendMessage(ChatColor.RED + "Setup-Modus deaktiviert.");
    }

    private void giveTeleportItem(Player player) {
        ItemStack teleportItem = new ItemStack(Material.COMPASS);
        ItemMeta meta = teleportItem.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Teleport");
        teleportItem.setItemMeta(meta);
        player.getInventory().setItem(0, teleportItem);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (buildModePlayers.contains(player.getUniqueId())) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.COMPASS && item.getItemMeta() != null &&
                    item.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Teleport")) {
                Location targetLocation = player.getTargetBlock(null, 100).getLocation();
                player.teleport(targetLocation);
                player.sendMessage(ChatColor.GREEN + "Teleportiert zu: " + ChatColor.YELLOW + "X: " + targetLocation.getBlockX() + " Y: " + targetLocation.getBlockY() + " Z: " + targetLocation.getBlockZ());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameState state = gameStateManager.getGameState(player);
        if (state != GameState.INGAME && !buildModePlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Du kannst keine Blöcke platzieren, wenn du nicht im INGAME- oder Build-Modus bist.");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GameState state = gameStateManager.getGameState(player);
        if (state != GameState.INGAME && !buildModePlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Du kannst keine Blöcke abbauen, wenn du nicht im INGAME- oder Build-Modus bist.");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Überprüfen, ob der Spieler im Setup-Modus ist
        if (setupModePlayers.contains(playerId)) {
            return;  // Verhindere jegliches Teleportieren im Setup-Modus
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        Region currentRegion = null;

        // Überprüfen, ob der Spieler sich in einer Region befindet
        for (Region region : regionManager.getRegions().values()) {
            if (region.isInRegion(from)) {
                currentRegion = region;
                break;
            }
        }

        // Wenn der Spieler eine Region verlässt und nicht im Setup-Modus ist, teleportiere ihn
        if (currentRegion != null && !currentRegion.isInRegion(to)) {
            regionManager.teleportToRandomSpawnInRegion(player, currentRegion);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (buildModePlayers.contains(player.getUniqueId())) {
            deactivateBuildMode(player);
        }
        if (setupModePlayers.contains(player.getUniqueId())) {
            deactivateSetupMode(player);
        }
    }
}
