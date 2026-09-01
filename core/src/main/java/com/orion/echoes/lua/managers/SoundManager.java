package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.math.MathUtils;

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
    private Sound disparoPulso;
    private Music musicaMenu;
    private boolean musicaAtiva = true;

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
            carregar("sounds/coleta.ogg");

        coletaOxigenio =
            carregar("sounds/coleta_oxigenio.ogg");

        coletaComida =
            carregar("sounds/coleta_comida.ogg");

        coletaGelo =
            carregar("sounds/coleta_gelo.ogg");

        processarGelo =
            carregar("sounds/processar_gelo.ogg");

        semGelo =
            carregar("sounds/sem_gelo.ogg");

        baseRecarregando =
            carregar("sounds/base_recarregando.ogg");

        alertaOxigenio =
            carregar("sounds/alerta_oxigenio.ogg");

        menuIniciar =
            carregar("sounds/menu_iniciar.ogg");

        pause =
            carregar("sounds/pause.ogg");

        unpause =
            carregar("sounds/unpause.ogg");

        gameOver =
            carregar("sounds/game_over.ogg");

        vitoria =
            carregar("sounds/vitoria.ogg");

        passoLunar =
            carregar("sounds/passo_lunar.ogg");

        colisaoRocha =
            carregar("sounds/colisao_rocha.ogg");

        hoverUi =
            carregar("sounds/hover_ui.ogg");

        disparoPulso = carregar("sounds/disparo_pulso.ogg");
        if (Gdx.files.internal("sounds/menu_ambiente.ogg").exists()) {
            musicaMenu = Gdx.audio.newMusic(Gdx.files.internal("sounds/menu_ambiente.ogg"));
            musicaMenu.setLooping(true);
            musicaMenu.setVolume(.32f * volumeGeral);
        }

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

    private void tocarVariado(Sound sound, float volume, float pitchMin, float pitchMax) {
        if (sound == null) return;
        sound.play(Math.min(1f, volume * volumeGeral), MathUtils.random(pitchMin, pitchMax), 0f);
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
        tocarVariado(passoLunar, .22f, .92f, 1.07f);
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

    public void tocarDisparo() {
        tocarVariado(disparoPulso, .72f, .96f, 1.045f);
    }

    public void tocarAlertaInimigo() {
        tocarVariado(alertaOxigenio, .34f, 1.12f, 1.2f);
    }

    public void tocarMusicaMenu() {
        if (musicaAtiva && musicaMenu != null && !musicaMenu.isPlaying()) musicaMenu.play();
    }

    public void pararMusicaMenu() {
        if (musicaMenu != null) musicaMenu.stop();
    }

    public void alternarMusica() {
        musicaAtiva = !musicaAtiva;
        if (musicaAtiva) tocarMusicaMenu(); else pararMusicaMenu();
    }

    public boolean isMusicaAtiva() { return musicaAtiva; }

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
        if (musicaMenu != null) musicaMenu.setVolume(.32f * this.volumeGeral);
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
        disposeSom(disparoPulso);
        if (musicaMenu != null) {
            musicaMenu.dispose();
            musicaMenu = null;
        }

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
