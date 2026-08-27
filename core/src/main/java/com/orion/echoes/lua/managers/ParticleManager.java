package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;

public class ParticleManager implements Disposable {

    private final Array<ParticleEffectPool.PooledEffect> effects;
    private final ObjectMap<String, ParticleEffectPool> pools;
    private final ObjectMap<String, ParticleEffect> templates;
    private final Array<VisualFx> visualEffects = new Array<>();
    private final TextureRegion[] fxRegions = new TextureRegion[4];
    private final Pool<VisualFx> visualPool = new Pool<>(24, 96) {
        @Override protected VisualFx newObject() { return new VisualFx(); }
    };

    public ParticleManager(AssetManager assets) {

        effects =
            new Array<>();

        pools = new ObjectMap<>();
        templates = new ObjectMap<>();
        for (int i = 0; i < fxRegions.length; i++) fxRegions[i] = assets.fxRegion(i);
    }

    // ==========================================
    // POEIRA LUNAR
    // ==========================================

    public void criarPoeiraLunar(
        float x,
        float y
    ) {

        criarEfeito(
            "particles/poeira.p",
            x,
            y
        );
    }

    public void criarPoeiraLunar(float x, float y, boolean running, float dirX, float dirY) {
        float angle = MathUtils.atan2(dirY, dirX) * MathUtils.radiansToDegrees + 180f;
        if (running) {
            spawnVisual(1, x - 44f, y - 22f, 88f, 54f, .34f, angle,
                -dirX * 22f, -dirY * 22f, .74f, 1.08f);
        } else {
            spawnVisual(0, x - 24f, y - 18f, 48f, 40f, .3f, 0f,
                -dirX * 7f, -dirY * 7f, .62f, 1f);
        }
    }

    public void criarPoeiraMarte(float x, float y, boolean running, float dirX, float dirY) {
        float angle = MathUtils.atan2(dirY, dirX) * MathUtils.radiansToDegrees + 180f;
        spawnVisual(running ? 1 : 0, x - (running ? 44f : 24f), y - 18f,
            running ? 88f : 48f, running ? 54f : 40f, running ? .36f : .31f,
            running ? angle : 0f, -dirX * (running ? 24f : 8f), -dirY * (running ? 24f : 8f),
            running ? .72f : .6f, running ? 1.1f : 1f, .82f, .42f, .25f);
    }

    // ==========================================
    // COLETA
    // ==========================================

    public void criarEfeitoColeta(
        float x,
        float y
    ) {

        criarEfeito(
            "particles/coleta.p",
            x,
            y
        );
        // O brilho acompanha o objeto recolhido sem esconder a animação das mãos.
        spawnVisual(2, x - 25f, y - 10f, 50f, 58f, .46f, 0f,
            0f, 16f, .48f, .88f);
    }

    public void criarImpactoTraje(float x, float y) {
        spawnVisual(3, x - 46f, y - 40f, 92f, 92f, .32f,
            MathUtils.random(-12f, 12f), 0f, 3f, .58f, 1.04f);
    }

    private void spawnVisual(int region, float x, float y, float width, float height,
                             float duration, float rotation, float velocityX, float velocityY,
                             float startScale, float endScale) {
        spawnVisual(region, x, y, width, height, duration, rotation, velocityX, velocityY,
            startScale, endScale, 1f, 1f, 1f);
    }

    private void spawnVisual(int region, float x, float y, float width, float height,
                             float duration, float rotation, float velocityX, float velocityY,
                             float startScale, float endScale, float red, float green, float blue) {
        VisualFx fx = visualPool.obtain();
        fx.region = fxRegions[region];
        fx.x = x;
        fx.y = y;
        fx.width = width;
        fx.height = height;
        fx.duration = duration;
        fx.life = duration;
        fx.rotation = rotation;
        fx.velocityX = velocityX;
        fx.velocityY = velocityY;
        fx.startScale = startScale;
        fx.endScale = endScale;
        fx.red = red;
        fx.green = green;
        fx.blue = blue;
        visualEffects.add(fx);
    }

