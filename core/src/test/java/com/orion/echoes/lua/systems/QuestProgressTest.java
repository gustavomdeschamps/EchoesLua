package com.orion.echoes.lua.systems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.orion.echoes.lua.events.EventBus;
import com.orion.echoes.lua.systems.MissionState.PartType;
import com.orion.echoes.lua.systems.MissionState.SystemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Passos numerados da quest, do primeiro reparo ao desembarque. */
class QuestProgressTest {

    private MissionState mission;

    @BeforeEach
    void setUp() {
        EventBus.getInstance().clear();
        mission = new MissionState();
    }

    @Test
    @DisplayName("A quest avanca um passo por marco concluido")
    void questAdvancesWithProgress() {
        assertEquals(1, mission.getQuestStep(100f));

        mission.collect(PartType.ANTENA);
        assertEquals(2, mission.getQuestStep(100f));

        mission.repair(SystemType.COMUNICACAO);
        mission.collect(PartType.ENERGIA);
        mission.repair(SystemType.ENERGIA);
        mission.collect(PartType.EXTRACAO);
        mission.repair(SystemType.EXTRACAO);
        assertEquals(3, mission.getQuestStep(100f), "tres sistemas online, falta a arma");

        mission.collect(PartType.ARMA_A);
        mission.collect(PartType.ARMA_B);
        mission.collect(PartType.ARMA_C);
        mission.craftWeapon();
        mission.setTotalEnemies(2);
        assertEquals(4, mission.getQuestStep(100f), "arma pronta, faltam os hostis");

        mission.registerEnemyDefeated();
        mission.registerEnemyDefeated();
        assertEquals(5, mission.getQuestStep(100f), "portal liberado, falta atravessar");

        mission.setMarsProgress(true, false);
        assertEquals(6, mission.getQuestStep(100f), "em Marte, colonia por restaurar");

        mission.setMarsProgress(true, true);
        assertEquals(MissionState.QUEST_TOTAL_STEPS, mission.getQuestStep(100f));
    }

    @Test
    @DisplayName("Oxigenio baixo segura a quest no passo do portal")
    void lowOxygenHoldsThePortalStep() {
        concluirFaseLunar();
        assertEquals(5, mission.getQuestStep(10f), "sem O2 o portal nao abre");
        assertEquals(5, mission.getQuestStep(90f));
    }

    /** Leva a missao ate o ponto em que o portal e a unica pendencia. */
    private void concluirFaseLunar() {
        mission.collect(PartType.ANTENA);
        mission.repair(SystemType.COMUNICACAO);
        mission.collect(PartType.ENERGIA);
        mission.repair(SystemType.ENERGIA);
        mission.collect(PartType.EXTRACAO);
        mission.repair(SystemType.EXTRACAO);
        mission.collect(PartType.ARMA_A);
        mission.collect(PartType.ARMA_B);
        mission.collect(PartType.ARMA_C);
        mission.craftWeapon();
        mission.setTotalEnemies(1);
        mission.registerEnemyDefeated();
    }

    @Test
    @DisplayName("Todo passo tem titulo proprio e nao vazio")
    void everyStepHasATitle() {
        for (float oxygen : new float[] {100f, 10f}) {
            String title = mission.getQuestTitle(oxygen);
            assertTrue(title != null && !title.isBlank());
        }
        // O passo final so e alcancavel com a Lua inteira concluida: nao da
        // para estar em Marte sem ter atravessado o portal.
        concluirFaseLunar();
        mission.setMarsProgress(true, true);
        assertEquals("Alcançar a plataforma de extração", mission.getQuestTitle(100f));
    }
}
