package com.orion.echoes.lua.systems;

import com.orion.echoes.lua.entities.*;
import com.orion.echoes.lua.save.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartThreeCampaignTest {
    @Test void lightKeyRequiresAllThreeLives() {
        BossCalisto boss=new BossCalisto();
        assertFalse(boss.receiveDamage(125f)); assertEquals(2,boss.getForma());
        assertEquals(180f,boss.getHp()); assertTrue(boss.isMutating());
        boss.receiveDamage(10000f); assertEquals(2,boss.getForma(),"mutation grace rejects damage");
        boss.update(1.3f); assertFalse(boss.receiveDamage(180f)); assertEquals(3,boss.getForma());
        assertEquals(260f,boss.getHp()); boss.update(1.3f);
        assertTrue(boss.receiveDamage(260f)); assertFalse(boss.isAlive());
        assertFalse(boss.receiveDamage(1f));
    }
    @Test void damageDoesNotSkipLivesAndSpeedIncreases() {
        BossCalisto boss=new BossCalisto(); float speed=boss.getSpeed();
        boss.receiveDamage(100000f); assertEquals(2,boss.getForma()); assertTrue(boss.getSpeed()>speed);
    }
    @Test void inventoryCannotMintAnUnknownKeyOrConsumeMissingFood() {
        Inventario inventory=new Inventario(); inventory.add("DEBUG_ALL");
        assertFalse(inventory.tem("DEBUG_ALL")); assertFalse(inventory.consumirComida());
        inventory.add("COMIDA"); assertTrue(inventory.consumirComida()); assertEquals(0,inventory.getComida());
    }
    @Test void upgradesCapAndReduceActualDamageMultiplier() {
        Inventario inventory=new Inventario();
        for(int i=0;i<3;i++){assertTrue(inventory.melhorarArma());assertTrue(inventory.melhorarArmadura());}
        assertFalse(inventory.melhorarArma());assertFalse(inventory.melhorarArmadura());
        // Base do projeto + 5 por nivel, conforme o passo do guia.
        assertEquals(new Inventario().getDano()+15f,inventory.getDano());
        assertEquals(.55f,inventory.getMultiplicadorDanoRecebido(),.001f);
    }
    @Test void checkpointKeepsKeysFoodUpgradesAndMutation() {
        CampaignState state=new CampaignState(7);state.setPhase(CampaignState.Phase.CALLISTO);
        state.getInventario().add(Inventario.CHAVE_TITA);state.getInventario().add("COMIDA");
        state.getInventario().melhorarArma();state.getInventario().melhorarArmadura();state.setBossCalisto(2,93f);
        GameSaveData data=new GameSaveData();LunarCheckpoint.applyCampaign(data,state);
        CampaignState restored=LunarCheckpoint.toCampaign(data);
        assertEquals(CampaignState.Phase.CALLISTO,restored.getPhase());
        assertTrue(restored.getInventario().tem(Inventario.CHAVE_TITA));assertFalse(restored.getInventario().tem(Inventario.CHAVE_LUZ));
        assertEquals(1,restored.getInventario().getComida());assertEquals(1,restored.getInventario().getNivelArma());
        BossCalisto boss=new BossCalisto(restored.getFormaBossCalisto(),restored.getHpBossCalisto());
        assertEquals(2,boss.getForma());assertEquals(93f,boss.getHp());
    }
    @Test void oldSaveDoesNotInventKeys() {
        GameSaveData old=new GameSaveData();old.versao=5;
        assertEquals(0,LunarCheckpoint.toCampaign(old).getInventario().exportarChaves().length);
    }
    @Test void everyWorldRoundTripsThePhaseToken() {
        CampaignState state=new CampaignState(1);
        for(CampaignState.Phase phase:CampaignState.Phase.values()) {
            state.setPhase(phase);assertEquals(phase,CampaignState.phaseFromToken(state.phaseToken()));
        }
    }
    @Test void planetaryBossesHaveSeparateHealthBudgets() {
        assertEquals(150f,new BossLua().getHpMax());assertEquals(205f,new BossMarte().getHpMax());
    }
    @Test void defeatedBossPersistsBeforeItsPhysicalKeyIsCollected() {
        CampaignState state=new CampaignState(11);
        state.setBossDefeated(CampaignState.Phase.LUNAR,true);
        state.setBossDefeated(CampaignState.Phase.MARS,true);
        state.setBossDefeated(CampaignState.Phase.TITAN,true);
        assertFalse(state.getInventario().tem(Inventario.CHAVE_LUA));
        GameSaveData data=new GameSaveData();LunarCheckpoint.applyCampaign(data,state);
        CampaignState restored=LunarCheckpoint.toCampaign(data);
        assertTrue(restored.isBossDefeated(CampaignState.Phase.LUNAR));
        assertTrue(restored.isBossDefeated(CampaignState.Phase.MARS));
        assertTrue(restored.isBossDefeated(CampaignState.Phase.TITAN));
        assertFalse(restored.isBossDefeated(CampaignState.Phase.CALLISTO));
        assertFalse(restored.getInventario().tem(Inventario.CHAVE_LUA));
    }
    @Test void arenasRequireWorldMissionsNotJustAWeapon() {
        CampaignState state=new CampaignState(2);assertFalse(state.luaMissoesOk());assertFalse(state.marteMissoesOk());
        state.fromLunarArray(new int[]{0,0,0,0,0,0,0,1,1,1,0,1,4,4});assertTrue(state.luaMissoesOk());
        state.setMarsProgress(0,3,4,true);assertFalse(state.marteMissoesOk());
        state.setDialogoTita(true);state.setCombateOk(true);assertTrue(state.marteMissoesOk());
    }
}
