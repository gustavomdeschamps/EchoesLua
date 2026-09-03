package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.systems.CombatTarget;

/** Predador anfíbio de Titã: espécie, silhueta e animação próprias. */
public final class TitanEnemy extends Entidade implements CombatTarget {
    public static final float MAX_HP = 80f;
    public static final float SPEED = 70f;
    public static final float CHASE_RADIUS = 160f;
    private static final float SPRITE_SIZE = 126f;
    private final TextureRegion[][] frames = new TextureRegion[4][4];
    private final Vector2 direction = new Vector2();
    private float hp = MAX_HP;
    private float time;
    private float hitTimer;
    private float attackCooldown;
    private boolean facingLeft;

    public TitanEnemy(float x, float y, AssetManager assets) {
        super(x, y, 70f, 54f);
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                frames[row][column] = assets.titanEnemyFrame(column, row);
            }
        }
        syncBounds();
    }

    public void update(float delta, Astronauta player, float worldWidth, float worldHeight) {
        if (!ativo) return;
        time += delta;
        hitTimer = Math.max(0f, hitTimer - delta);
        attackCooldown = Math.max(0f, attackCooldown - delta);
        direction.set(player.getBounds().x + player.getBounds().width / 2f - centerX(),
            player.getBounds().y + player.getBounds().height / 2f - centerY());
        float distance = direction.len();
        if (distance <= CHASE_RADIUS && distance > .001f && hitTimer <= 0f) {
            direction.scl(1f / distance);
            facingLeft = direction.x < 0f;
            position.x = MathUtils.clamp(position.x + direction.x * SPEED * delta,
                0f, worldWidth - width);
            position.y = MathUtils.clamp(position.y + direction.y * SPEED * delta,
                0f, worldHeight - height);
            syncBounds();
        }
    }

    public boolean canDamage(Astronauta player) {
        if (!ativo || attackCooldown > 0f || !bounds.overlaps(player.getBounds())) return false;
        attackCooldown = 1.05f;
        return true;
    }

    private void syncBounds() {
        bounds.set(position.x + 10f, position.y + 6f, width - 20f, height - 12f);
    }

    private TextureRegion frame() {
        int row = hp <= 0f ? 3 : hitTimer > 0f ? 3
            : direction.len() <= CHASE_RADIUS && direction.len2() > 1f ? 1 : 0;
        int column = hp <= 0f ? 3 : (int)(time / (row == 1 ? .12f : .22f)) % 4;
        TextureRegion frame = frames[row][column];
        if (frame.isFlipX() != facingLeft) frame.flip(true, false);
        return frame;
    }

    @Override public boolean receiveDamage(float damage) {
        if (!ativo || damage <= 0f) return false;
        hp = Math.max(0f, hp - damage);
        hitTimer = .16f;
        if (hp == 0f) ativo = false;
        return hp == 0f;
    }

    @Override public boolean isAlive() { return ativo && hp > 0f; }
    @Override public float centerX() { return position.x + width / 2f; }
    @Override public float centerY() { return position.y + height / 2f; }
    public float getHealthRatio() { return hp / MAX_HP; }

    @Override public void update(float delta) { }

    @Override public void render(SpriteBatch batch) {
        if (!ativo && hp > 0f) return;
        if (hitTimer > 0f) batch.setColor(1f, .55f, .3f, 1f);
        batch.draw(frame(), centerX() - SPRITE_SIZE / 2f,
            position.y - 28f, SPRITE_SIZE, SPRITE_SIZE);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override public void dispose() { }
}
