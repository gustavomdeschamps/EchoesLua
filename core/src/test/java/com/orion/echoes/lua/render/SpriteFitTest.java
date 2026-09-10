package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpriteFitTest {
    private static TextureRegion region(int width, int height) {
        return new TextureRegion() {
            @Override public int getRegionWidth() { return width; }
            @Override public int getRegionHeight() { return height; }
        };
    }

    @Test void portraitSpriteIsCenteredWithoutBeingStretched() {
        Rectangle fitted = SpriteFit.fit(region(88, 128), 100f, 40f,
            64f, 64f, new Rectangle());

        assertEquals(44f, fitted.width, .001f);
        assertEquals(64f, fitted.height, .001f);
        assertEquals(110f, fitted.x, .001f);
        assertEquals(40f, fitted.y, .001f);
    }

    @Test void wideSpriteStaysGroundedInsideItsBox() {
        Rectangle fitted = SpriteFit.fit(region(128, 105), 20f, 30f,
            64f, 64f, new Rectangle());

        assertEquals(64f, fitted.width, .001f);
        assertEquals(52.5f, fitted.height, .001f);
        assertEquals(20f, fitted.x, .001f);
        assertEquals(30f, fitted.y, .001f);
    }

    @Test void invalidMetadataFallsBackToRequestedBox() {
        Rectangle fitted = SpriteFit.fit(region(0, 0), 3f, 4f,
            5f, 6f, new Rectangle());

        assertEquals(new Rectangle(3f, 4f, 5f, 6f), fitted);
    }
}
