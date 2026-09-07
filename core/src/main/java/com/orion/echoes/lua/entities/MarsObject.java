package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

    private final Kind kind;
    /** Retangulo realmente desenhado; hitbox e flutuacao seguem ele. */
    private final Rectangle drawRect = new Rectangle();
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
            body = physics.createStaticBody(bounds.x + bounds.width / 2f,
                bounds.y + bounds.height / 2f, bounds.width, bounds.height, "MARS_ROCK");
        } else if ((isStation() || kind == Kind.HABITAT) && physics != null) {
            // O volume visual alto não bloqueia: só a sapata apoiada no solo.
            float hitWidth = drawRect.width * (kind == Kind.HABITAT ? .72f : .58f);
            float hitHeight = drawRect.height * .22f;
            body = physics.createStaticBody(drawRect.x + drawRect.width / 2f,
                drawRect.y + hitHeight / 2f + drawRect.height * .05f,
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
