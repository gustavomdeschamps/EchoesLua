package com.orion.echoes.lua;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;

import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.config.AppSettings;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.screens.LoadingScreen;

public class EchoesLua extends Game {

    private SpriteBatch batch;

    private AssetManager assets;

    private SoundManager sounds;
    private AppSettings settings;
    private CampaignState campaign;
    private Cursor defaultCursor;
    private Cursor targetCursor;

    @Override
    public void create() {

        batch =
            new SpriteBatch();

        assets =
            new AssetManager();

        assets.queue();

        settings = new AppSettings();
        campaign = new CampaignState();
        aplicarModoDeTela();

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

    /**
     * Aplica a preferencia de tela cheia.
     *
     * A opcao existia em AppSettings e era gravada, mas nada a lia: o jogo
     * abria sempre no modo do launcher e o botao nao mudava nada. Trocar de
     * modo so quando o estado difere evita recriar o contexto a toa.
     */
    public void aplicarModoDeTela() {
        if (settings == null || Gdx.graphics == null) return;
        // QA abre em janela de proposito; a preferencia salva nao pode desfazer isso.
        if (Boolean.getBoolean("echoes.windowed")) return;
        if (Gdx.graphics.isFullscreen() == settings.isFullscreen()) return;
        if (settings.isFullscreen()) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        }
    }

    /** Instala os dois cursores autorais fornecidos pelo pacote final. */
    public void installCustomCursors() {
        disposeCursors();
        try {
            Pixmap normal = new Pixmap(Gdx.files.internal("textures/ui/cursor_default.png"));
            Pixmap target = new Pixmap(Gdx.files.internal("textures/ui/cursor_target.png"));
            defaultCursor = Gdx.graphics.newCursor(normal, 3, 3);
            targetCursor = Gdx.graphics.newCursor(target, 16, 16);
            normal.dispose();
            target.dispose();
            useDefaultCursor();
        } catch (RuntimeException unsupported) {
            // Algumas plataformas não aceitam cursor customizado; o jogo deve
            // continuar funcional com o cursor nativo.
            disposeCursors();
        }
    }

    public void useDefaultCursor() {
        if (defaultCursor != null) Gdx.graphics.setCursor(defaultCursor);
    }

    public void useTargetCursor() {
        if (targetCursor != null) Gdx.graphics.setCursor(targetCursor);
    }

    private void disposeCursors() {
        if (defaultCursor != null) defaultCursor.dispose();
        if (targetCursor != null) targetCursor.dispose();
        defaultCursor = null;
        targetCursor = null;
    }

    @Override
    public void dispose() {

        super.dispose();
        disposeCursors();

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
