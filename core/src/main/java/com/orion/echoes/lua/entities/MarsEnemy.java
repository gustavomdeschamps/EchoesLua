package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.orion.echoes.lua.managers.AssetManager;

/** Drone ou rover hostil de Marte, animado sem spritesheet. */
public final class MarsEnemy extends Entidade {
    private final Sprite sprite;
    private final Vector2 direction = new Vector2();
    private final Rectangle testBounds = new Rectangle();
    private final boolean drone;
    private final float worldWidth;
    private final float worldHeight;
    private float hp = 2f;
    private float elapsed;
    private float damageCooldown;
    private float hitTimer;
    private float deathTimer;

    public MarsEnemy(float x, float y, boolean drone, AssetManager assets,
                     float worldWidth, float worldHeight) {
        super(x, y, drone ? 70f : 82f, drone ? 58f : 64f);
        this.drone = drone;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        bounds.set(x + (drone ? 9f : 11f), y + 5f, drone ? 52f : 60f, drone ? 29f : 34f);
        sprite = new Sprite(assets.marsRegion(drone ? 0 : 1, 2));
        sprite.setSize(drone ? 102f : 112f, drone ? 82f : 88f);
        sprite.setOriginCenter();
    }

    public void update(float delta, Astronauta target, Array<MarsObject> rocks) {
        elapsed += delta;
        damageCooldown = Math.max(0f, damageCooldown - delta);
        if (!ativo) {
            deathTimer = Math.max(0f, deathTimer - delta);
            float alpha = deathTimer / .4f;
            sprite.setColor(1f, 1f, 1f, alpha);
            sprite.setScale(.72f + alpha * .28f);
            sprite.setRotation((1f - alpha) * 30f);
            placeSprite(-8f * (1f - alpha));
            return;
        }
        direction.set(target.getBounds().x + target.getBounds().width / 2f - centerX(),
            target.getBounds().y + target.getBounds().height / 2f - centerY()).nor();
        float step = (drone ? 92f : 72f) * delta;
        float nextX = MathUtils.clamp(position.x + direction.x * step, 0f, worldWidth - width);
        if (free(nextX, position.y, rocks)) position.x = nextX;
        float nextY = MathUtils.clamp(position.y + direction.y * step, 0f, worldHeight - height);
        if (free(position.x, nextY, rocks)) position.y = nextY;
        bounds.setPosition(position.x + (drone ? 9f : 11f), position.y + 5f);

        float gait = MathUtils.sin(elapsed * (drone ? 7f : 10f));
        sprite.setScale(1f + gait * .018f, 1f - gait * .012f);
        sprite.setRotation(direction.x * -4f + gait * 1.5f);
        placeSprite(drone ? 10f + Math.abs(gait) * 4f : -7f + Math.abs(gait) * 2f);
        if (hitTimer > 0f) {
            hitTimer = Math.max(0f, hitTimer - delta);
            sprite.setColor(1f, .42f, .28f, 1f);
        } else sprite.setColor(1f, 1f, 1f, 1f);
    }

    private void placeSprite(float bob) {
        sprite.setPosition(centerX() - sprite.getWidth() / 2f, position.y - 10f + bob);
    }

    private boolean free(float x, float y, Array<MarsObject> rocks) {
        testBounds.set(x + width * .14f, y + height * .08f, width * .72f, height * .5f);
        for (MarsObject rock : rocks) if (testBounds.overlaps(rock.getBounds())) return false;
        return true;
    }

    public boolean canDamage(Astronauta player) {
        if (!ativo || damageCooldown > 0f || !bounds.overlaps(player.getBounds())) return false;
        damageCooldown = 1.15f;
        return true;
    }

    public boolean takeHit() {
        if (!ativo) return false;
        hp--;
        hitTimer = .15f;
        if (hp <= 0f) {
            ativo = false;
            deathTimer = .4f;
            return true;
        }
        return false;
    }

    public float centerX() { return position.x + width / 2f; }
    public float centerY() { return position.y + height / 2f; }
    public float getHealthRatio() { return hp / 2f; }
    @Override public void update(float delta) { }
    @Override public void render(SpriteBatch batch) { if (ativo || deathTimer > 0f) sprite.draw(batch); }
    @Override public void dispose() { }
}
