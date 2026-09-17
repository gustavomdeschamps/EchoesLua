package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite;

/** Draws a trimmed region inside the stable canvas recorded by TexturePacker. */
public final class AtlasRegionRenderer {
    private AtlasRegionRenderer() { }

    public static void draw(SpriteBatch batch, TextureRegion region,
                            float canvasX, float canvasY,
                            float canvasWidth, float canvasHeight) {
        if (!(region instanceof TextureAtlas.AtlasRegion atlas)) {
            batch.draw(region, canvasX, canvasY, canvasWidth, canvasHeight);
            return;
        }
        float scaleX = canvasWidth / Math.max(1f, atlas.originalWidth);
        float scaleY = canvasHeight / Math.max(1f, atlas.originalHeight);
        batch.draw(atlas,
            canvasX + atlas.offsetX * scaleX,
            canvasY + atlas.offsetY * scaleY,
            atlas.packedWidth * scaleX,
            atlas.packedHeight * scaleY);
    }

    public static void draw(SpriteBatch batch, TextureRegion region,
                            float canvasX, float canvasY,
                            float canvasWidth, float canvasHeight,
                            float scale, float rotation) {
        if (!(region instanceof TextureAtlas.AtlasRegion atlas)) {
            batch.draw(region, canvasX, canvasY, canvasWidth / 2f, canvasHeight / 2f,
                canvasWidth, canvasHeight, scale, scale, rotation);
            return;
        }
        float scaleX = canvasWidth / Math.max(1f, atlas.originalWidth);
        float scaleY = canvasHeight / Math.max(1f, atlas.originalHeight);
        float drawX = canvasX + atlas.offsetX * scaleX;
        float drawY = canvasY + atlas.offsetY * scaleY;
        batch.draw(atlas, drawX, drawY,
            canvasWidth / 2f - atlas.offsetX * scaleX,
            canvasHeight / 2f - atlas.offsetY * scaleY,
            atlas.packedWidth * scaleX, atlas.packedHeight * scaleY,
            scale, scale, rotation);
    }

    public static float originalWidth(TextureRegion region) {
        if (region instanceof AtlasSprite sprite) return sprite.getAtlasRegion().originalWidth;
        return region instanceof TextureAtlas.AtlasRegion atlas
            ? atlas.originalWidth : region.getRegionWidth();
    }

    public static float originalHeight(TextureRegion region) {
        if (region instanceof AtlasSprite sprite) return sprite.getAtlasRegion().originalHeight;
        return region instanceof TextureAtlas.AtlasRegion atlas
            ? atlas.originalHeight : region.getRegionHeight();
    }
}
