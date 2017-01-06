package it.feargames.auracheck.config;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

/**
 * The config properties holder
 */
public class ConfigProperties implements SettingsHolder {

    @Comment("The life time of the NPCs")
    public final static Property<Integer> TICKS_TO_KILL = newProperty("ticksToKill", 10);

    @Comment("The number of the NPCs")
    public final static Property<Integer> FAKE_AMOUNT = newProperty("amountOfFakePlayers", 4);

    @Comment("Should the NPCs be invisible?")
    public final static Property<Boolean> INVISIBILITY = newProperty("invisibility", false);

    @Comment("The amount of killed NPCs required to trigger the command")
    public final static Property<Integer> COMMAND_TRIGGER = newProperty("commandTrigger", 3);

    @Comment("The command, available variables: %p = playername")
    public final static Property<String> COMMAND = newProperty("command", "kick %p Don't use cheats!");

    private ConfigProperties(){
    }
}
