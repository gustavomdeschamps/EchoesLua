package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Body;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.render.SpriteFit;
import com.orion.echoes.lua.physics.PhysicsWorld;

/** Prop marciano com atlas próprio e estados simples de coleta/ativação. */
public final class MarsObject extends Entidade {
    public enum Kind {
        HABITAT(0, 0), SOLAR_STATION(1, 0), OXYGEN_STATION(2, 0), COMMS_STATION(3, 0),
        MINERAL(0, 1), MEDKIT(1, 1), POWER_CELL(2, 1), ROCK(3, 1),
        DRONE(0, 2), CRAWLER(1, 2), BEACON(2, 2), LANDING_PAD(3, 2);
        final int column, row;
        Kind(int column, int row) { this.column = column; this.row = row; }
    }

    /** Contrato igual ao de RepairStation: 0 offline, 1 ativando, 2/3 em operação. */
    private enum VisualState { OFFLINE_IDLE, ACTIVATING, ONLINE_LOOP }

    private static final float ACTIVATION_DURATION = .8f;
    private static final float ONLINE_FRAME_INTERVAL = .5f;

    private final Kind kind;
    /** Retangulo realmente desenhado; hitbox e flutuacao seguem ele. */
    private final Rectangle drawRect = new Rectangle();
    private final Rectangle collisionBounds = new Rectangle();
    private final Sprite sprite;
    /**
     * Quadros dedicados de estação (docs/NEW_VISUAL_ASSETS.md), ou {@code null}
     * enquanto mars_station_sheet_v2.png não tiver sido fornecido — nesse caso
     * a estação usa a região estática antiga com o pulso procedural existente.
     */
    private final TextureRegion[] stationFrames;
    private VisualState state = VisualState.OFFLINE_IDLE;
    private float stateTime;
    private final float baseY;
    private final Body body;
    private float elapsed;
    private float activation;
    private boolean enabled;

    public MarsObject(float x, float y, float width, float height, Kind kind,
                      AssetManager assets, PhysicsWorld physics) {
        super(x, y, width, height);
        this.kind = kind;
        this.baseY = y;
        this.stationFrames = isStation() ? loadStationFrames(assets, kind) : null;
        sprite = new Sprite(kind == Kind.ROCK
            ? assets.marsObstacleRegion(Math.abs(((int)x * 31 + (int)y * 17)) % 6)
            : stationFrames != null ? stationFrames[0]
            : assets.marsRegion(kind.column, kind.row));
        /*
         * As celulas do atlas sao quadradas. Esticar a arte para preencher um
         * retangulo de outra proporcao achatava a plataforma de pouso em 32% e
         * o habitat em 20% - era isso que fazia os props parecerem deformados.
         * Aqui a arte e encaixada sem deformar, e o retangulo resultante vira
         * a referencia tambem da hitbox.
         */
        SpriteFit.fit(sprite, x, y, width, height, drawRect);
        sprite.setSize(drawRect.width, drawRect.height);
        sprite.setPosition(drawRect.x, drawRect.y);
        sprite.setOriginCenter();
        if (kind == Kind.ROCK && physics != null) {
            float hitWidth = drawRect.width * .68f;
            float hitHeight = drawRect.height * .27f;
            bounds.set(drawRect.x + (drawRect.width - hitWidth) / 2f,
                drawRect.y + drawRect.height * .08f, hitWidth, hitHeight);
            collisionBounds.set(bounds);
            body = physics.createStaticBody(bounds.x + bounds.width / 2f,
                bounds.y + bounds.height / 2f, bounds.width, bounds.height, "MARS_ROCK");
        } else if ((isStation() || kind == Kind.HABITAT) && physics != null) {
            // O volume visual alto não bloqueia: só a sapata apoiada no solo.
            float hitWidth = drawRect.width * (kind == Kind.HABITAT ? .72f : .58f);
            float hitHeight = drawRect.height * .22f;
            collisionBounds.set(drawRect.x + (drawRect.width - hitWidth) / 2f,
                drawRect.y + drawRect.height * .05f, hitWidth, hitHeight);
            body = physics.createStaticBody(collisionBounds.x + collisionBounds.width / 2f,
                collisionBounds.y + collisionBounds.height / 2f,
                hitWidth, hitHeight, "MARS_STRUCTURE");
            // Area de interacao: o volume desenhado com uma folga de alcance.
            bounds.set(drawRect.x - 24f, drawRect.y - 18f,
                drawRect.width + 48f, drawRect.height * .72f + 36f);
        } else if (isCollectible()) {
            body = null;
            bounds.set(drawRect.x + drawRect.width * .18f, drawRect.y + drawRect.height * .16f,
                drawRect.width * .64f, drawRect.height * .64f);
        } else {
            body = null;
        }
    }

