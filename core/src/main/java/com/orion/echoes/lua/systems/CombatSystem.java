package com.orion.echoes.lua.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.Enemy;
import com.orion.echoes.lua.entities.EnemyPulse;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.ParticleManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.ui.UiTheme;
import com.orion.echoes.lua.world.LunarWorld;

/**
 * Combate da fase lunar: hostis, pulsos inimigos, tiro do jogador e a leitura
 * visual disso tudo.
 *
 * Cada acerto responde em cinco canais - som posicionado, particula, hitstop,
 * shake e texto - e e por isso que esta logica ficou junta, e nao espalhada
 * pela tela.
 */
public final class CombatSystem {

    private static final float SHOT_RANGE = 420f;
    private static final float SHOT_WIDTH = 48f;
    private static final float SHOT_FX_TIME = .14f;
    private static final float ENEMY_CONTACT_DAMAGE = 12f;
    private static final float HEALTH_BAR_WIDTH = 68f;
    private static final float HEALTH_BAR_HEIGHT = 7f;

    private final SpriteBatch batch;
    private final AssetManager assets;
    private final OrthographicCamera camera;
    private final ParticleManager particles;
    private final SoundManager sounds;
    private final JuiceSystem juice;
    private final FeedbackSystem feedback;

    private final Vector2 shotStart = new Vector2();
    private final Vector2 shotEnd = new Vector2();
    private float shotFxTimer;

    public CombatSystem(SpriteBatch batch, AssetManager assets, OrthographicCamera camera,
                        ParticleManager particles, SoundManager sounds,
                        JuiceSystem juice, FeedbackSystem feedback) {
        this.batch = batch;
        this.assets = assets;
        this.camera = camera;
        this.particles = particles;
        this.sounds = sounds;
        this.juice = juice;
        this.feedback = feedback;
    }

    public void update(float delta, LunarWorld world) {
        shotFxTimer = Math.max(0f, shotFxTimer - delta);
        updateEnemies(delta, world);
        updatePulses(delta, world);
    }

    private void updateEnemies(float delta, LunarWorld world) {
        Astronauta player = world.getPlayer();
        for (Enemy enemy : world.getEnemies()) {
            enemy.update(delta, player, world.getObstacles());
            if (enemy.consumeTelegraphStarted()) {
                particles.criarAlertaInimigo(enemy.centerX(), enemy.centerY(), false);
                sounds.tocarAlertaInimigo(enemy.centerX(), enemy.centerY());
            }
            if (enemy.consumeRangedShot()) {
                world.getEnemyPulses().add(new EnemyPulse(enemy.centerX(), enemy.centerY(),
                    enemy.getAimDirection().x, enemy.getAimDirection().y, assets));
                sounds.tocarEspacial("disparo_pulso", SoundManager.Bus.SFX, .5f,
                    enemy.centerX(), enemy.centerY());
            }
            if (enemy.canDamage(player)) {
                player.receberDano(ENEMY_CONTACT_DAMAGE, enemy.centerX(), enemy.centerY());
                feedback.show("Hostil atingiu o traje: -12 O2.");
                hurtPlayer(player);
            }
        }
    }

    /** Pulsos do hostil atirador: viajam, batem em rocha ou acertam o traje. */
    private void updatePulses(float delta, LunarWorld world) {
        Astronauta player = world.getPlayer();
        for (int index = world.getEnemyPulses().size - 1; index >= 0; index--) {
            EnemyPulse pulse = world.getEnemyPulses().get(index);
            pulse.update(delta);
            if (pulse.collideWith(world.getObstacles())) {
                particles.criarImpactoTiro(pulse.centerX(), pulse.centerY());
            } else if (pulse.hits(player)) {
                player.receberDano(GameConfig.ENEMY_PULSE_DAMAGE,
                    pulse.centerX(), pulse.centerY());
                feedback.show("Pulso hostil atingiu o traje.");
                hurtPlayer(player);
            }
            if (!pulse.isAtivo()) world.getEnemyPulses().removeIndex(index);
        }
    }

    private void hurtPlayer(Astronauta player) {
        particles.criarImpactoTraje(
            player.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
            player.getPosition().y + GameConfig.PLAYER_HEIGHT / 2f);
        juice.trigger(JuiceSystem.Preset.PLAYER_HURT);
    }

