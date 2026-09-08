package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.systems.MissionState;
import com.orion.echoes.lua.physics.PhysicsWorld;

/**
 * Estação de reparo lunar.
 *
 * A folha tem contrato fixo por coluna: 0 offline, 1 ativando/boot, 2 e 3
 * alternam em operação. A animação é uma máquina de estados simples — nunca
 * troca de coluna por acaso, e o piscar rápido de meio segundo que existia
 * antes (um flicker de 0.16s a cada 2.4s) saiu por parecer bug em vez de
 * indicador de sistema offline.
 */
public class RepairStation extends Entidade {

    private enum VisualState { OFFLINE_IDLE, ACTIVATING, ONLINE_LOOP }

    /** Duração da transição offline → online, visível na coluna 1 da folha. */
    private static final float ACTIVATION_DURATION = .8f;
    /** Cadência do loop A/B em operação; rápido o bastante para ler como vivo. */
    private static final float ONLINE_FRAME_INTERVAL = .5f;

    private final MissionState.SystemType type;
    private final TextureRegion[] frames = new TextureRegion[4];
    private boolean online;
    private VisualState state = VisualState.OFFLINE_IDLE;
    private float stateTime;
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
        stateTime += delta;
        if (state == VisualState.ACTIVATING && stateTime >= ACTIVATION_DURATION) {
            state = VisualState.ONLINE_LOOP;
            stateTime = 0f;
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        int frame = switch (state) {
            case OFFLINE_IDLE -> 0;
            case ACTIVATING -> 1;
            case ONLINE_LOOP -> 2 + (int) (stateTime / ONLINE_FRAME_INTERVAL) % 2;
        };
        // Pulso mínimo de energia, só em operação: a animação principal vem
        // dos quadros, não de escalar o sprite inteiro.
        float pulse = state == VisualState.ONLINE_LOOP
            ? 1f + MathUtils.sin(stateTime * 3.6f) * .012f : 1f;
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
            state = VisualState.ACTIVATING;
            stateTime = 0f;
        }
        return repaired;
    }

    /** Restaura o estado visual a partir de um save/portal, sem reproduzir a ativação. */
    public void sync(MissionState mission) {
        boolean repaired = mission.isRepaired(type);
        if (repaired == online) return;
        online = repaired;
        state = online ? VisualState.ONLINE_LOOP : VisualState.OFFLINE_IDLE;
        stateTime = 0f;
    }

    public MissionState.SystemType getType() {
        return type;
    }

    public Body getBody() { return body; }

    @Override
    public void dispose() { }
}
