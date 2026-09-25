package com.orion.echoes.lua.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.ui.PauseSettingsModel;
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
    private final com.badlogic.gdx.graphics.g2d.NinePatch buttonPatch;
    private float elapsed;

    /*
     * Opcoes dentro da pausa.
     *
     * Sem isto, mudar volume ou desligar o tremor exigia abandonar a partida
     * e ir ao menu. O modelo e injetado por quem constroi a tela, entao a
     * pausa nao conhece AppSettings nem carrega preferencia nenhuma.
     */
    private PauseSettingsModel settings;
    private boolean settingsOpen;
    private boolean quitRequested;
    private boolean resumeRequested;
    private boolean menuRequested;
    private boolean sliderDragging;
    private final com.badlogic.gdx.math.Vector2 pointer = new com.badlogic.gdx.math.Vector2();
    public boolean consumeResumeRequested() { boolean value=resumeRequested;resumeRequested=false;return value; }
    public boolean consumeMenuRequested() { boolean value=menuRequested;menuRequested=false;return value; }

    public PauseOverlay(SpriteBatch batch, AssetManager assets) {
        this.ui = new TerminalUi(batch, assets);
        this.buttonPatch = assets.uiButtonPatch();
    }

    /** Chame uma vez, na borda em que a pausa abre — reinicia a entrada em cena. */
    public void open() {
        elapsed = 0f;
        settingsOpen = false;
        resumeRequested=false; menuRequested=false; quitRequested=false;
        sliderDragging=false;
    }

    public void setSettings(PauseSettingsModel model) { settings = model; }

    public boolean isSettingsOpen() { return settingsOpen; }

    /** True uma unica vez quando o jogador pede para sair pelo atalho da pausa. */
    public boolean consumeQuitRequested() {
        boolean pending = quitRequested;
        quitRequested = false;
        return pending;
    }

    /**
     * Trata as teclas proprias da pausa e diz se consumiu a tecla.
     *
     * Fica aqui, e nao nas tres telas, porque o tratamento seria identico nas
     * tres e ja havia triplicacao no ESC e no M. Quem chama roda isto antes do
     * proprio tratamento de pausa: com as opcoes abertas, ESC fecha as opcoes
     * em vez de despausar, e nenhuma tecla vaza para o gameplay.
     */
    public boolean handlePauseKeys() {
        ui.unproject(pointer.set(Gdx.input.getX(), Gdx.input.getY()));
        if (sliderDragging && settingsOpen && settings != null) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                settings.setRatio(settings.getSelected(),
                    (pointer.x - PANEL_X - 250f) / BAR_WIDTH);
                return true;
            }
            sliderDragging = false;
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float actionY = 54f + rise(appear(DELAY_ACTIONS));
            if (elapsed >= DELAY_ACTIONS && pointer.y >= actionY && pointer.y <= actionY + 70f
                && pointer.x >= PANEL_X) {
                int index=(int)((pointer.x-PANEL_X)/280f);
                if(index >= 0 && index < (settingsOpen ? 2 : 4)
                    && pointer.x-PANEL_X-index*280f <= 264f) {
                    if(settingsOpen) {
                        settingsOpen=false;
                        if(index==1) resumeRequested=true;
                        return true;
                    }
                    if(index==0)resumeRequested=true;
                    if(index==1) {settingsOpen=true;return true;}
                    if(index==2)menuRequested=true;
                    if(index==3) {quitRequested=true;return true;}
                    return true;
                }
            }
            if (settingsOpen && settings != null) {
                float firstLine = PANEL_Y + PANEL_HEIGHT - 78f + rise(appear(DELAY_PANEL));
                for (int index = 0; index < settings.size(); index++) {
                    float line = firstLine - index * 38f;
                    if (pointer.x >= PANEL_X + 18f && pointer.x <= PANEL_X + PANEL_WIDTH - 18f
                        && pointer.y >= line - 9f && pointer.y <= line + 21f) {
                        settings.select(index);
                        if (settings.kind(index) == PauseSettingsModel.Kind.SLIDER
                            && pointer.x >= PANEL_X + 250f && pointer.x <= PANEL_X + 250f + BAR_WIDTH) {
                            settings.setRatio(index, (pointer.x - PANEL_X - 250f) / BAR_WIDTH);
                            sliderDragging = true;
                        } else if (settings.kind(index) == PauseSettingsModel.Kind.TOGGLE)
                            settings.activate();
                        return true;
                    }
                }
            }
        }
        if (settingsOpen) {
            if (settings != null) {
                if (justPressed(Input.Keys.UP, Input.Keys.W)) settings.moveSelection(-1);
                if (justPressed(Input.Keys.DOWN, Input.Keys.S)) settings.moveSelection(1);
                if (justPressed(Input.Keys.RIGHT, Input.Keys.D)) settings.adjust(1);
                if (justPressed(Input.Keys.LEFT, Input.Keys.A)) settings.adjust(-1);
                if (justPressed(Input.Keys.ENTER, Input.Keys.SPACE)) settings.activate();
            }
            if (justPressed(Input.Keys.ESCAPE, Input.Keys.O)) settingsOpen = false;
            // Consome tudo: com o painel aberto nada pode chegar ao gameplay.
            return true;
        }
        if (settings != null && Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            settingsOpen = true;
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            quitRequested = true;
            return true;
        }
        return false;
    }

    private static boolean justPressed(int first, int second) {
        return Gdx.input.isKeyJustPressed(first) || Gdx.input.isKeyJustPressed(second);
    }

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
        ui.rect(0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, SCRIM);
        ui.endShapes();

        float titleIn = appear(DELAY_TITLE);
        float subtitleIn = appear(DELAY_SUBTITLE);
        float panelIn = appear(DELAY_PANEL);
        float actionsIn = appear(DELAY_ACTIONS);

        ui.beginText();
        ui.text(worldTag, .68f, fade(accent, titleIn), PANEL_X, 640f + rise(titleIn));
        ui.title("JOGO PAUSADO", 1.35f, fade(UiTheme.TEXT, titleIn), PANEL_X - 4f, 588f + rise(titleIn));
        ui.text(flavorText, .74f, fade(UiTheme.TEXT_MUTED, subtitleIn), PANEL_X, 512f + rise(subtitleIn));
        ui.endText();

        if (settingsOpen) drawSettingsPanel(accent, panelIn);
        else drawObjectivePanel(accent, panelIn, missionLabel);
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

    /**
     * Lista de opcoes, uma linha por ajuste.
     *
     * Cada linha mostra rotulo, barra e valor escrito. O valor em texto nao e
     * redundancia: sem ele o jogador ajusta sem saber onde parou, e a leitura
     * nao pode depender so da cor da barra.
     */
    private void drawSettingsPanel(Color accent, float progress) {
        if (progress <= 0f || settings == null) return;
        float lift = rise(progress);
        ui.beginShapes();
        ui.panel(PANEL_X, PANEL_Y + lift, PANEL_WIDTH, PANEL_HEIGHT, fade(accent, progress));
        ui.endShapes();

        float line = PANEL_Y + PANEL_HEIGHT - 78f + lift;
        for (int index = 0; index < settings.size(); index++) {
            boolean current = index == settings.getSelected();
            ui.beginShapes();
            if (current) {
                ui.rect(PANEL_X + 18f, line - 9f, PANEL_WIDTH - 36f, 30f,
                    fade(SELECTION, progress));
            }
            ui.rect(PANEL_X + 250f, line + 2f, BAR_WIDTH, 8f,
                fade(UiTheme.TEXT_MUTED, progress * .45f));
            ui.rect(PANEL_X + 250f, line + 2f, BAR_WIDTH * settings.ratio(index), 8f,
                fade(current ? accent : UiTheme.TEXT_MUTED, progress));
            ui.endShapes();

            ui.beginText();
            ui.text(settings.label(index), .70f,
                fade(current ? UiTheme.TEXT : UiTheme.TEXT_MUTED, progress),
                PANEL_X + 30f, line + 18f);
            String value = settings.valueText(index);
            ui.text(value, .64f, fade(current ? accent : UiTheme.TEXT_MUTED, progress),
                PANEL_X + PANEL_WIDTH - 30f - ui.textWidth(value, .64f), line + 18f);
            ui.endText();
            line -= 38f;
        }

        ui.beginText();
        ui.text("CONFIGURAÇÕES", .68f, fade(accent, progress),
            PANEL_X + 28f, PANEL_Y + PANEL_HEIGHT - 40f + lift);
        ui.text("Clique ou arraste para ajustar  ·  ESC volta à pausa",
            .62f, fade(UiTheme.TEXT_MUTED, progress), PANEL_X + 28f, PANEL_Y + 30f + lift);
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

    /**
     * Atalhos da pausa.
     *
     * Com as opcoes abertas a linha muda: ESC deixa de despausar e passa a
     * fechar o painel, e anunciar RETOMAR ali seria mentira.
     */
    private void drawActions(Color accent, float progress) {
        if (progress <= 0f) return;
        float y = 96f + rise(progress);
        ui.beginShapes();
        int count = settingsOpen ? 2 : 4;
        ui.unproject(pointer.set(Gdx.input.getX(), Gdx.input.getY()));
        for (int i=0;i<count;i++) {
            float x = PANEL_X + i * 280f;
            boolean hover = pointer.x >= x && pointer.x <= x + 264f
                && pointer.y >= y - 42f && pointer.y <= y + 28f;
            ui.patch(buttonPatch,x,y-42f,264f,70f,
                fade(hover ? UiTheme.CYAN : Color.WHITE,progress));
        }
        ui.endShapes();
        ui.beginText();
        if (settingsOpen) {
            action("VOLTAR", "AO PAUSE", accent, progress, PANEL_X, y);
            action("RETOMAR", "JOGO", accent, progress, PANEL_X + 280f, y);
        } else {
            action("RETOMAR", "ESC ou ENTER", accent, progress, PANEL_X, y);
            action("CONFIGURAÇÕES", "O", accent, progress, PANEL_X + 280f, y);
            action("MENU", "M", accent, progress, PANEL_X + 560f, y);
            action("SAIR", "Q", accent, progress, PANEL_X + 840f, y);
        }
        ui.endText();
    }

    private void action(String label, String key, Color accent, float progress,
                        float x, float y) {
        ui.text(label, .68f, fade(UiTheme.TEXT, progress),
            x+(264f-ui.textWidth(label,.68f))*.5f, y+7f);
        ui.text(key, .52f, fade(UiTheme.CYAN, progress),
            x+(264f-ui.textWidth(key,.52f))*.5f, y-18f);
    }

    /*
     * Entrada escalonada da pausa.
     *
     * Com reducao de movimento os blocos aparecem prontos: o fade continua
     * (nao desloca nada na tela), mas o deslize de 18px de cada bloco some.
     * Sem isto a opcao de acessibilidade so valeria dentro do gameplay, e a
     * tela de pausa - que o jogador abre justamente para descansar a vista -
     * continuaria deslizando.
     */
    private float appear(float delay) {
        if (reduceMotion) return 1f;
        return Interpolation.pow3Out.apply(MathUtils.clamp((elapsed - delay) / APPEAR_TIME, 0f, 1f));
    }

    public void setReduceMotion(boolean value) { reduceMotion = value; }

    private static final Color SCRIM = new Color(.012f, .016f, .021f, .86f);
    private static final Color SELECTION = new Color(1f, 1f, 1f, .09f);
    private static final float BAR_WIDTH = 260f;
    private final Color faded = new Color();

    private boolean reduceMotion;

    private float rise(float progress) {
        return reduceMotion ? 0f : (1f - progress) * APPEAR_RISE;
    }

    /*
     * A pausa desenha por volta de dez rotulos por quadro e cada um pedia um
     * Color novo so para variar o alpha da entrada. A instancia e reusada:
     * o valor e consumido dentro da mesma chamada de desenho.
     */
    private Color fade(Color base, float progress) {
        return faded.set(base.r, base.g, base.b, base.a * progress);
    }

    public void resize(int width, int height) { ui.resize(width, height); }

    public void dispose() { ui.dispose(); }
}
