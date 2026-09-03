package com.orion.echoes.lua.systems;

import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.events.EventBus;
import com.orion.echoes.lua.events.EventType;

/** Centraliza inventario e progressao da missao Lua -> Marte. */
public class MissionState {

    public enum PartType {
        ANTENA("Antena"),
        ENERGIA("Energia"),
        EXTRACAO("Extração"),
        ESTUFA("Estufa"),
        ARMA_A("Arma A"),
        ARMA_B("Arma B"),
        ARMA_C("Arma C");

        private final String label;

        PartType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum SystemType {
        COMUNICACAO("Comunicação", PartType.ANTENA),
        ENERGIA("Energia", PartType.ENERGIA),
        EXTRACAO("Extração", PartType.EXTRACAO),
        ESTUFA("Estufa", PartType.ESTUFA);

        private final String label;
        private final PartType requiredPart;

        SystemType(String label, PartType requiredPart) {
            this.label = label;
            this.requiredPart = requiredPart;
        }

        public String getLabel() {
            return label;
        }

        public PartType getRequiredPart() {
            return requiredPart;
        }
    }

    private final ObjectIntMap<PartType> parts = new ObjectIntMap<>();
    private final ObjectSet<SystemType> repairedSystems = new ObjectSet<>();
    private boolean weaponCrafted;
    private int enemiesDefeated;
    private int totalEnemies;
    private boolean marsVisited;
    private boolean marsMissionComplete;

    public void collect(PartType type) {
        parts.getAndIncrement(type, 0, 1);
        EventBus.getInstance().publish(EventType.MISSION_PART_COLLECTED, type);
    }

    public int getPartCount(PartType type) {
        return parts.get(type, 0);
    }

    public boolean repair(SystemType type) {
        if (repairedSystems.contains(type) || getPartCount(type.getRequiredPart()) < 1) {
            return false;
        }
        parts.getAndIncrement(type.getRequiredPart(), 0, -1);
        repairedSystems.add(type);
        EventBus.getInstance().publish(EventType.COLONY_SYSTEM_REPAIRED, type);
        return true;
    }

    public boolean craftWeapon() {
        if (weaponCrafted || !hasAllWeaponParts()) {
            return false;
        }
        for (PartType type : new PartType[] {PartType.ARMA_A, PartType.ARMA_B, PartType.ARMA_C}) {
            parts.getAndIncrement(type, 0, -1);
        }
        weaponCrafted = true;
        EventBus.getInstance().publish(EventType.WEAPON_CRAFTED);
        return true;
    }

    public boolean hasAllWeaponParts() {
        return getPartCount(PartType.ARMA_A) > 0
            && getPartCount(PartType.ARMA_B) > 0
            && getPartCount(PartType.ARMA_C) > 0;
    }

    public void registerEnemyDefeated() {
        enemiesDefeated++;
        EventBus.getInstance().publish(EventType.ENEMY_DEFEATED, enemiesDefeated);
    }

    public boolean isPortalUnlocked(float oxygen) {
        return repairedSystems.size >= 3
            && weaponCrafted
            && totalEnemies > 0
            && enemiesDefeated >= totalEnemies
            && oxygen > 25f;
    }

    public String getObjective(float oxygen) {
        if (repairedSystems.size < 3) {
            return "Colete peças e reative três sistemas";
        }
        if (!weaponCrafted) {
            return hasAllWeaponParts()
                ? "Leve as partes até a bancada de montagem"
                : "Encontre as 3 partes da arma";
        }
        if (enemiesDefeated < totalEnemies) {
            return "Neutralize os hostis: " + enemiesDefeated + "/" + totalEnemies;
        }
        if (oxygen <= 25f) {
            return "Recupere O2 acima de 25%";
        }
        return "Portal online: atravesse para Marte";
    }

    // =====================================================
    // QUEST
    //
    // O mesmo progresso do texto de objetivo, exposto como passo numerado
    // para o HUD mostrar onde o jogador esta na campanha.
    // =====================================================

    /** Total de marcos da campanha, do primeiro reparo ao desembarque. */
    public static final int QUEST_TOTAL_STEPS = 7;

    /**
     * Passo atual, de 1 a {@value #QUEST_TOTAL_STEPS}.
     *
     * Os dois ultimos passos pertencem a Marte e so avancam quando a tela
     * marciana informa o progresso por {@link #setMarsProgress}.
     */
    public int getQuestStep(float oxygen) {
        if (getRepairCount() < 3) return hasAnyPart() ? 2 : 1;
        if (!weaponCrafted) return 3;
        if (enemiesDefeated < totalEnemies) return 4;
        if (!isPortalUnlocked(oxygen)) return 5;
        if (!marsMissionComplete) return marsVisited ? 6 : 5;
        return QUEST_TOTAL_STEPS;
    }

