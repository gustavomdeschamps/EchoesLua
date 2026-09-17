package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Renderiza o solo sem esticar um quadrado sobre o mapa inteiro. */
public final class TerrainRenderer {
    private static final float TILE_WORLD_SIZE = 860f;

    private TerrainRenderer() { }

    public static void draw(SpriteBatch batch, Texture texture, float worldWidth, float worldHeight) {
        batch.draw(texture, 0f, 0f, worldWidth, worldHeight, 0f, 0f,
            worldWidth / TILE_WORLD_SIZE, worldHeight / TILE_WORLD_SIZE);
    }
}
