package com.orion.echoes.lua.systems;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.orion.echoes.lua.config.GameConfig;

/** Relógio único para impactos; nunca altera diretamente as regras de gameplay. */
public final class JuiceSystem {
    public enum Preset { SHOT_HIT, ENEMY_KILL, PLAYER_HURT, DASH, COLLECT, REPAIR, CRAFT }

    private final Vector2 cameraOffset = new Vector2();
    private float hitStopTimer;
    private float slowMotionTimer;
    private float slowMotionScale = 1f;
    private float trauma;
    private float zoomPunch;
    private float flashTimer;
    private float flashDuration;
    private boolean shakeEnabled = true;

    public void trigger(Preset preset) {
        switch (preset) {
            case SHOT_HIT -> apply(GameConfig.JUICE_HIT_HITSTOP,
                GameConfig.JUICE_HIT_TRAUMA, GameConfig.JUICE_HIT_ZOOM, 0f, 1f, 0f);
            case ENEMY_KILL -> apply(GameConfig.JUICE_KILL_HITSTOP,
                GameConfig.JUICE_KILL_TRAUMA, GameConfig.JUICE_KILL_ZOOM,
                GameConfig.JUICE_KILL_SLOW_TIME, GameConfig.JUICE_KILL_TIME_SCALE, 0f);
            case PLAYER_HURT -> apply(GameConfig.JUICE_HURT_HITSTOP,
                GameConfig.JUICE_HURT_TRAUMA, 0f, 0f, 1f,
                GameConfig.JUICE_HURT_FLASH_TIME);
            case DASH -> apply(0f, GameConfig.JUICE_DASH_TRAUMA,
                GameConfig.JUICE_DASH_ZOOM, 0f, 1f, 0f);
            case COLLECT -> apply(0f, GameConfig.JUICE_COLLECT_TRAUMA,
                GameConfig.JUICE_COLLECT_ZOOM, 0f, 1f, 0f);
            case REPAIR -> apply(0f, GameConfig.JUICE_REPAIR_TRAUMA,
                GameConfig.JUICE_REPAIR_ZOOM, 0f, 1f, 0f);
            case CRAFT -> apply(0f, GameConfig.JUICE_CRAFT_TRAUMA,
                GameConfig.JUICE_CRAFT_ZOOM, GameConfig.JUICE_CRAFT_SLOW_TIME,
                GameConfig.JUICE_CRAFT_TIME_SCALE, 0f);
        }
    }

    private void apply(float hitStop, float addedTrauma, float zoom,
                       float slowTime, float timeScale, float flash) {
        hitStopTimer = Math.max(hitStopTimer, hitStop);
        trauma = MathUtils.clamp(Math.max(trauma, addedTrauma), 0f, 1f);
        zoomPunch = Math.max(zoomPunch, zoom);
        if (slowTime > slowMotionTimer) {
            slowMotionTimer = slowTime;
            slowMotionScale = timeScale;
        }
        if (flash > flashTimer) {
            flashTimer = flash;
            flashDuration = flash;
        }
    }

    public void update(float realDelta) {
        hitStopTimer = Math.max(0f, hitStopTimer - realDelta);
        slowMotionTimer = Math.max(0f, slowMotionTimer - realDelta);
        flashTimer = Math.max(0f, flashTimer - realDelta);
        trauma = Math.max(0f, trauma - GameConfig.JUICE_TRAUMA_DECAY * realDelta);
        zoomPunch = Math.max(0f, zoomPunch - GameConfig.JUICE_ZOOM_RECOVERY * realDelta);
        if (!shakeEnabled || trauma <= 0f) {
            cameraOffset.setZero();
            return;
        }
        float amplitude = trauma * trauma * GameConfig.JUICE_MAX_SHAKE_PIXELS;
        cameraOffset.set(MathUtils.random(-amplitude, amplitude),
            MathUtils.random(-amplitude, amplitude));
    }

    public float gameplayDelta(float realDelta) {
        if (hitStopTimer > 0f) return 0f;
        return realDelta * (slowMotionTimer > 0f ? slowMotionScale : 1f);
    }

    public Vector2 getCameraOffset() { return cameraOffset; }
    public float getZoomPunch() { return zoomPunch; }
    public float getDamageFlashAlpha() {
        return flashDuration <= 0f ? 0f : flashTimer / flashDuration;
    }
    public void setShakeEnabled(boolean enabled) {
        shakeEnabled = enabled;
        if (!enabled) cameraOffset.setZero();
    }
    public boolean isShakeEnabled() { return shakeEnabled; }
}
