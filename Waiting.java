package de.geisti.smashdashmash.GameStateManager;

import de.geisti.smashdashmash.Handler.LiveHandler;
import de.geisti.smashdashmash.Main;
import de.geisti.smashdashmash.Setup.Countdown;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;

public class Waiting implements Listener {

    private final Main plugin;
    private final GameStateManager gameStateManager;
    private final Countdown countdown;
    private int minPlayers;

    public Waiting(Main plugin, GameStateManager gameStateManager) {
        this.plugin = plugin;
        this.gameStateManager = gameStateManager;
        this.countdown = new Countdown(plugin, gameStateManager);

        // Lade die settings.yml
        loadSettings();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void loadSettings() {
        File settingsFile = new File(plugin.getDataFolder(), "settings.yml");
        if (!settingsFile.exists()) {
            plugin.saveResource("settings.yml", false);
        }

        FileConfiguration settingsConfig = YamlConfiguration.loadConfiguration(settingsFile);
        this.minPlayers = settingsConfig.getInt("min-players", 2);
    }

    public void setWaitItems(Player player) {
        // Slot 0: Netherite Armor with custom name
        ItemStack teamSelector = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta teamSelectorMeta = teamSelector.getItemMeta();
        teamSelectorMeta.setDisplayName(ChatColor.AQUA + "Team " + ChatColor.WHITE + "| Selector");
        teamSelector.setItemMeta(teamSelectorMeta);
        player.getInventory().setItem(0, teamSelector);

        // Slot 1: Book with Quill with custom name
        ItemStack tutorialBook = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta tutorialBookMeta = tutorialBook.getItemMeta();
        tutorialBookMeta.setDisplayName(ChatColor.AQUA + "Smash " + ChatColor.WHITE + "| Tutorial");
        tutorialBook.setItemMeta(tutorialBookMeta);
        player.getInventory().setItem(1, tutorialBook);

        // Slot 3: Maze with custom name
        ItemStack characterSelector = new ItemStack(Material.MUSIC_DISC_MELLOHI); // Maze item can be customized
        ItemMeta characterSelectorMeta = characterSelector.getItemMeta();
        characterSelectorMeta.setDisplayName(ChatColor.AQUA + "Character " + ChatColor.WHITE + "| Selector");
        characterSelector.setItemMeta(characterSelectorMeta);
        player.getInventory().setItem(3, characterSelector);

        // Slot 4: Paper with custom name
        ItemStack mapVoting = new ItemStack(Material.PAPER);
        ItemMeta mapVotingMeta = mapVoting.getItemMeta();
        mapVotingMeta.setDisplayName(ChatColor.AQUA + "Map " + ChatColor.WHITE + "| Voting");
        mapVoting.setItemMeta(mapVotingMeta);
        player.getInventory().setItem(4, mapVoting);

        // Slot 5: Heavy core with custom name
        ItemStack itemVoting = new ItemStack(Material.NETHER_STAR); // Assuming heavy_core means Nether Star
        ItemMeta itemVotingMeta = itemVoting.getItemMeta();
        itemVotingMeta.setDisplayName(ChatColor.AQUA + "Item " + ChatColor.GRAY + "| Voting");
        itemVoting.setItemMeta(itemVotingMeta);
        player.getInventory().setItem(5, itemVoting);

        // Slot 7: Command Block with custom name
        ItemStack playerSettings = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta playerSettingsMeta = playerSettings.getItemMeta();
        playerSettingsMeta.setDisplayName(ChatColor.AQUA + "Player " + ChatColor.WHITE + "| Settings");
        playerSettings.setItemMeta(playerSettingsMeta);
        player.getInventory().setItem(7, playerSettings);

        // Slot 8: Slime Block with custom name
        ItemStack exitItem = new ItemStack(Material.SLIME_BLOCK);
        ItemMeta exitItemMeta = exitItem.getItemMeta();
        exitItemMeta.setDisplayName(ChatColor.AQUA + "Smash " + ChatColor.WHITE + "| " + ChatColor.RED + "verlassen" + ChatColor.WHITE + " (right or left Click)");
        exitItem.setItemMeta(exitItemMeta);
        player.getInventory().setItem(8, exitItem);

        // Prevent moving, dropping, or placing items
        player.getInventory().setHeldItemSlot(0); // Default to slot 0
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        GameState state = gameStateManager.getGameState(player);
        if (state == GameState.WAITING) {
            event.setCancelled(true); // Prevent moving items in the inventory
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        GameState state = gameStateManager.getGameState(player);
        if (state == GameState.WAITING) {
            event.setCancelled(true); // Prevent dropping items
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameState state = gameStateManager.getGameState(player);
        if (state == GameState.WAITING) {
            event.setCancelled(true); // Prevent placing blocks
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GameState state = gameStateManager.getGameState(player);
        if (state == GameState.WAITING) {
            event.setCancelled(true); // Prevent using items (right-click actions)
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkStartConditions();
    }

    public void checkStartConditions() {
        int onlinePlayers = plugin.getServer().getOnlinePlayers().size();
        if (onlinePlayers >= minPlayers && !countdown.isRunning()) {
            countdown.startCountdown(60);
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Genügend Spieler sind online. Der Countdown startet jetzt!");
        }
    }

    public Countdown getCountdown() {
        return countdown;
    }

    public int getMinPlayers() {
        return minPlayers;
    }
}
