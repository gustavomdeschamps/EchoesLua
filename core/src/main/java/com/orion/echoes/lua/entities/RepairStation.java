package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.systems.MissionState;
import com.orion.echoes.lua.physics.PhysicsWorld;

public class RepairStation extends Entidade {
    private final MissionState.SystemType type;
    private final TextureRegion[] frames = new TextureRegion[4];
    private boolean online;
    private float elapsed;
    private float activation;
    private final Body body;

    public RepairStation(float x, float y, MissionState.SystemType type, AssetManager assets,
                         PhysicsWorld physics) {
        super(x, y, 154f, 154f);
        this.type = type;
        int row = switch (type) {
            case COMUNICACAO -> 0;
            case ENERGIA -> 1;
            case EXTRACAO -> 2;
            case ESTUFA -> 3;
        };
        for (int column = 0; column < frames.length; column++) {
            frames[column] = assets.repairStationFrame(row, column);
        }
        // Área de interação confortável, colisão apenas no pedestal visível.
        bounds.set(x - 22f, y - 14f, 198f, 124f);
        body = physics.createStaticBody(x + 77f, y + 24f, 92f, 34f, "REPAIR_STATION");
    }

    @Override
    public void update(float delta) {
        elapsed += delta;
        activation = Math.max(0f, activation - delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        int frame = !online ? (elapsed % 2.4f < .16f ? 1 : 0)
            : (activation > 0f ? 1 : 2 + (int)(elapsed / .24f) % 2);
        float pulse = online ? 1f + MathUtils.sin(elapsed * 3.6f) * .012f : 1f;
        float size = 190f * pulse;
        batch.draw(frames[frame], position.x - 18f + (190f - size) / 2f,
            position.y - 18f + (190f - size) / 2f, size, size);
    }

    public boolean isPlayerNear(Astronauta astronauta) {
        return bounds.overlaps(astronauta.getBounds());
    }

    public boolean repair(MissionState mission) {
        boolean repaired = mission.repair(type);
        if (repaired) {
            online = true;
            activation = 1f;
        }
        return repaired;
    }

    public void sync(MissionState mission) {
        if (mission.isRepaired(type)) {
            online = true;
        } else {
            online = false;
        }
    }

    public MissionState.SystemType getType() {
        return type;
    }

    public Body getBody() { return body; }

    @Override
    public void dispose() { }
}
