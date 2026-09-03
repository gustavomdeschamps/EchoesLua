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

    public int versao = CURRENT_VERSION;

    /**
     * Versao 3: o save deixou de ser so da fase lunar.
     *
     * Os campos abaixo guardam a campanha inteira - em que fase o jogador
     * parou, a semente que reproduz o layout da Lua, a municao e o progresso
     * marciano. Saves da versao 2 continuam carregando: o Json apenas deixa
     * estes campos nos valores padrao.
     */
    public static final int CURRENT_VERSION = 4;

    public String fase = "LUA";
    public long semente;
    public int municao;
    public int totalHostisLunares;
    public boolean marteVisitado;
    public boolean marteConcluido;
    public int marteNucleos;
    public int marteEstacoes;
    public int marteHostis;
    public boolean dialogoTita;
    public boolean combateOk;
    public boolean amostraOk;
    public boolean entrouTita;

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

        this.versao = CURRENT_VERSION;
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
