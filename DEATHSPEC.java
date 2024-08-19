package de.geisti.smashdashmash.GameStateManager;

import de.geisti.smashdashmash.RegionManager.Region;
import de.geisti.smashdashmash.RegionManager.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DEATHSPEC implements Listener {

    private final JavaPlugin plugin;
    private final GameStateManager gameStateManager;
    private final RegionManager regionManager;
    private final Map<UUID, GameMode> originalGameModes = new HashMap<>();

    public DEATHSPEC(JavaPlugin plugin, GameStateManager gameStateManager, RegionManager regionManager) {
        this.plugin = plugin;
        this.gameStateManager = gameStateManager;
        this.regionManager = regionManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void handleDeathState(Player player) {
        setInvisibleAndInvulnerable(player);
        teleportToRandomSpawnInCurrentRegion(player);
        giveDeathOrSpectatorItems(player);
        player.setHealth(player.getMaxHealth()); // Set full health within valid range
        originalGameModes.put(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.ADVENTURE); // Set to adventure mode
    }

    public void handleSpectatorState(Player player) {
        setInvisibleAndInvulnerable(player);
        teleportToRandomSpawnInCurrentRegion(player);
        giveDeathOrSpectatorItems(player);
        player.setHealth(1.0); // Set health to max for spectator mode
        originalGameModes.put(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR); // Set to spectator mode
    }

    private void setInvisibleAndInvulnerable(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (gameStateManager.getGameState(onlinePlayer) == GameState.INGAME) {
                onlinePlayer.hidePlayer(plugin, player); // Make the player invisible to others
            }
        }
        player.setCollidable(false); // Prevent pushing other players
        player.setInvulnerable(true); // Prevent taking damage
    }

    private void giveDeathOrSpectatorItems(Player player) {
        player.getInventory().clear(); // Clear the player's inventory

        // Slot 0: Compass with custom name
        ItemStack teleporter = new ItemStack(Material.COMPASS);
        ItemMeta teleporterMeta = teleporter.getItemMeta();
        teleporterMeta.setDisplayName(ChatColor.AQUA + "Teleporter " + ChatColor.WHITE + "| (Right / Left Click)");
        teleporter.setItemMeta(teleporterMeta);
        player.getInventory().setItem(0, teleporter);

        player.getInventory().setHeldItemSlot(0); // Set default selected slot to 0
    }

    private void teleportToRandomSpawnInCurrentRegion(Player player) {
        for (Region region : regionManager.getRegions().values()) {
            if (region.isInRegion(player.getLocation())) {
                regionManager.teleportToRandomSpawnInRegion(player, region);
                break;
            }
        }
    }

    private void openTeleporterGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.AQUA + "Teleporter " + ChatColor.WHITE + "| INGAME");

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            GameState state = gameStateManager.getGameState(onlinePlayer);
            ItemStack item;
            ItemMeta meta;

            if (state == GameState.INGAME) {
                item = new ItemStack(Material.PLAYER_HEAD, 1);
                meta = item.getItemMeta();
                meta.setDisplayName(onlinePlayer.getName());
                item.setItemMeta(meta);
                gui.addItem(item);
            } else if (state == GameState.DEATH) {
                item = new ItemStack(Material.RED_STAINED_GLASS, 1);
                meta = item.getItemMeta();
                meta.setDisplayName(onlinePlayer.getName());
                item.setItemMeta(meta);
                gui.addItem(item);
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.COMPASS && item.getItemMeta() != null &&
                item.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Teleporter " + ChatColor.WHITE + "| (Right / Left Click)")) {
            openTeleporterGUI(player);
            event.setCancelled(true); // Prevent any other action
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.AQUA + "Teleporter " + ChatColor.WHITE + "| INGAME")) {
            event.setCancelled(true); // Prevent moving items in the GUI

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            String clickedPlayerName = event.getCurrentItem().getItemMeta().getDisplayName();
            Player target = Bukkit.getPlayer(clickedPlayerName);

            if (target != null) {
                if (event.isLeftClick()) {
                    player.teleport(target); // Teleport to the clicked player
                } else if (event.isRightClick()) {
                    player.setGameMode(GameMode.SPECTATOR);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.teleport(target);
                        }
                    }.runTask(plugin);
                }
            }

            player.closeInventory();
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            GameState state = gameStateManager.getGameState(player);

            if (state == GameState.DEATH || state == GameState.SPECTATOR) {
                event.setCancelled(true); // Prevent any damage
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            GameState state = gameStateManager.getGameState(player);

            if (state == GameState.DEATH || state == GameState.SPECTATOR) {
                event.setCancelled(true); // Prevent damage by entities
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof Player) {
            Player player = (Player) event.getHitEntity();
            GameState state = gameStateManager.getGameState(player);

            if (state == GameState.DEATH || state == GameState.SPECTATOR) {
                event.setCancelled(true); // Prevent being hit by projectiles
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        GameState state = gameStateManager.getGameState(player);

        if (state == GameState.DEATH || state == GameState.SPECTATOR) {
            event.setCancelled(true); // Prevent dropping items
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameState state = gameStateManager.getGameState(player);

        if (state == GameState.DEATH || state == GameState.SPECTATOR) {
            event.setCancelled(true); // Prevent placing blocks
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            GameState state = gameStateManager.getGameState(onlinePlayer);

            if (state == GameState.DEATH || state == GameState.SPECTATOR) {
                joiningPlayer.hidePlayer(plugin, onlinePlayer); // Hide DEATH and SPECTATOR players from newly joined players
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        originalGameModes.remove(player.getUniqueId());
    }
}
