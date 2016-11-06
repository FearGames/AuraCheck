/*
 * Copyright (C) 2014 Maciej Mionskowski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.feargames.auracheck;

import com.comphenix.packetwrapper.WrapperPlayClientUseEntity;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuraCheck extends JavaPlugin implements Listener {
    private Map<UUID, Checker> runningChecks;
    private boolean isRegistered;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        runningChecks = new HashMap<>();
        isRegistered = false;

        // Load config
        saveDefaultConfig();
        config = getConfig();

        // Register listeners
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeCheck(event.getPlayer());
    }

    public void registerPacketListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(this, WrapperPlayClientUseEntity.TYPE) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        WrapperPlayClientUseEntity packet = new WrapperPlayClientUseEntity(event.getPacket());
                        if (packet.getType() != EntityUseAction.ATTACK) {
                            return;
                        }
                        Checker checker = runningChecks.get(event.getPlayer().getUniqueId());
                        if (checker == null) {
                            return;
                        }
                        checker.markAsKilled(packet.getTargetID());
                    }

                });
        isRegistered = true;
    }

    public void unregisterPacketListener() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        isRegistered = false;
    }

    public Checker removeCheck(Player player) {
        Checker checker = runningChecks.remove(player.getUniqueId());
        if (checker != null && runningChecks.isEmpty()) {
            this.unregisterPacketListener();
        }
        return checker;
    }

    public Checker addCheck(Player player) {
        if (!isRegistered) {
            registerPacketListener();
        }

        Checker checker = new Checker(this, player);
        runningChecks.put(player.getUniqueId(), checker);

        return checker;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) {
            return false;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Configuration successfully reloaded!");
            return true;
        }
        if (args[0].equals("*")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Checker checker = addCheck(player);
                Checker.Callback callback = (amount, killed, invoker, target) -> {
                    // If the invoker is not online just stop
                    if (invoker instanceof Player && !((Player) invoker).isOnline()) {
                        return;
                    }

                    // Ignore player with 0 killed entities
                    if (killed == 0) {
                        return;
                    }

                    if (killed < config.getInt("commandTrigger")) {
                        invoker.sendMessage(ChatColor.DARK_PURPLE + target.getName() + " has killed " + killed + " out of " + amount);
                        return;
                    }

                    String command = config.getString("command").replaceAll("%p", target.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    invoker.sendMessage(ChatColor.RED + target.getName() + " have been kicked!" + ChatColor.DARK_PURPLE + " Killed " + killed + " out of " + amount);
                };

                checker.invoke(sender, callback);
            }
            return true;
        }

        Player player = Bukkit.getPlayerExact(args[0]);
        if(player == null) {
            sender.sendMessage(ChatColor.RED + "Player is not online.");
            return true;
        }

        Checker checker = addCheck(player);
        Checker.Callback callback = (amount, killed, invoker, target) -> {
            // If the invoker is not online just stop
            if (invoker instanceof Player && !((Player) invoker).isOnline()) {
                return;
            }

            if (killed < config.getInt("commandTrigger")) {
                invoker.sendMessage(ChatColor.DARK_PURPLE + target.getName() + " has killed " + killed + " out of " + amount);
                return;
            }

            String command = config.getString("command").replaceAll("%p", target.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            invoker.sendMessage(ChatColor.RED + target.getName() + " have been kicked!" + ChatColor.DARK_PURPLE + " Killed " + killed + " out of " + amount);
        };
        checker.invoke(sender,callback);

        return true;
    }
}
