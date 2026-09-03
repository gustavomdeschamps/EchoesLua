package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.orion.echoes.lua.managers.AssetManager;
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

    private final Kind kind;
    private final Sprite sprite;
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
        sprite = new Sprite(kind == Kind.ROCK
            ? assets.marsObstacleRegion(Math.abs(((int)x * 31 + (int)y * 17)) % 6)
            : assets.marsRegion(kind.column, kind.row));
        sprite.setSize(width, height);
        sprite.setPosition(x, y);
        sprite.setOriginCenter();
        if (kind == Kind.ROCK && physics != null) {
            float hitWidth = width * .68f;
            float hitHeight = height * .27f;
            bounds.set(x + (width - hitWidth) / 2f, y + height * .08f, hitWidth, hitHeight);
            body = physics.createStaticBody(bounds.x + bounds.width / 2f,
                bounds.y + bounds.height / 2f, bounds.width, bounds.height, "MARS_ROCK");
        } else if ((isStation() || kind == Kind.HABITAT) && physics != null) {
            // O volume visual alto não bloqueia: só a sapata apoiada no solo.
            float hitWidth = width * (kind == Kind.HABITAT ? .72f : .58f);
            float hitHeight = height * .22f;
            body = physics.createStaticBody(x + width / 2f, y + hitHeight / 2f + height * .05f,
                hitWidth, hitHeight, "MARS_STRUCTURE");
            bounds.set(x - 24f, y - 18f, width + 48f, height * .72f + 36f);
        } else if (isCollectible()) {
            body = null;
            bounds.set(x + width * .18f, y + height * .16f, width * .64f, height * .64f);
        } else {
            body = null;
        }
    }

    @Override
    public void update(float delta) {
        if (!ativo) return;
        elapsed += delta;
        activation = Math.max(0f, activation - delta / .55f);
        if (isCollectible()) {
            sprite.setPosition(position.x, baseY + MathUtils.sin(elapsed * 2.5f + position.x) * 7f);
            sprite.setRotation(MathUtils.sin(elapsed * 1.5f + position.y) * 3f);
        } else if (isStation() || kind == Kind.BEACON) {
            float pulse = enabled ? .014f : .005f;
            float kick = activation > 0f ? Interpolation.swingOut.apply(activation) * .09f : 0f;
            sprite.setScale(1f + MathUtils.sin(elapsed * 2f) * pulse + kick);
        }
    }

    @Override public void render(SpriteBatch batch) { if (ativo) sprite.draw(batch); }
    public Kind getKind() { return kind; }
    public boolean isCollectible() { return kind == Kind.MINERAL || kind == Kind.MEDKIT || kind == Kind.POWER_CELL; }
    public boolean isStation() { return kind == Kind.SOLAR_STATION || kind == Kind.OXYGEN_STATION || kind == Kind.COMMS_STATION; }
    public boolean isBlocking() { return kind == Kind.ROCK || isStation() || kind == Kind.HABITAT; }
    public void collect() { ativo = false; }
    public void activate() { if (!enabled) { enabled = true; activation = 1f; sprite.setColor(1f, .82f, .58f, 1f); } }
    public boolean isEnabled() { return enabled; }
    public Body getBody() { return body; }
    @Override public void dispose() { }
}
