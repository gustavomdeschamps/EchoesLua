package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Projétil hostil visível, com núcleo e rastro; não é dano invisível. */
public final class TitanProjectile {
    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private final Rectangle bounds = new Rectangle();
    private final TextureRegion glow;
    private final float damage;
    private float life = 3.6f;
    private boolean active = true;

    public TitanProjectile(float x, float y, float dirX, float dirY, float speed,
                           float damage, TextureRegion glow) {
        position.set(x, y);
        velocity.set(dirX, dirY).nor().scl(speed);
        this.damage = damage;
        this.glow = glow;
        sync();
    }

    public void update(float delta) {
        if (!active) return;
        position.mulAdd(velocity, delta);
        life -= delta;
        if (life <= 0f) active = false;
        sync();
    }

    private void sync() { bounds.set(position.x - 9f, position.y - 9f, 18f, 18f); }
    public boolean hits(Astronauta player) {
        if (!active || !bounds.overlaps(player.getBounds())) return false;
        active = false;
        return true;
    }
    public void render(SpriteBatch batch) {
        if (!active) return;
        float angle = velocity.angleDeg();
        batch.setColor(.1f, .9f, 1f, .35f);
        batch.draw(glow, position.x - velocity.x * .07f - 18f,
            position.y - velocity.y * .07f - 9f, 36f, 18f);
        batch.setColor(1f, .72f, .22f, 1f);
        batch.draw(glow, position.x - 12f, position.y - 12f,
            12f, 12f, 24f, 24f, 1f, 1f, angle);
        batch.setColor(Color.WHITE);
    }
    public boolean isActive() { return active; }
    public float getDamage() { return damage; }
    public float x() { return position.x; }
    public float y() { return position.y; }
}
