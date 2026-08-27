package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.MissionSprite;
import com.orion.echoes.lua.systems.MissionState;

public class MissionCollectible extends Entidade {
    private final MissionState.PartType type;
    private final Sprite sprite;
    private final float baseY;
    private float time;

    public MissionCollectible(float x, float y, MissionState.PartType type, AssetManager assets) {
        super(x, y, 54f, 54f);
        this.type = type;
        this.baseY = y;
        sprite = new Sprite(assets.missionRegion(spriteFor(type)));
        sprite.setSize(62f, 62f);
        sprite.setOriginCenter();
    }

    private MissionSprite spriteFor(MissionState.PartType type) {
        return switch (type) {
            case ANTENA -> MissionSprite.PART_ANTENNA;
            case ENERGIA -> MissionSprite.PART_ENERGY;
            case EXTRACAO -> MissionSprite.PART_EXTRACTION;
            case ESTUFA -> MissionSprite.PART_GREENHOUSE;
            case ARMA_A -> MissionSprite.WEAPON_A;
            case ARMA_B -> MissionSprite.WEAPON_B;
            case ARMA_C -> MissionSprite.WEAPON_C;
        };
    }

    @Override
    public void update(float delta) {
        if (!ativo) return;
        time += delta;
        float y = baseY + MathUtils.sin(time * 2.5f + position.x * .01f) * 9f;
        sprite.setPosition(position.x, y);
        sprite.setRotation(MathUtils.sin(time * 1.7f) * 5f);
    }

    @Override
    public void render(SpriteBatch batch) {
        if (ativo) sprite.draw(batch);
    }

    public boolean collectIfOverlapping(Astronauta astronauta, MissionState mission) {
        if (!ativo || !bounds.overlaps(astronauta.getBounds())) return false;
        ativo = false;
        mission.collect(type);
        return true;
    }

    public MissionState.PartType getType() {
        return type;
    }

    @Override
    public void dispose() { }
}
