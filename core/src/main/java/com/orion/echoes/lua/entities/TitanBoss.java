package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.systems.CombatTarget;

/**
 * Chefe de Titã.
 *
 * Duas coisas o separam dos hostis comuns. A primeira é a escala: 340px de
 * sprite contra os 104px do jogador, mais de três vezes a altura dele — um
 * chefe precisa ocupar a tela. A segunda é o ataque: em vez de causar dano
 * por encostar, ele para, prepara o golpe por quase um segundo e desaba num
 * impacto em área. O aviso é a mecânica: dá tempo de sair, e quem fica é
 * punido num raio maior que o alcance do próprio braço.
 *
 * A hitbox sai do retângulo desenhado, como no resto do jogo, então nunca
 * descola do corpo apesar do tamanho.
 */
public final class TitanBoss extends Entidade implements CombatTarget {

    /** Vigia o território, avança, prepara, desaba, se recompõe. */
    private enum State {
        GUARDA, AVANCA, PREPARA, IMPACTO,
        PREPARA_VOLLEY, VOLLEY, PREPARA_BURST, BURST, DANO, MORTO
    }

    private final TextureRegion[][] frames = new TextureRegion[4][4];
    private final Vector2 direction = new Vector2();
    private final Rectangle movementBounds = new Rectangle();

    private State state = State.GUARDA;
    private float hp = GameConfig.BOSS_MAX_HP;
    private float time;
    private float stateTime;
    private float attackCooldown;
    private boolean facingLeft;
    private boolean slamPending;
    private boolean volleyPending;
    private boolean burstPending;
    private int attackCycle;

