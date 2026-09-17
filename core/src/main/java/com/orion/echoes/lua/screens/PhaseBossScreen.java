package com.orion.echoes.lua.screens;

import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.entities.BossLua;
import com.orion.echoes.lua.entities.BossMarte;
import com.orion.echoes.lua.systems.CampaignState;

/** The crater arena is entered only through a mission-authorized world portal. */
public final class PhaseBossScreen extends ExpeditionArenaScreen {

    /**
     * Pre-requisito da arena.
     *
     * A versao anterior lancava IllegalArgumentException no construtor. Mesmo
     * que os portais ja filtrem, uma tela que derruba o jogo quando a condicao
     * falha transforma um erro de regra em crash na troca de tela. O teste
     * ficou como consulta: quem chama pergunta antes e mostra a mensagem.
     */
    public static boolean liberada(CampaignState campaign, boolean mars) {
        return mars ? campaign.marteMissoesOk() : campaign.luaMissoesOk();
    }

    public static String bloqueio(boolean mars) {
        return mars
            ? "BLOQUEADO — reative as três estações e neutralize os hostis antes do Titã-Ferrugem."
            : "BLOQUEADO — repare os sistemas, monte o rifle e limpe a cratera antes do Guardião.";
    }

    public PhaseBossScreen(EchoesLua game, CampaignState campaign, boolean mars) {
        super(game, campaign, mars ? CampaignState.Phase.MARS : CampaignState.Phase.LUNAR,
            mars ? new BossMarte() : new BossLua());
    }
}
