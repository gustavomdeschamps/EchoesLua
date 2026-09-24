package com.orion.echoes.lua.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.managers.AssetManager;

/**
 * Interior funcional das estacoes principais.
 *
 * Nao e uma caixa de confirmacao sobre o mapa: ocupa a tela como uma pequena
 * sala, interrompe o movimento e distribui as acoes em bancadas reconheciveis.
 * Assim entrar na base passa a ser uma mudanca de espaco e nao apenas apertar
 * E em cima de um sprite.
 */
public final class WorkshopInterior {
    private static final Color HEADER_SHADE = new Color(.008f, .018f, .028f, .88f);
    private static final Color FLOOR_SHADE = new Color(.008f, .018f, .028f, .89f);
    private static final Color DIVIDER = new Color(.31f, .51f, .61f, .35f);
    public interface Actions {
        String status();
        void recharge();
        void craft();
        void processIce();
    }

    private final TerminalUi ui;
    private final AssetManager assets;
    private boolean open;
    private String title = "MÓDULO DE APOIO";
    private Actions actions;
    private float time;

    public WorkshopInterior(SpriteBatch batch, AssetManager assets) {
        this.ui = new TerminalUi(batch, assets);
        this.assets = assets;
    }

    public void open(String title, Actions actions) {
        if (open) return;
        this.title = title;
        this.actions = actions;
        this.open = true;
        this.time = 0f;
    }

    public boolean isOpen() { return open; }

    /** Retorna true enquanto a oficina possui o foco exclusivo do teclado. */
    public boolean handleInput(float delta) {
        if (!open) return false;
        time += Math.min(delta, 1f / 30f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            open = false;
            return true;
        }
        if (actions != null) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) actions.recharge();
            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) actions.craft();
            if (Gdx.input.isKeyJustPressed(Input.Keys.G)) actions.processIce();
        }
        return true;
    }

    public void render() {
        if (!open) return;
        float glow = .72f + MathUtils.sin(time * 2.4f) * .12f;
        ui.image(assets.workshopInteriorTexture, 0f, 0f, 1280f, 720f, Color.WHITE);
        ui.beginShapes();
        ui.rect(0f, 624f, 1280f, 96f, HEADER_SHADE);
        ui.rect(0f, 0f, 1280f, 171f, FLOOR_SHADE);
        ui.rect(0f, 622f, 1280f, 2f, UiTheme.CYAN_DIM);
        ui.rect(426f, 28f, 1f, 124f, DIVIDER);
        ui.rect(852f, 28f, 1f, 124f, DIVIDER);
        ui.rect(40f, 139f, 344f, 3f, UiTheme.CYAN);
        ui.rect(466f, 139f, 344f, 3f, UiTheme.AMBER);
        ui.rect(892f, 139f, 344f, 3f, UiTheme.GREEN);
        ui.rect(40f, 22f, 118f * glow, 2f, UiTheme.CYAN_DIM);
        ui.endShapes();

        ui.beginText();
        ui.title(title, .78f, UiTheme.TEXT, 40f, 682f);
        ui.text("OFICINA PRESSURIZADA", .49f, UiTheme.CYAN, 42f, 645f);
        ui.text("E / ESC  SAIR", .50f, UiTheme.TEXT_MUTED, 1110f, 651f);
        ui.text("SUPORTE DE VIDA", .61f, UiTheme.TEXT, 42f, 119f);
        ui.text("R  REABASTECER", .54f, UiTheme.CYAN, 42f, 82f);
        ui.text("BANCADA DE CAMPO", .61f, UiTheme.TEXT, 468f, 119f);
        ui.text("F  FABRICAR", .54f, UiTheme.AMBER, 468f, 82f);
        ui.text("PROCESSADOR CRIO", .61f, UiTheme.TEXT, 894f, 119f);
        ui.text("G  PROCESSAR GELO", .54f, UiTheme.GREEN, 894f, 82f);
        String status = actions == null ? "Sistemas aguardando operador." : actions.status();
        ui.textWrapped(status, .50f, UiTheme.TEXT_MUTED, 42f, 46f, 1180f);
        ui.endText();
    }

    public void resize(int width, int height) { ui.resize(width, height); }
    public void dispose() { ui.dispose(); }
}
