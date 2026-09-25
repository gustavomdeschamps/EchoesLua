package com.orion.echoes.lua.systems;

import com.orion.echoes.lua.config.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Os três multiplicadores usados pelo combate, suporte de vida e perseguição. */
final class DifficultyBalanceTest {
    @Test void sameAttackAndSecondOfMovementProduceDifferentOutcomes() {
        float[] expectedDamage = {14.4f, 20f, 27f};
        float[] expectedOxygen = {1.6f, 2f, 2.5f};
        float[] expectedEnemyTravel = {64.8f, 72f, 82.8f};
        Inventario.Difficulty[] levels = Inventario.Difficulty.values();
        for (int i = 0; i < levels.length; i++) {
            Inventario inventory = new Inventario();
            inventory.setDifficulty(levels[i]);
            assertEquals(expectedDamage[i], 20f * inventory.getMultiplicadorDanoRecebido(), .001f);
            assertEquals(expectedOxygen[i],
                GameConfig.OXYGEN_CONSUMPTION * levels[i].oxygenMultiplier(), .001f);
            assertEquals(expectedEnemyTravel[i],
                GameConfig.ENEMY_BASE_SPEED * levels[i].enemySpeedMultiplier(), .001f);
        }
        assertTrue(expectedDamage[0] < expectedDamage[1] && expectedDamage[1] < expectedDamage[2]);
        assertTrue(expectedOxygen[0] < expectedOxygen[1] && expectedOxygen[1] < expectedOxygen[2]);
        assertTrue(expectedEnemyTravel[0] < expectedEnemyTravel[1]
            && expectedEnemyTravel[1] < expectedEnemyTravel[2]);
    }
}
