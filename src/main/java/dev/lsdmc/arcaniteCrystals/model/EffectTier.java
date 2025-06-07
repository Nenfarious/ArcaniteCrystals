// src/main/java/dev/lsdmc/arcaniteCrystals/model/EffectTier.java
package dev.lsdmc.arcaniteCrystals.model;

/**
 * Defines available effect tiers for crystals.
 */
public enum EffectTier {
    TIER1(1),
    TIER2(2),
    TIER3(3);

    private final int level;

    EffectTier(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
