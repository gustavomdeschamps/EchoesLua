package com.orion.echoes.lua.systems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.events.EventBus;
import com.orion.echoes.lua.systems.MissionState.PartType;
import com.orion.echoes.lua.systems.MissionState.SystemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Estado que atravessa o portal nos dois sentidos e alimenta o save. */
class CampaignStateTest {

    @BeforeEach
    void setUp() {
        EventBus.getInstance().clear();
    }

    @Test
    @DisplayName("Campanha nova comeca na Lua, sem progresso")
    void freshCampaignStartsOnTheMoon() {
        CampaignState campaign = new CampaignState(42L);
        assertEquals(CampaignState.Phase.LUNAR, campaign.getPhase());
        assertEquals(42L, campaign.getSeed());
        assertFalse(campaign.hasLunarProgress());
        assertFalse(campaign.hasVisitedMars());
        assertEquals(0, campaign.getAmmo());
    }

    @Test
    @DisplayName("Ida e volta pelo portal preserva o progresso lunar")
    void portalRoundTripKeepsLunarProgress() {
        MissionState outbound = new MissionState();
        outbound.collect(PartType.ANTENA);
        outbound.repair(SystemType.COMUNICACAO);
        outbound.collect(PartType.ARMA_A);
        outbound.collect(PartType.ARMA_B);
        outbound.collect(PartType.ARMA_C);
        outbound.craftWeapon();
        outbound.setTotalEnemies(4);
        outbound.registerEnemyDefeated();
        outbound.registerEnemyDefeated();

        CampaignState campaign = new CampaignState(7L);
        campaign.captureMission(outbound);
        campaign.setVitals(63.5f, 41.25f);
        campaign.setAmmo(9);

        // Volta de Marte: uma missao nova recebe o progresso guardado.
        MissionState inbound = new MissionState();
        campaign.restoreMission(inbound);

        assertTrue(inbound.isRepaired(SystemType.COMUNICACAO));
        assertTrue(inbound.hasWeapon());
        assertEquals(2, inbound.getEnemiesDefeated());
        assertEquals(63.5f, campaign.getOxygen());
        assertEquals(41.25f, campaign.getEnergy());
        assertEquals(9, campaign.getAmmo());
        assertTrue(campaign.hasLunarProgress());
    }

    @Test
    @DisplayName("A semente nao muda, entao a Lua e a mesma na volta")
    void seedSurvivesTheRoundTrip() {
        CampaignState campaign = new CampaignState(123456789L);
        campaign.setPhase(CampaignState.Phase.MARS);
        campaign.setMarsProgress(2, 1, 3, false);
        campaign.setPhase(CampaignState.Phase.LUNAR);
        assertEquals(123456789L, campaign.getSeed());
    }

    @Test
    @DisplayName("Progresso marciano e guardado e marca a visita")
    void marsProgressIsRecorded() {
        CampaignState campaign = new CampaignState(1L);
        assertFalse(campaign.hasVisitedMars());
        campaign.setMarsProgress(3, 2, 1, false);
        assertTrue(campaign.hasVisitedMars());
        assertEquals(3, campaign.getMinerals());
        assertEquals(2, campaign.getMarsStationsOnline());
        assertEquals(1, campaign.getMarsHostilesDefeated());
        assertFalse(campaign.isMarsMissionComplete());

        campaign.setMarsProgress(0, 3, 4, true);
        assertTrue(campaign.isMarsMissionComplete());
    }

    @Test
    @DisplayName("Vitais e municao respeitam os limites")
    void valuesAreClamped() {
        CampaignState campaign = new CampaignState(1L);
        campaign.setVitals(999f, -50f);
        assertEquals(GameConfig.MAX_OXYGEN, campaign.getOxygen());
        assertEquals(0f, campaign.getEnergy());
        campaign.setAmmo(9999);
        assertEquals(GameConfig.AMMO_MAX, campaign.getAmmo());
        campaign.setAmmo(-3);
        assertEquals(0, campaign.getAmmo());
    }

    @Test
    @DisplayName("Serializacao lunar faz round-trip completo")
    void lunarArrayRoundTrip() {
        MissionState mission = new MissionState();
        mission.collect(PartType.ESTUFA);
        mission.collect(PartType.ESTUFA);
        mission.repair(SystemType.ESTUFA);
        mission.setTotalEnemies(4);
        mission.registerEnemyDefeated();

        CampaignState source = new CampaignState(5L);
        source.captureMission(mission);

        CampaignState target = new CampaignState(5L);
        target.fromLunarArray(source.toLunarArray());

        MissionState restored = new MissionState();
        target.restoreMission(restored);
        assertEquals(1, restored.getPartCount(PartType.ESTUFA));
        assertTrue(restored.isRepaired(SystemType.ESTUFA));
        assertEquals(1, restored.getEnemiesDefeated());
        assertEquals(4, target.getLunarTotalEnemies());
    }

    @Test
    @DisplayName("Array curto ou nulo nao corrompe o estado")
    void malformedArrayIsIgnored() {
        CampaignState campaign = new CampaignState(1L);
        campaign.fromLunarArray(null);
        campaign.fromLunarArray(new int[] {1, 2, 3});
        assertFalse(campaign.hasLunarProgress());
    }
}