    // ==========================================
    // PROCESSAMENTO
    // ==========================================

    public void criarProcessamento(
        float x,
        float y
    ) {

        criarEfeito(
            "particles/faisca.p",
            x,
            y
        );
    }

    // ==========================================
    // ALERTA O2
    // ==========================================

    public void criarAlertaOxigenio(
        float x,
        float y
    ) {

        criarEfeito(
            "particles/alerta_oxigenio.p",
            x,
            y
        );
    }

    // ==========================================
    // CRIAR EFEITO
    // ==========================================

    private void criarEfeito(
        String arquivo,
        float x,
        float y
    ) {

        if (
            !Gdx.files
                .internal(arquivo)
                .exists()
        ) {

            return;
        }

        ParticleEffectPool pool = pools.get(arquivo);
        if (pool == null) {
            ParticleEffect template = new ParticleEffect();
            template.load(Gdx.files.internal(arquivo), Gdx.files.internal("particles"));
            templates.put(arquivo, template);
            pool = new ParticleEffectPool(template, 4, 32);
            pools.put(arquivo, pool);
        }

        ParticleEffectPool.PooledEffect effect = pool.obtain();

        effect.setPosition(
            x,
            y
        );

        effect.start();

        effects.add(
            effect
        );
    }

    // ==========================================
    // UPDATE
    // ==========================================

    public void update(
        float delta
    ) {

        for (
            int i =
            effects.size - 1;

            i >= 0;

            i--
        ) {

            ParticleEffectPool.PooledEffect effect =
                effects.get(i);

            effect.update(
                delta
            );

            if (effect.isComplete()) {

                effects.removeIndex(
                    i
                );

                effect.free();
            }
        }

        for (int i = visualEffects.size - 1; i >= 0; i--) {
            VisualFx fx = visualEffects.get(i);
            fx.life -= delta;
            fx.x += fx.velocityX * delta;
            fx.y += fx.velocityY * delta;
            if (fx.life <= 0f) visualPool.free(visualEffects.removeIndex(i));
        }
    }

    // ==========================================
    // RENDER
    // ==========================================

    public void render(
        SpriteBatch batch
    ) {

        for (
            ParticleEffectPool.PooledEffect effect
            : effects
        ) {

            effect.draw(batch);
        }
        for (VisualFx fx : visualEffects) {
            float progress = 1f - fx.life / fx.duration;
            float alpha = Interpolation.fade.apply(MathUtils.clamp(1f - progress, 0f, 1f));
            float scale = MathUtils.lerp(fx.startScale, fx.endScale, Interpolation.pow2Out.apply(progress));
            batch.setColor(fx.red, fx.green, fx.blue, alpha);
            batch.draw(fx.region, fx.x, fx.y, fx.width / 2f, fx.height / 2f,
                fx.width, fx.height, scale, scale, fx.rotation);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    // ==========================================
    // LIMPAR
    // ==========================================

    public void clear() {

        for (
            ParticleEffectPool.PooledEffect effect
            : effects
        ) {
            effect.free();
        }

        effects.clear();
        while (visualEffects.size > 0) visualPool.free(visualEffects.pop());

        for (ParticleEffectPool pool : pools.values()) {
            pool.clear();
        }

        for (ParticleEffect template : templates.values()) {
            template.dispose();
        }

        pools.clear();
        templates.clear();
    }

    // ==========================================
    // DISPOSE
    // ==========================================

    @Override
    public void dispose() {

        clear();
    }

    private static final class VisualFx implements Pool.Poolable {
        TextureRegion region;
        float x, y, width, height, duration, life, rotation;
        float velocityX, velocityY, startScale, endScale;
        float red = 1f, green = 1f, blue = 1f;
        @Override public void reset() {
            region = null;
            x = y = width = height = duration = life = rotation = 0f;
            velocityX = velocityY = 0f;
            startScale = endScale = 1f;
            red = green = blue = 1f;
        }
    }
}
