package de.geisti.smashdashmash.GameStateManager;

import de.geisti.smashdashmash.Handler.LiveHandler;
import de.geisti.smashdashmash.Main;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GameStateManager {
    private final Map<Player, GameState> playerStates = new HashMap<>();
    private final LiveHandler liveHandler;
    private final Main plugin;

    public GameStateManager(LiveHandler liveHandler) {
        this.liveHandler = liveHandler;
        this.plugin = liveHandler.getPlugin(); // Hol dir das Plugin über den LiveHandler
    }

    public void setGameState(Player player, GameState gameState) {
        playerStates.put(player, gameState);
        liveHandler.handleGameStateChange(player, gameState);

        // Wenn der Spieler in den DEATH- oder SPECTATOR-Zustand versetzt wird
        if (gameState == GameState.DEATH) {
            plugin.getDeathSpec().handleDeathState(player);
        } else if (gameState == GameState.SPECTATOR) {
            plugin.getDeathSpec().handleSpectatorState(player);
        } else if (gameState == GameState.WAITING) {
            plugin.getWaiting().setWaitItems(player);
        }
    }

    public GameState getGameState(Player player) {
        return playerStates.getOrDefault(player, GameState.WAITING);
    }

    public boolean isAnyPlayerInNonWaitingState() {
        return playerStates.values().stream().anyMatch(state -> state != GameState.WAITING);
    }

    public void removePlayer(Player player) {
        playerStates.remove(player);
    }
}
