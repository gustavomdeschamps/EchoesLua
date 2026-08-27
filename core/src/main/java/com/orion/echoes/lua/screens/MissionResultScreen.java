package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.ui.TerminalUi;
import com.orion.echoes.lua.ui.UiTheme;

/** Encerramento com composicao de capa, sem o painel generico central. */
abstract class MissionResultScreen implements Screen {
    protected final EchoesLua game;
    private final float missionTime;
    private final boolean success;
    private final Rectangle retryButton = new Rectangle(74f, 54f, 270f, 52f);
    private final Rectangle menuButton = new Rectangle(362f, 54f, 190f, 52f);
    private final Vector2 pointer = new Vector2();
    private TerminalUi ui;
    private float revealTime;
    private int selected;

    MissionResultScreen(EchoesLua game, float missionTime, boolean success) {
        this.game = game;
        this.missionTime = missionTime;
        this.success = success;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
        ui = new TerminalUi(game.getBatch(), game.getAssets());
        if (success) game.getSounds().tocarVitoria(); else game.getSounds().tocarGameOver();
    }

    @Override
    public void render(float delta) {
        revealTime = Math.min(.55f, revealTime + Math.min(delta, 1f / 30f));
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        ui.unproject(pointer);
        if (retryButton.contains(pointer)) selected = 0;
        if (menuButton.contains(pointer)) selected = 1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) selected = 1 - selected;
        Color accent = success ? UiTheme.GREEN : UiTheme.RED;
        Texture background = success ? game.getAssets().marsBackgroundTexture : game.getAssets().backgroundLuaTexture;
        Color tint = success ? new Color(.5f, .31f, .2f, 1f) : new Color(.14f, .11f, .14f, 1f);
        ui.clear(UiTheme.VOID);
        ui.image(background, 0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, tint);

        float reveal = Interpolation.pow3Out.apply(revealTime / .55f);
        ui.beginShapes();
        ui.rect(0f, 0f, GameConfig.WINDOW_WIDTH, 250f, new Color(.018f, .026f, .032f, .95f));
        ui.rect(0f, 248f, GameConfig.WINDOW_WIDTH * reveal, 4f, accent);
        drawButton(retryButton, selected == 0, accent);
        drawButton(menuButton, selected == 1, accent);
        ui.endShapes();

        ui.beginText();
        ui.text(success ? "MARTE" : "SINAL PERDIDO", .82f, accent, 74f, 662f);
        ui.title(success ? "MISSÃO CONCLUÍDA" : "O OXIGÊNIO ACABOU", 1.8f, UiTheme.TEXT, 70f, 600f);
        ui.text(success ? "A colônia voltou a transmitir." : "A Lua permanece em silêncio.",
            1.0f, UiTheme.TEXT_MUTED, 74f, 552f);
        ui.text(String.format("Tempo de operação  %.1f s", missionTime), .8f, UiTheme.TEXT_MUTED, 74f, 199f);
        ui.text("JOGAR NOVAMENTE", .88f, selected == 0 ? UiTheme.TEXT : UiTheme.TEXT_MUTED, 98f, 87f);
        ui.text("MENU", .88f, selected == 1 ? UiTheme.TEXT : UiTheme.TEXT_MUTED, 386f, 87f);
        ui.endText();
        handleInput();
    }

    private void drawButton(Rectangle bounds, boolean active, Color accent) {
        ui.rect(bounds.x, bounds.y, bounds.width, bounds.height,
            active ? new Color(.2f, .27f, .3f, .94f) : new Color(.04f, .06f, .075f, .86f));
        if (active) {
            ui.rect(bounds.x + 18f, bounds.y, 64f, 3f, accent);
            ui.rect(bounds.x + bounds.width - 18f, bounds.y + bounds.height - 10f, 7f, 7f, accent);
        }
    }

    private void handleInput() {
        boolean click = Gdx.input.justTouched()
            && (retryButton.contains(pointer) || menuButton.contains(pointer));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) selected = 1;
        if (!(click || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))) return;
        if (selected == 0) {
            game.getSounds().tocarInicio();
            game.setScreen(new LunarScreen(game, game.getBatch(), game.getAssets()));
        } else {
            game.setScreen(new MenuScreen(game));
        }
        dispose();
    }

    @Override public void resize(int width, int height) { ui.resize(width, height); }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { if (ui != null) { ui.dispose(); ui = null; } }
}
