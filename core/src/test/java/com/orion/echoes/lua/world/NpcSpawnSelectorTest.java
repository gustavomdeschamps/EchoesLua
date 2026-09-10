package com.orion.echoes.lua.world;

import com.badlogic.gdx.math.Vector2;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSpawnSelectorTest {
    private static final float[][] ANCHORS = {{10f, 20f}, {30f, 40f}, {50f, 60f}, {70f, 80f}};

    @Test void sameCampaignKeepsNpcAtSameSafeAnchor() {
        assertEquals(NpcSpawnSelector.choose(42L, "ayyub", ANCHORS),
            NpcSpawnSelector.choose(42L, "ayyub", ANCHORS));
    }

    @Test void differentCampaignsUseMoreThanOneAnchor() {
        Set<Vector2> selected = new HashSet<>();
        for (long seed = 0; seed < 32; seed++) selected.add(NpcSpawnSelector.choose(seed, "ayyub", ANCHORS));
        assertTrue(selected.size() >= 3);
    }

    @Test void rejectsMissingSafeAnchors() {
        assertThrows(IllegalArgumentException.class,
            () -> NpcSpawnSelector.choose(1L, "ayyub", new float[0][]));
    }
}
