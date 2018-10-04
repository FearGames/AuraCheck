package it.feargames.auracheck.config;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import ch.jalu.configme.migration.PlainMigrationService;
import it.feargames.auracheck.annotations.DataFolder;
import it.feargames.auracheck.utils.FileUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;

/**
 * The settings manager provider
 */
public class SettingsProvider implements Provider<SettingsManager> {

    @Inject
    @DataFolder
    private File dataFolder;

    SettingsProvider() {
    }

    /**
     * Loads the plugin's settings.
     *
     * @return the settings instance, or null if it could not be constructed
     */
    @Override
    public SettingsManager get() {
        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            FileUtils.create(configFile);
        }
        return SettingsManagerBuilder.withYamlFile(configFile)
                .migrationService(new PlainMigrationService())
                .configurationData(ConfigProperties.class).create();
    }
}
