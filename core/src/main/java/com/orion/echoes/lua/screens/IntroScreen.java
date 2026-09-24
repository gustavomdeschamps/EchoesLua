package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;

/**
 * Reproduz a abertura enviada pelo autor. O MP4 foi convertido em quadros JPEG
 * e áudio Ogg para funcionar no LibGDX sem decoder nativo extra. Apenas um
 * quadro fica na GPU por vez; os 240 quadros não consomem centenas de MB.
 */
public final class IntroScreen implements Screen {
    // frame_001 is an unrelated still image; the actual movie starts at 002.
    private static final int FIRST_VIDEO_FRAME = 2;
    private static final int FRAME_COUNT = 239;
    private static final float FPS = 24f;
    private static final float DURATION = FRAME_COUNT / FPS;
    private final EchoesLua game;
    private final SpriteBatch batch;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(
        GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
    private Texture frame;
    private Music audio;
    private int loadedFrame = -1;
    private float elapsed;
    private boolean finished;
    private boolean skipRequested;

    public IntroScreen(EchoesLua game) {
        this.game = game;
        this.batch = game.getBatch();
        camera.position.set(GameConfig.WINDOW_WIDTH / 2f,
            GameConfig.WINDOW_HEIGHT / 2f, 0f);
        camera.update();
    }

    @Override public void show() {
        game.getSounds().pararMusicaMenu();
        loadFrame(FIRST_VIDEO_FRAME);
        audio = Gdx.audio.newMusic(Gdx.files.internal("video/intro/audio.ogg"));
        audio.setVolume(game.getSettings().getMusicVolume());
        audio.play();
    }

    @Override public void render(float delta) {
        if (!game.getAssets().isReady()) game.getAssets().update();
        // Tempo real: limitar delta atrasaria o vídeo em relação ao áudio.
        elapsed += Math.max(delta, 0f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.justTouched()) skipRequested = true;
        if ((skipRequested || elapsed >= DURATION) && game.getAssets().isReady()) {
            finish();
            return;
        }
        int index = Math.min(FIRST_VIDEO_FRAME + FRAME_COUNT - 1,
            FIRST_VIDEO_FRAME + (int)(elapsed * FPS));
        if (index != loadedFrame) loadFrame(index);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(frame, 0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        float fadeIn = 1f - MathUtils.clamp(elapsed / .35f, 0f, 1f);
        float fadeOut = MathUtils.clamp((elapsed - DURATION + .5f) / .5f, 0f, 1f);
        float fade = Math.max(fadeIn, fadeOut);
        if (fade > 0f && game.getAssets().isReady()) {
            batch.setColor(0f, 0f, 0f, fade);
            batch.draw(game.getAssets().uiWhiteTexture, 0f, 0f,
                GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void loadFrame(int index) {
        Texture next = new Texture(Gdx.files.internal(
            String.format("video/intro/frame_%03d.jpg", index)));
        next.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        if (frame != null) frame.dispose();
        frame = next;
        loadedFrame = index;
    }

    private void finish() {
        if (finished) return;
        finished = true;
        game.getSounds().load();
        game.installCustomCursors();
        game.setScreen(new MenuScreen(game));
        dispose();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() { if (audio != null) audio.pause(); }
    @Override public void resume() { if (audio != null) audio.play(); }
    @Override public void hide() { }
    @Override public void dispose() {
        if (frame != null) { frame.dispose(); frame = null; }
        if (audio != null) { audio.dispose(); audio = null; }
    }
}
