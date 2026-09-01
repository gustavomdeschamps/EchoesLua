package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.AppSettings;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.save.GameSaveData;
import com.orion.echoes.lua.save.LunarCheckpoint;
import com.orion.echoes.lua.save.SaveManager;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.ui.UiFactory;
import com.orion.echoes.lua.ui.UiTheme;

/** Menu Scene2D com navegação curta, tipografia legível e opções persistentes. */
public final class MenuScreen implements Screen {
    private final EchoesLua game;
    private Stage stage;
    private Skin skin;
    private Table page;
    private boolean leaving;
    private boolean fadeCompleted;

    public MenuScreen(EchoesLua game) { this.game = game; }

    @Override public void show() {
        stage = new Stage(new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT), game.getBatch());
        skin = UiFactory.create(game.getAssets());
        stage.addActor(new MenuBackdrop());
        Gdx.input.setInputProcessor(stage);
        game.aplicarPreferenciasDeAudio();
        game.getSounds().tocarMusicaMenu();
        showMain(false);
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(.45f));
    }

    private void showMain(boolean animate) {
        Table content = basePage();
        content.add(title("ECHOES", 1.65f)).left().row();
        content.add(label("FASE LUNAR  //  SINAL ORION", UiTheme.CYAN)).left().padBottom(44f).row();
        content.add(button("NOVO JOGO", this::startGame)).width(410f).height(58f).padBottom(10f).row();
        if (new SaveManager().hasSave()) {
            content.add(button("CONTINUAR CAMPANHA", this::continueGame))
                .width(410f).height(58f).padBottom(10f).row();
        }
        content.add(button("COMO JOGAR", () -> showHowToPlay(true))).width(410f).height(58f).padBottom(10f).row();
        content.add(button("CONFIGURAÇÕES", () -> showSettings(true))).width(410f).height(58f).padBottom(10f).row();
        content.add(button("SAIR", Gdx.app::exit)).width(410f).height(58f).row();
        swap(content, animate);
    }

    private void showHowToPlay(boolean animate) {
        Table content = basePage();
        content.add(title("COMO JOGAR", 1.2f)).left().colspan(2).padBottom(25f).row();
        helpCard(content, "MOVIMENTO", "WASD ou setas  ·  SHIFT para correr", UiTheme.CYAN);
        helpCard(content, "AÇÃO", "E interage  ·  ESPAÇO executa o dash", UiTheme.AMBER);
        helpCard(content, "COMBATE", "Mouse mira  ·  botão esquerdo dispara", UiTheme.GREEN);
        helpCard(content, "MISSÃO", "Reative três sistemas, fabrique a arma e abra o portal", UiTheme.RED);
        content.add(button("VOLTAR", () -> showMain(true))).width(210f).height(54f).left().colspan(2).padTop(24f).row();
        swap(content, animate);
    }

    private void showSettings(boolean animate) {
        AppSettings settings = game.getSettings();
        Table content = basePage();
        content.add(title("CONFIGURAÇÕES", 1.15f)).left().colspan(2).padBottom(18f).row();
        addSlider(content, "MÚSICA", settings.getMusicVolume(), value -> {
            settings.setMusicVolume(value);
            game.aplicarPreferenciasDeAudio();
        });
        addSlider(content, "EFEITOS", settings.getSfxVolume(), value -> {
            settings.setSfxVolume(value);
            game.aplicarPreferenciasDeAudio();
        });
        addSlider(content, "INTERFACE", settings.getUiVolume(), value -> {
            settings.setUiVolume(value);
            game.aplicarPreferenciasDeAudio();
            game.getSounds().tocarHoverUi();
        });
        addSlider(content, "ESCALA DO HUD", (settings.getHudScale() - .85f) / .35f,
            value -> settings.setHudScale(.85f + value * .35f));
        content.add(toggle("SHAKE DE CÂMERA", settings.isShakeEnabled(), settings::setShakeEnabled)).width(300f).height(50f).pad(7f);
        content.add(toggle("MODO DALTÔNICO", settings.isColorblindEnabled(), settings::setColorblindEnabled)).width(300f).height(50f).pad(7f).row();
        content.add(toggle("TELA CHEIA", settings.isFullscreen(), this::setFullscreen)).width(300f).height(50f).pad(7f);
        content.add(toggle("MOVIMENTO POR SETAS", settings.isArrowMovement(), settings::setArrowMovement)).width(300f).height(50f).pad(7f).row();
        content.add(button("VOLTAR", () -> showMain(true))).width(210f).height(52f).left().colspan(2).padTop(12f).row();
        swap(content, animate);
    }

    private Table basePage() {
        Table content = new Table();
        content.setSize(700f, 640f);
        content.setPosition(52f, 38f);
        content.align(Align.topLeft);
        content.pad(38f);
        content.setBackground(new NinePatchDrawable(game.getAssets().uiDialogPatch()));
        return content;
    }

    private void helpCard(Table table, String heading, String description, Color color) {
        Label copy = label(description, UiTheme.TEXT);
        copy.setWrap(true);
        table.add(label(heading, color)).width(170f).left().pad(8f);
        table.add(copy).width(410f).left().pad(8f).row();
    }

    private interface FloatChange { void set(float value); }
    private void addSlider(Table table, String text, float value, FloatChange change) {
        Slider slider = new Slider(0f, 1f, .05f, false, skin);
        slider.setValue(value);
        slider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { change.set(slider.getValue()); }
        });
        table.add(label(text, UiTheme.TEXT_MUTED)).width(190f).left().pad(7f);
        table.add(slider).width(390f).height(32f).pad(7f).row();
    }

    private interface BoolChange { void set(boolean value); }
    private TextButton toggle(String label, boolean initial, BoolChange change) {
        TextButton button = new TextButton(label + "  " + (initial ? "LIGADO" : "DESLIGADO"), skin);
        final boolean[] value = {initial};
        button.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                value[0] = !value[0];
                button.setText(label + "  " + (value[0] ? "LIGADO" : "DESLIGADO"));
                change.set(value[0]);
            }
        });
        return button;
    }

    private TextButton button(String text, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.getSounds().tocarHoverUi();
                action.run();
            }
        });
        return button;
    }

    private Label label(String text, Color color) {
        Label label = new Label(text, skin);
        label.setColor(color);
        return label;
    }

    private Label title(String text, float scale) {
        Label label = new Label(text, skin, "title");
        label.setFontScale(scale);
        return label;
    }

    private void swap(Table next, boolean animate) {
        if (page != null) page.remove();
        page = next;
        stage.addActor(page);
        if (animate) {
            page.getColor().a = 0f;
            page.setX(page.getX() - 24f);
            page.addAction(Actions.parallel(Actions.fadeIn(.24f), Actions.moveBy(24f, 0f, .3f)));
        }
    }

    private void setFullscreen(boolean enabled) {
        game.getSettings().setFullscreen(enabled);
        if (enabled) Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        else Gdx.graphics.setWindowedMode(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
    }

    /** Comeca do zero: campanha nova, semente nova, Lua intacta. */
    private void startGame() {
        if (leaving) return;
        game.startNewCampaign();
        leaving = true;
        game.getSounds().pararMusicaMenu();
        game.getSounds().tocarInicio();
        /*
         * A acao apenas marca o fim do fade. Trocar de tela aqui dentro
         * quebraria o frame: Actions.run executa dentro de stage.act(), e o
         * dispose() que vem junto anularia o stage antes do stage.draw()
         * logo abaixo, no meio do mesmo render.
         */
        stage.getRoot().addAction(
            Actions.sequence(Actions.fadeOut(.28f), Actions.run(() -> fadeCompleted = true)));
    }

    /**
     * Retoma a campanha salva na fase em que ela parou.
     *
     * E aqui que a persistencia fica visivel: o save guarda a semente, entao a
     * Lua carregada e exatamente a mesma, com o mesmo progresso.
     */
    private void continueGame() {
        if (leaving) return;
        GameSaveData data = new SaveManager().load();
        if (data == null) return;
        game.setCampaign(LunarCheckpoint.toCampaign(data));
        leaving = true;
        game.getSounds().pararMusicaMenu();
        game.getSounds().tocarInicio();
        stage.getRoot().addAction(
            Actions.sequence(Actions.fadeOut(.28f), Actions.run(() -> fadeCompleted = true)));
    }

    @Override public void render(float delta) {
        if (stage == null) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !leaving) showMain(true);
        stage.act(Math.min(delta, 1f / 30f));

        // A troca acontece depois do act e antes do draw, com o stage ainda vivo.
        if (fadeCompleted) {
            fadeCompleted = false;
            CampaignState campaign = game.getCampaign();
            game.setScreen(campaign.getPhase() == CampaignState.Phase.MARS
                ? new MarsScreen(game, campaign)
                : new LunarScreen(game, game.getBatch(), game.getAssets(), campaign));
            dispose();
            return;
        }

        stage.draw();
    }
    @Override public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() {
        if (stage != null) { stage.dispose(); stage = null; }
        if (skin != null) { skin.dispose(); skin = null; }
    }

    private final class MenuBackdrop extends Actor {
        MenuBackdrop() { setBounds(0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT); }
        @Override public void draw(Batch batch, float parentAlpha) {
            batch.setColor(.18f, .23f, .29f, 1f);
            batch.draw(game.getAssets().backgroundLuaTexture, 0f, 0f, getWidth(), getHeight());
            batch.setColor(.82f, .8f, .74f, .72f);
            batch.draw(game.getAssets().introKeyArtTexture, 520f, 0f, 1280f, 720f);
            batch.setColor(.015f, .025f, .035f, .63f);
            batch.draw(game.getAssets().uiWhiteTexture, 0f, 0f, getWidth(), getHeight());
            batch.setColor(Color.WHITE);
        }
    }
}
