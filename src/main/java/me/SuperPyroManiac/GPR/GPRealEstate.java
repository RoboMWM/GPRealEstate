package me.SuperPyroManiac.GPR;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import me.SuperPyroManiac.GPR.feature.MailSellerOnSale;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GPRealEstate
        extends JavaPlugin
{
    Logger log;
    DataStore dataStore;
    public static boolean vaultPresent = false;
    public static Economy econ = null;
    public static Permission perms = null;

    public void onEnable()
    {
        this.log = getLogger();
        this.dataStore = new DataStore(this);

        new GPREListener(this).registerEvents();
        if (checkVault())
        {
            this.log.info("Vault has been detected and enabled.");
            if (setupEconomy())
            {
                this.log.info("Vault is using " + econ.getName() + " as the economy plugin.");
            }
            else
            {
                this.log.warning("No compatible economy plugin detected [Vault].");
                this.log.warning("Disabling plugin.");
                getPluginLoader().disablePlugin(this);
                return;
            }
            if (setupPermissions())
            {
                this.log.info("Vault is using " + perms.getName() + " for the permissions.");
            }
            else
            {
                this.log.warning("No compatible permissions plugin detected [Vault].");
                this.log.warning("Disabling plugin.");
                getPluginLoader().disablePlugin(this);
                return;
            }
        }
        loadConfig(false);
        new MailSellerOnSale(this);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if ((command.getName().equalsIgnoreCase("gpre")) && (sender.hasPermission("gprealestate.command")))
        {
            if (args.length == 0)
            {
                sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "Unknown. Use 'gpre help' for info.");
                return true;
            }
            if (args.length == 1)
            {
                if ((args[0].equalsIgnoreCase("version")) && (sender.hasPermission("gprealestate.admin")))
                {
                    sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "You are running " + ChatColor.RED + this.dataStore.pdf.getName() + ChatColor.GREEN + " version " + ChatColor.RED + this.dataStore.pdf.getVersion());
                    return true;
                }
                if ((args[0].equalsIgnoreCase("reload")) && (sender.hasPermission("gprealestate.admin")))
                {
                    loadConfig(true);
                    sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "The config file was succesfully reloaded.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("help"))
                {
                    sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "Commands: -Permission");
                    sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "gpre version | -gprealestate.admin");
                    sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "gpre reload: | -gprealestate.admin");

                    return true;
                }
                sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "Unknown. Use 'gpre help' for info");
                return true;
            }
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "You do not have permissions to use this command.");
            return false;
        }
        return false;
    }

    private void loadConfig(boolean reload)
    {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(this.dataStore.configFilePath));
        FileConfiguration outConfig = new YamlConfiguration();

        this.dataStore.cfgSignShort = config.getString("GPRealEstate.Keywords.Signs.Short", "[RE]");
        this.dataStore.cfgSignLong = config.getString("GPRealEstate.Keywords.Signs.Long", "[RealEstate]");

        this.dataStore.cfgRentKeywords = this.dataStore.stringToList(config.getString("GPRealEstate.Keywords.Actions.Renting", "Rent;Renting;Rental;For Rent"));
        this.dataStore.cfgSellKeywords = this.dataStore.stringToList(config.getString("GPRealEstate.Keywords.Actions.Selling", "Sell;Selling;For Sale"));

        this.dataStore.cfgReplaceRent = config.getString("GPRealEstate.Keywords.Actions.ReplaceRent", "FOR LEASE");
        this.dataStore.cfgReplaceSell = config.getString("GPRealEstate.Keywords.Actions.ReplaceSell", "FOR SALE");
        this.dataStore.cfgReplaceValue = config.getInt("GPRealEstate.Keywords.Actions.BuyPrice", 5);

        this.dataStore.cfgEnableLeasing = config.getBoolean("GPRealEstate.Rules.EnableLeasing", false);
        this.dataStore.cfgAllowSellingParentAC = config.getBoolean("GPRealEstate.Rules.AllowSellingParentAC", false);
        this.dataStore.cfgIgnoreClaimSize = config.getBoolean("GPRealEstate.Rules.IgnoreSizeLimit", false);
        this.dataStore.cfgTransferClaimBlocks = config.getBoolean("GPRealEstate.Rules.TransferClaimBlocks", true);

        if (!reload) {
            this.log.info("Signs will be using the keywords \"" + this.dataStore.cfgSignShort + "\" or \"" + this.dataStore.cfgSignLong + "\"");
        }
        outConfig.set("GPRealEstate.Keywords.Signs.Short", this.dataStore.cfgSignShort);
        outConfig.set("GPRealEstate.Keywords.Signs.Long", this.dataStore.cfgSignLong);
        outConfig.set("GPRealEstate.Keywords.Actions.Renting", this.dataStore.listToString(this.dataStore.cfgRentKeywords));
        outConfig.set("GPRealEstate.Keywords.Actions.Selling", this.dataStore.listToString(this.dataStore.cfgSellKeywords));
        outConfig.set("GPRealEstate.Keywords.Actions.ReplaceRent", this.dataStore.cfgReplaceRent);
        outConfig.set("GPRealEstate.Keywords.Actions.ReplaceSell", this.dataStore.cfgReplaceSell);
        outConfig.set("GPRealEstate.Keywords.Actions.BuyPrice", Integer.valueOf(this.dataStore.cfgReplaceValue));
        outConfig.set("GPRealEstate.Rules.EnableLeasing", Boolean.valueOf(this.dataStore.cfgEnableLeasing));
        outConfig.set("GPRealEstate.Rules.IgnoreSizeLimit", Boolean.valueOf(this.dataStore.cfgIgnoreClaimSize));
        outConfig.set("GPRealEstate.Rules.AllowSellingParentAC", Boolean.valueOf(this.dataStore.cfgAllowSellingParentAC));
        outConfig.set("GPRealEstate.Rules.TransferClaimBlocks", this.dataStore.cfgTransferClaimBlocks);
        try
        {
            outConfig.save(this.dataStore.configFilePath);
        }
        catch (IOException exception)
        {
            this.log.info("Unable to write to the configuration file at \"" + this.dataStore.configFilePath + "\"");
        }
    }

    public void addLogEntry(String entry)
    {
        try
        {
            File logFile = new File(this.dataStore.logFilePath);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            FileWriter fw = new FileWriter(logFile, true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println(entry);
            pw.flush();
            pw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private boolean checkVault()
    {
        vaultPresent = getServer().getPluginManager().getPlugin("Vault") != null;
        return vaultPresent;
    }

    private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = (Economy)rsp.getProvider();
        return econ != null;
    }

    private boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = (Permission)rsp.getProvider();
        return perms != null;
    }
}
