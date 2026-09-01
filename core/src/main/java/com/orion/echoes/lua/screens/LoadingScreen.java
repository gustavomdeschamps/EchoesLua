package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
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

/** Tela funcional: mantém o render responsivo enquanto o AssetManager trabalha. */
public final class LoadingScreen implements Screen {
    private static final float MINIMUM_DISPLAY_TIME = .55f;
    private final EchoesLua game;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(
        GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
    private float elapsed;
    private float displayedProgress;
    private boolean leaving;

    public LoadingScreen(EchoesLua game) {
        this.game = game;
        this.batch = game.getBatch();
        camera.position.set(GameConfig.WINDOW_WIDTH / 2f, GameConfig.WINDOW_HEIGHT / 2f, 0f);
        camera.update();
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(delta, GameConfig.MAX_FRAME_DELTA);
        elapsed += safeDelta;
        boolean loaded = game.getAssets().update();
        displayedProgress = MathUtils.lerp(displayedProgress,
            game.getAssets().getProgress(), 1f - (float) Math.pow(.001f, safeDelta));

        Gdx.gl.glClearColor(.012f, .017f, .024f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        drawInterface();

        if (!leaving && loaded && elapsed >= MINIMUM_DISPLAY_TIME) {
            leaving = true;
            game.getSounds().load();
            dispose();
            game.setScreen(new IntroScreen(game));
        }
    }

    private void drawInterface() {
        float reveal = Interpolation.fade.apply(MathUtils.clamp(elapsed / .42f, 0f, 1f));
        float barX = 456f;
        float barY = 264f;
        float barW = 368f;
        float barH = 5f;

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.035f, .055f, .069f, 1f);
        shapes.rect(0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        shapes.setColor(.08f, .11f, .13f, reveal);
        shapes.rect(barX, barY, barW, barH);
        shapes.setColor(.35f, .78f, .76f, reveal);
        shapes.rect(barX, barY, barW * displayedProgress, barH);
        shapes.setColor(.84f, .49f, .25f, reveal * .85f);
        shapes.circle(640f, 400f, 56f, 48);
        shapes.setColor(.035f, .055f, .069f, 1f);
        shapes.circle(658f, 414f, 54f, 48);
        shapes.setColor(.35f, .78f, .76f, reveal * .75f);
        shapes.rect(622f, 363f, 36f, 2f);
        shapes.end();

        String status = "PREPARANDO MISSÃO  " + MathUtils.round(displayedProgress * 100f) + "%";
        font.getData().setScale(.92f);
        font.setColor(new Color(.82f, .87f, .86f, reveal));
        layout.setText(font, status);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.draw(batch, status, 640f - layout.width / 2f, 236f);
        batch.end();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void show() { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        shapes.dispose();
        font.dispose();
    }
}
