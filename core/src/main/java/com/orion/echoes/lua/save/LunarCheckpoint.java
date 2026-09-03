package com.orion.echoes.lua.save;

import com.orion.echoes.lua.entities.MissionCollectible;
import com.orion.echoes.lua.entities.RepairStation;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.systems.MissionState;
import com.orion.echoes.lua.world.LunarWorld;

/**
 * Traducao entre a campanha em jogo e o {@link GameSaveData} serializado.
 *
 * Ao recarregar nao basta restaurar os numeros: as pecas ja usadas precisam
 * sumir do chao e os hostis derrotados precisam continuar derrotados, senao o
 * jogador reencontra o que ja tinha resolvido. O mesmo vale na volta de Marte
 * pelo portal, e por isso a sincronizacao mora aqui e nao no save.
 */
public final class LunarCheckpoint {

    private LunarCheckpoint() { }

    public static GameSaveData capture(LunarWorld world, CampaignState campaign) {
        MissionState mission = world.getMission();
        GameSaveData data = world.getPlayer().toSaveData();
        data.pecaAntena = mission.getPartCount(MissionState.PartType.ANTENA);
        data.pecaEnergia = mission.getPartCount(MissionState.PartType.ENERGIA);
        data.pecaExtracao = mission.getPartCount(MissionState.PartType.EXTRACAO);
        data.pecaEstufa = mission.getPartCount(MissionState.PartType.ESTUFA);
        data.armaParteA = mission.getPartCount(MissionState.PartType.ARMA_A);
        data.armaParteB = mission.getPartCount(MissionState.PartType.ARMA_B);
        data.armaParteC = mission.getPartCount(MissionState.PartType.ARMA_C);
        data.comunicacaoReparada = mission.isRepaired(MissionState.SystemType.COMUNICACAO);
        data.energiaReparada = mission.isRepaired(MissionState.SystemType.ENERGIA);
        data.extracaoReparada = mission.isRepaired(MissionState.SystemType.EXTRACAO);
        data.estufaReparada = mission.isRepaired(MissionState.SystemType.ESTUFA);
        data.armaCraftada = mission.hasWeapon();
        data.inimigosEliminados = mission.getEnemiesDefeated();
        applyCampaign(data, campaign);
        return data;
    }

    /** Grava os campos de campanha comuns as duas fases. */
    public static void applyCampaign(GameSaveData data, CampaignState campaign) {
        data.fase = campaign.phaseToken();
        data.semente = campaign.getSeed();
        data.municao = campaign.getAmmo();
        data.totalHostisLunares = campaign.getLunarTotalEnemies();
        data.marteVisitado = campaign.hasVisitedMars();
        data.marteConcluido = campaign.isMarsMissionComplete();
        data.marteNucleos = campaign.getMinerals();
        data.marteEstacoes = campaign.getMarsStationsOnline();
        data.marteHostis = campaign.getMarsHostilesDefeated();
        data.dialogoTita = campaign.isDialogoTita();
        data.combateOk = campaign.isCombateOk();
        data.amostraOk = campaign.isAmostraOk();
        data.entrouTita = campaign.isEntrouTita();
        data.versao = GameSaveData.CURRENT_VERSION;
    }

    /** Reconstroi a campanha a partir de um save, sem tocar no mundo. */
    public static CampaignState toCampaign(GameSaveData data) {
        CampaignState campaign = new CampaignState(data.semente);
        campaign.setPhase(CampaignState.phaseFromToken(data.fase));
        campaign.setVitals(data.oxigenio, data.energia);
        campaign.setAmmo(data.municao);
        campaign.setMissionTime(data.tempoVivo);
        campaign.setResources(data.gelo, data.agua, data.combustivel);
        campaign.fromLunarArray(new int[] {
            data.pecaAntena, data.pecaEnergia, data.pecaExtracao, data.pecaEstufa,
            data.armaParteA, data.armaParteB, data.armaParteC,
            data.comunicacaoReparada ? 1 : 0, data.energiaReparada ? 1 : 0,
            data.extracaoReparada ? 1 : 0, data.estufaReparada ? 1 : 0,
            data.armaCraftada ? 1 : 0, data.inimigosEliminados, data.totalHostisLunares
        });
        if (data.marteVisitado) {
            campaign.setMarsProgress(data.marteNucleos, data.marteEstacoes,
                data.marteHostis, data.marteConcluido);
        }
        campaign.setDialogoTita(data.dialogoTita);
        campaign.setCombateOk(data.combateOk);
        campaign.setAmostraOk(data.amostraOk);
        campaign.setEntrouTita(data.entrouTita);
        return campaign;
    }

    public static void apply(GameSaveData data, LunarWorld world, CampaignState campaign) {
        MissionState mission = world.getMission();
        world.getPlayer().fromSaveData(data);
        mission.restore(data.pecaAntena, data.pecaEnergia, data.pecaExtracao, data.pecaEstufa,
            data.armaParteA, data.armaParteB, data.armaParteC,
            data.comunicacaoReparada, data.energiaReparada, data.extracaoReparada,
            data.estufaReparada, data.armaCraftada, data.inimigosEliminados);
        mission.setMarsProgress(data.marteVisitado, data.marteConcluido);
        if (campaign != null) {
            campaign.setVitals(data.oxigenio, data.energia);
            campaign.setAmmo(data.municao);
            campaign.captureMission(mission);
            if (data.marteVisitado) {
                campaign.setMarsProgress(data.marteNucleos, data.marteEstacoes,
                    data.marteHostis, data.marteConcluido);
            }
        }
        syncWorld(world);
    }

    /** Repoe no mundo o que o progresso diz que ja foi consumido ou derrotado. */
    public static void syncWorld(LunarWorld world) {
        MissionState mission = world.getMission();
        for (RepairStation station : world.getRepairStations()) station.sync(mission);
        for (MissionCollectible collectible : world.getCollectibles()) {
            collectible.setAtivo(!isConsumed(collectible.getType(), mission));
        }
        for (int index = 0; index < world.getEnemies().size; index++) {
            world.getEnemies().get(index).setAtivo(index >= mission.getEnemiesDefeated());
        }
    }

    /** Uma peca some do chao se esta no inventario ou ja virou reparo/arma. */
    private static boolean isConsumed(MissionState.PartType type, MissionState mission) {
        if (mission.getPartCount(type) > 0) return true;
        return switch (type) {
            case ANTENA -> mission.isRepaired(MissionState.SystemType.COMUNICACAO);
            case ENERGIA -> mission.isRepaired(MissionState.SystemType.ENERGIA);
            case EXTRACAO -> mission.isRepaired(MissionState.SystemType.EXTRACAO);
            case ESTUFA -> mission.isRepaired(MissionState.SystemType.ESTUFA);
            case ARMA_A, ARMA_B, ARMA_C -> mission.hasWeapon();
        };
    }
}
