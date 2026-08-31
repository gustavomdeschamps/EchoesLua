package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
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
    private final TextureRegion[][] actionFrames = new TextureRegion[4][6];
    private final TextureRegion[][] energyFrames = new TextureRegion[4][6];
    private final Pool<VisualFx> visualPool = new Pool<>(24, 96) {
        @Override protected VisualFx newObject() { return new VisualFx(); }
    };

    public ParticleManager(AssetManager assets) {

        effects =
            new Array<>();

        pools = new ObjectMap<>();
        templates = new ObjectMap<>();
        for (int row = 0; row < 4; row++) for (int column = 0; column < 6; column++) {
            actionFrames[row][column] = assets.actionFxFrame(column, row);
            energyFrames[row][column] = assets.energyFxFrame(column, row);
        }
    }

    // ==========================================
    // POEIRA LUNAR
    // ==========================================

    public void criarPoeiraLunar(
        float x,
        float y
    ) {

        spawnAnimated(actionFrames[0], x - 36f, y - 22f, 72f, 54f, .34f,
            MathUtils.random(-5f, 5f), .82f, 1.08f, false, 1f, 1f, 1f);
    }

    public void criarPoeiraLunar(float x, float y, boolean running, float dirX, float dirY) {
        spawnAnimated(actionFrames[running ? 1 : 0], x - dirX * 22f - 44f, y - 26f,
            running ? 92f : 76f, running ? 66f : 54f, running ? .38f : .32f,
            -dirX * 4f, .78f, 1.12f, false, 1f, 1f, 1f);
    }

    public void criarPoeiraMarte(float x, float y, boolean running, float dirX, float dirY) {
        spawnAnimated(actionFrames[running ? 1 : 0], x - dirX * 24f - 46f, y - 28f,
            running ? 96f : 78f, running ? 68f : 56f, running ? .4f : .34f,
            -dirX * 5f, .8f, 1.16f, false, 1f, .55f, .35f);
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
        criarEfeito("particles/faisca.p", x, y + 3f);
        spawnAnimated(actionFrames[2], x - 58f, y - 48f, 116f, 116f, .48f,
            0f, .72f, 1.08f, true, 1f, 1f, 1f);
    }

    public void criarImpactoTraje(float x, float y) {
        spawnAnimated(energyFrames[3], x - 52f, y - 52f, 104f, 104f, .34f,
            MathUtils.random(-7f, 7f), .72f, 1.05f, true, 1f, 1f, 1f);
        criarEfeito("particles/faisca.p", x, y);
    }

    private void spawnAnimated(TextureRegion[] frames, float x, float y, float width, float height,
                               float duration, float rotation, float startScale, float endScale,
                               boolean additive, float red, float green, float blue) {
        VisualFx fx = visualPool.obtain();
        fx.frames = frames;
        fx.x = x;
        fx.y = y;
        fx.width = width;
        fx.height = height;
        fx.duration = duration;
        fx.life = duration;
        fx.rotation = rotation;
        fx.startScale = startScale;
        fx.endScale = endScale;
        fx.additive = additive;
        fx.red = red;
        fx.green = green;
        fx.blue = blue;
        visualEffects.add(fx);
    }

    public void criarImpactoTiro(float x, float y) {
        spawnAnimated(actionFrames[3], x - 44f, y - 44f, 88f, 88f, .3f,
            MathUtils.random(0f, 360f), .7f, 1.08f, true, 1f, 1f, 1f);
    }

    public void criarMorteInimigo(float x, float y) {
        spawnAnimated(energyFrames[0], x - 68f, y - 68f, 136f, 136f, .52f,
            0f, .72f, 1.12f, true, 1f, 1f, 1f);
    }

    public void criarMuzzleFlash(float x, float y, float angle) {
        spawnAnimated(energyFrames[1], x - 38f, y - 38f, 76f, 76f, .16f,
            angle, .78f, 1.05f, true, 1f, 1f, 1f);
    }

    public void criarPortal(float x, float y) {
        spawnAnimated(energyFrames[2], x - 92f, y - 92f, 184f, 184f, .72f,
            0f, .72f, 1.12f, true, 1f, 1f, 1f);
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
        spawnAnimated(actionFrames[2], x - 48f, y - 48f, 96f, 96f, .42f,
            0f, .76f, 1.05f, true, 1f, 1f, 1f);
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
        spawnAnimated(energyFrames[3], x - 54f, y - 54f, 108f, 108f, .46f,
            0f, .8f, 1.08f, true, 1f, 1f, 1f);
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
            int frameIndex = Math.min(fx.frames.length - 1, (int)(progress * fx.frames.length));
            if (fx.additive) batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(fx.red, fx.green, fx.blue, alpha);
            batch.draw(fx.frames[frameIndex], fx.x, fx.y, fx.width / 2f, fx.height / 2f,
                fx.width, fx.height, scale, scale, fx.rotation);
            if (fx.additive) batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
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
        TextureRegion[] frames;
        float x, y, width, height, duration, life, rotation;
        float velocityX, velocityY, startScale, endScale;
        float red = 1f, green = 1f, blue = 1f;
        boolean additive;
        @Override public void reset() {
            frames = null;
            x = y = width = height = duration = life = rotation = 0f;
            velocityX = velocityY = 0f;
            startScale = endScale = 1f;
            red = green = blue = 1f;
            additive = false;
        }
    }
}
