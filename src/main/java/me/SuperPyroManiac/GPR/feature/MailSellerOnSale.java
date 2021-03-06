package me.SuperPyroManiac.GPR.feature;

import me.SuperPyroManiac.GPR.events.GPRSaleEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created on 3/11/2020.
 * A class for each method? I guess
 * @author RoboMWM
 */
public class MailSellerOnSale implements Listener
{
    private Plugin plugin;
    public MailSellerOnSale(Plugin plugin)
    {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSale(GPRSaleEvent event)
    {
        //ignore admin claim sales
        if (event.getClaim().ownerID == null)
            return;

        String name = event.getClaim().getOwnerName();

        String command = ChatColor.translateAlternateColorCodes('&', "mail send " + name +
                " &f[&6GPAuctions&f] &a" + event.getBuyer().getName() + "&b has purchased your claim at &a" +
                getfriendlyLocationString(event.getClaim().getLesserBoundaryCorner()) + " &b. The sale price was &a" +
                event.getPrice() + "&b.");
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
    }

    public static String getfriendlyLocationString(Location location)
    {
        return location.getWorld().getName() + ": x" + location.getBlockX() + ", z" + location.getBlockZ();
    }

}
