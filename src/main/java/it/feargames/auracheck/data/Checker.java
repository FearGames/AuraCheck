package it.feargames.auracheck.data;

import com.comphenix.packetwrapper.WrapperPlayServerNamedEntitySpawn;
import com.comphenix.packetwrapper.WrapperPlayServerPlayerInfo;
import com.comphenix.packetwrapper.WrapperPlayServerSpawnEntityLiving;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import it.feargames.auracheck.AuraCheck;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

import static it.feargames.auracheck.utils.EntityPacketUtils.*;

public class Checker {

    private Set<Integer> spawnedEntities;

    // Parameters
    private int npcCount;
    private int ticksToKill;
    private boolean invisible;
    private boolean useMobs;
    private final Player target;

    public Checker(Player target, boolean useMobs, int npcCount, int ticksToKill, boolean invisible) {
        spawnedEntities = new HashSet<>();

        this.target = target;
        this.useMobs = useMobs;
        this.npcCount = npcCount;
        this.ticksToKill = ticksToKill;
        this.invisible = invisible;
    }

    public void invoke(AuraCheck plugin, CheckerManager checkerManager, CommandSender invoker, final Callback callback) {
        for (int i = 1; i <= npcCount; i++) {
            int degrees = 360 / (npcCount - 1) * i;
            double radians = Math.toRadians(degrees);
            if(useMobs) {
                WrapperPlayServerSpawnEntityLiving spawnWrapper;
                if (i == 1) {
                    spawnWrapper = getMobSpawnWrapper(target.getLocation().add(0, 2, 0).toVector(), EntityType.ZOMBIE, invisible);
                } else {
                    spawnWrapper = getMobSpawnWrapper(target.getLocation().add(2 * Math.cos(radians), 0.2, 2 * Math.sin(radians)).toVector(), EntityType.ZOMBIE, invisible);
                }
                spawnWrapper.sendPacket(target);
                spawnedEntities.add(spawnWrapper.getEntityID());
            } else {
                WrapperPlayServerNamedEntitySpawn spawnWrapper;
                if (i == 1) {
                    spawnWrapper = getPlayerSpawnWrapper(target.getLocation().add(0, 2, 0).toVector(), invisible);
                } else {
                    spawnWrapper = getPlayerSpawnWrapper(target.getLocation().add(2 * Math.cos(radians), 0.2, 2 * Math.sin(radians)).toVector(), invisible);
                }
                WrapperPlayServerPlayerInfo infoWrapper = getPlayerInfoWrapper(spawnWrapper.getPlayerUUID(), PlayerInfoAction.ADD_PLAYER);
                infoWrapper.sendPacket(target);
                spawnWrapper.sendPacket(target);
                spawnedEntities.add(spawnWrapper.getEntityID());
                WrapperPlayServerPlayerInfo removeInfoWrapper = getPlayerInfoWrapper(spawnWrapper.getPlayerUUID(), PlayerInfoAction.REMOVE_PLAYER);
                removeInfoWrapper.sendPacket(target);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int killed = end();
            checkerManager.removeCheck(target);
            callback.done(npcCount, killed, invoker, target);
        }, ticksToKill);
    }

    public void markAsKilled(Integer id) {
        if (!spawnedEntities.remove(id)) {
            return;
        }
        getKillWrapper(id).sendPacket(target);
    }

    public int end() {
        int killed = npcCount - spawnedEntities.size();

        // Kill remaining entities
        if (target.isOnline()) {
            for (Integer entry : spawnedEntities) {
                getKillWrapper(entry).sendPacket(target);
            }
        }
        spawnedEntities.clear();

        return killed;
    }

    public interface Callback {
        void done(int amount, int killed, CommandSender invoker, Player target);
    }
}
