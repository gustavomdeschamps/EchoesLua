package com.orion.echoes.lua;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.screens.MenuScreen;

public class EchoesLua extends Game {

    private SpriteBatch batch;

    private AssetManager assets;

    private SoundManager sounds;

    @Override
    public void create() {

        batch =
            new SpriteBatch();

        assets =
            new AssetManager();

        assets.load();

        sounds =
            SoundManager.getInstance();

        sounds.load();

        setScreen(
            new MenuScreen(this)
        );
    }

    public SpriteBatch getBatch() {

        return batch;
    }

    public AssetManager getAssets() {

        return assets;
    }

    public SoundManager getSounds() {

        return sounds;
    }

    @Override
    public void dispose() {

        super.dispose();

        if (sounds != null) {
            sounds.dispose();
        }

        if (assets != null) {
            assets.dispose();
        }

        if (batch != null) {
            batch.dispose();
        }
    }
}
