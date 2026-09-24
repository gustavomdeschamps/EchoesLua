package com.orion.echoes.lua.systems;

import com.orion.echoes.lua.config.GameConfig;

/**
 * Estado da campanha inteira, carregado entre as fases.
 *
 * Existe porque o portal passou a ser bidirecional: voltar de Marte precisa
 * reconstruir a Lua exatamente como o jogador a deixou, e nao comecar outra
 * partida. A semente reproduz o layout, o resto reproduz o progresso.
 *
 * E tambem o que o save grava: salvar a campanha e gravar este objeto.
 */
public final class CampaignState {

    /** Em qual fase o jogador esta agora. */
    public enum Phase { LUNAR, MARS, TITAN, CALLISTO, AHARIN }
    private final Inventario inventario = new Inventario();
    public Inventario getInventario() { return inventario; }
    private int formaBossCalisto = 1;
    private float hpBossCalisto = 100f;
    private boolean bossLuaDefeated, bossMarteDefeated, bossTitanDefeated, bossCalistoDefeated;
    public int getFormaBossCalisto() { return formaBossCalisto; }
    public float getHpBossCalisto() { return hpBossCalisto; }
    public void setBossCalisto(int forma, float hp) {
        formaBossCalisto = Math.max(1, Math.min(3, forma));
        hpBossCalisto = Math.max(0f, hp);
    }
    public boolean isBossDefeated(Phase bossPhase) {
        return switch (bossPhase) {
            case LUNAR -> bossLuaDefeated;
            case MARS -> bossMarteDefeated;
            case TITAN -> bossTitanDefeated;
            case CALLISTO -> bossCalistoDefeated;
            default -> false;
        };
    }
    public void setBossDefeated(Phase bossPhase, boolean value) {
        switch (bossPhase) {
            case LUNAR -> bossLuaDefeated = value;
            case MARS -> bossMarteDefeated = value;
            case TITAN -> bossTitanDefeated = value;
            case CALLISTO -> bossCalistoDefeated = value;
            default -> { }
        }
    }

    private final long seed;
    private Phase phase = Phase.LUNAR;

    // Vitais e equipamento acompanham o jogador nas duas direcoes.
    private float oxygen = GameConfig.MAX_OXYGEN;
    private float energy = GameConfig.MAX_ENERGY;
    private int ammo;
    private float missionTime;

    // Progresso lunar.
    private int antenna, energyPart, extraction, greenhouse;
    private int weaponA, weaponB, weaponC;
    private boolean communicationFixed, energyFixed, extractionFixed, greenhouseFixed;
    private boolean weaponCrafted;
    private int lunarEnemiesDefeated;
    private int lunarTotalEnemies;
    private int ice, water, fuel;

    // Progresso marciano.
    private int minerals;
    private int marsStationsOnline;
    private int marsHostilesDefeated;
    private boolean marsVisited;
    private boolean marsMissionComplete;

    // Prova M07: autorização do portal para Titã.
    private boolean dialogoTita;
    private boolean combateOk;
    private boolean amostraOk;
    private boolean entrouTita;
    private boolean dialogoLua;
    private boolean dialogoExplorador;
    public boolean isDialogoLua() { return dialogoLua; }
    public void setDialogoLua(boolean value) { dialogoLua = value; }
    public boolean isDialogoExplorador() { return dialogoExplorador; }
    public void setDialogoExplorador(boolean value) { dialogoExplorador = value; }

    /*
     * Aberturas por mundo (WorldIntroScreen).
     *
     * O portal e bidirecional: sem essas flags, voltar de Titã para Marte e
     * depois avançar de novo reapresentaria a cinematic inteira. Cada flag
     * marca "este mundo já foi apresentado nesta campanha" e nunca volta a
     * false. Saves antigos (sem estes campos) recebem um default seguro em
     * LunarCheckpoint, derivado do progresso já registrado.
     */
    private boolean lunarIntroShown;
    private boolean marsIntroShown;
    private boolean titanIntroShown;
    public boolean isLunarIntroShown() { return lunarIntroShown; }
    public void setLunarIntroShown(boolean value) { lunarIntroShown = value; }
    public boolean isMarsIntroShown() { return marsIntroShown; }
    public void setMarsIntroShown(boolean value) { marsIntroShown = value; }
    public boolean isTitanIntroShown() { return titanIntroShown; }
    public void setTitanIntroShown(boolean value) { titanIntroShown = value; }

    public CampaignState() {
        this(System.nanoTime());
    }

    public CampaignState(long seed) {
        this.seed = seed;
    }

    // =====================================================
    // LEITURA E ESCRITA DIRETA
    // =====================================================

    public long getSeed() { return seed; }

