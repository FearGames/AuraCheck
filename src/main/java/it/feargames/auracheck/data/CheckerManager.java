package it.feargames.auracheck.data;

import ch.jalu.configme.SettingsManager;
import com.comphenix.packetwrapper.WrapperPlayClientUseEntity;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import it.feargames.auracheck.AuraCheck;
import it.feargames.auracheck.config.ConfigProperties;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class CheckerManager {

    @Inject
    private AuraCheck plugin;
    @Inject
    private SettingsManager settings;

    private Map<Player, Checker> runningChecks = new HashMap<>();
    private boolean isRegistered = false;

    CheckerManager() {
    }

    public Checker removeCheck(Player player) {
        Checker checker = runningChecks.remove(player);
        if (checker != null && runningChecks.isEmpty()) {
            unregisterPacketListener();
        }
        return checker;
    }

    public Checker addCheck(Player player, boolean useMobs) {
        if (!isRegistered) {
            registerPacketListener();
        }

        Checker checker = new Checker(
                player,
                useMobs,
                settings.getProperty(ConfigProperties.FAKE_AMOUNT),
                settings.getProperty(ConfigProperties.TICKS_TO_KILL),
                settings.getProperty(ConfigProperties.INVISIBILITY));
        runningChecks.put(player, checker);

        return checker;
    }

    private void registerPacketListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, WrapperPlayClientUseEntity.TYPE) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        WrapperPlayClientUseEntity packet = new WrapperPlayClientUseEntity(event.getPacket());
                        if (packet.getType() != EnumWrappers.EntityUseAction.ATTACK) {
                            return;
                        }
                        Checker checker = runningChecks.get(event.getPlayer());
                        if (checker == null) {
                            return;
                        }
                        checker.markAsKilled(packet.getTargetID());
                    }

                });
        isRegistered = true;
    }

    private void unregisterPacketListener() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
        isRegistered = false;
    }
}
