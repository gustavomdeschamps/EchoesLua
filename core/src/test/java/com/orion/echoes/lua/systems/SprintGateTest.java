package com.orion.echoes.lua.systems;

import com.orion.echoes.lua.config.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintGateTest {
    @Test
    void holdingShiftAtEmptyEnergyDoesNotAlternateAnimationStates() {
        SprintGate gate = new SprintGate();
        assertTrue(gate.resolve(true, true, 50f));
        assertFalse(gate.resolve(true, true, .5f));
        assertFalse(gate.resolve(true, true, 2f));
        assertFalse(gate.resolve(true, true, GameConfig.PLAYER_RUN_RESUME_ENERGY - .1f));
        assertTrue(gate.resolve(true, true, GameConfig.PLAYER_RUN_RESUME_ENERGY));
    }

    @Test
    void releasingShiftClearsTheExhaustedLatch() {
        SprintGate gate = new SprintGate();
        assertFalse(gate.resolve(true, true, 0f));
        assertFalse(gate.resolve(false, true, 2f));
        assertTrue(gate.resolve(true, true, 2f));
    }

    @Test
    void runningRequiresMovement() {
        assertFalse(new SprintGate().resolve(true, false, 100f));
    }
}
