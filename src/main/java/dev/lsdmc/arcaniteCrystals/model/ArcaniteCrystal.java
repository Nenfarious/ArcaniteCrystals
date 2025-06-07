// src/main/java/dev/lsdmc/arcaniteCrystals/model/ArcaniteCrystal.java
package dev.lsdmc.arcaniteCrystals.model;

import java.util.List;
import java.util.UUID;

/**
 * Data class representing a rolled Arcanite Crystal instance.
 */
public class ArcaniteCrystal {

    private final UUID owner;
    private final List<String> abilities;
    private final int energy;

    public ArcaniteCrystal(UUID owner, List<String> abilities, int energy) {
        this.owner = owner;
        this.abilities = List.copyOf(abilities);
        this.energy = energy;
    }

    public UUID getOwner() {
        return owner;
    }

    public List<String> getAbilities() {
        return abilities;
    }

    public int getEnergy() {
        return energy;
    }
}
