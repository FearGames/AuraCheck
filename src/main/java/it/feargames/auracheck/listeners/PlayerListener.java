package it.feargames.auracheck.listeners;

import it.feargames.auracheck.data.CheckerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.inject.Inject;

public class PlayerListener implements Listener {

    private CheckerManager checkerManager;

    @Inject
    PlayerListener(CheckerManager checkerManager) {
        this.checkerManager = checkerManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        checkerManager.removeCheck(event.getPlayer());
    }
}
