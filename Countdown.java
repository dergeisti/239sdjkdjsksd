package de.geisti.smashdashmash.Setup;

import de.geisti.smashdashmash.GameStateManager.GameState;
import de.geisti.smashdashmash.GameStateManager.GameStateManager;
import de.geisti.smashdashmash.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Countdown {

    private final GameStateManager gameStateManager;
    private final Main plugin;
    private int countdownTime = 60;
    private boolean isRunning = false;

    public Countdown(Main plugin, GameStateManager gameStateManager) {
        this.plugin = plugin;
        this.gameStateManager = gameStateManager;
    }

    public void startCountdown(int startAt) {
        countdownTime = startAt;
        isRunning = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (countdownTime <= 0) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        gameStateManager.setGameState(player, GameState.INGAME);
                    }
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Das Spiel beginnt jetzt!");
                    cancel();
                    isRunning = false;
                    return;
                }

                // Zahlen im Chat anzeigen
                if (countdownTime % 5 == 0 || countdownTime <= 10) {
                    Bukkit.broadcastMessage(ChatColor.AQUA + "Das Spiel beginnt in " + ChatColor.RED + countdownTime + ChatColor.AQUA + " Sekunden!");
                    playSound(Sound.BLOCK_NOTE_BLOCK_BELL);
                }

                // Goat Horn sound bei 5 Sekunden
                if (countdownTime == 5) {
                    playSound(Sound.EVENT_RAID_HORN);
                }

                // Zahlen in der XP-Leiste anzeigen
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setLevel(countdownTime);
                }

                countdownTime--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void playSound(Sound sound) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void forceStart() {
        startCountdown(5);
    }
}
