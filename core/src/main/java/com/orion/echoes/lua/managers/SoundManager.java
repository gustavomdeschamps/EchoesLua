package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

public class SoundManager implements Disposable {

    private static SoundManager instance;

    private Sound coleta;
    private Sound coletaOxigenio;
    private Sound coletaComida;
    private Sound coletaGelo;

    private Sound processarGelo;
    private Sound semGelo;

    private Sound baseRecarregando;
    private Sound alertaOxigenio;

    private Sound menuIniciar;

    private Sound pause;
    private Sound unpause;

    private Sound gameOver;
    private Sound vitoria;

    private Sound passoLunar;
    private Sound colisaoRocha;

    private Sound hoverUi;

    private float volumeGeral = 0.75f;

    private boolean carregado = false;

    private SoundManager() {
    }

    public static SoundManager getInstance() {

        if (instance == null) {
            instance = new SoundManager();
        }

        return instance;
    }

    public void load() {

        if (carregado) {
            return;
        }

        coleta =
            carregar("sounds/coleta.wav");

        coletaOxigenio =
            carregar("sounds/coleta_oxigenio.wav");

        coletaComida =
            carregar("sounds/coleta_comida.wav");

        coletaGelo =
            carregar("sounds/coleta_gelo.wav");

        processarGelo =
            carregar("sounds/processar_gelo.wav");

        semGelo =
            carregar("sounds/sem_gelo.wav");

        baseRecarregando =
            carregar("sounds/base_recarregando.wav");

        alertaOxigenio =
            carregar("sounds/alerta_oxigenio.wav");

        menuIniciar =
            carregar("sounds/menu_iniciar.wav");

        pause =
            carregar("sounds/pause.wav");

        unpause =
            carregar("sounds/unpause.wav");

        gameOver =
            carregar("sounds/game_over.wav");

        vitoria =
            carregar("sounds/vitoria.wav");

        passoLunar =
            carregar("sounds/passo_lunar.wav");

        colisaoRocha =
            carregar("sounds/colisao_rocha.wav");

        hoverUi =
            carregar("sounds/hover_ui.wav");

        carregado = true;
    }

    private Sound carregar(String caminho) {

        if (!Gdx.files.internal(caminho).exists()) {

            System.out.println(
                "Som nao encontrado: "
                    + caminho
            );

            return null;
        }

        return Gdx.audio.newSound(
            Gdx.files.internal(caminho)
        );
    }

    private void tocar(
        Sound sound,
        float volume
    ) {

        if (sound == null) {
            return;
        }

        sound.play(
            Math.min(
                1f,
                volume * volumeGeral
            )
        );
    }

    // =========================================
    // COLETAS
    // =========================================

    public void tocarColeta() {

        tocar(
            coleta,
            0.65f
        );
    }

    public void tocarOxigenio() {

        tocar(
            coletaOxigenio,
            0.85f
        );
    }

    public void tocarComida() {

        tocar(
            coletaComida,
            0.75f
        );
    }

    public void tocarGelo() {

        tocar(
            coletaGelo,
            0.85f
        );
    }

    // =========================================
    // BASE
    // =========================================

    public void tocarProcessarGelo() {

        tocar(
            processarGelo,
            0.9f
        );
    }

    public void tocarSemGelo() {

        tocar(
            semGelo,
            0.75f
        );
    }

    public void tocarBaseRecarregando() {

        tocar(
            baseRecarregando,
            0.55f
        );
    }

    // =========================================
    // OXIGÊNIO
    // =========================================

    public void tocarAlertaOxigenio() {

        tocar(
            alertaOxigenio,
            0.8f
        );
    }

    // =========================================
    // TELAS
    // =========================================

    public void tocarInicio() {

        tocar(
            menuIniciar,
            0.8f
        );
    }

    public void tocarPause() {

        tocar(
            pause,
            0.65f
        );
    }

    public void tocarUnpause() {

        tocar(
            unpause,
            0.65f
        );
    }

    public void tocarGameOver() {

        tocar(
            gameOver,
            0.9f
        );
    }

    public void tocarVitoria() {

        tocar(
            vitoria,
            0.9f
        );
    }

    // =========================================
    // MUNDO
    // =========================================

    public void tocarPassoLunar() {

        tocar(
            passoLunar,
            0.22f
        );
    }

    public void tocarColisaoRocha() {

        tocar(
            colisaoRocha,
            0.35f
        );
    }

    // =========================================
    // UI
    // =========================================

    public void tocarHoverUi() {

        tocar(
            hoverUi,
            0.35f
        );
    }

    // =========================================
    // VOLUME
    // =========================================

    public float getVolumeGeral() {

        return volumeGeral;
    }

    public void setVolumeGeral(
        float volumeGeral
    ) {

        this.volumeGeral =
            Math.max(
                0f,
                Math.min(
                    1f,
                    volumeGeral
                )
            );
    }

    @Override
    public void dispose() {

        disposeSom(coleta);
        disposeSom(coletaOxigenio);
        disposeSom(coletaComida);
        disposeSom(coletaGelo);

        disposeSom(processarGelo);
        disposeSom(semGelo);

        disposeSom(baseRecarregando);
        disposeSom(alertaOxigenio);

        disposeSom(menuIniciar);

        disposeSom(pause);
        disposeSom(unpause);

        disposeSom(gameOver);
        disposeSom(vitoria);

        disposeSom(passoLunar);
        disposeSom(colisaoRocha);

        disposeSom(hoverUi);

        carregado = false;
    }

    private void disposeSom(
        Sound sound
    ) {

        if (sound != null) {
            sound.dispose();
        }
    }
}
