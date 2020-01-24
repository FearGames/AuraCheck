package it.feargames.auracheck.utils;

import com.comphenix.packetwrapper.WrapperPlayServerEntityDestroy;
import com.comphenix.packetwrapper.WrapperPlayServerNamedEntitySpawn;
import com.comphenix.packetwrapper.WrapperPlayServerPlayerInfo;
import com.comphenix.packetwrapper.WrapperPlayServerSpawnEntityLiving;
import com.comphenix.protocol.wrappers.*;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet utilities for entities.
 */
public class EntityPacketUtils {

    // Packet data serializer
    private final static WrappedDataWatcher.Serializer SERIALIZER = WrappedDataWatcher.Registry.get(Byte.class);
    private final static WrappedDataWatcher.WrappedDataWatcherObject OBJECT = new WrappedDataWatcher.WrappedDataWatcherObject(0, SERIALIZER);

    // Utility class
    private EntityPacketUtils() {
    }

    /**
     * Get the wrapper for an living entity spawn packet
     *
     * @param location The location of the entity
     * @param entityType The entity type
     * @param invisible Should the entity be invisible?
     * @return the packet wrapper
     */
    public static WrapperPlayServerSpawnEntityLiving getMobSpawnWrapper(Vector location, EntityType entityType, boolean invisible) {
        WrapperPlayServerSpawnEntityLiving wrapper = new WrapperPlayServerSpawnEntityLiving();
        wrapper.setEntityID(RandomUtils.RANDOM.nextInt(20000));
        wrapper.setX(location.getX());
        wrapper.setY(location.getY());
        wrapper.setZ(location.getZ());
        wrapper.setType(entityType);

        // NPCs should use V2 UUIDs
        StringBuilder uuid = new StringBuilder(UUID.randomUUID().toString());
        uuid.setCharAt(14, '2');
        wrapper.setUniqueId(UUID.fromString(uuid.toString()));

        wrapper.setYaw(0.0F);
        wrapper.setPitch(-45.0F);

        /*
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setObject(OBJECT, invisible ? (byte) 0x20 : (byte) 0);
        wrapper.setMetadata(watcher);
         */

        return wrapper;
    }

    /**
     * Get the wrapper for a player spawn packet
     *
     * @param location The location of the player
     * @param invisible Should the player be invisible?
     * @return the packet wrapper
     */
    public static WrapperPlayServerNamedEntitySpawn getPlayerSpawnWrapper(Vector location, boolean invisible) {
        WrapperPlayServerNamedEntitySpawn wrapper = new WrapperPlayServerNamedEntitySpawn();
        wrapper.setEntityID(RandomUtils.RANDOM.nextInt(20000));
        wrapper.setPosition(location);

        // NPCs should use V2 UUIDs
        StringBuilder uuid = new StringBuilder(UUID.randomUUID().toString());
        uuid.setCharAt(14, '2');
        wrapper.setPlayerUUID(UUID.fromString(uuid.toString()));

        wrapper.setYaw(0.0F);
        wrapper.setPitch(-45.0F);

        /*
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setObject(OBJECT, invisible ? (byte) 0x20 : (byte) 0);
        wrapper.setMetadata(watcher);
         */

        return wrapper;
    }

    /**
     * Get the wrapper for a player info packet
     *
     * @param playerUUID the fake player uuid
     * @param action the action
     * @return the packet wrapper
     */
    public static WrapperPlayServerPlayerInfo getPlayerInfoWrapper(UUID playerUUID, EnumWrappers.PlayerInfoAction action) {
        WrapperPlayServerPlayerInfo wrapper = new WrapperPlayServerPlayerInfo();
        wrapper.setAction(action);
        String playerName = RandomUtils.randomNickname();
        WrappedGameProfile profile = new WrappedGameProfile(playerUUID, playerName);
        PlayerInfoData data = new PlayerInfoData(profile, 1, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(playerName));
        List<PlayerInfoData> dataList = new ArrayList<>();
        dataList.add(data);
        wrapper.setData(dataList);
        return wrapper;
    }

    /**
     * Get the wrapper for an entity destroy packet
     *
     * @param entity the id of the entity
     * @return the packet wrapper
     */
    public static WrapperPlayServerEntityDestroy getKillWrapper(int entity) {
        WrapperPlayServerEntityDestroy wrapper = new WrapperPlayServerEntityDestroy();
        wrapper.setEntityIds(new int[]{entity});
        return wrapper;
    }
}
