package com.orion.echoes.lua.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;

/** Preferências persistentes de acessibilidade, vídeo, controles e mixagem. */
public final class AppSettings {
    private static final String PREFS_NAME = "echoes-lua-settings-v2";
    private final Preferences preferences;

    private float musicVolume;
    private float sfxVolume;
    private float uiVolume;
    private float ambientVolume;
    private float hudScale;
    private boolean shakeEnabled;
    private boolean colorblindEnabled;
    private boolean reduceMotion;
    private boolean fullscreen;
    private boolean arrowMovement;

    public AppSettings() {
        preferences = Gdx.app.getPreferences(PREFS_NAME);
        musicVolume = preferences.getFloat("musicVolume", 0.7f);
        sfxVolume = preferences.getFloat("sfxVolume", 0.8f);
        uiVolume = preferences.getFloat("uiVolume", 0.75f);
        ambientVolume = preferences.getFloat("ambientVolume", 0.8f);
        hudScale = preferences.getFloat("hudScale", 1f);
        shakeEnabled = preferences.getBoolean("shakeEnabled", true);
        colorblindEnabled = preferences.getBoolean("colorblindEnabled", false);
        reduceMotion = preferences.getBoolean("reduceMotion", false);
        // O launcher abre a campanha ocupando o monitor; o padrao guardado
        // precisa descrever esse estado, senao a primeira leitura da opcao
        // jogaria todo mundo para janela sem ninguem ter pedido.
        fullscreen = preferences.getBoolean("fullscreen", true);
        arrowMovement = preferences.getBoolean("arrowMovement", false);
    }

    public void save() {
        preferences.putFloat("musicVolume", musicVolume);
        preferences.putFloat("sfxVolume", sfxVolume);
        preferences.putFloat("uiVolume", uiVolume);
        preferences.putFloat("ambientVolume", ambientVolume);
        preferences.putFloat("hudScale", hudScale);
        preferences.putBoolean("shakeEnabled", shakeEnabled);
        preferences.putBoolean("colorblindEnabled", colorblindEnabled);
        preferences.putBoolean("reduceMotion", reduceMotion);
        preferences.putBoolean("fullscreen", fullscreen);
        preferences.putBoolean("arrowMovement", arrowMovement);
        preferences.flush();
    }

    public float getMusicVolume() { return musicVolume; }
    public void setMusicVolume(float value) { musicVolume = MathUtils.clamp(value, 0f, 1f); save(); }
    public float getSfxVolume() { return sfxVolume; }
    public void setSfxVolume(float value) { sfxVolume = MathUtils.clamp(value, 0f, 1f); save(); }
    public float getUiVolume() { return uiVolume; }
    public void setUiVolume(float value) { uiVolume = MathUtils.clamp(value, 0f, 1f); save(); }
    public float getAmbientVolume() { return ambientVolume; }
    public void setAmbientVolume(float value) { ambientVolume = MathUtils.clamp(value, 0f, 1f); save(); }
    public float getHudScale() { return hudScale; }
    public void setHudScale(float value) { hudScale = MathUtils.clamp(value, 0.85f, 1.2f); save(); }
    public boolean isShakeEnabled() { return shakeEnabled; }
    public void setShakeEnabled(boolean value) { shakeEnabled = value; save(); }
    public boolean isColorblindEnabled() { return colorblindEnabled; }
    public void setColorblindEnabled(boolean value) { colorblindEnabled = value; save(); }
    public boolean isReduceMotion() { return reduceMotion; }
    public void setReduceMotion(boolean value) { reduceMotion = value; save(); }
    public boolean isFullscreen() { return fullscreen; }
    public void setFullscreen(boolean value) { fullscreen = value; save(); }
    public boolean isArrowMovement() { return arrowMovement; }
    public void setArrowMovement(boolean value) { arrowMovement = value; save(); }
}
