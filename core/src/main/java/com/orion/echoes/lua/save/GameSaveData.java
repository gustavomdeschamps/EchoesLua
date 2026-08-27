package com.orion.echoes.lua.save;

/**
 * Dados salvos da Fase Lunar.
 *
 * Essa classe é convertida para JSON pelo SaveManager.
 */
public class GameSaveData {

    // =====================================================
    // POSIÇÃO DO ASTRONAUTA
    // =====================================================

    public float posX;
    public float posY;

    // =====================================================
    // STATUS
    // =====================================================

    public float oxigenio;
    public float energia;
    public float tempoVivo;

    // =====================================================
    // INVENTÁRIO LUNAR
    // =====================================================

    public int gelo;
    public int agua;
    public int combustivel;

    // =====================================================
    // CONTADORES DE COLETA
    // =====================================================

    public int oxigeniosColetados;
    public int comidasColetadas;
    public int rochasGeloColetadas;

    // =====================================================
    // BASE
    // =====================================================

    public boolean baseDescoberta;

    public int pecaAntena;
    public int pecaEnergia;
    public int pecaExtracao;
    public int pecaEstufa;
    public int armaParteA;
    public int armaParteB;
    public int armaParteC;
    public boolean comunicacaoReparada;
    public boolean energiaReparada;
    public boolean extracaoReparada;
    public boolean estufaReparada;
    public boolean armaCraftada;
    public int inimigosEliminados;

    // =====================================================
    // VERSÃO DO SAVE
    // =====================================================

    public int versao = 2;

    /*
     * Construtor vazio obrigatório
     * para o JSON da LibGDX.
     */
    public GameSaveData() {

    }

    public GameSaveData(
        float posX,
        float posY,
        float oxigenio,
        float energia,
        float tempoVivo
    ) {

        this.posX =
            posX;

        this.posY =
            posY;

        this.oxigenio =
            oxigenio;

        this.energia =
            energia;

        this.tempoVivo =
            tempoVivo;

        this.gelo = 0;
        this.agua = 0;
        this.combustivel = 0;

        this.oxigeniosColetados = 0;
        this.comidasColetadas = 0;
        this.rochasGeloColetadas = 0;

        this.baseDescoberta = false;

        this.versao = 1;
    }

    @Override
    public String toString() {

        return "GameSaveData{" +

            "posX=" + posX +

            ", posY=" + posY +

            ", oxigenio=" + oxigenio +

            ", energia=" + energia +

            ", tempoVivo=" + tempoVivo +

            ", gelo=" + gelo +

            ", agua=" + agua +

            ", combustivel=" + combustivel +

            ", versao=" + versao +

            '}';
    }
}
