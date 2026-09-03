package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.managers.AssetManager;

/** Portal de Titã com arte realmente distinta para bloqueado e liberado. */
public final class TitanPortal extends Entidade {
    private final TextureRegion blocked;
    private final TextureRegion online;
    private float time;
    private boolean unlocked;

    public TitanPortal(float x, float y, AssetManager assets) {
        super(x, y, 220f, 158f);
        blocked = assets.titanPortalState(false);
        online = assets.titanPortalState(true);
        bounds.set(x + 28f, y + 20f, width - 56f, height - 40f);
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
        batch.draw(unlocked ? online : blocked,
            position.x + (width - drawW) / 2f, position.y + (height - drawH) / 2f,
            drawW, drawH);
    }
    @Override public void dispose() { }
}
