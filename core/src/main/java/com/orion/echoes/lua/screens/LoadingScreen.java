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

/**
 * Abertura sobre a imagem de apresentacao.
 *
 * A antiga tela da lua desenhada em codigo saiu: o jogo abre direto na key
 * art e so mantem uma linha de progresso discreta no rodape, porque os
 * atlas ainda estao carregando e o render precisa continuar respondendo.
 * A arte e carregada em Texture propria justamente por isso - o atlas de UI
 * que a contem so fica pronto no fim deste carregamento.
 */
public final class LoadingScreen implements Screen {
    private static final float MINIMUM_DISPLAY_TIME = .55f;
    private final EchoesLua game;
    private final SpriteBatch batch;
    private final Texture pixel = solidPixel();
    private final Texture keyArt = carregarKeyArt();
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
        float barY = 58f;
        float barW = 368f;
        float barH = 4f;

        String status = "PREPARANDO MISSÃO  " + MathUtils.round(displayedProgress * 100f) + "%";
        font.getData().setScale(.86f);
        font.setColor(new Color(.82f, .87f, .86f, reveal * .9f));
        layout.setText(font, status);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // A key art cobre a tela inteira; o leve escurecimento existe so para
        // o texto do rodape continuar legivel sobre o regolito claro.
        batch.setColor(reveal, reveal, reveal, 1f);
        batch.draw(keyArt, 0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        batch.setColor(.01f, .014f, .02f, .55f * reveal);
        batch.draw(pixel, 0f, 0f, GameConfig.WINDOW_WIDTH, 118f);
        batch.setColor(.08f, .11f, .13f, reveal * .85f);
        batch.draw(pixel, barX, barY, barW, barH);
        batch.setColor(.35f, .78f, .76f, reveal);
        batch.draw(pixel, barX, barY, barW * displayedProgress, barH);
        batch.setColor(Color.WHITE);
        font.draw(batch, status, 640f - layout.width / 2f, 92f);
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

    /** Key art da abertura, carregada direto do arquivo porque o atlas ainda nao existe. */
    private static Texture carregarKeyArt() {
        Texture texture = new Texture(Gdx.files.internal("textures/intro_keyart_v2.png"));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    @Override
    public void dispose() {
        pixel.dispose();
        keyArt.dispose();
        font.dispose();
    }
}