    public Phase getPhase() { return phase; }
    public void setPhase(Phase value) { phase = value == null ? Phase.LUNAR : value; }

    public float getOxygen() { return oxygen; }
    public float getEnergy() { return energy; }

    public void setVitals(float oxygenValue, float energyValue) {
        oxygen = clamp(oxygenValue, 0f, GameConfig.MAX_OXYGEN);
        energy = clamp(energyValue, 0f, GameConfig.MAX_ENERGY);
    }

    public int getAmmo() { return ammo; }
    public void setAmmo(int value) { ammo = Math.max(0, Math.min(GameConfig.AMMO_MAX, value)); }

    public float getMissionTime() { return missionTime; }
    public void setMissionTime(float value) { missionTime = Math.max(0f, value); }

    public int getIce() { return ice; }
    public int getWater() { return water; }
    public int getFuel() { return fuel; }

    public void setResources(int iceValue, int waterValue, int fuelValue) {
        ice = Math.max(0, iceValue);
        water = Math.max(0, waterValue);
        fuel = Math.max(0, fuelValue);
    }

    public int getMinerals() { return minerals; }
    public int getMarsStationsOnline() { return marsStationsOnline; }
    public int getMarsHostilesDefeated() { return marsHostilesDefeated; }
    public boolean hasVisitedMars() { return marsVisited; }

    public boolean isMarsMissionComplete() { return marsMissionComplete; }

    public void setMarsProgress(int mineralsValue, int stationsOnline,
                                int hostilesDefeated, boolean complete) {
        minerals = Math.max(0, mineralsValue);
        marsStationsOnline = Math.max(0, stationsOnline);
        marsHostilesDefeated = Math.max(0, hostilesDefeated);
        marsMissionComplete = complete;
        marsVisited = true;
    }

    public void markMarsVisited() { marsVisited = true; }

    public boolean isDialogoTita() { return dialogoTita; }
    public void setDialogoTita(boolean value) { dialogoTita = value; }
    public boolean isCombateOk() { return combateOk; }
    public void setCombateOk(boolean value) { combateOk = value; }
    public boolean isAmostraOk() { return amostraOk; }
    public void setAmostraOk(boolean value) { amostraOk = value; }
    public boolean isEntrouTita() { return entrouTita; }
    public void setEntrouTita(boolean value) { entrouTita = value; }

    public boolean portalLiberado() {
        return dialogoTita && (combateOk || amostraOk);
    }

    public String missaoAtual() {
        if (phase == Phase.CALLISTO) return inventario.tem(Inventario.CHAVE_LUZ)
            ? "Leve a Chave de Luz ao portal de Aharin."
            : bossCalistoDefeated ? "Recolha a Chave de Luz caída na arena." : "Derrote as três formas do Sentinela de Calisto.";
        if (phase == Phase.AHARIN) return "Converse com as entidades de Luz.";
        if (phase == Phase.TITAN && inventario.tem(Inventario.CHAVE_TITA))
            return "Atravesse o portal de Calisto junto à cratera do Soberano.";
        if (phase == Phase.TITAN && bossTitanDefeated)
            return "Recolha a Chave de Titã caída junto ao Soberano.";
        if (phase == Phase.TITAN) return dialogoExplorador
            ? "Use a estação e enfrente o Soberano a nordeste."
            : "Fale com a pesquisadora junto à estação.";
        /*
         * A Lua tem objetivo proprio.
         *
         * Sem este ramo o texto caia direto nas frases de Marte, e o HUD lunar
         * mandava "investigue o portal instavel no setor de extracao" - uma
         * tarefa de outra fase - logo depois de o jogador ganhar a chave da Lua.
         */
        if (phase == Phase.LUNAR) {
            if (inventario.tem(Inventario.CHAVE_LUA))
                return "Chave da Lua conquistada. Atravesse o portal para Marte.";
            if (bossLuaDefeated) return "Recolha a Chave da Lua caída na arena.";
            return luaMissoesOk()
                ? "Cratera liberada. Enfrente o Guardião da Cratera no portal."
                : "Repare os sistemas, monte o rifle e limpe a cratera.";
        }
        if (!dialogoTita) return "Investigue o portal instável no setor de extração.";
        if (!combateOk && !amostraOk) return "Prove capacidade de combate ou colete uma amostra de metano.";
        return inventario.tem(Inventario.CHAVE_MARTE) ? "Portal autorizado. Atravesse para Titã."
            : bossMarteDefeated ? "Recolha a Chave de Marte caída na arena."
            : "Reative as estações, neutralize os hostis e enfrente o Titã-Ferrugem no portal.";
    }

    public String statusPortal() {
        return portalLiberado() ? "PORTAL TITA ONLINE" : "PORTAL TITA BLOQUEADO";
    }

