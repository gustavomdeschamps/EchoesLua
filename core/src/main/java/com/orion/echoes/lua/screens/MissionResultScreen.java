package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.ui.TerminalUi;
import com.orion.echoes.lua.ui.UiTheme;

/**
 * Encerramento da campanha, em vitoria ou derrota.
 *
 * A tela antiga tinha tres problemas de acabamento: os botoes eram uma linha
 * de 1px em vez das texturas de botao que o jogo ja possui, o painel da
 * direita era um retangulo vazio de 380x496, e nada entrava em cena - tudo
 * aparecia de uma vez. Aqui a composicao entra escalonada, o painel virou o
 * relatorio da missao e os botoes usam o mesmo kit do menu.
 */
abstract class MissionResultScreen implements Screen {

    /** Instante em que cada bloco comeca a entrar, em segundos. */
    private static final float DELAY_TITLE = .12f;
    private static final float DELAY_SUBTITLE = .30f;
    private static final float DELAY_REPORT = .46f;
    private static final float DELAY_BUTTONS = .78f;
    private static final float APPEAR_TIME = .42f;
    /** Deslocamento vertical de entrada de cada bloco. */
    private static final float APPEAR_RISE = 22f;

    private static final float REPORT_X = 770f;
    private static final float REPORT_Y = 204f;
    private static final float REPORT_WIDTH = 432f;
    private static final float REPORT_HEIGHT = 360f;

    protected final EchoesLua game;
    private final float missionTime;
    private final boolean success;

    private final Rectangle retryButton = new Rectangle(74f, 74f, 288f, 58f);
    private final Rectangle menuButton = new Rectangle(382f, 74f, 196f, 58f);
    private final Vector2 pointer = new Vector2();

    private TerminalUi ui;
    private NinePatch buttonPatch;
    private NinePatch buttonHoverPatch;
    private NinePatch buttonPressedPatch;
    private float elapsed;
    private int selected;
    private boolean leaving;

    MissionResultScreen(EchoesLua game, float missionTime, boolean success) {
        this.game = game;
        this.missionTime = missionTime;
        this.success = success;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
        ui = new TerminalUi(game.getBatch(), game.getAssets());
        buttonPatch = game.getAssets().uiButtonPatch();
        buttonHoverPatch = game.getAssets().uiButtonHoverPatch();
        buttonPressedPatch = game.getAssets().uiButtonPressedPatch();
        if (success) game.getSounds().tocarVitoria();
        else game.getSounds().tocarGameOver();
    }

    @Override
    public void render(float delta) {
        // A troca de tela libera a UI dentro do proprio frame; sem esta guarda
        // um render extra cairia em nulo, como ja aconteceu no menu.
        if (ui == null) return;
        elapsed += Math.min(delta, 1f / 30f);
        atualizarSelecao();

        Color accent = success ? UiTheme.GREEN : UiTheme.RED;
        drawBackground();
        drawComposition(accent);
        drawHero(appear(DELAY_SUBTITLE));
        drawReport(accent);
        drawButtons(accent);
        handleInput();
    }

    // =====================================================
    // ENTRADA EM CENA
    // =====================================================

    /**
     * Progresso de entrada de um bloco, de 0 a 1.
     *
     * Escalonar os blocos e o que faz a tela parecer encenada em vez de
     * simplesmente aparecer pronta no primeiro frame.
     */
    private float appear(float delay) {
        if (reduceMotion()) return 1f;
        return Interpolation.pow3Out.apply(
            MathUtils.clamp((elapsed - delay) / APPEAR_TIME, 0f, 1f));
    }

    /** Le a preferencia na hora: a tela pode abrir logo apos o jogador troca-la. */
    private boolean reduceMotion() {
        return game.getSettings() != null && game.getSettings().isReduceMotion();
    }

    private static final Color SUCCESS_TINT = new Color(.78f, .84f, .94f, 1f);
    private static final Color FAILURE_TINT = new Color(.13f, .11f, .14f, 1f);

    private float rise(float progress) {
        return reduceMotion() ? 0f : (1f - progress) * APPEAR_RISE;
    }

    /*
     * Instancia reusada: cada cor e consumida na chamada de desenho seguinte,
     * e nenhuma linha pede dois fade() ao mesmo tempo.
     */
    private final Color faded = new Color();

    private Color fade(Color base, float progress) {
        return faded.set(base.r, base.g, base.b, base.a * progress);
    }

    // =====================================================
    // COMPOSICAO
    // =====================================================

    /*
     * Enquadramento da arte de vitoria.
     *
     * A ilustracao traz um homem fotorrealista em primeiro plano que nao e o
     * astronauta do jogo - apresenta-lo como o personagem seria trocar a
     * identidade do protagonista no ultimo quadro da campanha. O recorte abaixo
     * fica com o cenario do retorno (Terra nascendo sobre a crista lunar, ceu
     * profundo e o terco direito calmo para o relatorio) e o personagem entra
     * como ele e: o sprite do proprio jogo, sobre a crista.
     */
    private static final float ART_U = .46f, ART_V = .22f, ART_W = .54f, ART_H = .5389f;
    private TextureRegion victoryFraming;

