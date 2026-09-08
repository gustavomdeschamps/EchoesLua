package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.ui.TerminalUi;
import com.orion.echoes.lua.ui.UiTheme;

/**
 * Pausa compartilhada pelas três fases.
 *
 * Antes cada mundo desenhava sua própria pausa na mão — três blocos quase
 * idênticos de nine-patch e texto cru, sem entrada em cena, com o painel de
 * status escrito diferente em cada tela. Aqui a composição é uma só, no mesmo
 * nível de acabamento de {@link com.orion.echoes.lua.screens.MissionResultScreen}
 * (painéis com sombra em duas camadas, texto escalonado ao abrir), e cada
 * mundo só entra com o próprio accent, rótulo e linhas de status.
 *
 * A gameplay continua congelada por fora: quem chama decide isso (nenhum
 * update de física, oxigênio ou inimigos roda enquanto pausado). Aqui dentro,
 * a UI anima com o relógio real, então a entrada não trava mesmo com o jogo
 * congelado.
 */
public final class PauseOverlay {

    private static final float DELAY_TITLE = .06f;
    private static final float DELAY_SUBTITLE = .16f;
    private static final float DELAY_PANEL = .24f;
    private static final float DELAY_ACTIONS = .40f;
    private static final float APPEAR_TIME = .30f;
    private static final float APPEAR_RISE = 18f;
    private static final float PANEL_X = 74f;
    private static final float PANEL_Y = 150f;
    private static final float PANEL_WIDTH = 700f;
    private static final float PANEL_HEIGHT = 300f;
    private static final float SIDE_X = PANEL_X + PANEL_WIDTH + 24f;
    private static final float SIDE_WIDTH = GameConfig.WINDOW_WIDTH - SIDE_X - 74f;

    private final TerminalUi ui;
    private float elapsed;

    public PauseOverlay(SpriteBatch batch, AssetManager assets) {
        this.ui = new TerminalUi(batch, assets);
    }

    /** Chame uma vez, na borda em que a pausa abre — reinicia a entrada em cena. */
    public void open() { elapsed = 0f; }

    /**
     * @param accent cor do mundo atual (ciano na Lua, óxido em Marte, âmbar em Titã)
     * @param worldTag rótulo curto do cabeçalho, ex. "ECHOES · LUA"
     * @param missionLabel objetivo atual da missão, já resolvido por quem chama
     * @param flavorText uma linha de clima, ex. "A missão está congelada."
     * @param statLabels rótulos da coluna de status (O2, energia, munição, ...)
     * @param statValues valores correspondentes, mesmo tamanho de statLabels
     */
    public void render(Color accent, String worldTag, String missionLabel, String flavorText,
                       String[] statLabels, String[] statValues) {
        elapsed += Math.min(com.badlogic.gdx.Gdx.graphics.getDeltaTime(), 1f / 30f);

        ui.beginShapes();
        ui.rect(0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT,
            new Color(.012f, .016f, .021f, .86f));
        ui.endShapes();

        float titleIn = appear(DELAY_TITLE);
        float subtitleIn = appear(DELAY_SUBTITLE);
        float panelIn = appear(DELAY_PANEL);
        float actionsIn = appear(DELAY_ACTIONS);

        ui.beginText();
        ui.text(worldTag, .68f, fade(accent, titleIn), PANEL_X, 640f + rise(titleIn));
        ui.title("MISSÃO\nPAUSADA", 1.55f, fade(UiTheme.TEXT, titleIn), PANEL_X - 4f, 588f + rise(titleIn));
        ui.text(flavorText, .74f, fade(UiTheme.TEXT_MUTED, subtitleIn), PANEL_X, 452f + rise(subtitleIn));
        ui.endText();

        drawObjectivePanel(accent, panelIn, missionLabel);
        drawStatusPanel(accent, panelIn, statLabels, statValues);
        drawActions(accent, actionsIn);

        ui.resetFontScale();
    }

    private void drawObjectivePanel(Color accent, float progress, String missionLabel) {
        if (progress <= 0f) return;
        ui.beginShapes();
        ui.panel(PANEL_X, PANEL_Y + rise(progress), PANEL_WIDTH, PANEL_HEIGHT, fade(accent, progress));
        ui.endShapes();

        ui.beginText();
        ui.text("OBJETIVO ATUAL", .68f, fade(accent, progress),
            PANEL_X + 28f, PANEL_Y + PANEL_HEIGHT - 40f + rise(progress));
        ui.textWrapped(missionLabel, .82f, fade(UiTheme.TEXT, progress),
            PANEL_X + 28f, PANEL_Y + PANEL_HEIGHT - 82f + rise(progress), PANEL_WIDTH - 56f);
        ui.text("Nenhum recurso é consumido enquanto a missão está pausada.",
            .64f, fade(UiTheme.TEXT_MUTED, progress), PANEL_X + 28f, PANEL_Y + 34f + rise(progress));
        ui.endText();
    }

    private void drawStatusPanel(Color accent, float progress, String[] labels, String[] values) {
        if (progress <= 0f) return;
        float height = Math.max(PANEL_HEIGHT, 46f + labels.length * 40f);
        float y = PANEL_Y + PANEL_HEIGHT - height;
        ui.beginShapes();
        ui.panel(SIDE_X, y + rise(progress), SIDE_WIDTH, height, fade(accent, progress));
        ui.endShapes();

        ui.beginText();
        ui.text("EXPEDIÇÃO", .68f, fade(accent, progress),
            SIDE_X + 24f, y + height - 34f + rise(progress));
        float line = y + height - 74f + rise(progress);
        for (int i = 0; i < labels.length; i++) {
            ui.text(labels[i], .64f, fade(UiTheme.TEXT_MUTED, progress), SIDE_X + 24f, line);
            float width = ui.textWidth(values[i], .7f);
            ui.text(values[i], .7f, fade(UiTheme.TEXT, progress),
                SIDE_X + SIDE_WIDTH - 24f - width, line);
            line -= 36f;
        }
        ui.endText();
    }

    private void drawActions(Color accent, float progress) {
        if (progress <= 0f) return;
        ui.beginText();
        ui.text("RETOMAR", .84f, fade(UiTheme.TEXT, progress), PANEL_X, 96f + rise(progress));
        ui.text("ESC ou ENTER", .62f, fade(accent, progress), PANEL_X + 190f, 96f + rise(progress));
        ui.text("VOLTAR AO MENU", .72f, fade(UiTheme.TEXT_MUTED, progress), PANEL_X + 400f, 96f + rise(progress));
        ui.text("M", .68f, fade(accent, progress), PANEL_X + 640f, 96f + rise(progress));
        ui.endText();
    }

    private float appear(float delay) {
        return Interpolation.pow3Out.apply(MathUtils.clamp((elapsed - delay) / APPEAR_TIME, 0f, 1f));
    }

    private float rise(float progress) { return (1f - progress) * APPEAR_RISE; }

    private Color fade(Color base, float progress) {
        return new Color(base.r, base.g, base.b, base.a * progress);
    }

    public void resize(int width, int height) { ui.resize(width, height); }

    public void dispose() { ui.dispose(); }
}
