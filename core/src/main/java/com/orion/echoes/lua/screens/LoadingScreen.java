package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.orion.echoes.lua.EchoesLua;

/**
 * Carrega os atlas sem exibir uma segunda arte de abertura. O primeiro quadro
 * visível da apresentação pertence exclusivamente ao vídeo de IntroScreen.
 */
public final class LoadingScreen implements Screen {
    private final EchoesLua game;
    private boolean leaving;

    public LoadingScreen(EchoesLua game) {
        this.game = game;
    }

    @Override
    public void render(float delta) {
        boolean loaded = game.getAssets().update();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (!leaving && loaded) {
            leaving = true;
            game.getSounds().load();
            game.installCustomCursors();
            dispose();
            game.setScreen(Boolean.getBoolean("echoes.spriteQa")
                ? new SpriteQaScreen(game) : new IntroScreen(game));
        }
    }

    @Override public void resize(int width, int height) { }
    @Override public void show() { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override public void dispose() { }
}
