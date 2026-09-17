package com.orion.echoes.lua.systems;

import com.orion.echoes.lua.save.GameSaveData;
import com.orion.echoes.lua.save.LunarCheckpoint;
import com.orion.echoes.lua.screens.PhaseBossScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Corrente de chaves da campanha, elo por elo.
 *
 * Sao quatro portas e quatro chefes, e cada chave so existe porque um chefe
 * caiu: Guardiao da Cratera -> CHAVE_LUA, Tita-Ferrugem -> CHAVE_MARTE,
 * Soberano do Metano -> CHAVE_TITA, Sentinela de Calisto -> CHAVE_LUZ. Se
 * alguem apagar uma constante, trocar o id gravado no save ou soltar um mundo
 * sem porta, a campanha vira um beco sem saida no meio da partida - que e um
 * defeito caro de descobrir jogando. Este teste quebra antes disso.
 */
class CampaignKeyChainTest {

    /** A rota inteira, na ordem em que o jogador a percorre. */
    private static final String[] ROTA = {
        Inventario.CHAVE_LUA, Inventario.CHAVE_MARTE,
        Inventario.CHAVE_TITA, Inventario.CHAVE_LUZ
    };

    @Test void aRotaTemAsQuatroChavesEElasSaoDistintas() {
        Inventario inventario = new Inventario();
        for (String chave : ROTA) {
            assertFalse(inventario.tem(chave), chave + " nao pode nascer no inventario");
            inventario.add(chave);
            assertTrue(inventario.tem(chave), chave + " precisa ser aceita por Inventario.add");
        }
        assertEquals(ROTA.length, inventario.exportarChaves().length,
            "as quatro chaves da rota precisam ser ids distintos");
    }

    @Test void inventarioRecusaChaveInventada() {
        Inventario inventario = new Inventario();
        inventario.add("CHAVE_CALISTO");
        inventario.add("CHAVE_LUA ");
        assertEquals(0, inventario.exportarChaves().length,
            "so as quatro constantes da rota valem como chave");
    }

    @Test void oSaveDevolveAsQuatroChavesJuntas() {
        CampaignState campanha = new CampaignState(7L);
        for (String chave : ROTA) campanha.getInventario().add(chave);
        GameSaveData data = new GameSaveData();
        LunarCheckpoint.applyCampaign(data, campanha);

        CampaignState restaurada = LunarCheckpoint.toCampaign(data);
        for (String chave : ROTA) {
            assertTrue(restaurada.getInventario().tem(chave),
                chave + " se perdeu no ciclo de save/load");
        }
        assertEquals(ROTA.length, restaurada.getInventario().exportarChaves().length);
    }

    /**
     * Porta da Lua: a arena do Guardiao so abre com os reparos, o rifle e a
     * cratera limpa. E dela que sai a CHAVE_LUA.
     */
    @Test void aPortaLunarSoAbreComAMissaoLunarInteira() {
        CampaignState campanha = new CampaignState(3L);
        assertFalse(PhaseBossScreen.liberada(campanha, false), "campanha nova nao entra na arena lunar");
        // 3 reparos + arma montada + cratera limpa (5 de 5).
        campanha.fromLunarArray(new int[] {0,0,0,0, 0,0,0, 1,1,1,0, 1, 5, 5});
        assertTrue(PhaseBossScreen.liberada(campanha, false));
        assertFalse(campanha.getInventario().tem(Inventario.CHAVE_LUA),
            "chegar na arena nao entrega a chave; derrotar o chefe entrega");
    }

    /**
     * Porta de Marte: estacoes, hostis e o selo do portal. Era exatamente aqui
     * que o contador de hostis estourava e prendia o jogador diante do portal.
     */
    @Test void aPortaMarcianaSoAbreComEstacoesHostisEPortalLiberado() {
        CampaignState campanha = new CampaignState(3L);
        campanha.setPhase(CampaignState.Phase.MARS);
        campanha.setMarsProgress(0, 3, 4, true);
        assertFalse(PhaseBossScreen.liberada(campanha, true),
            "sem o selo do portal a arena marciana segue fechada");

        campanha.setDialogoTita(true);
        campanha.setCombateOk(true);
        assertTrue(campanha.portalLiberado());
        assertTrue(PhaseBossScreen.liberada(campanha, true));

        // Missao marciana incompleta nunca abre, por mais liberado que esteja o selo.
        campanha.setMarsProgress(0, 2, 4, false);
        assertFalse(PhaseBossScreen.liberada(campanha, true));
    }

    /** Cada chave conquistada reaponta o objetivo para o proximo corpo da rota. */
    @Test void cadaChaveEmpurraOObjetivoParaOProximoMundo() {
        CampaignState campanha = new CampaignState(5L);

        campanha.setPhase(CampaignState.Phase.LUNAR);
        campanha.getInventario().add(Inventario.CHAVE_LUA);
        assertTrue(campanha.missaoAtual().toLowerCase().contains("marte"));

        campanha.setPhase(CampaignState.Phase.MARS);
        campanha.setDialogoTita(true);
        campanha.setCombateOk(true);
        campanha.getInventario().add(Inventario.CHAVE_MARTE);
        assertTrue(campanha.missaoAtual().toLowerCase().contains("titã"));

        campanha.setPhase(CampaignState.Phase.TITAN);
        campanha.getInventario().add(Inventario.CHAVE_TITA);
        assertTrue(campanha.missaoAtual().toLowerCase().contains("calisto"));

        campanha.setPhase(CampaignState.Phase.CALLISTO);
        campanha.getInventario().add(Inventario.CHAVE_LUZ);
        assertTrue(campanha.missaoAtual().toLowerCase().contains("aharin"));
    }
}