    private static TextureRegion[] loadStationFrames(AssetManager assets, Kind kind) {
        if (!assets.hasMarsStationSheet()) return null;
        TextureRegion[] frames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) frames[i] = assets.marsStationFrame(kind, i);
        return frames;
    }

    @Override
    public void update(float delta) {
        if (!ativo) return;
        elapsed += delta;
        activation = Math.max(0f, activation - delta / .55f);
        if (isCollectible()) {
            sprite.setPosition(position.x, baseY + MathUtils.sin(elapsed * 2.5f + position.x) * 7f);
            sprite.setRotation(MathUtils.sin(elapsed * 1.5f + position.y) * 3f);
        } else if (stationFrames != null) {
            updateStationAnimation(delta);
        } else if (isStation() || kind == Kind.BEACON) {
            // Sem a folha dedicada ainda: mantém o pulso procedural como
            // aproximação, em vez de deixar o prop completamente estático.
            float pulse = enabled ? .014f : .005f;
            float kick = activation > 0f ? Interpolation.swingOut.apply(activation) * .09f : 0f;
            sprite.setScale(1f + MathUtils.sin(elapsed * 2f) * pulse + kick);
        }
    }

    private void updateStationAnimation(float delta) {
        stateTime += delta;
        if (state == VisualState.ACTIVATING && stateTime >= ACTIVATION_DURATION) {
            state = VisualState.ONLINE_LOOP;
            stateTime = 0f;
        }
        int frame = switch (state) {
            case OFFLINE_IDLE -> 0;
            case ACTIVATING -> 1;
            case ONLINE_LOOP -> 2 + (int) (stateTime / ONLINE_FRAME_INTERVAL) % 2;
        };
        sprite.setRegion(stationFrames[frame]);
    }

    @Override public void render(SpriteBatch batch) { if (ativo) sprite.draw(batch); }
    public Kind getKind() { return kind; }
    public boolean isCollectible() { return kind == Kind.MINERAL || kind == Kind.MEDKIT || kind == Kind.POWER_CELL; }
    public boolean isStation() { return kind == Kind.SOLAR_STATION || kind == Kind.OXYGEN_STATION || kind == Kind.COMMS_STATION; }
    public boolean isBlocking() { return kind == Kind.ROCK || isStation() || kind == Kind.HABITAT; }
    public Rectangle getCollisionBounds() { return collisionBounds; }
    public void collect() { ativo = false; }

    public void activate() {
        if (enabled) return;
        enabled = true;
        activation = 1f;
        if (stationFrames != null) {
            state = VisualState.ACTIVATING;
            stateTime = 0f;
        } else {
            // Fallback sem folha dedicada: o tingimento sinaliza "online" no lugar de um quadro real.
            sprite.setColor(1f, .82f, .58f, 1f);
        }
    }

    /**
     * Restaura o estado "já online" ao reabrir Marte pelo portal ou por save.
     *
     * Diferente de {@link #activate()}, não reproduz a animação de ativação —
     * a estação já estava ligada quando o jogador saiu, então reaparece
     * direto no loop de operação.
     */
    public void restoreOnline() {
        enabled = true;
        if (stationFrames != null) {
            state = VisualState.ONLINE_LOOP;
            stateTime = 0f;
            sprite.setRegion(stationFrames[2]);
        } else {
            sprite.setColor(1f, .82f, .58f, 1f);
        }
    }

    public boolean isEnabled() { return enabled; }
    public Body getBody() { return body; }
    @Override public void dispose() { }
}
