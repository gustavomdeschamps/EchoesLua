package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.systems.CombatTarget;

/** Predador anfíbio de Titã: espécie, silhueta e animação próprias. */
public final class TitanEnemy extends Entidade implements CombatTarget {
    public static final float MAX_HP = 110f;
    public static final float SPEED = 82f;
    public static final float CHASE_RADIUS = 520f;
    private static final float SPRITE_SIZE = 144f;
    private final TextureRegion[][] frames = new TextureRegion[4][4];
    private final Vector2 direction = new Vector2();
    private final Rectangle movementBounds = new Rectangle();
    private float hp = MAX_HP;
    private float time;
    private float hitTimer;
    private float attackCooldown;
    private float telegraphTimer;
    private boolean shotPending;
    private boolean moving;
    private float deathTime;
    private boolean facingLeft;
    private final float spawnX;
    private final float spawnY;

    public TitanEnemy(float x, float y, AssetManager assets) {
        super(x, y, 90f, 70f);
        spawnX = x;
        spawnY = y;
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                frames[row][column] = assets.titanEnemyFrame(column, row);
            }
        }
        syncBounds();
    }

    public void update(float delta, Astronauta player, float worldWidth, float worldHeight,
                       Array<Rectangle> obstacles) {
        if (!ativo) return;
        time += delta;
        if (hp <= 0f) {
            deathTime += delta;
            if (deathTime >= .72f) ativo = false;
            return;
        }
        hitTimer = Math.max(0f, hitTimer - delta);
        attackCooldown = Math.max(0f, attackCooldown - delta);
        direction.set(player.getBounds().x + player.getBounds().width / 2f - centerX(),
            player.getBounds().y + player.getBounds().height / 2f - centerY());
        float distance = direction.len();
        if (telegraphTimer > 0f) {
            telegraphTimer -= delta;
            moving = false;
            if (telegraphTimer <= 0f) {
                shotPending = true;
                attackCooldown = 1.8f;
            }
        } else if (distance > 115f && distance <= CHASE_RADIUS && distance > .001f && hitTimer <= 0f) {
            direction.scl(1f / distance);
            facingLeft = direction.x < 0f;
            move(direction.x, direction.y, delta, worldWidth, worldHeight, obstacles);
            moving = true;
        } else if (distance > CHASE_RADIUS && hitTimer <= 0f) {
            // Patrulha a área de nascimento: inimigos distantes não parecem congelados.
            float patrolAngle = time * .48f + (spawnX + spawnY) * .013f;
            float targetX = spawnX + MathUtils.cos(patrolAngle) * 125f;
            float targetY = spawnY + MathUtils.sin(patrolAngle * .83f) * 92f;
            direction.set(targetX - centerX(), targetY - centerY());
            if (direction.len2() > 16f) {
                direction.nor();
                facingLeft = direction.x < 0f;
                move(direction.x * .48f, direction.y * .48f, delta,
                    worldWidth, worldHeight, obstacles);
                moving = true;
            } else moving = false;
        } else {
            moving = false;
            if (distance <= 450f && attackCooldown <= 0f && hitTimer <= 0f) telegraphTimer = .55f;
        }
        // A hitbox acompanha inclusive os quadros de ataque e o leve bob visual.
        syncBounds();
    }

    public boolean canDamage(Astronauta player) {
        if (!ativo || attackCooldown > 0f || !bounds.overlaps(player.getBounds())) return false;
        attackCooldown = 1.05f;
        return true;
    }

    private void syncBounds() {
        bounds.set(centerX() - 43f, position.y + 14f + MathUtils.sin(time * 3f) * 2f,
            86f, 48f);
    }

    private void move(float dx, float dy, float delta, float worldWidth, float worldHeight,
                      Array<Rectangle> obstacles) {
        float nextX = MathUtils.clamp(position.x + dx * SPEED * delta, 0f, worldWidth - width);
        if (free(nextX, position.y, obstacles)) position.x = nextX;
        float nextY = MathUtils.clamp(position.y + dy * SPEED * delta, 0f, worldHeight - height);
        if (free(position.x, nextY, obstacles)) position.y = nextY;
    }

    private boolean free(float x, float y, Array<Rectangle> obstacles) {
        movementBounds.set(x + width / 2f - 43f,
            y + 14f + MathUtils.sin(time * 3f) * 2f, 86f, 48f);
        for (Rectangle obstacle : obstacles) if (movementBounds.overlaps(obstacle)) return false;
        return true;
    }

    private TextureRegion frame() {
        int row = hp <= 0f ? 3 : hitTimer > 0f ? 3
            : telegraphTimer > 0f ? 2 : moving ? 1 : 0;
        int column = hp <= 0f ? Math.min(3, (int)(deathTime / .16f))
            : (int)(time / (row == 1 ? .12f : .22f)) % 4;
        TextureRegion frame = frames[row][column];
        if (frame.isFlipX() != facingLeft) frame.flip(true, false);
        return frame;
    }

    @Override public boolean receiveDamage(float damage) {
        if (!ativo || damage <= 0f) return false;
        hp = Math.max(0f, hp - damage);
        hitTimer = .16f;
        return hp == 0f;
    }

    @Override public boolean isAlive() { return ativo && hp > 0f; }
    public boolean consumeShot() {
        boolean result = shotPending;
        shotPending = false;
        return result;
    }
    public float shotDirectionX() { return direction.x; }
    public float shotDirectionY() { return direction.y; }
    public boolean isTelegraphing() { return telegraphTimer > 0f; }
    @Override public float centerX() { return position.x + width / 2f; }
    @Override public float centerY() { return position.y + height / 2f; }
    public float getHealthRatio() { return hp / MAX_HP; }

    @Override public void update(float delta) { }

    @Override public void render(SpriteBatch batch) {
        if (!ativo) return;
        if (hitTimer > 0f) batch.setColor(1f, .55f, .3f, 1f);
        batch.draw(frame(), centerX() - SPRITE_SIZE / 2f,
            position.y - 32f + MathUtils.sin(time * 3f) * 2f, SPRITE_SIZE, SPRITE_SIZE);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override public void dispose() { }
}
