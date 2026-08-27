package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.MissionSprite;

public class CraftingStation extends Entidade {
    private final Sprite sprite;

    public CraftingStation(float x, float y, AssetManager assets) {
        super(x, y, 118f, 88f);
        sprite = new Sprite(assets.missionRegion(MissionSprite.CRAFTING_TERMINAL));
        sprite.setSize(width, height);
        sprite.setPosition(x, y);
    }

    public boolean isPlayerNear(Astronauta astronauta) {
        return bounds.overlaps(astronauta.getBounds());
    }

    @Override public void update(float delta) { }
    @Override public void render(SpriteBatch batch) { sprite.draw(batch); }
    @Override public void dispose() { }
}
