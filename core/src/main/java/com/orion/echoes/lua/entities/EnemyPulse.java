package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;

/**
 * Pulso disparado pelo hostil atirador.
 *
 * Viaja em linha reta, morre no primeiro obstaculo e some sozinho depois de
 * um tempo. O jogador tem como desviar: a velocidade e menor que a do dash.
 */
public final class EnemyPulse extends Entidade {

    private final Vector2 velocity = new Vector2();
    private final TextureRegion frame;
    private float life;

    public EnemyPulse(float x, float y, float directionX, float directionY, AssetManager assets) {
        super(x - GameConfig.ENEMY_PULSE_SIZE / 2f, y - GameConfig.ENEMY_PULSE_SIZE / 2f,
            GameConfig.ENEMY_PULSE_SIZE, GameConfig.ENEMY_PULSE_SIZE);
        bounds.set(position.x, position.y, GameConfig.ENEMY_PULSE_SIZE, GameConfig.ENEMY_PULSE_SIZE);
        velocity.set(directionX, directionY).nor().scl(GameConfig.ENEMY_PULSE_SPEED);
        frame = assets.energyFxFrame(2, 1);
    }

    @Override
    public void update(float delta) {
        if (!ativo) return;
        life += delta;
        if (life >= GameConfig.ENEMY_PULSE_LIFETIME) {
            ativo = false;
            return;
        }
        position.add(velocity.x * delta, velocity.y * delta);
        bounds.setPosition(position.x, position.y);
        if (position.x < -60f || position.y < -60f
            || position.x > GameConfig.WORLD_WIDTH + 60f
            || position.y > GameConfig.WORLD_HEIGHT + 60f) {
            ativo = false;
        }
    }

    /** Consome o pulso ao encostar em obstaculo; devolve true quando some. */
    public boolean collideWith(Array<Obstacle> obstacles) {
        if (!ativo) return false;
        for (Obstacle obstacle : obstacles) {
            if (bounds.overlaps(obstacle.getBounds())) {
                ativo = false;
                return true;
            }
        }
        return false;
    }

    public boolean hits(Astronauta astronauta) {
        if (!ativo || !bounds.overlaps(astronauta.getBounds())) return false;
        ativo = false;
        return true;
    }

    @Override
    public void render(SpriteBatch batch) {
        if (!ativo) return;
        float pulse = .85f + MathUtils.sin(life * 22f) * .15f;
        float size = GameConfig.ENEMY_PULSE_SIZE * 2.1f * pulse;
        batch.setColor(1f, .72f, .95f, .95f);
        batch.draw(frame, centerX() - size / 2f, centerY() - size / 2f, size, size);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public float centerX() { return position.x + width / 2f; }
    public float centerY() { return position.y + height / 2f; }

    @Override public void dispose() { }
}
