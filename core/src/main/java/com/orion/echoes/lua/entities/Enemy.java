package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;

/** Predador lunar com movimento procedural estável, sem troca de quadros incompatíveis. */
public class Enemy extends Entidade {
    private static final float SPEED = 76f;
    private static final float DETECTION_RANGE = 480f;
    private final Sprite sprite;
    private final Vector2 direction = new Vector2();
    private final Rectangle movementBounds = new Rectangle();
    private final float spawnX;
    private final float spawnY;
    private float hp = 3f;
    private float contactCooldown;
    private float elapsed;
    private float hitFlashTimer;
    private float deathTimer;
    private float targetDistance;
    private boolean defeated;

    public Enemy(float x, float y, AssetManager assets) {
        super(x, y, 74f, 58f);
        bounds.set(x + 9f, y + 5f, 56f, 30f);
        spawnX = x;
        spawnY = y;
        sprite = new Sprite(assets.lunarEnemyTexture);
        sprite.setSize(102f, 76f);
        sprite.setOriginCenter();
        updateSpriteTransform(1f, 1f, 0f);
    }

    public void update(float delta, Astronauta target, Array<Obstacle> obstacles) {
        elapsed += delta;
        contactCooldown = Math.max(0f, contactCooldown - delta);
        if (defeated) {
            deathTimer = Math.max(0f, deathTimer - delta);
            float life = deathTimer / .48f;
            sprite.setColor(1f, 1f, 1f, life);
            sprite.setRotation((1f - life) * 22f);
            updateSpriteTransform(.68f + life * .32f, .68f + life * .32f, -9f * (1f - life));
            return;
        }
        if (!ativo) return;

        float targetX = target.getBounds().x + target.getBounds().width / 2f;
        float targetY = target.getBounds().y + target.getBounds().height / 2f;
        direction.set(targetX - centerX(), targetY - centerY());
        targetDistance = direction.len();
        if (targetDistance <= DETECTION_RANGE) direction.nor();
        else direction.set(MathUtils.cos(elapsed * .72f + spawnX),
            MathUtils.sin(elapsed * .53f + spawnY)).nor();

        float speed = targetDistance < 110f ? SPEED * 1.16f : SPEED;
        float step = speed * delta;
        float nextX = MathUtils.clamp(position.x + direction.x * step, 0f, GameConfig.WORLD_WIDTH - width);
        if (isFree(nextX, position.y, obstacles)) position.x = nextX;
        float nextY = MathUtils.clamp(position.y + direction.y * step, 0f, GameConfig.WORLD_HEIGHT - height);
        if (isFree(position.x, nextY, obstacles)) position.y = nextY;
        bounds.setPosition(position.x + 9f, position.y + 5f);

        float gait = MathUtils.sin(elapsed * (targetDistance < 110f ? 12f : 8.5f));
        float lunge = targetDistance < 110f ? Math.max(0f, gait) * .07f : 0f;
        updateSpriteTransform(1f + gait * .024f + lunge, 1f - gait * .016f - lunge * .3f,
            Math.abs(gait) * 2.4f);
        sprite.setRotation(direction.x * -3f + gait * 1.1f);
        if (hitFlashTimer > 0f) {
            hitFlashTimer = Math.max(0f, hitFlashTimer - delta);
            sprite.setColor(1f, .36f, .52f, 1f);
        } else {
            float corePulse = .94f + MathUtils.sin(elapsed * 4f) * .06f;
            sprite.setColor(corePulse, .9f, 1f, 1f);
        }
    }

    private void updateSpriteTransform(float scaleX, float scaleY, float bob) {
        sprite.setPosition(centerX() - sprite.getWidth() / 2f, position.y - 10f + bob);
        sprite.setScale(scaleX, scaleY);
    }

    private boolean isFree(float x, float y, Array<Obstacle> obstacles) {
        movementBounds.set(x + width * .12f, y + height * .08f, width * .76f, height * .54f);
        for (Obstacle obstacle : obstacles) if (movementBounds.overlaps(obstacle.getBounds())) return false;
        return true;
    }

    @Override public void update(float delta) { }
    @Override public void render(SpriteBatch batch) { if (ativo || deathTimer > 0f) sprite.draw(batch); }

    public boolean canDamage(Astronauta astronauta) {
        if (!ativo || contactCooldown > 0f || !bounds.overlaps(astronauta.getBounds())) return false;
        contactCooldown = 1.1f;
        return true;
    }

    public boolean hit(float x, float y, float range) {
        if (!ativo || Vector2.dst(centerX(), centerY(), x, y) > range) return false;
        return takeHit();
    }

    public boolean takeHit() {
        if (!ativo) return false;
        hp--;
        hitFlashTimer = .15f;
        if (hp <= 0f) {
            defeated = true;
            deathTimer = .48f;
            ativo = false;
            return true;
        }
        return false;
    }

    public float centerX() { return position.x + width / 2f; }
    public float centerY() { return position.y + height / 2f; }
    public float getHealthRatio() { return hp / 3f; }
    @Override public void dispose() { }
}
