package me.SuperPyroManiac.GPR.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created on 7/12/2019.
 *
 * @author RoboMWM
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

    private Claim claim;

    public GPRSaleEvent(Claim claim)
    {
        this.claim = claim;
    }

    public Claim getClaim()
    {
        return claim;
    }
}
