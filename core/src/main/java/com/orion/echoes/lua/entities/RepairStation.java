package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.MissionSprite;
import com.orion.echoes.lua.systems.MissionState;
import com.orion.echoes.lua.physics.PhysicsWorld;

public class RepairStation extends Entidade {
    private final MissionState.SystemType type;
    private final Sprite sprite;
    private boolean online;
    private float elapsed;
    private float activation;
    private final Body body;

    public RepairStation(float x, float y, MissionState.SystemType type, AssetManager assets,
                         PhysicsWorld physics) {
        super(x, y, 154f, 154f);
        this.type = type;
        sprite = new Sprite(assets.missionRegion(spriteFor(type)));
        sprite.setSize(170f, 170f);
        sprite.setOriginCenter();
        sprite.setPosition(x - 8f, y - 8f);
        sprite.setColor(.78f, .82f, .88f, 1f);
        // Área de interação confortável, colisão apenas no pedestal visível.
        bounds.set(x - 22f, y - 14f, 198f, 124f);
        body = physics.createStaticBody(x + 77f, y + 24f, 92f, 34f, "REPAIR_STATION");
    }

    private MissionSprite spriteFor(MissionState.SystemType type) {
        return switch (type) {
            case COMUNICACAO -> MissionSprite.STATION_COMMUNICATION;
            case ENERGIA -> MissionSprite.STATION_ENERGY;
            case EXTRACAO -> MissionSprite.STATION_EXTRACTION;
            case ESTUFA -> MissionSprite.STATION_GREENHOUSE;
        };
    }

    @Override
    public void update(float delta) {
        elapsed += delta;
        activation = Math.max(0f, activation - delta / .42f);
        float idle = 1f + MathUtils.sin(elapsed * (online ? 2.2f : 1.25f)) * (online ? .012f : .006f);
        float kick = activation > 0f ? Interpolation.swingOut.apply(activation) * .09f : 0f;
        sprite.setScale(idle + kick);
    }

    @Override
    public void render(SpriteBatch batch) {
        sprite.draw(batch);
    }

    public boolean isPlayerNear(Astronauta astronauta) {
        return bounds.overlaps(astronauta.getBounds());
    }

    public boolean repair(MissionState mission) {
        boolean repaired = mission.repair(type);
        if (repaired) {
            online = true;
            activation = 1f;
            sprite.setColor(.78f, 1f, .88f, 1f);
        }
        return repaired;
    }

    public void sync(MissionState mission) {
        if (mission.isRepaired(type)) {
            online = true;
            sprite.setColor(.78f, 1f, .88f, 1f);
        } else {
            online = false;
            sprite.setColor(.78f, .82f, .88f, 1f);
        }
    }

    public MissionState.SystemType getType() {
        return type;
    }

    public Body getBody() { return body; }

    @Override
    public void dispose() { }
}
