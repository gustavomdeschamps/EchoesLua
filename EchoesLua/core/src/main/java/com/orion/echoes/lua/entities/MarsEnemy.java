package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;

/** Drone ou rover marciano com leitura antecipada de ataque e animação real. */
public final class MarsEnemy extends Entidade {
    private enum State { IDLE, CHASE, TELEGRAPH, ATTACK, HIT, DYING }
    private final TextureRegion[][] frames = new TextureRegion[4][4];
    private final Vector2 direction = new Vector2();
    private final Rectangle testBounds = new Rectangle();
    private final boolean drone;
    private final float worldWidth, worldHeight;
    private State state = State.CHASE;
    private float hp = 3f, elapsed, stateTime, damageCooldown;
    private boolean facingLeft, telegraphStarted;

    public MarsEnemy(float x, float y, boolean drone, AssetManager assets,
                     float worldWidth, float worldHeight) {
        super(x, y, drone ? 70f : 82f, drone ? 58f : 64f);
        this.drone = drone;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        sincronizarHitbox();
        for (int row = 0; row < 4; row++)
            for (int column = 0; column < 4; column++)
                frames[row][column] = assets.marsEnemyFrame(drone, column, row);
    }

    public void update(float delta, Astronauta target, Array<MarsObject> rocks) {
        elapsed += delta;
        stateTime += delta;
        damageCooldown = Math.max(0f, damageCooldown - delta);
        if (state == State.DYING) {
            if (stateTime >= .5f) ativo = false;
            return;
        }
        if (!ativo) return;
        direction.set(target.getBounds().x + target.getBounds().width / 2f - centerX(),
            target.getBounds().y + target.getBounds().height / 2f - centerY());
        float distance = direction.len();
        if (distance > .001f) direction.scl(1f / distance);
        if (Math.abs(direction.x) > .08f) facingLeft = direction.x < 0f;
        float attackRange = drone ? 104f : 88f;

        switch (state) {
            case HIT -> { if (stateTime >= .17f) change(State.CHASE); }
            case TELEGRAPH -> { if (stateTime >= (drone ? .46f : .34f)) change(State.ATTACK); }
            case ATTACK -> {
                if (stateTime < .2f) move(direction.x * (drone ? 1.45f : 1.7f),
                    direction.y * (drone ? 1.45f : 1.7f), delta, rocks);
                if (stateTime >= .37f) change(State.CHASE);
            }
            default -> {
                if (distance <= attackRange) change(State.TELEGRAPH);
                else {
                    changeIfNeeded(State.CHASE);
                    float strafe = drone ? MathUtils.sin(elapsed * 1.2f) * .28f : 0f;
                    move(direction.x - direction.y * strafe, direction.y + direction.x * strafe, delta, rocks);
                }
            }
        }
    }

    private void move(float dx, float dy, float delta, Array<MarsObject> rocks) {
        float speed = drone ? 86f : 68f;
        float nextX = MathUtils.clamp(position.x + dx * speed * delta, 0f, worldWidth - width);
        if (free(nextX, position.y, rocks)) position.x = nextX;
        float nextY = MathUtils.clamp(position.y + dy * speed * delta, 0f, worldHeight - height);
        if (free(position.x, nextY, rocks)) position.y = nextY;
        sincronizarHitbox();
    }

    /**
     * Hitbox derivada do sprite, incluindo a flutuacao do drone.
     *
     * Este era o pior desalinhamento do jogo: o drone subia ate 11px que a
     * caixa nao acompanhava, entao mirar no sprite fazia o tiro passar por
     * baixo. Agora corpo e desenho sobem juntos.
     */
    private void sincronizarHitbox() {
        bounds.set(hitboxX(position.x), hitboxY(position.y), hitboxWidth(), hitboxHeight());
    }

    private float spriteSize() {
        return drone ? GameConfig.MARS_DRONE_SPRITE_SIZE : GameConfig.MARS_CRAWLER_SPRITE_SIZE;
    }

    /** Deslocamento vertical do sprite; o drone paira, o crawler nao. */
    private float spriteBob() {
        return drone ? 8f + MathUtils.sin(elapsed * 5f) * 3f : 0f;
    }

