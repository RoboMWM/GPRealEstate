package me.SuperPyroManiac.GPR;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import me.SuperPyroManiac.GPR.events.GPRListEvent;
import me.SuperPyroManiac.GPR.events.GPRSaleEvent;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.PluginManager;

public class GPREListener
        implements Listener
{
    private GPRealEstate plugin;
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();

    public GPREListener(GPRealEstate plugin)
    {
        this.plugin = plugin;
    }

    public void registerEvents()
    {
        PluginManager pm = this.plugin.getServer().getPluginManager();
        pm.registerEvents(this, this.plugin);
    }

    private boolean makePayment(Player buyer, OfflinePlayer seller, Double price, Claim claim)
    {
        if (!GPRealEstate.econ.has(buyer, price.doubleValue()))
        {
            buyer.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have enough money!");
            return false;
        }
        EconomyResponse ecoresp = GPRealEstate.econ.withdrawPlayer(buyer, price.doubleValue());
        if (!ecoresp.transactionSuccess())
        {
            buyer.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "Could not withdraw the money!");
            return false;
        }
        if (!seller.getName().equalsIgnoreCase("server"))
        {
            ecoresp = GPRealEstate.econ.depositPlayer(seller, price.doubleValue());
            if (!ecoresp.transactionSuccess())
            {
                buyer.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "Could not transfer money, refunding Player!");
                GPRealEstate.econ.depositPlayer(buyer, price.doubleValue());
                return false;
            }
        }

        //fire sale event
        plugin.getServer().getPluginManager().callEvent(new GPRSaleEvent(claim, buyer, price));

        //RoboMWM - transfer accrued claim blocks
        if (!plugin.dataStore.cfgTransferClaimBlocks || claim.parent != null)
            return true; //Don't transfer claim blocks for subclaims.
        DataStore dataStore = GriefPrevention.instance.dataStore;
        PlayerData buyerData = dataStore.getPlayerData(buyer.getUniqueId());
        PlayerData sellerData = dataStore.getPlayerData(seller.getUniqueId());

        //Withdraw bonus, then accrued, from seller
        sellerData.setBonusClaimBlocks(sellerData.getBonusClaimBlocks() - claim.getArea());
        if (sellerData.getBonusClaimBlocks() < 0)
        {
            sellerData.setAccruedClaimBlocks(sellerData.getAccruedClaimBlocks() + sellerData.getBonusClaimBlocks());
            sellerData.setBonusClaimBlocks(0);
        }

        buyerData.setBonusClaimBlocks(buyerData.getBonusClaimBlocks() + claim.getArea());
        //RoboMWM end - transfer accrued claim blocks

        return true;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event)
    {
        if ((event.getLine(0).equalsIgnoreCase(this.plugin.dataStore.cfgSignShort)) || (event.getLine(0).equalsIgnoreCase(this.plugin.dataStore.cfgSignLong)))
        {
            Player player = event.getPlayer();
            Location location = event.getBlock().getLocation();

            GriefPrevention gp = GriefPrevention.instance;
            Claim claim = gp.dataStore.getClaimAt(location, false, null);
            if (claim == null)
            {
                player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "The sign you placed is not inside a claim!");
                event.setCancelled(true);
                return;
            }
            if (event.getLine(1).isEmpty())
            {
                int newValue = this.plugin.dataStore.cfgReplaceValue;
                int claimValue = gp.dataStore.getClaimAt(event.getBlock().getLocation(), false, null).getArea();
                String thePrice = Integer.toString(newValue * claimValue);
                event.setLine(1, thePrice);
                this.plugin.addLogEntry(
                        "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made a claim for sale at [" +
                                player.getLocation().getWorld() + ", " +
                                "X: " + player.getLocation().getBlockX() + ", " +
                                "Y: " + player.getLocation().getBlockY() + ", " +
                                "Z: " + player.getLocation().getBlockZ() + "] " +
                                "Price: " + thePrice + " " + GPRealEstate.econ.currencyNamePlural());
            }
            String price = event.getLine(1);
            try
            {
                Double.parseDouble(event.getLine(1));
            }
            catch (NumberFormatException e)
            {
                player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "The price you entered is not a valid number!");
                event.setCancelled(true);
                return;
            }
            if (claim.parent == null)
            {
                if (player.getName().equalsIgnoreCase(claim.getOwnerName()))
                {
                    if (!GPRealEstate.perms.has(player, "gprealestate.claim.sell"))
                    {
                        player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to sell claims!");
                        event.setCancelled(true);
                        return;
                    }
                    event.setLine(0, this.plugin.dataStore.cfgSignLong);
                    event.setLine(1, ChatColor.DARK_GREEN + this.plugin.dataStore.cfgReplaceSell);
                    event.setLine(2, player.getName());
                    event.setLine(3, price + " " + GPRealEstate.econ.currencyNamePlural());

                    player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.AQUA + "You are now selling this claim for " + ChatColor.GREEN + price + " " + GPRealEstate.econ.currencyNamePlural());

                    this.plugin.addLogEntry(
                            "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made a claim for sale at [" +
                                    player.getLocation().getWorld() + ", " +
                                    "X: " + player.getLocation().getBlockX() + ", " +
                                    "Y: " + player.getLocation().getBlockY() + ", " +
                                    "Z: " + player.getLocation().getBlockZ() + "] " +
                                    "Price: " + price + " " + GPRealEstate.econ.currencyNamePlural());
                    new GPRListEvent(claim, Double.parseDouble(price));
                }
                else if (claim.isAdminClaim())
                {
                    if (player.hasPermission("gprealestate.admin"))
                    {
                        if (this.plugin.dataStore.cfgAllowSellingParentAC)
                        {
                            event.setLine(0, this.plugin.dataStore.cfgSignLong);
                            event.setLine(1, ChatColor.DARK_GREEN + this.plugin.dataStore.cfgReplaceSell);
                            event.setLine(2, player.getName());
                            event.setLine(3, price + " " + GPRealEstate.econ.currencyNamePlural());

                            player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.AQUA + "You are now selling this admin claim for " + ChatColor.GREEN + price + " " + GPRealEstate.econ.currencyNamePlural());

                            this.plugin.addLogEntry(
                                    "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made an admin claim for sale at " +
                                            "[" + player.getLocation().getWorld() + ", " +
                                            "X: " + player.getLocation().getBlockX() + ", " +
                                            "Y: " + player.getLocation().getBlockY() + ", " +
                                            "Z: " + player.getLocation().getBlockZ() + "] " +
                                            "Price: " + price + " " + GPRealEstate.econ.currencyNamePlural());
                            new GPRListEvent(claim, Double.parseDouble(price));
                        }
                        else
                        {
                            player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You cannot sell admin claims, they can only be leased!");
                            event.setCancelled(true);
                        }
                    }
                    else
                    {
                        player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You can only sell claims you own!");
                        event.setCancelled(true);
                    }
                }
                else
                {
                    player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You can only sell claims you own!");
                    event.setCancelled(true);
                }
            }
            else if (claim.parent.isAdminClaim())
            {
                if (GPRealEstate.perms.has(player, "gprealestate.admin"))
                {
                    event.setLine(0, this.plugin.dataStore.cfgSignLong);
                    event.setLine(1, ChatColor.DARK_GREEN + this.plugin.dataStore.cfgReplaceSell);
                    event.setLine(2, player.getName());
                    event.setLine(3, price + " " + GPRealEstate.econ.currencyNamePlural());

                    player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.AQUA + "You are now selling access to this admin subclaim for " + ChatColor.GREEN + price + " " + GPRealEstate.econ.currencyNamePlural());

                    this.plugin.addLogEntry(
                            "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made an admin subclaim access for sale at " +
                                    "[" + player.getLocation().getWorld() + ", " +
                                    "X: " + player.getLocation().getBlockX() + ", " +
                                    "Y: " + player.getLocation().getBlockY() + ", " +
                                    "Z: " + player.getLocation().getBlockZ() + "] " +
                                    "Price: " + price + " " + GPRealEstate.econ.currencyNamePlural());
                    new GPRListEvent(claim, Double.parseDouble(price));
                }
            }
            else if ((player.getName().equalsIgnoreCase(claim.parent.getOwnerName())) || (claim.managers.equals(player.getName()))) {
                if (GPRealEstate.perms.has(player, "gprealestate.subclaim.sell"))
                {
                    String period = event.getLine(2);
                    if (period.isEmpty())
                    {
                        event.setLine(0, this.plugin.dataStore.cfgSignLong);
                        event.setLine(1, ChatColor.DARK_GREEN + this.plugin.dataStore.cfgReplaceSell);
                        event.setLine(2, player.getName());
                        event.setLine(3, price + " " + GPRealEstate.econ.currencyNamePlural());

                        player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.AQUA + "You are now selling access to this subclaim for " + ChatColor.GREEN + price + " " + GPRealEstate.econ.currencyNamePlural());

                        this.plugin.addLogEntry(
                                "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made a subclaim access for sale at " +
                                        "[" + player.getLocation().getWorld() + ", " +
                                        "X: " + player.getLocation().getBlockX() + ", " +
                                        "Y: " + player.getLocation().getBlockY() + ", " +
                                        "Z: " + player.getLocation().getBlockZ() + "] " +
                                        "Price: " + price + " " + GPRealEstate.econ.currencyNamePlural());
                        new GPRListEvent(claim, Double.parseDouble(price), true);
                    }
                }
                else
                {
                    player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to sell subclaims!");
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onSignInteract(PlayerInteractEvent event)
    {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
        {
            Material type = event.getClickedBlock().getType();
            if (Tag.SIGNS.isTagged(type))
            {
                Sign sign = (Sign)event.getClickedBlock().getState();
                if ((sign.getLine(0).equalsIgnoreCase(this.plugin.dataStore.cfgSignShort)) || (sign.getLine(0).equalsIgnoreCase(this.plugin.dataStore.cfgSignLong)))
                {
                    Player player = event.getPlayer();

                    Location location = event.getClickedBlock().getLocation();

                    GriefPrevention gp = GriefPrevention.instance;
                    Claim claim = gp.dataStore.getClaimAt(location, false, null);

                    String[] delimit = sign.getLine(3).split(" ");
                    Double price = Double.valueOf(Double.valueOf(delimit[0].trim()).doubleValue());

                    String status = ChatColor.stripColor(sign.getLine(1));
                    if (claim == null)
                    {
                        player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "This sign is no longer within a claim!");
                        event.getClickedBlock().setType(Material.AIR); return;
                    }
                    String claimType;
                    if (event.getPlayer().isSneaking())
                    {
                        String message = "";
                        if (event.getPlayer().hasPermission("gprealestate.info"))
                        {
                            claimType = claim.parent == null ? "claim" : "subclaim";

                            message = message + ChatColor.BLUE + "-----= " + ChatColor.WHITE + "[" + ChatColor.GOLD + "RealEstate Info" + ChatColor.WHITE + "]" + ChatColor.BLUE + " =-----\n";
                            if (status.equalsIgnoreCase(this.plugin.dataStore.cfgReplaceSell))
                            {
                                message = message + ChatColor.AQUA + "This " + ChatColor.GREEN + claimType.toUpperCase() + ChatColor.AQUA + " is for sale, for " + ChatColor.GREEN + price + " " + GPRealEstate.econ.currencyNamePlural() + "\n";
                                if (claimType.equalsIgnoreCase("claim"))
                                {
                                    message = message + ChatColor.AQUA + "The current owner is: " + ChatColor.GREEN + claim.getOwnerName();
                                }
                                else
                                {
                                    message = message + ChatColor.AQUA + "The main claim owner is: " + ChatColor.GREEN + claim.getOwnerName() + "\n";
                                    message = message + ChatColor.LIGHT_PURPLE + "Note: " + ChatColor.AQUA + "You will only buy access to this subclaim!";
                                }
                            }
                            else if ((claimType.equalsIgnoreCase("subclaim")) && (status.equalsIgnoreCase(this.plugin.dataStore.cfgReplaceRent)))
                            {
                                message = message + ChatColor.AQUA + "This " + ChatColor.GREEN + claimType.toUpperCase() + ChatColor.AQUA + " is for lease, for " + ChatColor.GREEN + price + " " + GPRealEstate.econ.currencyNamePlural() + "\n";
                                message = message + ChatColor.AQUA + "The leasing period has to be renewed every " + ChatColor.GREEN + "X days";
                            }
                            else
                            {
                                message = ChatColor.RED + "Ouch! Something went wrong!";
                            }
                        }
                        else
                        {
                            message = ChatColor.RED + "You do not have permissions to get RealEstate info!";
                        }
                        event.getPlayer().sendMessage(message);
                    }
                    else
                    {
                        if (claim.getOwnerName().equalsIgnoreCase(player.getName()))
                        {
                            player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You already own this claim!");
                            return;
                        }
                        if ((!sign.getLine(2).equalsIgnoreCase(claim.getOwnerName())) && (!claim.isAdminClaim()))
                        {
                            player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "The listed player does not have the rights to sell/lease this claim!");
                            event.getClickedBlock().setType(Material.AIR);
                            return;
                        }
                        if (claim.parent == null)
                        {
                            if (GPRealEstate.perms.has(player, "gprealestate.claim.buy"))
                            {
                                if ((claim.getArea() <= gp.dataStore.getPlayerData(player.getUniqueId()).getAccruedClaimBlocks()) || (player.hasPermission("gprealestate.ignore.limit")))
                                {
                                    if (makePayment(player, Bukkit.getOfflinePlayer(sign.getLine(2)), price, claim))
                                    {
                                        try
                                        {
                                            for (Claim child : claim.children)
                                            {
                                                child.clearPermissions();
                                                child.managers.remove(child.getOwnerName());
                                            }
                                            claim.clearPermissions();
                                            gp.dataStore.changeClaimOwner(claim, player.getUniqueId());
                                        }
                                        catch (Exception e)
                                        {
                                            e.printStackTrace();
                                            return;
                                        }
                                        if (claim.getOwnerName().equalsIgnoreCase(player.getName()))
                                        {
                                            player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.AQUA + "You have successfully purchased this claim for " + ChatColor.GREEN + price + GPRealEstate.econ.currencyNamePlural());
                                            this.plugin.addLogEntry(
                                                    "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " Has purchased a claim at " +
                                                            "[" + player.getLocation().getWorld() + ", " +
                                                            "X: " + player.getLocation().getBlockX() + ", " +
                                                            "Y: " + player.getLocation().getBlockY() + ", " +
                                                            "Z: " + player.getLocation().getBlockZ() + "] " +
                                                            "Price: " + price + " " + GPRealEstate.econ.currencyNamePlural());
                                        }
                                        else
                                        {
                                            player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "Cannot purchase claim!");
                                            return;
                                        }
                                        event.getClickedBlock().breakNaturally();
                                    }
                                }
                                else {
                                    player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have enough claim blocks available.");
                                }
                            }
                            else {
                                player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to buy claims!");
                            }
                        }
                        else if (status.equalsIgnoreCase(this.plugin.dataStore.cfgReplaceSell))
                        {
                            if (GPRealEstate.perms.has(player, "gprealestate.subclaim.buy"))
                            {
                                if (makePayment(player, Bukkit.getOfflinePlayer(sign.getLine(2)), price, claim))
                                {
                                    claim.clearPermissions();
                                    if (claim.parent.isAdminClaim())
                                    {
                                        if (player != Bukkit.getOfflinePlayer(sign.getLine(2)))
                                        {
                                            GPRealEstate.econ.withdrawPlayer(Bukkit.getOfflinePlayer(sign.getLine(2)), price.doubleValue());
                                            claim.setPermission(player.getUniqueId().toString(), ClaimPermission.Build);
                                            gp.dataStore.saveClaim(claim);
                                            event.getClickedBlock().breakNaturally();

                                            player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.AQUA + "You have successfully purchased this admin subclaim for " + ChatColor.GREEN + price + GPRealEstate.econ.currencyNamePlural());
                                            this.plugin.addLogEntry(
                                                    "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " Has purchased an admin subclaim at " +
                                                            "[" + player.getLocation().getWorld() + ", " +
                                                            "X: " + player.getLocation().getBlockX() + ", " +
                                                            "Y: " + player.getLocation().getBlockY() + ", " +
                                                            "Z: " + player.getLocation().getBlockZ() + "] " +
                                                            "Price: " + price + " " + GPRealEstate.econ.currencyNamePlural());
                                        }
                                        else
                                        {
                                            player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You can't buy the same claim you are selling!");
                                        }
                                    }
                                    else
                                    {
                                        if (!sign.getLine(2).equalsIgnoreCase("server")) {
                                            claim.managers.remove(sign.getLine(2));
                                        }
                                        claim.managers.add(player.getUniqueId().toString());
                                        claim.setPermission(player.getUniqueId().toString(), ClaimPermission.Build);
                                        gp.dataStore.saveClaim(claim);
                                        event.getClickedBlock().breakNaturally();

                                        player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.AQUA + "You have successfully purchased this subclaim for " + ChatColor.GREEN + price + GPRealEstate.econ.currencyNamePlural());

                                        this.plugin.addLogEntry(
                                                "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " Has purchased a subclaim at " +
                                                        "[" + player.getLocation().getWorld() + ", " +
                                                        "X: " + player.getLocation().getBlockX() + ", " +
                                                        "Y: " + player.getLocation().getBlockY() + ", " +
                                                        "Z: " + player.getLocation().getBlockZ() + "] " +
                                                        "Price: " + price + " " + GPRealEstate.econ.currencyNamePlural());
                                    }
                                }
                            }
                            else {
                                player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to buy subclaims!");
                            }
                        }
                        else if ((status.equalsIgnoreCase(this.plugin.dataStore.cfgReplaceSell)) && (this.plugin.dataStore.cfgEnableLeasing))
                        {
                            if (GPRealEstate.perms.has(player, "gprealestate.subclaim.buy")) {
                                player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.DARK_PURPLE + "The leasing function is currently being worked on!");
                            } else {
                                player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to lease subclaims!");
                            }
                        }
                        else
                        {
                            player.sendMessage(this.plugin.dataStore.chatPrefix + ChatColor.RED + "This sign was misplaced!");
                            event.getClickedBlock().setType(Material.AIR);
                            return;
                        }
                    }
                }
            }
        }
    }
}
