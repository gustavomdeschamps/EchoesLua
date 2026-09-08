package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.managers.AssetManager;

/** Portal de Titã com arte realmente distinta para bloqueado e liberado. */
public final class TitanPortal extends Entidade {
    private final TextureRegion[][] frames = new TextureRegion[2][4];
    private float time;
    private boolean unlocked;

    public TitanPortal(float x, float y, AssetManager assets) {
        super(x, y, 180f, 250f);
        for (int row = 0; row < 2; row++) for (int column = 0; column < 4; column++) {
            frames[row][column] = assets.titanPortalFrame(row == 1, column);
        }
        // Interior da porta, não a base inteira.
        bounds.set(x + 42f, y + 30f, width - 84f, height - 54f);
    }

    @Override public void update(float delta) { time += delta; }
    public void setUnlocked(boolean value) { unlocked = value; }
    public boolean isPlayerNear(Astronauta player) {
        return bounds.overlaps(player.getBounds());
    }
    @Override public void render(SpriteBatch batch) {
        float pulse = unlocked ? 1f + MathUtils.sin(time * 3.5f) * .015f : 1f;
        float drawW = width * pulse;
        float drawH = height * pulse;
        TextureRegion frame = frames[unlocked ? 1 : 0][(int)(time / (unlocked ? .10f : .22f)) % 4];
        batch.draw(frame,
            position.x + (width - drawW) / 2f, position.y + (height - drawH) / 2f,
            drawW, drawH);
    }
    @Override public void dispose() { }
}
