package it.feargames.auracheck.tasks;

import it.feargames.auracheck.AuraCheck;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoTask extends BukkitRunnable {

    private AuraCheck auraCheck;

    public AutoTask(AuraCheck auraCheck) {
        this.auraCheck = auraCheck;
    }

    @Override
    public void run() {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(ChatColor.GOLD + "[AuraCheck] An auto check is running!");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("auracheck.check")) {
                player.sendMessage(ChatColor.GOLD + "[AuraCheck] An auto check is running!");
            }
            auraCheck.checkPlayer(Bukkit.getConsoleSender(), player, true, false);
        }
        sender.sendMessage(ChatColor.GOLD + "[AuraCheck] Auto check completed!");
    }
}
