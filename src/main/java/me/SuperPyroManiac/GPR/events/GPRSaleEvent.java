package me.SuperPyroManiac.GPR.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created on 7/12/2019.
 *
 * @author RoboMWM
 *
 * Fired right when a sale is about to be made (payment prerequisites have been met)
 */
public class GPRSaleEvent extends Event
{
    // Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() {
        return handlers;
    }
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    private double price;
    private Claim claim;
    private OfflinePlayer buyer; //buyer is always online, but whatever

    public GPRSaleEvent(Claim claim, OfflinePlayer buyer, double price)
    {
        this.claim = claim;
        this.buyer = buyer;
        this.price = price;
    }

    public Claim getClaim()
    {
        return claim;
    }

    public OfflinePlayer getBuyer()
    {
        return buyer;
    }

    public double getPrice()
    {
        return price;
    }
}
