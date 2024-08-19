package de.geisti.smashdashmash.GameStateManager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameStateCommand implements CommandExecutor {

    private final GameStateManager gameStateManager;

    public GameStateCommand(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gamestate")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Bitte benutze /gamestate <Spielername>");
                return true;
            }
            Player target = sender.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Spieler nicht gefunden.");
                return true;
            }
            GameState state = gameStateManager.getGameState(target);
            sender.sendMessage(ChatColor.GREEN + "Der Spieler " + target.getName() + " ist im Zustand: " + state);
            return true;
        }

        if (command.getName().equalsIgnoreCase("setgamestate")) {
            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Bitte benutze /setgamestate <Spielername> <Gamestate>");
                return true;
            }
            Player target = sender.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Spieler nicht gefunden.");
                return true;
            }
            try {
                GameState state = GameState.valueOf(args[1].toUpperCase());
                gameStateManager.setGameState(target, state);
                sender.sendMessage(ChatColor.GREEN + "Der Spieler " + target.getName() + " wurde in den Zustand " + state + " versetzt.");
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Ungültiger GameState.");
            }
            return true;
        }

        return false;
    }
}
