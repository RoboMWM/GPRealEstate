package me.SuperPyroManiac.GPR.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created on 3/18/2020.
 *
 * Called when a new claim has been put up for sale
 *
 * @author RoboMWM
 */
public class GPRListEvent extends Event
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

    private Claim claim;
    private double price;
    private boolean subClaim;

    public GPRListEvent(Claim claim, double price)
    {
        this.claim = claim;
        this.price = price;
    }

    public GPRListEvent(Claim claim, double price, boolean subClaim)
    {
        this.claim = claim;
        this.price = price;
        this.subClaim = subClaim;
    }

    public boolean isSubClaim()
    {
        return subClaim;
    }

    public Claim getClaim()
    {
        return claim;
    }

    public double getPrice()
    {
        return price;
    }
}
