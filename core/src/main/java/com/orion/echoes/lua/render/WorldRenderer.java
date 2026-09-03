package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Enemy;
import com.orion.echoes.lua.entities.EnemyPulse;
import com.orion.echoes.lua.entities.Item;
import com.orion.echoes.lua.entities.MissionCollectible;
import com.orion.echoes.lua.entities.Obstacle;
import com.orion.echoes.lua.entities.RepairStation;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.ParticleManager;
import com.orion.echoes.lua.world.LunarWorld;

/**
 * Desenha o mundo lunar em um unico par begin/end.
 *
 * Manter tudo num batch so e o que preserva o ganho do atlas: qualquer
 * renderer alternado no meio forcaria um flush de GPU por troca.
 */
public final class WorldRenderer {

    private final SpriteBatch batch;
    private final AssetManager assets;
    private final OrthographicCamera camera;

    public WorldRenderer(SpriteBatch batch, AssetManager assets, OrthographicCamera camera) {
        this.batch = batch;
        this.assets = assets;
        this.camera = camera;
    }

    public void render(LunarWorld world, ParticleManager particles) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawGround();
        drawLandmarks();
        world.getBase().render(batch);
        for (Item item : world.getItems()) item.render(batch);
        for (MissionCollectible collectible : world.getCollectibles()) collectible.render(batch);
        for (RepairStation station : world.getRepairStations()) station.render(batch);
        world.getCraftingStation().render(batch);
        world.getPortal().render(batch);
        for (Enemy enemy : world.getEnemies()) enemy.render(batch);
        for (EnemyPulse pulse : world.getEnemyPulses()) pulse.render(batch);
        for (Obstacle obstacle : world.getObstacles()) obstacle.render(batch);
        world.getPlayer().render(batch);
        particles.render(batch);
        batch.end();
    }

    private void drawGround() {
        batch.draw(assets.backgroundLuaTexture, 0, 0,
            GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT, 0f, 0f,
            GameConfig.WORLD_WIDTH / assets.backgroundLuaTexture.getWidth(),
            GameConfig.WORLD_HEIGHT / assets.backgroundLuaTexture.getHeight());
    }

    private void drawLandmarks() {
        batch.setColor(.78f, .82f, .88f, .86f);
        batch.draw(assets.landmarkRegion(0, 0), 1540f, 1440f, 250f, 185f);
        batch.draw(assets.landmarkRegion(1, 0), 2440f, 1080f, 205f, 165f);
        batch.draw(assets.landmarkRegion(2, 0), 330f, 1510f, 230f, 150f);
        batch.draw(assets.landmarkRegion(3, 0), 2100f, 250f, 180f, 205f);
        batch.setColor(1f, 1f, 1f, 1f);
    }
}
