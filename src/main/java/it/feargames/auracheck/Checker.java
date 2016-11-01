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

import com.comphenix.packetwrapper.WrapperPlayServerEntityDestroy;
import com.comphenix.packetwrapper.WrapperPlayServerNamedEntitySpawn;
import com.comphenix.packetwrapper.WrapperPlayServerPlayerInfo;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;


public class Checker {
    private final AuraCheck plugin;
    private Set<Integer> spawnedEntities;
    private CommandSender invoker;
    private Player checked;
    private int npcCount;

    public Checker(AuraCheck plugin, Player checked) {
        this.plugin = plugin;
        this.checked = checked;
        spawnedEntities = new HashSet<>();
        npcCount = plugin.getConfig().getInt("amountOfFakePlayers");
    }

    public void invoke(CommandSender player, final Callback callback) {
        this.invoker = player;

        for (int i = 1; i <= npcCount; i++) {
            int degrees = 360 / (npcCount - 1) * i;
            double radians = Math.toRadians(degrees);
            WrapperPlayServerNamedEntitySpawn spawnWrapper;
            if (i == 1) {
                spawnWrapper = getSpawnWrapper(checked.getLocation().add(0, 2, 0).toVector(), plugin);
            } else {
                spawnWrapper = getSpawnWrapper(checked.getLocation().add(2 * Math.cos(radians), 0.2, 2 * Math.sin(radians)).toVector(), plugin);
            }
            WrapperPlayServerPlayerInfo infoWrapper = getInfoWrapper(spawnWrapper.getPlayerUUID(), PlayerInfoAction.ADD_PLAYER);
            infoWrapper.sendPacket(checked);
            spawnWrapper.sendPacket(checked);
            spawnedEntities.add(spawnWrapper.getEntityID());
            WrapperPlayServerPlayerInfo removeInfoWrapper = getInfoWrapper(spawnWrapper.getPlayerUUID(), PlayerInfoAction.REMOVE_PLAYER);
            removeInfoWrapper.sendPacket(checked);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int killed = end();
            plugin.removeCheck(checked);
            callback.done(npcCount, killed, invoker, checked);
        }, plugin.getConfig().getInt("ticksToKill", 10));
    }

    public void markAsKilled(Integer id) {
        if (!spawnedEntities.remove(id)) {
            return;
        }
        getKillWrapper(id).sendPacket(checked);
    }

    public int end() {
        int killed = plugin.getConfig().getInt("amountOfFakePlayers") - spawnedEntities.size();

        // Kill remaining entities
        if (checked.isOnline()) {
            for (Integer entry : spawnedEntities) {
                getKillWrapper(entry).sendPacket(checked);
            }
        }
        spawnedEntities.clear();

        return killed;
    }

    public interface Callback {
        void done(int amount, int killed, CommandSender invoker, Player target);
    }

    // TODO: move into utils
    public static WrapperPlayServerNamedEntitySpawn getSpawnWrapper(Vector location, AuraCheck plugin) {
        WrapperPlayServerNamedEntitySpawn wrapper = new WrapperPlayServerNamedEntitySpawn();
        wrapper.setEntityID(RandomUtils.RANDOM.nextInt(20000));
        wrapper.setPosition(location);
        wrapper.setPlayerUUID(UUID.randomUUID());
        wrapper.setYaw(0.0F);
        wrapper.setPitch(-45.0F);
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setObject(0, plugin.getConfig().getBoolean("invisibility", false) ? (byte) 0x20 : (byte) 0);
        watcher.setObject(6, 0.5F);
        watcher.setObject(11, (byte) 1);
        wrapper.setMetadata(watcher);
        return wrapper;
    }

    public static WrapperPlayServerPlayerInfo getInfoWrapper(UUID playerUUID, PlayerInfoAction action) {
        WrapperPlayServerPlayerInfo wrapper = new WrapperPlayServerPlayerInfo();
        wrapper.setAction(action);
        String playerName = RandomUtils.randomNickname();
        WrappedGameProfile profile = new WrappedGameProfile(playerUUID, playerName);
        PlayerInfoData data = new PlayerInfoData(profile, 1, NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(playerName));
        List<PlayerInfoData> dataList = new ArrayList<>();
        dataList.add(data);
        wrapper.setData(dataList);
        return wrapper;
    }

    public static WrapperPlayServerEntityDestroy getKillWrapper(int entity) {
        WrapperPlayServerEntityDestroy wrapper = new WrapperPlayServerEntityDestroy();
        wrapper.setEntityIds(new int[]{entity});
        return wrapper;
    }
}