    private TextureRegion victoryFraming() {
        if (victoryFraming == null) {
            Texture art = game.getAssets().victoryReturnTexture;
            victoryFraming = new TextureRegion(art,
                Math.round(art.getWidth() * ART_U), Math.round(art.getHeight() * ART_V),
                Math.round(art.getWidth() * ART_W), Math.round(art.getHeight() * ART_H));
        }
        return victoryFraming;
    }

    /** Arte de fundo com deriva lenta, para a tela nao ficar estatica. */
    private void drawBackground() {
        Color tint = success ? SUCCESS_TINT : FAILURE_TINT;
        float drift = reduceMotion() ? 0f
            : Interpolation.sine.apply(MathUtils.clamp(elapsed / 14f, 0f, 1f)) * 12f;
        ui.clear(UiTheme.VOID);
        if (success) {
            ui.image(victoryFraming(), -drift, -drift * .4f,
                GameConfig.WINDOW_WIDTH + drift * 2f, GameConfig.WINDOW_HEIGHT + drift, tint);
        } else {
            ui.image(game.getAssets().backgroundLuaTexture, -drift, -drift * .4f,
                GameConfig.WINDOW_WIDTH + drift * 2f, GameConfig.WINDOW_HEIGHT + drift, tint);
        }
    }

    /**
     * O astronauta da campanha, de pe na crista.
     *
     * Usa o mesmo quadro de repouso das fases: nao ha animacao inventada nem
     * gesto fabricado por corte de imagem - o que aparece e o personagem que o
     * jogador conduziu ate aqui.
     */
    private void drawHero(float progress) {
        if (!success || progress <= 0f) return;
        // Fica entre a coluna de titulo e o painel do relatorio: sobre a crista
        // e sem cobrir o texto do desfecho.
        float size = 196f;
        float x = 522f, y = 178f + rise(progress);
        // Sombra e sprite no mesmo batch: region() abre o proprio batch e, se
        // chamado dentro de beginText(), quebraria o par begin/end.
        ui.beginShapes();
        ui.rect(x + size * .26f, y + 6f, size * .46f, size * .07f, new Color(0f, 0f, 0f, .34f * progress));
        ui.sprite(game.getAssets().astronautFrame(0, 0), x, y, size, size,
            faded.set(1f, 1f, 1f, progress));
        ui.endShapes();
    }

    private void drawComposition(Color accent) {
        float titleIn = appear(DELAY_TITLE);
        float subtitleIn = appear(DELAY_SUBTITLE);

        ui.beginShapes();
        // Faixa inferior: assenta os botoes e separa a leitura do fundo.
        ui.rect(0f, 0f, GameConfig.WINDOW_WIDTH, 168f, new Color(.016f, .024f, .030f, .95f));
        // Vinheta so no desfecho de derrota: na vitoria ela escurecia a arte
        // do retorno sem motivo, com cara de dano em tela.
        if (!success) ui.sprite(game.getAssets().uiDamageVignetteTexture, 0f, 0f,
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, new Color(1f, 1f, 1f, .48f));
        ui.endShapes();

        ui.beginText();
        ui.text("ECHOES  ·  " + (success ? "TERRA" : game.getCampaign().phaseToken()),
            .72f, fade(accent, titleIn), 74f, 640f + rise(titleIn));
        ui.title(success ? "MISSÃO\nCONCLUÍDA" : "FIM DA\nEXPEDIÇÃO",
            1.65f, fade(UiTheme.TEXT, titleIn), 70f, 548f + rise(titleIn));
        ui.text(success
                ? "As colônias voltaram a se ouvir.\nSua expedição chegou ao fim."
                : "O suporte de vida do traje chegou ao limite.",
            .72f, fade(success ? UiTheme.TEXT : UiTheme.TEXT_MUTED, subtitleIn), 74f, 386f + rise(subtitleIn));
        ui.endText();
    }

