package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.MissionSprite;

/** Portal em duas camadas: estrutura física e energia com abertura temporal. */
public class Portal extends Entidade {
    private static final float OPEN_DURATION = 1.35f;
    private final Sprite frameSprite;
    private final Sprite energySprite;
    private float time;
    private float openTime;
    private boolean unlocked;

    public Portal(float x, float y, AssetManager assets) {
        super(x, y, 150f, 170f);
        frameSprite = new Sprite(assets.missionRegion(MissionSprite.PORTAL_OFFLINE));
        energySprite = new Sprite(assets.missionRegion(MissionSprite.PORTAL_ONLINE));
        configure(frameSprite, x, y);
        configure(energySprite, x, y);
        energySprite.setColor(1f, 1f, 1f, 0f);
    }

    private void configure(Sprite sprite, float x, float y) {
        sprite.setSize(176f, 208f);
        sprite.setPosition(x - 13f, y - 15f);
        sprite.setOriginCenter();
    }

    @Override
    public void update(float delta) {
        time += delta;
        if (unlocked) openTime = Math.min(OPEN_DURATION, openTime + delta);
        float frameBreath = 1f + MathUtils.sin(time * 1.6f) * .006f;
        frameSprite.setScale(frameBreath);
        if (unlocked && openTime >= OPEN_DURATION) {
            energySprite.setScale(1f + MathUtils.sin(time * 3.4f) * .018f);
            energySprite.setColor(.88f, .98f, 1f, .98f);
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        frameSprite.draw(batch);
        if (!unlocked) return;

        float progress = MathUtils.clamp(openTime / OPEN_DURATION, 0f, 1f);
        float reveal = Interpolation.exp5Out.apply(progress);
        float pulse = MathUtils.sin(progress * MathUtils.PI * 5f) * (1f - progress);

        energySprite.setScale(.28f + reveal * .72f + pulse * .045f);
        energySprite.setColor(.72f, .96f, 1f, MathUtils.clamp(progress * 1.35f, 0f, 1f));
        energySprite.draw(batch);

        if (progress < 1f) {
            for (int ring = 1; ring <= 2; ring++) {
                float delayed = MathUtils.clamp(progress - ring * .14f, 0f, 1f);
                if (delayed <= 0f) continue;
                energySprite.setScale(.55f + delayed * (.7f + ring * .12f));
                energySprite.setColor(.35f, .9f, 1f, (1f - delayed) * .2f);
                energySprite.draw(batch);
            }
        }
        energySprite.setColor(.88f, .98f, 1f, 1f);
    }

    public void setUnlocked(boolean value) {
        if (value && !unlocked) openTime = 0f;
        unlocked = value;
        if (!value) openTime = 0f;
    }

    public boolean isPlayerNear(Astronauta astronauta) {
        return bounds.overlaps(astronauta.getBounds());
    }

    @Override
    public void dispose() { }
}
