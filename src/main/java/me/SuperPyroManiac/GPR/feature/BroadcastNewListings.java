package me.SuperPyroManiac.GPR.feature;

import me.SuperPyroManiac.GPR.events.GPRListEvent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Created on 3/18/2020.
 *
 * @author RoboMWM
 */
public class BroadcastNewListings implements Listener
{
    private Economy economy;
    private Plugin plugin;

    public BroadcastNewListings(Plugin plugin, Economy economy)
    {
        this.plugin = plugin;
        this.economy = economy;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onNewClaimForSale(GPRListEvent event)
    {
        StringBuilder message = new StringBuilder("broadcast &f[&6RealEstate&f] &bA real estate listing has been created at &a");
        message.append(getfriendlyLocationString(event.getClaim().getLesserBoundaryCorner()));
        message.append("&b. The ");
        String noun = "list";
        if (event.isSubClaim())
            noun = "Subclaim";
        message.append(noun);
        message.append(" price is &a");
        message.append(economy.format(event.getPrice()));
        message.append("&b.");

        String command = ChatColor.translateAlternateColorCodes('&', message.toString());

        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
    }

    public static String getfriendlyLocationString(Location location)
    {
        return location.getWorld().getName() + ": x" + location.getBlockX() + ", z" + location.getBlockZ();
    }
}