    /** Titulo curto do passo, para o cabecalho do HUD. */
    public String getQuestTitle(float oxygen) {
        return switch (getQuestStep(oxygen)) {
            case 1 -> "Encontrar as peças de reparo";
            case 2 -> "Reativar três sistemas da colônia";
            case 3 -> "Fabricar o rifle de pulso";
            case 4 -> "Neutralizar os hostis lunares";
            case 5 -> "Atravessar o portal para Marte";
            case 6 -> "Restaurar a colônia marciana";
            default -> "Alcançar a plataforma de extração";
        };
    }

    private boolean hasAnyPart() {
        for (PartType type : PartType.values()) if (getPartCount(type) > 0) return true;
        return getRepairCount() > 0;
    }

    /** A fase marciana reporta seu proprio avanco para a quest continuar. */
    public void setMarsProgress(boolean visited, boolean complete) {
        marsVisited = visited || marsVisited;
        marsMissionComplete = complete;
    }

    public boolean isMarsMissionComplete() { return marsMissionComplete; }

    // =====================================================
    // BENEFICIOS PASSIVOS
    //
    // Reparar deixou de ser apenas um item de checklist para o portal:
    // cada sistema online devolve uma vantagem jogavel distinta, entao a
    // ordem dos reparos passa a ser uma decisao do jogador.
    // =====================================================

    /** Comunicacao online: objetivos e coletaveis distantes ficam marcados. */
    public boolean isMapRevealed() {
        return isRepaired(SystemType.COMUNICACAO);
    }

    /** Energia online: a base recarrega bem mais rapido. */
    public float getRechargeMultiplier() {
        return isRepaired(SystemType.ENERGIA) ? GameConfig.PERK_ENERGY_RECHARGE : 1f;
    }

    /** Extracao online: cada rocha de gelo rende o dobro. */
    public float getIceYieldMultiplier() {
        return isRepaired(SystemType.EXTRACAO) ? GameConfig.PERK_EXTRACTION_YIELD : 1f;
    }

    /** Estufa online: oxigenio volta sozinho, devagar, mesmo em campo aberto. */
    public float getPassiveOxygenPerSecond() {
        return isRepaired(SystemType.ESTUFA) ? GameConfig.PERK_GREENHOUSE_OXYGEN : 0f;
    }

    /** Texto curto do ganho, usado no feedback imediato do reparo. */
    public static String getPerkLabel(SystemType type) {
        return switch (type) {
            case COMUNICACAO -> "Objetivos agora aparecem marcados no visor.";
            case ENERGIA -> "A base recarrega o traje em dobro.";
            case EXTRACAO -> "Cada rocha de gelo passa a render o dobro.";
            case ESTUFA -> "O traje volta a gerar oxigênio sozinho.";
        };
    }

    public boolean isRepaired(SystemType type) {
        return repairedSystems.contains(type);
    }

    public int getRepairCount() {
        return repairedSystems.size;
    }

    public boolean hasWeapon() {
        return weaponCrafted;
    }

    public int getEnemiesDefeated() {
        return enemiesDefeated;
    }

    public int getTotalEnemies() {
        return totalEnemies;
    }

    public void setTotalEnemies(int totalEnemies) {
        this.totalEnemies = Math.max(0, totalEnemies);
    }

    public void restore(
        int antenna, int energy, int extraction, int greenhouse,
        int weaponA, int weaponB, int weaponC,
        boolean communicationFixed, boolean energyFixed,
        boolean extractionFixed, boolean greenhouseFixed,
        boolean weaponCrafted, int enemiesDefeated
    ) {
        parts.clear();
        repairedSystems.clear();
        parts.put(PartType.ANTENA, Math.max(0, antenna));
        parts.put(PartType.ENERGIA, Math.max(0, energy));
        parts.put(PartType.EXTRACAO, Math.max(0, extraction));
        parts.put(PartType.ESTUFA, Math.max(0, greenhouse));
        parts.put(PartType.ARMA_A, Math.max(0, weaponA));
        parts.put(PartType.ARMA_B, Math.max(0, weaponB));
        parts.put(PartType.ARMA_C, Math.max(0, weaponC));
        if (communicationFixed) repairedSystems.add(SystemType.COMUNICACAO);
        if (energyFixed) repairedSystems.add(SystemType.ENERGIA);
        if (extractionFixed) repairedSystems.add(SystemType.EXTRACAO);
        if (greenhouseFixed) repairedSystems.add(SystemType.ESTUFA);
        this.weaponCrafted = weaponCrafted;
        this.enemiesDefeated = Math.max(0, enemiesDefeated);
    }
}
