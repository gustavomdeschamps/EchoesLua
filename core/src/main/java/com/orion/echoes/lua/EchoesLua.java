package com.orion.echoes.lua;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.config.AppSettings;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.screens.LoadingScreen;

public class EchoesLua extends Game {

    private SpriteBatch batch;

    private AssetManager assets;

    private SoundManager sounds;
    private AppSettings settings;
    private CampaignState campaign;

    @Override
    public void create() {

        batch =
            new SpriteBatch();

        assets =
            new AssetManager();

        assets.queue();

        settings = new AppSettings();
        campaign = new CampaignState();

        sounds =
            SoundManager.getInstance();

        setScreen(
            new LoadingScreen(this)
        );
    }

    @Override
    public void render() {
        /*
         * A trilha avanca no relogio real, antes da tela.
         * Assim fades e ducking continuam corretos em pausa,
         * hitstop e telas sem gameplay.
         */
        if (sounds != null) {
            sounds.update(com.badlogic.gdx.Gdx.graphics.getDeltaTime());
        }
        super.render();
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

    public AppSettings getSettings() { return settings; }

    /** Campanha em curso: sobrevive as trocas de fase e ao portal de volta. */
    public CampaignState getCampaign() { return campaign; }

    public void setCampaign(CampaignState value) {
        campaign = value == null ? new CampaignState() : value;
    }

    /** Zera a campanha; usado por "novo jogo" e pela tela de resultado. */
    public CampaignState startNewCampaign() {
        campaign = new CampaignState();
        return campaign;
    }

    /** Reaplica o mixer depois de qualquer mudanca na tela de opcoes. */
    public void aplicarPreferenciasDeAudio() {
        if (sounds != null) sounds.applySettings(settings);
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