    /** Tiro do jogador: raio instantaneo com tolerancia lateral de mira. */
    public void fire(LunarWorld world) {
        Astronauta player = world.getPlayer();
        MissionState mission = world.getMission();
        if (!mission.hasWeapon()) {
            feedback.show("Ataque indisponível: fabrique a arma na base.");
            return;
        }
        if (!player.consumirMunicao()) {
            feedback.show("Sem munição. Processe gelo na base para gerar células.");
            sounds.tocarSemGelo();
            return;
        }
        float x = player.getPosition().x + GameConfig.PLAYER_WIDTH / 2f;
        float y = player.getPosition().y + GameConfig.PLAYER_HEIGHT * .48f;
        float dirX = MathUtils.cosDeg(player.getAimAngle());
        float dirY = MathUtils.sinDeg(player.getAimAngle());

        Enemy target = null;
        float closest = SHOT_RANGE;
        for (Enemy enemy : world.getEnemies()) {
            if (!enemy.isAtivo()) continue;
            float dx = enemy.centerX() - x;
            float dy = enemy.centerY() - y;
            float alongRay = dx * dirX + dy * dirY;
            float perpendicular = Math.abs(dx * dirY - dy * dirX);
            if (alongRay > 0f && alongRay < closest && perpendicular < SHOT_WIDTH) {
                closest = alongRay;
                target = enemy;
            }
        }

        shotStart.set(x + dirX * 34f, y + dirY * 34f);
        shotEnd.set(x + dirX * SHOT_RANGE, y + dirY * SHOT_RANGE);
        if (target != null) {
            shotEnd.set(target.centerX(), target.centerY());
            boolean killed = target.takeHit();
            if (killed) {
                mission.registerEnemyDefeated();
                particles.criarMorteInimigo(target.centerX(), target.centerY());
                sounds.tocarMorteInimigo(target.centerX(), target.centerY());
            } else {
                particles.criarImpactoTiro(target.centerX(), target.centerY());
                sounds.tocarImpacto(target.centerX(), target.centerY());
            }
            feedback.show("Alvo atingido");
            juice.trigger(killed ? JuiceSystem.Preset.ENEMY_KILL : JuiceSystem.Preset.SHOT_HIT);
        }
        particles.criarMuzzleFlash(shotStart.x, shotStart.y, player.getAimAngle());
        player.triggerShot();
        sounds.tocarDisparo();
        shotFxTimer = SHOT_FX_TIME;
    }

    // =====================================================
    // LEITURA VISUAL
    // =====================================================

    public void render(LunarWorld world) {
        renderShot();
        renderHealthBars(world);
    }

    private void renderShot() {
        if (shotFxTimer <= 0f) return;
        float alpha = shotFxTimer / SHOT_FX_TIME;
        float progress = 1f - alpha;
        float px = MathUtils.lerp(shotStart.x, shotEnd.x, progress);
        float py = MathUtils.lerp(shotStart.y, shotEnd.y, progress);
        float dx = shotEnd.x - shotStart.x;
        float dy = shotEnd.y - shotStart.y;
        float length = Math.max(.001f, (float) Math.sqrt(dx * dx + dy * dy));
        dx /= length;
        dy /= length;

        /*
         * Texturas em vez de ShapeRenderer: o traco entra no mesmo batch das
         * entidades, sem alternar begin/end e sem o flush de GPU que isso custa.
         */
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(.45f, .95f, 1f, alpha * .35f);
        drawTrail(px - dx * 34f, py - dy * 34f, px, py, 7f * alpha);
        batch.setColor(.9f, 1f, 1f, alpha);
        drawTrail(px - dx * 42f, py - dy * 42f, px, py, 2f);
        float glow = (5f + 4f * alpha) * 2.6f;
        batch.draw(assets.energyFxFrame(1, 0), px - glow / 2f, py - glow / 2f, glow, glow);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Segmento texturizado: substitui rectLine sem sair do SpriteBatch. */
    private void drawTrail(float x1, float y1, float x2, float y2, float thickness) {
        float lineX = x2 - x1;
        float lineY = y2 - y1;
        float length = (float) Math.sqrt(lineX * lineX + lineY * lineY);
        if (length <= .001f) return;
        float angle = MathUtils.atan2(lineY, lineX) * MathUtils.radiansToDegrees;
        batch.draw(assets.uiWhiteTexture, x1, y1 - thickness / 2f,
            0f, thickness / 2f, length, thickness, 1f, 1f, angle);
    }

    private void renderHealthBars(LunarWorld world) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Enemy enemy : world.getEnemies()) {
            if (!enemy.isAtivo() || enemy.getHealthRatio() >= 1f) continue;
            float x = enemy.centerX() - HEALTH_BAR_WIDTH / 2f;
            float y = enemy.centerY() + 46f;
            batch.setColor(Color.WHITE);
            batch.draw(assets.uiBarTrackTexture, x, y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
            batch.setColor(UiTheme.MAGENTA);
            batch.draw(assets.uiBarFillTexture, x, y,
                HEALTH_BAR_WIDTH * enemy.getHealthRatio(), HEALTH_BAR_HEIGHT);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }
}