    private float hitboxWidth() {
        return spriteSize() * GameConfig.ENEMY_HITBOX_WIDTH_RATIO;
    }

    private float hitboxHeight() {
        return spriteSize() * GameConfig.ENEMY_HITBOX_HEIGHT_RATIO;
    }

    private float hitboxX(float originX) {
        return originX + width / 2f - hitboxWidth() / 2f;
    }

    private float hitboxY(float originY) {
        float spriteY = originY + GameConfig.MARS_ENEMY_SPRITE_OFFSET_Y + spriteBob();
        return spriteY + spriteSize() * GameConfig.ENEMY_HITBOX_BASE_RATIO;
    }

    private boolean free(float x, float y, Array<MarsObject> rocks) {
        testBounds.set(hitboxX(x), hitboxY(y), hitboxWidth(), hitboxHeight());
        for (MarsObject rock : rocks) if (rock.isBlocking() && testBounds.overlaps(rock.getBounds())) return false;
        return true;
    }

    private void change(State next) {
        if (state != next) {
            state = next;
            stateTime = 0f;
            if (next == State.TELEGRAPH) telegraphStarted = true;
        }
    }
    private void changeIfNeeded(State next) { if (state != next) change(next); }

    private TextureRegion currentFrame() {
        int row, column;
        switch (state) {
            case IDLE -> { row = 0; column = (int)(elapsed / .22f) % 4; }
            case CHASE -> { row = 1; column = (int)(stateTime / (drone ? .1f : .12f)) % 4; }
            case TELEGRAPH -> { row = 2; column = Math.min(1, (int)(stateTime / .18f)); }
            case ATTACK -> { row = 2; column = Math.min(3, 2 + (int)(stateTime / .17f)); }
            case HIT -> { row = 3; column = Math.min(1, (int)(stateTime / .085f)); }
            case DYING -> { row = 3; column = Math.min(3, 2 + (int)(stateTime / .24f)); }
            default -> { row = 0; column = 0; }
        }
        TextureRegion frame = frames[row][column];
        if (frame.isFlipX() != facingLeft) frame.flip(true, false);
        return frame;
    }

    public boolean canDamage(Astronauta player) {
        if (!ativo || state != State.ATTACK || stateTime < .07f || stateTime > .24f
            || damageCooldown > 0f || !bounds.overlaps(player.getBounds())) return false;
        damageCooldown = 1f;
        return true;
    }
    public boolean takeHit() {
        if (!ativo || state == State.DYING) return false;
        hp--;
        if (hp <= 0f) { change(State.DYING); return true; }
        change(State.HIT);
        return false;
    }
    /** Centro do sprite, acompanhando a flutuacao: e onde a mira encosta. */
    public float centerX() { return position.x + width / 2f; }

    public float centerY() {
        return position.y + GameConfig.MARS_ENEMY_SPRITE_OFFSET_Y + spriteBob() + spriteSize() / 2f;
    }
    public float getHealthRatio() { return hp / 3f; }
    public boolean consumeTelegraphStarted() {
        boolean started = telegraphStarted;
        telegraphStarted = false;
        return started;
    }
    @Override public void update(float delta) { }
    @Override public void render(SpriteBatch batch) {
        if (!ativo && state != State.DYING) return;
        float alpha = state == State.DYING ? MathUtils.clamp(1f - stateTime / .52f, 0f, 1f) : 1f;
        if (state == State.HIT) batch.setColor(1f, .55f, .35f, alpha);
        else batch.setColor(1f, 1f, 1f, alpha);
        float bob = spriteBob();
        if (state == State.TELEGRAPH) bob += MathUtils.sin(stateTime * 25f) * 2f;
        float size = spriteSize();
        batch.draw(currentFrame(), centerX() - size / 2f,
            position.y + GameConfig.MARS_ENEMY_SPRITE_OFFSET_Y + bob, size, size);
        batch.setColor(1f, 1f, 1f, 1f);
    }
    @Override public void dispose() { }
}