    /** Valores exigidos pelo save, mantendo a enum interna legível. */
    public String phaseToken() {
        return switch (phase) {
            case LUNAR -> "LUA";
            case MARS -> "MARTE";
            case TITAN -> "TITA";
            case CALLISTO -> "CALISTO";
            case AHARIN -> "AHARIN";
        };
    }

    public static Phase phaseFromToken(String token) {
        if ("CALISTO".equalsIgnoreCase(token)) return Phase.CALLISTO;
        if ("AHARIN".equalsIgnoreCase(token)) return Phase.AHARIN;
        if ("TITA".equalsIgnoreCase(token) || "TITAN".equalsIgnoreCase(token)) return Phase.TITAN;
        if ("MARTE".equalsIgnoreCase(token) || "MARS".equalsIgnoreCase(token)) return Phase.MARS;
        return Phase.LUNAR;
    }

    public int getLunarTotalEnemies() { return lunarTotalEnemies; }
    public boolean hasWeapon() { return weaponCrafted; }
    public boolean luaMissoesOk() {
        int repairs=(communicationFixed?1:0)+(energyFixed?1:0)+(extractionFixed?1:0)+(greenhouseFixed?1:0);
        return repairs>=3 && weaponCrafted && lunarTotalEnemies>0 && lunarEnemiesDefeated>=lunarTotalEnemies;
    }
    public boolean marteMissoesOk() {
        return marsStationsOnline>=3 && marsMissionComplete && portalLiberado();
    }

    // =====================================================
    // PONTE COM A MISSAO LUNAR
    // =====================================================

    /** Fotografa o estado da missao lunar antes de atravessar o portal. */
    public void captureMission(MissionState mission) {
        antenna = mission.getPartCount(MissionState.PartType.ANTENA);
        energyPart = mission.getPartCount(MissionState.PartType.ENERGIA);
        extraction = mission.getPartCount(MissionState.PartType.EXTRACAO);
        greenhouse = mission.getPartCount(MissionState.PartType.ESTUFA);
        weaponA = mission.getPartCount(MissionState.PartType.ARMA_A);
        weaponB = mission.getPartCount(MissionState.PartType.ARMA_B);
        weaponC = mission.getPartCount(MissionState.PartType.ARMA_C);
        communicationFixed = mission.isRepaired(MissionState.SystemType.COMUNICACAO);
        energyFixed = mission.isRepaired(MissionState.SystemType.ENERGIA);
        extractionFixed = mission.isRepaired(MissionState.SystemType.EXTRACAO);
        greenhouseFixed = mission.isRepaired(MissionState.SystemType.ESTUFA);
        weaponCrafted = mission.hasWeapon();
        lunarEnemiesDefeated = mission.getEnemiesDefeated();
        lunarTotalEnemies = mission.getTotalEnemies();
    }

    /** Devolve o progresso a uma missao recem-construida. */
    public void restoreMission(MissionState mission) {
        mission.restore(antenna, energyPart, extraction, greenhouse,
            weaponA, weaponB, weaponC,
            communicationFixed, energyFixed, extractionFixed, greenhouseFixed,
            weaponCrafted, lunarEnemiesDefeated);
    }

    /** True quando ja houve uma passagem pela Lua a ser restaurada. */
    public boolean hasLunarProgress() {
        return weaponCrafted || lunarEnemiesDefeated > 0 || communicationFixed
            || energyFixed || extractionFixed || greenhouseFixed
            || antenna + energyPart + extraction + greenhouse
               + weaponA + weaponB + weaponC > 0;
    }

    // =====================================================
    // SERIALIZACAO
    // =====================================================

    /** Campos crus, na ordem em que o save grava e le. */
    public int[] toLunarArray() {
        return new int[] {
            antenna, energyPart, extraction, greenhouse,
            weaponA, weaponB, weaponC,
            communicationFixed ? 1 : 0, energyFixed ? 1 : 0,
            extractionFixed ? 1 : 0, greenhouseFixed ? 1 : 0,
            weaponCrafted ? 1 : 0, lunarEnemiesDefeated, lunarTotalEnemies
        };
    }

    public void fromLunarArray(int[] values) {
        if (values == null || values.length < 14) return;
        antenna = values[0];
        energyPart = values[1];
        extraction = values[2];
        greenhouse = values[3];
        weaponA = values[4];
        weaponB = values[5];
        weaponC = values[6];
        communicationFixed = values[7] != 0;
        energyFixed = values[8] != 0;
        extractionFixed = values[9] != 0;
        greenhouseFixed = values[10] != 0;
        weaponCrafted = values[11] != 0;
        lunarEnemiesDefeated = values[12];
        lunarTotalEnemies = values[13];
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
