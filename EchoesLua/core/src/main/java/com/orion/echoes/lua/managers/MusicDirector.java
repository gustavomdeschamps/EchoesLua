package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.orion.echoes.lua.config.GameConfig;

/**
 * Musica adaptativa em camadas.
 *
 * As tres camadas de um mundo sao loops de mesma duracao tocando ao mesmo
 * tempo; a intensidade so muda o volume de cada uma, nunca o transporte.
 * Assim tensao e urgencia entram e saem em fase, sem corte perceptivel.
 */
public final class MusicDirector implements Disposable {

    public enum Track { NONE, MENU, LUNAR, MARS }

    private final Music[] layers = new Music[3];
    private Music menu;
    private Track track = Track.NONE;

    private float tensionTarget;
    private float urgencyTarget;
    private float tensionLevel;
    private float urgencyLevel;

    private float duckAmount;
    private float duckTimer;
    private float duck;

    private float busVolume = 1f;
    private boolean enabled = true;

    public void load() {
        menu = open("sounds/menu_ambiente.ogg");
    }

    private Music open(String path) {
        if (!Gdx.files.internal(path).exists()) return null;
        Music music = Gdx.audio.newMusic(Gdx.files.internal(path));
        music.setLooping(true);
        music.setVolume(0f);
        return music;
    }

    /** Troca de mundo: descarrega as camadas antigas e abre as novas em silencio. */
    public void play(Track next) {
        if (track == next) return;
        stopLayers();
        if (menu != null) menu.stop();
        track = next;
        tensionLevel = urgencyLevel = tensionTarget = urgencyTarget = 0f;
        switch (next) {
            case MENU -> {
                if (menu != null && enabled) {
                    menu.setVolume(GameConfig.MUSIC_MENU_VOLUME * busVolume);
                    menu.play();
                }
            }
            case LUNAR -> openLayers("lunar");
            case MARS -> openLayers("mars");
            case NONE -> { }
        }
    }

    private void openLayers(String world) {
        layers[0] = open("music/" + world + "_base.ogg");
        layers[1] = open("music/" + world + "_tension.ogg");
        layers[2] = open("music/" + world + "_urgency.ogg");
        if (!enabled) return;
        for (Music layer : layers) if (layer != null) layer.play();
        applyLayerVolumes();
    }

    private void stopLayers() {
        for (int index = 0; index < layers.length; index++) {
            if (layers[index] == null) continue;
            layers[index].stop();
            layers[index].dispose();
            layers[index] = null;
        }
    }

    /** Intensidade desejada, 0..1 cada, tipicamente vinda de inimigos e oxigenio. */
    public void setIntensity(float tension, float urgency) {
        tensionTarget = MathUtils.clamp(tension, 0f, 1f);
        urgencyTarget = MathUtils.clamp(urgency, 0f, 1f);
    }

    /** Abaixa a musica por alguns instantes para um stinger ou alerta respirar. */
    public void duck(float amount, float seconds) {
        duckAmount = Math.max(duckAmount, MathUtils.clamp(amount, 0f, 1f));
        duckTimer = Math.max(duckTimer, seconds);
    }

    public void update(float delta) {
        float response = 1f - (float) Math.exp(-GameConfig.MUSIC_FADE_RESPONSE * delta);
        tensionLevel = MathUtils.lerp(tensionLevel, tensionTarget, response);
        urgencyLevel = MathUtils.lerp(urgencyLevel, urgencyTarget, response);

        duckTimer = Math.max(0f, duckTimer - delta);
        float wantedDuck = duckTimer > 0f ? duckAmount : 0f;
        if (wantedDuck <= 0f && duck <= 0.001f) duckAmount = 0f;
        float duckResponse = 1f - (float) Math.exp(
            -(wantedDuck > duck ? GameConfig.MUSIC_DUCK_ATTACK : GameConfig.MUSIC_DUCK_RELEASE) * delta);
        duck = MathUtils.lerp(duck, wantedDuck, duckResponse);

        applyLayerVolumes();
        if (menu != null && menu.isPlaying()) {
            menu.setVolume(GameConfig.MUSIC_MENU_VOLUME * busVolume * (1f - duck));
        }
    }

    private void applyLayerVolumes() {
        float gain = busVolume * (1f - duck) * (enabled ? 1f : 0f);
        setLayer(0, GameConfig.MUSIC_BASE_VOLUME * gain);
        setLayer(1, GameConfig.MUSIC_TENSION_VOLUME * tensionLevel * gain);
        setLayer(2, GameConfig.MUSIC_URGENCY_VOLUME * urgencyLevel * gain);
    }

    private void setLayer(int index, float volume) {
        if (layers[index] != null) layers[index].setVolume(MathUtils.clamp(volume, 0f, 1f));
    }

    public void setBusVolume(float volume) {
        busVolume = MathUtils.clamp(volume, 0f, 1f);
        applyLayerVolumes();
        if (menu != null) menu.setVolume(GameConfig.MUSIC_MENU_VOLUME * busVolume * (1f - duck));
    }

    public void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) {
            for (Music layer : layers) if (layer != null) layer.pause();
            if (menu != null) menu.pause();
            return;
        }
        for (Music layer : layers) if (layer != null) layer.play();
        if (track == Track.MENU && menu != null) menu.play();
        applyLayerVolumes();
    }

    public boolean isEnabled() { return enabled; }

    public Track getTrack() { return track; }

    @Override
    public void dispose() {
        stopLayers();
        if (menu != null) {
            menu.dispose();
            menu = null;
        }
        track = Track.NONE;
    }
}
