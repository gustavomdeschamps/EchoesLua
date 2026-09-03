package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;

/** Abertura curta, temporal e sempre pulável. */
public final class IntroScreen implements Screen {
    private static final float DURATION = 5.8f;
    /** Pulsos de radio disparados em sequencia a partir do receptor. */
    private static final int RADIO_PULSES = 4;
    private static final float SCANLINE_STEP = 4f;
    /** Altura das barras de cinema: fechadas na entrada, finas durante a cena. */
    private static final float LETTERBOX_MAX = GameConfig.WINDOW_HEIGHT / 2f;
    private static final float LETTERBOX_MIN = 46f;
    private final EchoesLua game;
    private final SpriteBatch batch;
    private final AssetManager assets;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
    private final GlyphLayout layout = new GlyphLayout();
    private float elapsed;
    private boolean finished;

    public IntroScreen(EchoesLua game) {
        this.game = game;
        this.batch = game.getBatch();
        this.assets = game.getAssets();
        camera.position.set(GameConfig.WINDOW_WIDTH / 2f, GameConfig.WINDOW_HEIGHT / 2f, 0f);
        camera.update();
    }

    @Override
    public void render(float delta) {
        elapsed += Math.min(delta, 1f / 30f);
        if (elapsed > .35f && (Gdx.input.justTouched()
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))) {
            finish();
            return;
        }
        if (elapsed >= DURATION) {
            finish();
            return;
        }

        Gdx.gl.glClearColor(.005f, .007f, .012f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float drift = Interpolation.pow2Out.apply(MathUtils.clamp(elapsed / DURATION, 0f, 1f));
        float signalKick = MathUtils.clamp(1f - Math.abs(elapsed - 1.48f) / .16f, 0f, 1f);
        float jitter = MathUtils.sin(elapsed * 91f) * signalKick * 3f;
        float artW = 1328f;
        float artH = 747f;
        float artX = -24f - drift * 24f + jitter;
        float artY = -14f - drift * 12f;
        desenharKeyArt(artX, artY, artW, artH, signalKick);
        drawAt(assets.font, "REGISTRO DE CAMPO  //  L-01", .62f, 1.05f, 3.7f, 884f, 644f,
            new Color(.82f, .49f, .28f, 1f));
        drawAt(assets.titleFont, "ECHOES", 2.05f, 3.35f, 5.8f, 882f, 596f, Color.WHITE);
        drawAt(assets.titleFont, "FASE LUNAR", .68f, 3.55f, 5.8f, 888f, 538f,
            new Color(.78f, .8f, .76f, 1f));
        batch.end();

        renderRadioArcs();
        renderSignalTrace(MathUtils.clamp((elapsed - 1.25f) / .65f, 0f, 1f));
        renderScanlines();
        renderFilmGrain();
        renderVignette();
        renderLetterbox();
        renderSignalFlash(signalKick);

    }

