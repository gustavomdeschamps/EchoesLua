package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Body;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.MissionSprite;
import com.orion.echoes.lua.physics.PhysicsWorld;

public class CraftingStation extends Entidade {
    private final Sprite sprite;
    private final Body body;

    public CraftingStation(float x, float y, AssetManager assets, PhysicsWorld physics) {
        super(x, y, 118f, 88f);
        sprite = new Sprite(assets.missionRegion(MissionSprite.CRAFTING_TERMINAL));
        sprite.setSize(width, height);
        sprite.setPosition(x, y);
        bounds.set(x - 20f, y - 16f, width + 40f, height + 34f);
        body = physics.createStaticBody(x + width / 2f, y + 17f,
            width * .74f, 30f, "CRAFTING_STATION");
    }

    public boolean isPlayerNear(Astronauta astronauta) {
        return bounds.overlaps(astronauta.getBounds());
    }

    @Override public void update(float delta) { }
    @Override public void render(SpriteBatch batch) { sprite.draw(batch); }
    @Override public void dispose() { }
    public Body getBody() { return body; }
}
