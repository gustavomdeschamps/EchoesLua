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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;

/** Abertura curta, temporal e sempre pulável. */
public final class IntroScreen implements Screen {
    private static final float DURATION = 5.8f;
    private final EchoesLua game;
    private final SpriteBatch batch;
    private final AssetManager assets;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
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
        float artW = 1328f;
        float artH = 747f;
        batch.setColor(.82f, .82f, .78f, 1f);
        batch.draw(assets.introKeyArtTexture, -24f - drift * 24f, -14f - drift * 12f, artW, artH);
        batch.setColor(Color.WHITE);
        drawAt(assets.font, "REGISTRO DE CAMPO  //  L-01", .62f, 1.05f, 3.7f, 884f, 644f,
            new Color(.82f, .49f, .28f, 1f));
        drawAt(assets.titleFont, "ECHOES", 2.05f, 3.35f, 5.8f, 882f, 596f, Color.WHITE);
        drawAt(assets.titleFont, "FASE LUNAR", .68f, 3.55f, 5.8f, 888f, 538f,
            new Color(.78f, .8f, .76f, 1f));
        batch.end();

        renderSignalTrace(MathUtils.clamp((elapsed - 1.25f) / .65f, 0f, 1f));

        float fadeIn = 1f - MathUtils.clamp(elapsed / .9f, 0f, 1f);
        float fadeOut = MathUtils.clamp((elapsed - 5.15f) / .65f, 0f, 1f);
        float veil = Math.max(fadeIn, Interpolation.pow2In.apply(fadeOut));
        if (veil > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0f, 0f, 0f, veil);
            shapes.rect(0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT / 2f * veil);
            shapes.rect(0f, GameConfig.WINDOW_HEIGHT - GameConfig.WINDOW_HEIGHT / 2f * veil,
                GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT / 2f * veil);
            shapes.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    private void renderSignalTrace(float alpha) {
        if (alpha <= 0f) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.27f, .73f, .72f, alpha * .56f);
        float startX = 506f;
        for (int i = 0; i < 44; i++) {
            float x = startX + i * 11f;
            float wave = MathUtils.sin(i * .67f + elapsed * 4f) * (i > 16 && i < 25 ? 9f : 2f);
            shapes.rect(x, 116f + wave, 2f, 2f + Math.abs(wave));
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
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
    @Override public void dispose() { shapes.dispose(); }
}