    /**
     * Key art com aberracao cromatica no pico do sinal.
     *
     * Tres passagens deslocadas nos canais vermelho e azul: e o mesmo truque
     * de abertura de jogo comercial, e custa duas chamadas de desenho a mais
     * apenas durante o estalo. Fora do pico, desenha uma vez so.
     */
    private void desenharKeyArt(float x, float y, float width, float height, float kick) {
        if (kick <= .01f) {
            batch.setColor(.82f, .82f, .78f, 1f);
            batch.draw(assets.introKeyArtTexture, x, y, width, height);
            batch.setColor(Color.WHITE);
            return;
        }
        float split = kick * 6f;
        batch.setColor(.85f, .28f, .24f, .55f);
        batch.draw(assets.introKeyArtTexture, x - split, y, width, height);
        batch.setColor(.26f, .58f, .88f, .55f);
        batch.draw(assets.introKeyArtTexture, x + split, y, width, height);
        batch.setColor(.82f, .82f, .78f, 1f);
        batch.draw(assets.introKeyArtTexture, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    /**
     * Arcos concentricos de radio saindo do receptor.
     *
     * E a assinatura da marca definida no ART_BIBLE, que ate agora so existia
     * no documento. Os arcos sao incompletos de proposito, como manda a
     * regra, e nascem em pulsos sucessivos a partir do momento do sinal.
     */
    private void renderRadioArcs() {
        float start = 1.15f;
        if (elapsed < start) return;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float originX = 470f;
        float originY = 150f;
        for (int pulse = 0; pulse < RADIO_PULSES; pulse++) {
            float age = elapsed - start - pulse * .52f;
            if (age <= 0f) continue;
            float life = MathUtils.clamp(age / 2.2f, 0f, 1f);
            if (life >= 1f) continue;
            float radius = 40f + Interpolation.pow2Out.apply(life) * 320f;
            float alpha = (1f - life) * .5f;
            arco(originX, originY, radius, -32f, 150f, 2.5f,
                new Color(.27f, .73f, .72f, alpha));
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Arco incompleto desenhado por segmentos curtos e rotacionados. */
    private void arco(float centerX, float centerY, float radius,
                      float startDegrees, float sweepDegrees, float thickness, Color color) {
        int segments = Math.max(8, (int) (sweepDegrees / 4f));
        float step = sweepDegrees / segments;
        float chord = 2f * radius * MathUtils.sinDeg(step / 2f) + 1f;
        batch.setColor(color);
        for (int index = 0; index < segments; index++) {
            float angle = startDegrees + step * (index + .5f);
            float x = centerX + MathUtils.cosDeg(angle) * radius;
            float y = centerY + MathUtils.sinDeg(angle) * radius;
            batch.draw(assets.uiWhiteTexture, x, y - thickness / 2f,
                0f, thickness / 2f, chord, thickness, 1f, 1f, angle + 90f);
        }
    }

    /** Linhas horizontais finas descendo devagar, como um monitor antigo. */
    private void renderScanlines() {
        float presence = envelope(.5f, 5.4f, .6f) * .1f;
        if (presence <= 0f) return;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(.62f, .78f, .82f, presence);
        float offset = (elapsed * 22f) % SCANLINE_STEP;
        for (float y = -SCANLINE_STEP + offset; y < GameConfig.WINDOW_HEIGHT; y += SCANLINE_STEP) {
            rect(0f, y, GameConfig.WINDOW_WIDTH, 1f);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Escurecimento das bordas: concentra a leitura no centro da cena. */
    private void renderVignette() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(.04f, .05f, .07f, .62f);
        batch.draw(assets.uiDamageVignetteTexture, 0f, 0f,
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Barras de cinema que abrem na entrada e fecham na saida. */
    private void renderLetterbox() {
        float open = Interpolation.pow3Out.apply(MathUtils.clamp(elapsed / .8f, 0f, 1f));
        float closing = Interpolation.pow2In.apply(
            MathUtils.clamp((elapsed - 5.15f) / .65f, 0f, 1f));
        float bar = LETTERBOX_MAX * (1f - open) + LETTERBOX_MIN * open
            + (GameConfig.WINDOW_HEIGHT / 2f - LETTERBOX_MIN) * closing;
        if (bar <= 0f) return;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(0f, 0f, 0f, 1f);
        rect(0f, 0f, GameConfig.WINDOW_WIDTH, bar);
        rect(0f, GameConfig.WINDOW_HEIGHT - bar, GameConfig.WINDOW_WIDTH, bar);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Estalo branco curto no instante em que o sinal chega. */
    private void renderSignalFlash(float kick) {
        if (kick <= .02f) return;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(.86f, .93f, 1f, kick * .18f);
        rect(0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Retangulo solido no proprio batch; evita alternar para ShapeRenderer. */
    private void rect(float x, float y, float width, float height) {
        batch.draw(assets.uiWhiteTexture, x, y, width, height);
    }

    private void renderFilmGrain() {
        float presence = envelope(.42f, 5.3f, .5f);
        if (presence <= 0f) return;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (int i = 0; i < 18; i++) {
            float x = Math.floorMod(i * 173 + (int) (elapsed * (17 + i % 4)), 1280);
            float y = Math.floorMod(i * 97 + (int) (elapsed * (9 + i % 3)), 720);
            float alpha = (.025f + (i % 5) * .008f) * presence;
            batch.setColor(.86f, .79f, .68f, alpha);
            rect(x, y, i % 4 == 0 ? 2f : 1f, i % 3 == 0 ? 2f : 1f);
        }
        float scan = MathUtils.clamp((elapsed - .8f) / 4f, 0f, 1f);
        batch.setColor(.86f, .46f, .25f, .12f * presence);
        rect(0f, 710f - scan * 700f, 1280f, 2f);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void renderSignalTrace(float alpha) {
        if (alpha <= 0f) return;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(.27f, .73f, .72f, alpha * .56f);
        float startX = 506f;
        for (int i = 0; i < 44; i++) {
            float x = startX + i * 11f;
            float wave = MathUtils.sin(i * .67f + elapsed * 4f) * (i > 16 && i < 25 ? 9f : 2f);
            rect(x, 116f + wave, 2f, 2f + Math.abs(wave));
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawAt(BitmapFont font, String text, float scale, float start, float end,
                        float x, float y, Color color) {
        float alpha = envelope(start, end, .38f);
        if (alpha <= 0f) return;
        font.getData().setScale(scale);
        font.setColor(color.r, color.g, color.b, alpha);
        font.draw(batch, text, x, y);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private float envelope(float start, float end, float edge) {
        if (elapsed <= start || elapsed >= end) return 0f;
        return Math.min(MathUtils.clamp((elapsed - start) / edge, 0f, 1f),
            MathUtils.clamp((end - elapsed) / edge, 0f, 1f));
    }

    private void finish() {
        if (finished) return;
        finished = true;
        game.setScreen(new MenuScreen(game));
        dispose();
    }

    @Override public void show() { }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { }
}