    /**
     * Relatorio da missao.
     *
     * Este painel existia como um retangulo translucido sem nada dentro. Agora
     * mostra o que a campanha registrou, que e a unica leitura de desempenho
     * que o jogador recebe ao terminar.
     */
    private void drawReport(Color accent) {
        float reportIn = appear(DELAY_REPORT);
        if (reportIn <= 0f) return;
        CampaignState campaign = game.getCampaign();

        ui.beginShapes();
        ui.panel(REPORT_X, REPORT_Y + rise(reportIn), REPORT_WIDTH, REPORT_HEIGHT,
            fade(accent, reportIn));
        ui.endShapes();

        float top = REPORT_Y + REPORT_HEIGHT - 46f + rise(reportIn);
        ui.beginText();
        ui.text("SUA EXPEDIÇÃO", .74f, fade(accent, reportIn), REPORT_X + 28f, top);

        float line = top - 44f;
        line = reportRow(reportIn, "Tempo de operação",
            String.format("%.1f s", missionTime), line);
        line = reportRow(reportIn, "Sistemas lunares",
            campaign.getLunarTotalEnemies() > 0 || campaign.hasLunarProgress()
                ? repairedCount(campaign) + "/4" : "—", line);
        line = reportRow(reportIn, "Hostis lunares",
            campaign.toLunarArray()[12] + "/" + campaign.getLunarTotalEnemies(), line);
        if (campaign.hasVisitedMars()) {
            line = reportRow(reportIn, "Estações marcianas",
                campaign.getMarsStationsOnline() + "/3", line);
            line = reportRow(reportIn, "Núcleos recolhidos",
                String.valueOf(campaign.getMinerals()), line);
        }
        line = reportRow(reportIn, "Munição restante",
            campaign.getAmmo() + "/" + GameConfig.AMMO_MAX, line);
        reportRow(reportIn, "Oxigênio final",
            String.format("%.0f%%", campaign.getOxygen()), line);
        ui.endText();
    }

    /** Uma linha do relatorio: rotulo a esquerda, valor a direita. */
    private float reportRow(float progress, String label, String value, float y) {
        ui.text(label, .68f, fade(UiTheme.TEXT_MUTED, progress), REPORT_X + 28f, y);
        float width = ui.textWidth(value, .72f);
        ui.text(value, .72f, fade(UiTheme.TEXT, progress),
            REPORT_X + REPORT_WIDTH - 28f - width, y);
        return y - 38f;
    }

    private int repairedCount(CampaignState campaign) {
        int[] lunar = campaign.toLunarArray();
        return lunar[7] + lunar[8] + lunar[9] + lunar[10];
    }

    // =====================================================
    // BOTOES
    // =====================================================

    private void drawButtons(Color accent) {
        float buttonsIn = appear(DELAY_BUTTONS);
        if (buttonsIn <= 0f) return;
        boolean pressing = Gdx.input.isTouched();

        ui.beginShapes();
        drawButton(retryButton, selected == 0, pressing, buttonsIn);
        drawButton(menuButton, selected == 1, pressing, buttonsIn);
        ui.endShapes();

        ui.beginText();
        drawButtonLabel("JOGAR NOVAMENTE", retryButton, selected == 0, buttonsIn, accent);
        drawButtonLabel("MENU", menuButton, selected == 1, buttonsIn, accent);
        ui.endText();
    }

    private void drawButton(Rectangle bounds, boolean focused, boolean pressing, float progress) {
        NinePatch patch = buttonPatch;
        if (focused) patch = pressing ? buttonPressedPatch : buttonHoverPatch;
        ui.patch(patch, bounds.x, bounds.y + rise(progress),
            bounds.width, bounds.height, new Color(1f, 1f, 1f, progress));
    }

    private void drawButtonLabel(String text, Rectangle bounds, boolean focused,
                                 float progress, Color accent) {
        float scale = .82f;
        float width = ui.textWidth(text, scale);
        Color color = focused ? accent : UiTheme.TEXT_MUTED;
        ui.text(text, scale, fade(color, progress),
            bounds.x + (bounds.width - width) / 2f,
            bounds.y + bounds.height / 2f + 9f + rise(progress));
    }

    // =====================================================
    // ENTRADA DO JOGADOR
    // =====================================================

    private void atualizarSelecao() {
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        ui.unproject(pointer);
        if (retryButton.contains(pointer)) selected = 0;
        if (menuButton.contains(pointer)) selected = 1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
            || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            selected = 1 - selected;
            game.getSounds().tocarHoverUi();
        }
    }

    private void handleInput() {
        if (leaving) return;
        boolean clicked = Gdx.input.justTouched()
            && (retryButton.contains(pointer) || menuButton.contains(pointer));
        boolean escape = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
        if (escape) selected = 1;
        if (!(clicked || escape || Gdx.input.isKeyJustPressed(Input.Keys.ENTER))) return;

        leaving = true;
        game.getSounds().tocarInicio();
        // A campanha recomeca do zero: o desfecho encerrou a anterior.
        if (selected == 0) {
            game.setScreen(new LunarScreen(game, game.getBatch(), game.getAssets(),
                game.startNewCampaign()));
        } else {
            game.startNewCampaign();
            game.setScreen(new MenuScreen(game));
        }
        dispose();
    }

    @Override public void resize(int width, int height) { ui.resize(width, height); }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (ui != null) {
            ui.dispose();
            ui = null;
        }
    }
}
