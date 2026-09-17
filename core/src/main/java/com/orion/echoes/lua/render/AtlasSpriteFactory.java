package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Cria sprites sem perder o canvas e o deslocamento das regioes recortadas do atlas. */
public final class AtlasSpriteFactory {
    private AtlasSpriteFactory() { }

    public static Sprite create(TextureRegion region) {
        if (region instanceof TextureAtlas.AtlasRegion atlasRegion) {
            return new TextureAtlas.AtlasSprite(atlasRegion);
        }
        return new Sprite(region);
    }
}
