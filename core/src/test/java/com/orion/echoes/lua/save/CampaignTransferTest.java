package com.orion.echoes.lua.save;

import com.orion.echoes.lua.systems.CampaignState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CampaignTransferTest {
    @Test void everyPlanetPreservesPartialResourcesAndDialogueFlags() {
        for (CampaignState.Phase phase : CampaignState.Phase.values()) {
            CampaignState before = new CampaignState(1234L);
            before.setPhase(phase);
            before.setVitals(47.5f, 63f);
            before.setAmmo(7);
            before.setResources(3, 2, 1);
            before.setMissionTime(184f);
            before.setDialogoLua(true);
            before.setDialogoTita(true);
            before.setDialogoExplorador(true);
            before.setCombateOk(true);
            GameSaveData data = new GameSaveData();
            data.posX = 420f;
            data.posY = 360f;
            LunarCheckpoint.applyCampaign(data, before);
            CampaignState after = LunarCheckpoint.toCampaign(data);
            assertEquals(before.phaseToken(), after.phaseToken());
            assertEquals(47.5f, after.getOxygen());
            assertEquals(63f, after.getEnergy());
            assertEquals(7, after.getAmmo());
            assertEquals(3, after.getIce());
            assertEquals(2, after.getWater());
            assertEquals(1, after.getFuel());
            assertEquals(184f, after.getMissionTime());
            assertTrue(after.isDialogoLua());
            assertTrue(after.isDialogoTita());
            assertTrue(after.isDialogoExplorador());
            assertTrue(after.isCombateOk());
            assertEquals(420f, data.posX);
            assertEquals(360f, data.posY);
        }
    }

    @Test void worldIntroFlagsRoundTripOnAFreshSave() {
        CampaignState before = new CampaignState(1L);
        before.setLunarIntroShown(true);
        before.setMarsIntroShown(true);
        GameSaveData data = new GameSaveData();
        LunarCheckpoint.applyCampaign(data, before);
        CampaignState after = LunarCheckpoint.toCampaign(data);
        assertTrue(after.isLunarIntroShown());
        assertTrue(after.isMarsIntroShown());
        assertFalse(after.isTitanIntroShown());
    }

    @Test void legacySaveWithoutIntroFlagsDoesNotReplayCinematicsAlreadyEarned() {
        // Save anterior à versão 5: nunca gravou as flags de abertura, então
        // chegam com o default do Json (false). A Lua já foi vista por
        // definição (é sempre o primeiro mundo); Marte e Titã herdam da
        // visita/entrada já registradas, para o portal bidirecional não
        // reapresentar a cinematic completa a cada troca de fase.
        GameSaveData data = new GameSaveData();
        data.versao = 4;
        data.marteVisitado = true;
        data.entrouTita = false;
        CampaignState restored = LunarCheckpoint.toCampaign(data);
        assertTrue(restored.isLunarIntroShown());
        assertTrue(restored.isMarsIntroShown());
        assertFalse(restored.isTitanIntroShown());
    }
}
