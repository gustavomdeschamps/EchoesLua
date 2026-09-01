package com.orion.echoes.lua.systems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.orion.echoes.lua.events.EventBus;
import com.orion.echoes.lua.systems.MissionState.PartType;
import com.orion.echoes.lua.systems.MissionState.SystemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Regras de coleta, reparo, craft e liberacao do portal. */
class MissionStateTest {

    private MissionState mission;

    @BeforeEach
    void setUp() {
        EventBus.getInstance().clear();
        mission = new MissionState();
    }

    @Test
    @DisplayName("Coletar acumula pecas por tipo")
    void collectAccumulatesParts() {
        mission.collect(PartType.ANTENA);
        mission.collect(PartType.ANTENA);
        mission.collect(PartType.ENERGIA);
        assertEquals(2, mission.getPartCount(PartType.ANTENA));
        assertEquals(1, mission.getPartCount(PartType.ENERGIA));
        assertEquals(0, mission.getPartCount(PartType.ESTUFA));
    }

    @Test
    @DisplayName("Reparo consome a peca exigida e nao repete")
    void repairConsumesRequiredPart() {
        assertFalse(mission.repair(SystemType.COMUNICACAO), "sem peca nao repara");

        mission.collect(PartType.ANTENA);
        assertTrue(mission.repair(SystemType.COMUNICACAO));
        assertEquals(0, mission.getPartCount(PartType.ANTENA));
        assertTrue(mission.isRepaired(SystemType.COMUNICACAO));
        assertEquals(1, mission.getRepairCount());

        mission.collect(PartType.ANTENA);
        assertFalse(mission.repair(SystemType.COMUNICACAO), "sistema ja online nao repara de novo");
        assertEquals(1, mission.getPartCount(PartType.ANTENA), "peca nao pode ser consumida a toa");
    }

    @Test
    @DisplayName("Craft exige as tres partes e acontece uma unica vez")
    void craftRequiresEveryWeaponPart() {
        mission.collect(PartType.ARMA_A);
        mission.collect(PartType.ARMA_B);
        assertFalse(mission.hasAllWeaponParts());
        assertFalse(mission.craftWeapon());

        mission.collect(PartType.ARMA_C);
        assertTrue(mission.hasAllWeaponParts());
        assertTrue(mission.craftWeapon());
        assertTrue(mission.hasWeapon());
        assertEquals(0, mission.getPartCount(PartType.ARMA_A));
        assertFalse(mission.craftWeapon(), "craft nao se repete");
    }

    @Test
    @DisplayName("Portal exige 3 reparos, arma, hostis eliminados e O2 acima de 25")
    void portalRequiresEveryCondition() {
        prepararPortal();
        assertTrue(mission.isPortalUnlocked(26f));
        assertFalse(mission.isPortalUnlocked(25f), "O2 exatamente no limite nao libera");
        assertFalse(mission.isPortalUnlocked(10f));
    }

    @Test
    @DisplayName("Sem hostis registrados o portal continua bloqueado")
    void portalStaysLockedWithoutEnemyProgress() {
        prepararPortal();
        MissionState fresh = new MissionState();
        fresh.setTotalEnemies(3);
        assertFalse(fresh.isPortalUnlocked(100f));
    }

    @Test
    @DisplayName("Objetivo acompanha o estado atual da missao")
    void objectiveFollowsProgress() {
        assertEquals("Colete peças e reative três sistemas", mission.getObjective(100f));
        repararTres();
        assertEquals("Encontre as 3 partes da arma", mission.getObjective(100f));
        mission.collect(PartType.ARMA_A);
        mission.collect(PartType.ARMA_B);
        mission.collect(PartType.ARMA_C);
        assertEquals("Leve as partes até a bancada de montagem", mission.getObjective(100f));
        mission.craftWeapon();
        mission.setTotalEnemies(3);
        assertEquals("Neutralize os hostis: 0/3", mission.getObjective(100f));
        for (int index = 0; index < 3; index++) mission.registerEnemyDefeated();
        assertEquals("Recupere O2 acima de 25%", mission.getObjective(12f));
        assertEquals("Portal online: atravesse para Marte", mission.getObjective(80f));
    }

    @Test
    @DisplayName("restore reconstroi o estado sem duplicar reparos")
    void restoreRebuildsState() {
        mission.restore(1, 0, 0, 2, 1, 1, 1, true, true, false, false, false, 2);
        assertEquals(1, mission.getPartCount(PartType.ANTENA));
        assertEquals(2, mission.getPartCount(PartType.ESTUFA));
        assertEquals(2, mission.getRepairCount());
        assertTrue(mission.isRepaired(SystemType.ENERGIA));
        assertFalse(mission.isRepaired(SystemType.EXTRACAO));
        assertEquals(2, mission.getEnemiesDefeated());
        assertTrue(mission.hasAllWeaponParts());
        assertFalse(mission.hasWeapon());
    }

    @Test
    @DisplayName("Total de hostis nunca fica negativo")
    void totalEnemiesIsClamped() {
        mission.setTotalEnemies(-5);
        assertEquals(0, mission.getTotalEnemies());
    }

    private void repararTres() {
        mission.collect(PartType.ANTENA);
        mission.collect(PartType.ENERGIA);
        mission.collect(PartType.EXTRACAO);
        mission.repair(SystemType.COMUNICACAO);
        mission.repair(SystemType.ENERGIA);
        mission.repair(SystemType.EXTRACAO);
    }

    private void prepararPortal() {
        repararTres();
        mission.collect(PartType.ARMA_A);
        mission.collect(PartType.ARMA_B);
        mission.collect(PartType.ARMA_C);
        mission.craftWeapon();
        mission.setTotalEnemies(3);
        for (int index = 0; index < 3; index++) mission.registerEnemyDefeated();
    }
}
