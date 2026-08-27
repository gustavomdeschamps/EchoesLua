package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.ui.TerminalUi;
import com.orion.echoes.lua.ui.UiTheme;

/** Tela inicial de jogo: arte em destaque, poucas opcoes e ajuda separada. */
public final class MenuScreen implements Screen {
    private enum Page { MAIN, HOW_TO_PLAY, SETTINGS }

    private final EchoesLua game;
    private final Rectangle[] buttons = new Rectangle[4];
    private final Vector2 pointer = new Vector2();
    private final Color backgroundTint = new Color(.18f, .23f, .29f, 1f);
    private final Color heroTint = new Color(.82f, .8f, .74f, .96f);
    private TerminalUi ui;
    private Page page = Page.MAIN;
    private int selected;
    private float time;

    public MenuScreen(EchoesLua game) {
        this.game = game;
        for (int i = 0; i < buttons.length; i++) buttons[i] = new Rectangle();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
        ui = new TerminalUi(game.getBatch(), game.getAssets());
        game.getSounds().tocarMusicaMenu();
        layoutMainButtons();
    }

    @Override
    public void render(float delta) {
        time += Math.min(delta, 1f / 30f);
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        ui.unproject(pointer);
        updateSelection();

        ui.clear(UiTheme.VOID);
        ui.image(game.getAssets().backgroundLuaTexture, 0f, 0f,
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, backgroundTint);
        float reveal = Interpolation.pow3Out.apply(Math.min(1f, time / .5f));
        ui.beginShapes();
        ui.rect(0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT,
            new Color(.015f, .025f, .035f, page == Page.MAIN ? .48f : .82f));
        if (page == Page.MAIN) {
            ui.rect(0f, 0f, 548f, GameConfig.WINDOW_HEIGHT, new Color(.025f, .04f, .052f, .94f));
        }
        drawCurrentPageShapes(reveal);
        ui.endShapes();

        if (page == Page.MAIN) {
            float drift = MathUtils.sin(time * .42f) * 5f;
            // O menu compartilha a fita/gravador da abertura em vez de um emblema isolado.
            ui.image(game.getAssets().introKeyArtTexture, 548f + drift, 0f, 1280f, 720f, heroTint);
        }
        ui.beginText();
        drawCurrentPageText();
        ui.endText();
        if (reveal < 1f) {
            ui.beginShapes();
            ui.rect(0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT,
                new Color(.005f, .008f, .012f, (1f - reveal) * .72f));
            ui.endShapes();
        }
        handleInput();
    }

    private void drawCurrentPageShapes(float reveal) {
        if (page == Page.MAIN) {
            for (int i = 0; i < 4; i++) drawButton(buttons[i], i == selected, reveal,
                i == 0 ? UiTheme.CYAN : i == 1 ? UiTheme.GREEN : i == 2 ? UiTheme.AMBER : UiTheme.RED);
        } else if (page == Page.HOW_TO_PLAY) {
            ui.rect(58f, 402f, 548f, 126f, new Color(.05f, .15f, .19f, .95f));
            ui.rect(628f, 402f, 594f, 126f, new Color(.14f, .11f, .04f, .95f));
            ui.rect(58f, 244f, 548f, 126f, new Color(.08f, .13f, .09f, .95f));
            ui.rect(628f, 244f, 594f, 126f, new Color(.14f, .06f, .08f, .95f));
            drawButton(buttons[0], selected == 0, reveal, UiTheme.CYAN);
        } else {
            ui.rect(58f, 300f, 760f, 190f, new Color(.045f, .11f, .15f, .96f));
            ui.rect(90f, 401f, 680f, 14f, UiTheme.TRACK);
            ui.rect(90f, 401f, 680f * game.getSounds().getVolumeGeral(), 14f, UiTheme.CYAN);
            ui.rect(850f, 300f, 372f, 190f, new Color(.15f, .09f, .035f, .96f));
            drawButton(buttons[0], selected == 0, reveal, UiTheme.CYAN);
            drawButton(buttons[1], selected == 1, reveal, UiTheme.CYAN);
            drawButton(buttons[2], selected == 2, reveal, UiTheme.AMBER);
            drawButton(buttons[3], selected == 3, reveal, UiTheme.TEXT_MUTED);
        }
    }

    private void drawButton(Rectangle button, boolean active, float reveal, Color accent) {
        float pulse = active ? .86f + MathUtils.sin(time * 5f) * .08f : .78f;
        ui.rect(button.x + 4f, button.y - 4f, button.width, button.height,
            new Color(.005f, .012f, .018f, reveal * .82f));
        ui.rect(button.x, button.y, button.width, button.height,
            active ? new Color(accent.r * .28f, accent.g * .28f, accent.b * .28f, pulse * reveal)
                : new Color(.05f, .075f, .09f, .9f * reveal));
        if (active) {
            ui.rect(button.x + 18f, button.y, 72f, 3f, accent);
            ui.rect(button.x + button.width - 20f, button.y + button.height - 12f, 8f, 8f, accent);
        }
    }

