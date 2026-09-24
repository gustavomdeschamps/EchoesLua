package com.orion.echoes.lua.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalStateTest {
    @Test void lockedPortalCannotTravelAndRelockingCancelsTransition() {
        Portal portal = new Portal(20f, 30f, null);
        portal.beginTraversal(false);
        portal.update(1f);
        assertFalse(portal.isTraversalComplete());
        portal.setUnlocked(true);
        assertTrue(portal.isUnlocked());
        portal.beginTraversal(false);
        portal.update(.8f);
        assertTrue(portal.isTraversalComplete());
        portal.setUnlocked(false);
        assertFalse(portal.isUnlocked());
        assertFalse(portal.isTraversalComplete());
    }
}
