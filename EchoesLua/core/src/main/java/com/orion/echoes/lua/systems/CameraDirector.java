package com.orion.echoes.lua.systems;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.orion.echoes.lua.config.GameConfig;

/** Follow crítico, lookahead e zoom contextual sem acumular offset de shake. */
public final class CameraDirector {
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final JuiceSystem juice;
    private final float worldWidth;
    private final float worldHeight;
    private final Vector2 basePosition = new Vector2();

    public CameraDirector(OrthographicCamera camera, Viewport viewport,
                          JuiceSystem juice, float worldWidth, float worldHeight) {
        this.camera = camera;
        this.viewport = viewport;
        this.juice = juice;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        basePosition.set(camera.position.x, camera.position.y);
    }

    public void update(Vector2 target, Vector2 velocity, boolean combat, float delta) {
        float wantedZoom = combat ? GameConfig.CAMERA_COMBAT_ZOOM
            : GameConfig.CAMERA_EXPLORATION_ZOOM;
        float zoomResponse = 1f - (float) Math.exp(-GameConfig.CAMERA_ZOOM_RESPONSE * delta);
        camera.zoom = MathUtils.lerp(camera.zoom, wantedZoom - juice.getZoomPunch(), zoomResponse);

        float targetX = target.x + velocity.x * GameConfig.PPM * GameConfig.CAMERA_LOOKAHEAD_X;
        float targetY = target.y + velocity.y * GameConfig.PPM * GameConfig.CAMERA_LOOKAHEAD_Y;
        float halfWidth = viewport.getWorldWidth() * camera.zoom / 2f;
        float halfHeight = viewport.getWorldHeight() * camera.zoom / 2f;
        targetX = MathUtils.clamp(targetX, halfWidth, worldWidth - halfWidth);
        targetY = MathUtils.clamp(targetY, halfHeight, worldHeight - halfHeight);
        float response = 1f - (float) Math.exp(-GameConfig.CAMERA_RESPONSE * delta);
        basePosition.x = MathUtils.lerp(basePosition.x, targetX, response);
        basePosition.y = MathUtils.lerp(basePosition.y, targetY, response);
        camera.position.set(basePosition.x + juice.getCameraOffset().x,
            basePosition.y + juice.getCameraOffset().y, 0f);
        camera.update();
    }
}
