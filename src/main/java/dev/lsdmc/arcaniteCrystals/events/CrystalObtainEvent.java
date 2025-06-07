package dev.lsdmc.arcaniteCrystals.events;

import dev.lsdmc.arcaniteCrystals.model.ArcaniteCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CrystalObtainEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ArcaniteCrystal crystal;
    private boolean cancelled;

    public CrystalObtainEvent(Player player, ArcaniteCrystal crystal) {
        this.player = player;
        this.crystal = crystal;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public ArcaniteCrystal getCrystal() {
        return crystal;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
