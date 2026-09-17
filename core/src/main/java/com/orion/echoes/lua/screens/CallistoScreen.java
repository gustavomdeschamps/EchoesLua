package com.orion.echoes.lua.screens;

import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.entities.BossCalisto;
import com.orion.echoes.lua.systems.CampaignState;

public final class CallistoScreen extends ExpeditionArenaScreen {
    public CallistoScreen(EchoesLua game, CampaignState campaign) {
        super(game, campaign, CampaignState.Phase.CALLISTO,
            new BossCalisto(campaign.getFormaBossCalisto(), campaign.getHpBossCalisto()));
    }
}
