package com.orion.echoes.lua.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Identidade visual dos NPCs.
 *
 * Ayla, o Oficial de Marte e Lira precisam resolver para folhas diferentes -
 * este é o teste que impede uma regressão para o antigo boolean "ayla", onde
 * qualquer NPC que não fosse Ayla caía automaticamente na mesma folha
 * genérica. Não depende de OpenGL: exercita só o mapeamento de nomes, que é
 * dado puro em {@link Npc.Visual}.
 */
class NpcVisualTest {

    @Test
    void aylaHasNoFallbackAndOwnSheet() {
        assertEquals("npc_commander_ayla_sheet", Npc.Visual.AYLA.sheetKey());
        assertNull(Npc.Visual.AYLA.fallbackKey());
    }

    @Test
    void marsOfficerHasNoFallbackAndOwnSheet() {
        assertEquals("npc_colony_officer_sheet_v2", Npc.Visual.MARS_OFFICER.sheetKey());
        assertNull(Npc.Visual.MARS_OFFICER.fallbackKey());
    }

    @Test
    void liraHasOwnSheetKeyDistinctFromEveryoneElse() {
        assertEquals("npc_researcher_lira_sheet_v2", Npc.Visual.LIRA.sheetKey());
        assertNotEquals(Npc.Visual.MARS_OFFICER.sheetKey(), Npc.Visual.LIRA.sheetKey());
        assertNotEquals(Npc.Visual.AYLA.sheetKey(), Npc.Visual.LIRA.sheetKey());
    }

    @Test
    void liraFallsBackToMarsOfficerUntilHerOwnSheetIsProvided() {
        // Enquanto npc_researcher_lira_sheet_v2.png não existir, o fallback
        // documentado (docs/NEW_VISUAL_ASSETS.md) é a folha do Oficial de
        // Marte - nunca a de Ayla, e nunca deixar o jogo sem folha nenhuma.
        assertEquals(Npc.Visual.MARS_OFFICER.sheetKey(), Npc.Visual.LIRA.fallbackKey());
    }

    @Test
    void everyVisualHasADistinctSheetKey() {
        Npc.Visual[] values = Npc.Visual.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i].sheetKey(), values[j].sheetKey(),
                    values[i] + " e " + values[j] + " não podem ter o mesmo sheetKey");
            }
        }
    }
}