    public TitanBoss(float x, float y, AssetManager assets) {
        super(x, y, GameConfig.BOSS_SPRITE_SIZE * .42f, GameConfig.BOSS_SPRITE_SIZE * .30f);
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                frames[row][column] = assets.titanBossFrame(column, row);
            }
        }
        syncBounds();
    }

    // =====================================================
    // COMPORTAMENTO
    // =====================================================

    public void update(float delta, Astronauta player, float worldWidth, float worldHeight,
                       Array<Rectangle> obstacles) {
        time += delta;
        stateTime += delta;
        attackCooldown = Math.max(0f, attackCooldown - delta);

        if (state == State.MORTO) {
            if (stateTime >= 1.1f) ativo = false;
            return;
        }
        if (!ativo) return;

        float targetX = player.getBounds().x + player.getBounds().width / 2f;
        float targetY = player.getBounds().y + player.getBounds().height / 2f;
        direction.set(targetX - centerX(), targetY - centerY());
        float distance = direction.len();
        if (distance > .001f) direction.scl(1f / distance);
        if (Math.abs(direction.x) > .08f) facingLeft = direction.x < 0f;

        switch (state) {
            case DANO -> {
                if (stateTime >= .28f) change(State.AVANCA);
            }
            case PREPARA -> {
                // Parado durante o aviso: é o que torna o golpe evitável.
                if (stateTime >= GameConfig.BOSS_TELEGRAPH_TIME) {
                    change(State.IMPACTO);
                    slamPending = true;
                }
            }
            case IMPACTO -> {
                if (stateTime >= GameConfig.BOSS_SLAM_TIME) {
                    attackCooldown = GameConfig.BOSS_ATTACK_COOLDOWN;
                    change(State.AVANCA);
                }
            }
            case PREPARA_VOLLEY -> {
                if (stateTime >= .72f) { volleyPending = true; change(State.VOLLEY); }
            }
            case VOLLEY -> {
                if (stateTime >= .38f) { attackCooldown = 1.65f; change(State.AVANCA); }
            }
            case PREPARA_BURST -> {
                if (stateTime >= 1.0f) { burstPending = true; change(State.BURST); }
            }
            case BURST -> {
                if (stateTime >= .5f) { attackCooldown = 1.35f; change(State.AVANCA); }
            }
            case AVANCA -> {
                if (distance > GameConfig.BOSS_CHASE_RADIUS) {
                    change(State.GUARDA);
                } else if (attackCooldown <= 0f && distance <= 470f) {
                    attackCycle++;
                    if (hp <= GameConfig.BOSS_MAX_HP * .34f && attackCycle % 2 == 0) {
                        change(State.PREPARA_BURST);
                    } else if (hp <= GameConfig.BOSS_MAX_HP * .72f && attackCycle % 3 != 0) {
                        change(State.PREPARA_VOLLEY);
                    } else if (distance <= GameConfig.BOSS_ATTACK_RANGE) {
                        change(State.PREPARA);
                    } else {
                        change(State.PREPARA_VOLLEY);
                    }
                } else {
                    move(direction.x, direction.y, delta, worldWidth, worldHeight, obstacles);
                }
            }
            default -> {
                if (distance <= GameConfig.BOSS_CHASE_RADIUS) change(State.AVANCA);
            }
        }
    }

    private void move(float dx, float dy, float delta, float worldWidth, float worldHeight,
                      Array<Rectangle> obstacles) {
        float nextX = MathUtils.clamp(position.x + dx * GameConfig.BOSS_SPEED * delta,
            0f, worldWidth - width);
        if (free(nextX, position.y, obstacles)) position.x = nextX;
        float nextY = MathUtils.clamp(position.y + dy * GameConfig.BOSS_SPEED * delta,
            0f, worldHeight - height);
        if (free(position.x, nextY, obstacles)) position.y = nextY;
        syncBounds();
    }

    private boolean free(float x, float y, Array<Rectangle> obstacles) {
        float size = GameConfig.BOSS_SPRITE_SIZE;
        float spriteX = x + width / 2f - size / 2f;
        float spriteY = y + size * GameConfig.BOSS_SPRITE_OFFSET_Y_RATIO;
        float boxWidth = size * GameConfig.BOSS_HITBOX_WIDTH_RATIO;
        float boxHeight = size * GameConfig.BOSS_HITBOX_HEIGHT_RATIO;
        movementBounds.set(spriteX + (size - boxWidth) / 2f,
            spriteY + size * GameConfig.BOSS_HITBOX_BASE_RATIO, boxWidth, boxHeight);
        for (Rectangle obstacle : obstacles) if (movementBounds.overlaps(obstacle)) return false;
        return true;
    }

    private void change(State next) {
        if (state == next) return;
        state = next;
        stateTime = 0f;
    }

    /** Hitbox derivada do desenho: o corpo apoiado, não o quadro inteiro. */
    private void syncBounds() {
        float size = GameConfig.BOSS_SPRITE_SIZE;
        float spriteX = centerX() - size / 2f;
        float spriteY = position.y + size * GameConfig.BOSS_SPRITE_OFFSET_Y_RATIO;
        float boxWidth = size * GameConfig.BOSS_HITBOX_WIDTH_RATIO;
        float boxHeight = size * GameConfig.BOSS_HITBOX_HEIGHT_RATIO;
        bounds.set(spriteX + (size - boxWidth) / 2f,
            spriteY + size * GameConfig.BOSS_HITBOX_BASE_RATIO, boxWidth, boxHeight);
    }

    // =====================================================
    // ATAQUE
    // =====================================================

    /** True uma única vez por golpe, no frame do impacto. */
    public boolean consumeSlam() {
        boolean pending = slamPending;
        slamPending = false;
        return pending;
    }

    public boolean consumeVolley() {
        boolean pending = volleyPending;
        volleyPending = false;
        return pending;
    }

    public boolean consumeBurst() {
        boolean pending = burstPending;
        burstPending = false;
        return pending;
    }

    /** O impacto é em área: alcança quem ficou perto, não só quem encostou. */
    public boolean slamHits(Astronauta player) {
        float dx = player.getBounds().x + player.getBounds().width / 2f - centerX();
        float dy = player.getBounds().y + player.getBounds().height / 2f - centerY();
        return dx * dx + dy * dy <= GameConfig.BOSS_SLAM_RADIUS * GameConfig.BOSS_SLAM_RADIUS;
    }

    public boolean isTelegraphing() {
        return state == State.PREPARA || state == State.PREPARA_VOLLEY
            || state == State.PREPARA_BURST;
    }
    public boolean isBurstTelegraphing() { return state == State.PREPARA_BURST; }
    public boolean isVolleyTelegraphing() { return state == State.PREPARA_VOLLEY; }

    /** 0 a 1 durante o aviso; alimenta o indicador no chão. */
    public float getTelegraphProgress() {
        float duration = state == State.PREPARA_BURST ? 1f
            : state == State.PREPARA_VOLLEY ? .72f : GameConfig.BOSS_TELEGRAPH_TIME;
        return isTelegraphing() ? MathUtils.clamp(stateTime / duration, 0f, 1f) : 0f;
    }

    @Override
    public boolean receiveDamage(float damage) {
        if (hp <= 0f) return false;
        hp = Math.max(0f, hp - damage);
        if (hp == 0f) {
            change(State.MORTO);
            return true;
        }
        if (!isTelegraphing() && state != State.IMPACTO && state != State.VOLLEY
            && state != State.BURST) change(State.DANO);
        return false;
    }

    @Override public boolean isAlive() { return ativo && hp > 0f; }

    public float getHealthRatio() { return hp / GameConfig.BOSS_MAX_HP; }

    // =====================================================
    // RENDER
    // =====================================================

    @Override public void update(float delta) { }

    @Override
    public void render(SpriteBatch batch) {
        if (!ativo) return;
        float size = GameConfig.BOSS_SPRITE_SIZE;
        float alpha = state == State.MORTO
            ? MathUtils.clamp(1f - stateTime / 1.1f, 0f, 1f) : 1f;

        // O aviso pisca; o impacto clareia. A cor conta o que vem.
        if (state == State.DANO) batch.setColor(1f, .55f, .5f, alpha);
        else if (isTelegraphing()) {
            float pulse = .6f + MathUtils.sin(stateTime * 26f) * .4f;
            batch.setColor(1f, .72f + pulse * .18f, .45f, alpha);
        } else if (state == State.IMPACTO || state == State.VOLLEY || state == State.BURST)
            batch.setColor(1f, .95f, .82f, alpha);
        else batch.setColor(.82f, .70f, .58f, alpha);

        // Recuo no aviso e avanço no golpe: peso de corpo grande.
        float lunge = state == State.IMPACTO
            ? -avanco(stateTime / GameConfig.BOSS_SLAM_TIME) * 14f
            : state == State.PREPARA ? getTelegraphProgress() * 10f : 0f;
        int row = state == State.MORTO ? 3 : isTelegraphing()
            || state == State.IMPACTO || state == State.VOLLEY || state == State.BURST ? 2
            : state == State.DANO ? 0 : state == State.AVANCA ? 1 : 0;
        int column = (int) (time / .2f) % 4;
        TextureRegion frame = frames[row][column];
        if (frame.isFlipX() != facingLeft) frame.flip(true, false);
        batch.draw(frame, centerX() - size / 2f,
            position.y + size * GameConfig.BOSS_SPRITE_OFFSET_Y_RATIO + lunge, size, size);
        batch.setColor(Color.WHITE);
    }

    private static float avanco(float value) {
        return MathUtils.clamp(value, 0f, 1f);
    }

    public float centerX() { return position.x + width / 2f; }

    public float centerY() {
        return position.y + GameConfig.BOSS_SPRITE_SIZE * GameConfig.BOSS_SPRITE_OFFSET_Y_RATIO
            + GameConfig.BOSS_SPRITE_SIZE / 2f;
    }

    @Override public void dispose() { }
}
