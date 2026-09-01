package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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
    private final Texture pixel = solidPixel();
    private final Texture disc = softDisc();
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

        /*
         * Esta tela existe justamente porque os atlas ainda nao chegaram, entao
         * ela desenha com duas texturas minimas geradas em memoria, e nao com
         * ShapeRenderer: um renderer a mais so para a barra nao se paga.
         */
        String status = "PREPARANDO MISSÃO  " + MathUtils.round(displayedProgress * 100f) + "%";
        font.getData().setScale(.92f);
        font.setColor(new Color(.82f, .87f, .86f, reveal));
        layout.setText(font, status);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(.035f, .055f, .069f, 1f);
        batch.draw(pixel, 0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        batch.setColor(.08f, .11f, .13f, reveal);
        batch.draw(pixel, barX, barY, barW, barH);
        batch.setColor(.35f, .78f, .76f, reveal);
        batch.draw(pixel, barX, barY, barW * displayedProgress, barH);
        batch.setColor(.84f, .49f, .25f, reveal * .85f);
        batch.draw(disc, 584f, 344f, 112f, 112f);
        batch.setColor(.035f, .055f, .069f, 1f);
        batch.draw(disc, 604f, 360f, 108f, 108f);
        batch.setColor(.35f, .78f, .76f, reveal * .75f);
        batch.draw(pixel, 622f, 363f, 36f, 2f);
        batch.setColor(Color.WHITE);
        font.draw(batch, status, 640f - layout.width / 2f, 236f);
        batch.end();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void show() { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    /** Textura 1x1 usada como retangulo solido. */
    private static Texture solidPixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    /** Disco com borda suave, para o emblema circular da tela de carga. */
    private static Texture softDisc() {
        int size = 128;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float radius = size / 2f - 1f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - size / 2f + .5f;
                float dy = y - size / 2f + .5f;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float alpha = MathUtils.clamp(radius - distance, 0f, 1f);
                pixmap.setColor(1f, 1f, 1f, alpha);
                pixmap.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        pixel.dispose();
        disc.dispose();
        font.dispose();
    }
}
