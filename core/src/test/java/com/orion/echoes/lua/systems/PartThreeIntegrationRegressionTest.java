package com.orion.echoes.lua.systems;

import com.orion.echoes.lua.screens.PhaseBossScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Trava dos defeitos reproduzidos no percurso completo da campanha.
 *
 * Cada teste aqui nasceu de um problema visto em execução, não de uma
 * propriedade inventada do modelo: o encontro de Aharin disparando a meia tela
 * de distância, a arena de chefe derrubando o jogo por pré-requisito, o dano da
 * arma e o mapa da rota lendo a fase certa.
 */
class PartThreeIntegrationRegressionTest {

    // -----------------------------------------------------
    // Aharin: o E dependia apenas de "x > 560".
    // -----------------------------------------------------

    @Test void sanctuaryTalkNeedsTheSanctuaryNotJustTheRightHalfOfTheMap() {
        // Ponto de chegada do jogador, na passarela oeste: caminhável, longe.
        assertTrue(SanctuaryZone.walkable(457f, 281f), "a passarela de chegada precisa ser caminhável");
        assertFalse(SanctuaryZone.canTalk(457f, 281f), "não se conversa da passarela");
        // x > 560 era o gatilho antigo: continua sem conversar.
        assertFalse(SanctuaryZone.canTalk(600f, 300f));
        assertFalse(SanctuaryZone.canTalk(1180f, 380f), "a borda leste do terraço não alcança as entidades");
        // Junto das entidades, conversa.
        assertTrue(SanctuaryZone.canTalk(900f, 330f));
        for (float[] entity : SanctuaryZone.ENTITIES)
            assertTrue(SanctuaryZone.canTalk(entity[0], entity[1] - 40f));
    }

    @Test void sanctuaryFloorFollowsTheTerraceAndNotTheWholeScreen() {
        assertFalse(SanctuaryZone.walkable(60f, 600f), "o vazio de nuvens não é chão");
        assertFalse(SanctuaryZone.walkable(1240f, 660f));
        assertTrue(SanctuaryZone.walkable(SanctuaryZone.TERRACE_X, SanctuaryZone.TERRACE_Y));
        // Passarela e terraço precisam se tocar, senão o jogador fica preso.
        assertTrue(SanctuaryZone.walkable(690f, 292f) || SanctuaryZone.walkable(700f, 292f));
    }

    // -----------------------------------------------------
    // Arenas: pré-requisito é consulta, não exceção.
    // -----------------------------------------------------

    @Test void bossArenaAnswersThePrerequisiteInsteadOfThrowing() {
        CampaignState fresh = new CampaignState(7L);
        assertFalse(PhaseBossScreen.liberada(fresh, false));
        assertFalse(PhaseBossScreen.liberada(fresh, true));
        assertFalse(PhaseBossScreen.bloqueio(false).isBlank());
        assertFalse(PhaseBossScreen.bloqueio(true).isBlank());
        fresh.fromLunarArray(new int[]{0,0,0,0,0,0,0,1,1,1,0,1,4,4});
        assertTrue(PhaseBossScreen.liberada(fresh, false));
    }

    // -----------------------------------------------------
    // Arma: incremento de 5 por nível, teto 3, base do projeto.
    // -----------------------------------------------------

    @Test void weaponDamageKeepsTheGuidesFivePerLevelStep() {
        Inventario inventory = new Inventario();
        float base = inventory.getDano();
        for (int level = 1; level <= 3; level++) {
            assertTrue(inventory.melhorarArma());
            assertEquals(base + 5f * level, inventory.getDano(), .001f);
        }
        assertFalse(inventory.melhorarArma(), "o teto de nível 3 vem do guia");
        assertEquals(base + 15f, inventory.getDano(), .001f);
    }

    @Test void weaponDamageStillKillsEveryBossWithinTheMagazineBudget() {
        // Um pente cheio (30) precisa dar conta da forma mais dura de Calisto;
        // senão a fase vira beco sem saída por munição.
        Inventario inventory = new Inventario();
        inventory.melhorarArma(); inventory.melhorarArma();
        int shots = (int) Math.ceil(220f / inventory.getDano());
        assertTrue(shots <= com.orion.echoes.lua.config.GameConfig.AMMO_MAX,
            "forma 3 exigiria " + shots + " acertos");
    }

    // -----------------------------------------------------
    // Armadura: reduz de verdade e nunca zera o dano.
    // -----------------------------------------------------

    @Test void armourReducesDamageWithoutMakingThePlayerImmortal() {
        Inventario inventory = new Inventario();
        assertEquals(1f, inventory.getMultiplicadorDanoRecebido(), .001f);
        for (int i = 0; i < 3; i++) inventory.melhorarArmadura();
        float multiplier = inventory.getMultiplicadorDanoRecebido();
        assertEquals(.55f, multiplier, .001f);
        assertTrue(multiplier > 0f, "armadura no teto ainda deixa dano passar");
    }

    // -----------------------------------------------------
    // Rota: a fase atual vem do estado da campanha, não de um if solto.
    // -----------------------------------------------------

    @Test void routeGatesFollowTheKeysTheCampaignActuallyHolds() {
        CampaignState state = new CampaignState(9L);
        assertFalse(state.getInventario().tem(Inventario.CHAVE_TITA));
        state.setPhase(CampaignState.Phase.TITAN);
        assertEquals("TITA", state.phaseToken());
        assertFalse(state.missaoAtual().isBlank());
        state.getInventario().add(Inventario.CHAVE_TITA);
        assertTrue(state.missaoAtual().toLowerCase().contains("calisto"),
            "com a chave de Titã o objetivo passa a apontar Calisto");
    }

    @Test void callistoSaveNeverResurrectsAFinishedBossOrDuplicatesTheKey() {
        CampaignState state = new CampaignState(11L);
        state.setPhase(CampaignState.Phase.CALLISTO);
        state.setBossCalisto(3, 0f);
        state.getInventario().add(Inventario.CHAVE_LUZ);
        com.orion.echoes.lua.save.GameSaveData data = new com.orion.echoes.lua.save.GameSaveData();
        com.orion.echoes.lua.save.LunarCheckpoint.applyCampaign(data, state);
        CampaignState restored = com.orion.echoes.lua.save.LunarCheckpoint.toCampaign(data);
        assertEquals(3, restored.getFormaBossCalisto());
        assertEquals(1, restored.getInventario().exportarChaves().length);
        assertTrue(restored.getInventario().tem(Inventario.CHAVE_LUZ));
        // Recarregar não pode somar outro nível de equipamento.
        assertEquals(state.getInventario().getNivelArma(), restored.getInventario().getNivelArma());
        assertEquals(state.getInventario().getNivelArmadura(), restored.getInventario().getNivelArmadura());
    }
}
