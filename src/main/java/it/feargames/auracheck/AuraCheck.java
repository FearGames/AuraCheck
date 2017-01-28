package it.feargames.auracheck;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import it.feargames.auracheck.annotations.DataFolder;
import it.feargames.auracheck.config.ConfigProperties;
import it.feargames.auracheck.config.SettingsProvider;
import it.feargames.auracheck.listeners.PlayerListener;
import it.feargames.auracheck.data.Checker;
import it.feargames.auracheck.data.CheckerManager;
import it.feargames.auracheck.tasks.AutoTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class AuraCheck extends JavaPlugin {

    private Injector injector;
    private SettingsManager settings;
    private CheckerManager checkerManager;
    private BukkitTask autoTask;

    public AuraCheck() {
        autoTask = null;
    }

    @Override
    public void onEnable() {
        // Prepare the injector
        setupInjector();

        // Get the singletons from the injector
        settings = injector.getSingleton(SettingsManager.class);
        checkerManager = injector.getSingleton(CheckerManager.class);

        // Register listeners
        getServer().getPluginManager().registerEvents(injector.getSingleton(PlayerListener.class), this);

        // Setup the auto check task
        setupAutoTask();
    }

    private void setupInjector() {
        injector = new InjectorBuilder().addDefaultHandlers("it.feargames.auracheck").create();
        injector.register(AuraCheck.class, this);
        injector.register(Server.class, getServer());
        injector.register(PluginManager.class, getServer().getPluginManager());
        injector.register(BukkitScheduler.class, getServer().getScheduler());
        injector.provide(DataFolder.class, getDataFolder());
        injector.registerProvider(SettingsManager.class, SettingsProvider.class);
    }

    private void setupAutoTask() {
        if(autoTask != null) {
            autoTask.cancel();
        }
        if(settings.getProperty(ConfigProperties.AUTO)) {
            autoTask = new AutoTask(this).runTaskTimer(this, 20 * 10, 20 * 60 * 5);
        } else {
            autoTask = null;
        }
    }

    /**
     * The plugin's command handler
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // -> /ac OR /ac help
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        String subCmd = args[0];

        // -> /ac reload
        if (subCmd.equalsIgnoreCase("reload")) {
            settings.reload();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
            setupInjector();
            return true;
        }

        // -> /ac auto ...
        if (subCmd.equalsIgnoreCase("auto")) {
            // -> /ac auto
            if (args.length < 2) {
                sendHelp(sender);
                return true;
            }
            String param = args[1];
            // -> /ac auto on
            switch (param.toLowerCase()) {
                case "on":
                    sender.sendMessage(ChatColor.GREEN + "Auto mode enabled!");
                    settings.setProperty(ConfigProperties.AUTO, true);
                    settings.save();
                    break;
                case "off":
                    sender.sendMessage(ChatColor.RED + "Auto mode disabled!");
                    settings.setProperty(ConfigProperties.AUTO, false);
                    settings.save();
                    break;
                default:
                    sendHelp(sender);
            }
            setupAutoTask();
            return true;
        }

        // -> /ac check ...
        if (subCmd.equalsIgnoreCase("check")) {
            // -> /ac check
            if (args.length < 2) {
                sendHelp(sender);
                return true;
            }
            String param = args[1];

            // -> /ac check *
            if (param.equals("*")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkPlayer(sender, player, true, false);
                }
            }

            // -> /ac check playername
            else {
                Player player = Bukkit.getPlayerExact(param);
                if (player == null) {
                    sender.sendMessage(ChatColor.RED + "Player is not online.");
                } else {
                    checkPlayer(sender, player, false, false);
                }
            }
            return true;
        }

        // -> /ac checkmob ...
        if (subCmd.equalsIgnoreCase("checkmob")) {

            // -> /ac checkmob
            if (args.length < 2) {
                sendHelp(sender);
                return true;
            }
            String param = args[1];

            // -> /ac checkmob *
            if (param.equals("*")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkPlayer(sender, player, true, true);
                }
            }

            // -> /ac checkmob playername
            else {
                Player player = Bukkit.getPlayerExact(param);
                if (player == null) {
                    sender.sendMessage(ChatColor.RED + "Player is not online.");
                } else {
                    checkPlayer(sender, player, false, true);
                }
            }
            return true;
        }
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("-----[ AuraCheck Help ]-----");
        sender.sendMessage(" /ac help");
        sender.sendMessage(" /ac reload");
        sender.sendMessage(" /ac check <playername>");
        sender.sendMessage(" /ac check *");
        sender.sendMessage(" /ac checkmob <playername>");
        sender.sendMessage(" /ac checkmob *");
        sender.sendMessage(" /ac auto [on/off]");
        sender.sendMessage("----------------------------");
    }

    /**
     * Check a player
     *
     * @param sender      The output message receiver
     * @param player      The player
     * @param ignoreLegit Should we ignore the output if the player kills 0 NPCs?
     * @param useMobs     Should we use mobs instead of fake players?
     */
    public void checkPlayer(CommandSender sender, Player player, boolean ignoreLegit, boolean useMobs) {
        Checker checker = checkerManager.addCheck(player, useMobs);
        Checker.Callback callback = (amount, killed, invoker, target) -> {
            // If the invoker is not online just stop
            if (invoker instanceof Player && !((Player) invoker).isOnline()) {
                return;
            }

            // Ignore player with 0 killed entities
            if (ignoreLegit && killed == 0) {
                return;
            }

            if (killed < settings.getProperty(ConfigProperties.COMMAND_TRIGGER)) {
                invoker.sendMessage(ChatColor.DARK_PURPLE + target.getName() + " has killed " + killed + " out of " + amount);
                return;
            }

            String command = settings.getProperty(ConfigProperties.COMMAND).replaceAll("%p", target.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            invoker.sendMessage(ChatColor.RED + target.getName() + " have been kicked!" + ChatColor.DARK_PURPLE + " Killed " + killed + " out of " + amount);
        };
        checker.invoke(this, checkerManager, sender, callback);
    }
}
