package com.orion.echoes.lua.save;

import com.orion.echoes.lua.entities.MissionCollectible;
import com.orion.echoes.lua.entities.RepairStation;
import com.orion.echoes.lua.systems.MissionState;
import com.orion.echoes.lua.world.LunarWorld;

/**
 * Traducao entre o mundo em jogo e o {@link GameSaveData} serializado.
 *
 * Ao recarregar nao basta restaurar os numeros: as pecas ja usadas precisam
 * sumir do chao e os hostis derrotados precisam continuar derrotados, senao o
 * jogador reencontra o que ja tinha resolvido.
 */
public final class LunarCheckpoint {

    private LunarCheckpoint() { }

    public static GameSaveData capture(LunarWorld world) {
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
        return data;
    }

    public static void apply(GameSaveData data, LunarWorld world) {
        MissionState mission = world.getMission();
        world.getPlayer().fromSaveData(data);
        mission.restore(data.pecaAntena, data.pecaEnergia, data.pecaExtracao, data.pecaEstufa,
            data.armaParteA, data.armaParteB, data.armaParteC,
            data.comunicacaoReparada, data.energiaReparada, data.extracaoReparada,
            data.estufaReparada, data.armaCraftada, data.inimigosEliminados);

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