    private void drawCurrentPageText() {
        if (page == Page.MAIN) {
            ui.title("ECHOES", 2.6f, UiTheme.TEXT, 56f, 632f);
            ui.title("FASE LUNAR", .76f, UiTheme.CYAN, 62f, 570f);
            String[] labels = {"JOGAR", "COMO JOGAR", "CONFIGURAÇÕES", "SAIR"};
            for (int i = 0; i < labels.length; i++) {
                ui.title(labels[i], i == selected ? .78f : .68f,
                    i == selected ? UiTheme.TEXT : UiTheme.TEXT_MUTED,
                    buttons[i].x + 24f, buttons[i].y + 36f);
            }
        } else if (page == Page.HOW_TO_PLAY) {
            ui.title("COMO JOGAR", 1.28f, UiTheme.TEXT, 58f, 625f);
            ui.text("MOVIMENTO", .72f, UiTheme.CYAN, 88f, 492f);
            ui.title("WASD ou setas · SHIFT para correr", .62f, UiTheme.TEXT, 88f, 454f);
            ui.text("INTERAÇÃO", .72f, UiTheme.AMBER, 658f, 492f);
            ui.title("E perto de estações e do portal", .64f, UiTheme.TEXT, 658f, 454f);
            ui.text("MIRA E DISPARO", .72f, UiTheme.GREEN, 88f, 334f);
            ui.title("Mouse + botão esquerdo", .68f, UiTheme.TEXT, 88f, 296f);
            ui.text("OBJETIVO", .72f, UiTheme.RED, 658f, 334f);
            ui.title("Reative 3 sistemas e alcance Marte", .62f, UiTheme.TEXT, 658f, 296f);
            ui.title("VOLTAR", .68f, UiTheme.TEXT, buttons[0].x + 24f, buttons[0].y + 35f);
        } else {
            ui.title("CONFIGURAÇÕES", 1.28f, UiTheme.TEXT, 58f, 625f);
            ui.text("VOLUME GERAL", .76f, UiTheme.TEXT_MUTED, 90f, 458f);
            ui.text(String.format("%d%%", Math.round(game.getSounds().getVolumeGeral() * 100f)),
                .86f, UiTheme.TEXT, 716f, 426f);
            ui.title("DIMINUIR", .62f, selected == 0 ? UiTheme.TEXT : UiTheme.TEXT_MUTED,
                buttons[0].x + 24f, buttons[0].y + 34f);
            ui.title("AUMENTAR", .62f, selected == 1 ? UiTheme.TEXT : UiTheme.TEXT_MUTED,
                buttons[1].x + 24f, buttons[1].y + 34f);
            ui.text("MÚSICA DO MENU", .72f, UiTheme.AMBER, 882f, 458f);
            ui.title(game.getSounds().isMusicaAtiva() ? "LIGADA" : "DESLIGADA", .7f,
                selected == 2 ? UiTheme.TEXT : UiTheme.TEXT_MUTED,
                buttons[2].x + 24f, buttons[2].y + 34f);
            ui.title("VOLTAR", .62f, selected == 3 ? UiTheme.TEXT : UiTheme.TEXT_MUTED,
                buttons[3].x + 24f, buttons[3].y + 34f);
        }
    }

    private void updateSelection() {
        int count = page == Page.MAIN ? 4 : page == Page.SETTINGS ? 4 : 1;
        for (int i = 0; i < count; i++) if (buttons[i].contains(pointer)) selected = i;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) selected = (selected + 1) % count;
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) selected = (selected - 1 + count) % count;
    }

    private void handleInput() {
        boolean activate = Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || (Gdx.input.justTouched() && buttons[selected].contains(pointer));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (page == Page.MAIN) Gdx.app.exit(); else openPage(Page.MAIN);
            return;
        }
        if (!activate) return;
        if (page == Page.MAIN) {
            switch (selected) {
                case 0 -> startGame();
                case 1 -> openPage(Page.HOW_TO_PLAY);
                case 2 -> openPage(Page.SETTINGS);
                case 3 -> Gdx.app.exit();
            }
        } else if (page == Page.HOW_TO_PLAY) {
            openPage(Page.MAIN);
        } else {
            if (selected == 0) game.getSounds().setVolumeGeral(game.getSounds().getVolumeGeral() - .1f);
            if (selected == 1) game.getSounds().setVolumeGeral(game.getSounds().getVolumeGeral() + .1f);
            if (selected == 2) game.getSounds().alternarMusica();
            if (selected == 3) openPage(Page.MAIN);
        }
    }

    private void openPage(Page target) {
        page = target;
        selected = 0;
        time = 0f;
        if (target == Page.MAIN) layoutMainButtons();
        else if (target == Page.HOW_TO_PLAY) buttons[0].set(58f, 74f, 200f, 48f);
        else {
            buttons[0].set(90f, 326f, 210f, 52f);
            buttons[1].set(320f, 326f, 210f, 52f);
            buttons[2].set(882f, 344f, 308f, 52f);
            buttons[3].set(58f, 84f, 190f, 52f);
        }
    }

    private void layoutMainButtons() {
        for (int i = 0; i < 4; i++) buttons[i].set(58f, 408f - i * 70f, 430f, 56f);
    }

    private void startGame() {
        game.getSounds().pararMusicaMenu();
        game.getSounds().tocarInicio();
        game.setScreen(new LunarScreen(game, game.getBatch(), game.getAssets()));
        dispose();
    }

    @Override public void resize(int width, int height) { ui.resize(width, height); }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { if (ui != null) { ui.dispose(); ui = null; } }
}
