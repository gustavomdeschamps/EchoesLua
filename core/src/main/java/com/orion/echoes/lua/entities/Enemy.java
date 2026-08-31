package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;

/** Predador lunar com telegraph legível e animações 4x4 consistentes. */
public class Enemy extends Entidade {
    private enum State { IDLE, CHASE, TELEGRAPH, ATTACK, HIT, DYING }
    private static final float SPEED = 72f;
    private static final float DETECTION_RANGE = 480f;
    private static final float ATTACK_RANGE = 92f;
    private final TextureRegion[][] frames = new TextureRegion[4][4];
    private final Vector2 direction = new Vector2();
    private final Rectangle movementBounds = new Rectangle();
    private final float spawnX, spawnY;
    private State state = State.IDLE;
    private float hp = 3f, stateTime, contactCooldown, elapsed, targetDistance;
    private boolean facingLeft, defeated;

    public Enemy(float x, float y, AssetManager assets) {
        super(x, y, 72f, 58f);
        bounds.set(x + 10f, y + 6f, 52f, 30f);
        spawnX = x;
        spawnY = y;
        for (int row = 0; row < 4; row++)
            for (int column = 0; column < 4; column++)
                frames[row][column] = assets.lunarEnemyFrame(column, row);
    }

    public void update(float delta, Astronauta target, Array<Obstacle> obstacles) {
        elapsed += delta;
        stateTime += delta;
        contactCooldown = Math.max(0f, contactCooldown - delta);
        if (state == State.DYING) {
            if (stateTime >= .44f) ativo = false;
            return;
        }
        if (!ativo) return;
        float targetX = target.getBounds().x + target.getBounds().width / 2f;
        float targetY = target.getBounds().y + target.getBounds().height / 2f;
        direction.set(targetX - centerX(), targetY - centerY());
        targetDistance = direction.len();
        if (targetDistance > .001f) direction.scl(1f / targetDistance);
        if (Math.abs(direction.x) > .08f) facingLeft = direction.x < 0f;

        switch (state) {
            case HIT -> {
                if (stateTime >= .18f) changeState(targetDistance <= ATTACK_RANGE ? State.TELEGRAPH : State.CHASE);
            }
            case TELEGRAPH -> {
                if (stateTime >= .36f) changeState(State.ATTACK);
            }
            case ATTACK -> {
                if (stateTime < .19f) move(direction.x * 1.8f, direction.y * 1.8f, delta, obstacles);
                if (stateTime >= .34f) changeState(targetDistance <= ATTACK_RANGE ? State.TELEGRAPH : State.CHASE);
            }
            default -> {
                if (targetDistance <= ATTACK_RANGE) {
                    changeState(State.TELEGRAPH);
                } else {
                    if (targetDistance <= DETECTION_RANGE) changeStateIfNeeded(State.CHASE);
                    else {
                        changeStateIfNeeded(State.IDLE);
                        direction.set(MathUtils.cos(elapsed * .54f + spawnX * .01f),
                            MathUtils.sin(elapsed * .43f + spawnY * .01f)).nor();
                    }
                    float orbit = MathUtils.sin(elapsed * .7f + spawnX * .013f) * .18f;
                    move(direction.x - direction.y * orbit, direction.y + direction.x * orbit, delta, obstacles);
                }
            }
        }
    }

    private void move(float dx, float dy, float delta, Array<Obstacle> obstacles) {
        float speed = SPEED * (state == State.ATTACK ? 1.42f : 1f);
        float nextX = MathUtils.clamp(position.x + dx * speed * delta, 0f, GameConfig.WORLD_WIDTH - width);
        if (isFree(nextX, position.y, obstacles)) position.x = nextX;
        float nextY = MathUtils.clamp(position.y + dy * speed * delta, 0f, GameConfig.WORLD_HEIGHT - height);
        if (isFree(position.x, nextY, obstacles)) position.y = nextY;
        bounds.setPosition(position.x + 10f, position.y + 6f);
    }

    private boolean isFree(float x, float y, Array<Obstacle> obstacles) {
        movementBounds.set(x + 10f, y + 6f, 52f, 30f);
        for (Obstacle obstacle : obstacles) if (movementBounds.overlaps(obstacle.getBounds())) return false;
        return true;
    }

    private void changeState(State next) {
        if (state != next) { state = next; stateTime = 0f; }
    }
    private void changeStateIfNeeded(State next) { if (state != next) changeState(next); }

    private TextureRegion currentFrame() {
        int row, column;
        switch (state) {
            case IDLE -> { row = 0; column = (int)(elapsed / .22f) % 4; }
            case CHASE -> { row = 1; column = (int)(stateTime / .095f) % 4; }
            case TELEGRAPH -> { row = 2; column = Math.min(1, (int)(stateTime / .18f)); }
            case ATTACK -> { row = 2; column = Math.min(3, 2 + (int)(stateTime / .17f)); }
            case HIT -> { row = 3; column = Math.min(1, (int)(stateTime / .09f)); }
            case DYING -> { row = 3; column = Math.min(3, 2 + (int)(stateTime / .22f)); }
            default -> { row = 0; column = 0; }
        }
        TextureRegion frame = frames[row][column];
        if (frame.isFlipX() != facingLeft) frame.flip(true, false);
        return frame;
    }

    @Override public void update(float delta) { }
    @Override public void render(SpriteBatch batch) {
        if (!ativo && state != State.DYING) return;
        float alpha = state == State.DYING ? MathUtils.clamp(1f - stateTime / .5f, 0f, 1f) : 1f;
        if (state == State.HIT) batch.setColor(1f, .45f, .62f, alpha);
        else batch.setColor(1f, 1f, 1f, alpha);
        float pulse = state == State.TELEGRAPH ? MathUtils.sin(stateTime * 26f) * 2f : 0f;
        batch.draw(currentFrame(), position.x - 16f, position.y - 18f + pulse, 104f, 104f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public boolean canDamage(Astronauta astronauta) {
        if (!ativo || state != State.ATTACK || stateTime < .08f || stateTime > .24f
            || contactCooldown > 0f || !bounds.overlaps(astronauta.getBounds())) return false;
        contactCooldown = .9f;
        return true;
    }
    public boolean hit(float x, float y, float range) {
        if (!ativo || Vector2.dst(centerX(), centerY(), x, y) > range) return false;
        return takeHit();
    }
    public boolean takeHit() {
        if (!ativo || state == State.DYING) return false;
        hp--;
        if (hp <= 0f) { defeated = true; changeState(State.DYING); return true; }
        changeState(State.HIT);
        return false;
    }
    public float centerX() { return position.x + width / 2f; }
    public float centerY() { return position.y + height / 2f; }
    public float getHealthRatio() { return hp / 3f; }
    public boolean isDefeated() { return defeated; }
    @Override public void dispose() { }
}
