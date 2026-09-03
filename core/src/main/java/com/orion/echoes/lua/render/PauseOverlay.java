package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.ui.UiTheme;
import com.orion.echoes.lua.world.LunarWorld;

/** Tela de pausa: painel modal, atalhos e o resumo da missao congelada. */
public final class PauseOverlay {

    private final SpriteBatch batch;
    private final AssetManager assets;
    private final OrthographicCamera uiCamera;
    private final NinePatch panel;

    public PauseOverlay(SpriteBatch batch, AssetManager assets, OrthographicCamera uiCamera) {
        this.batch = batch;
        this.assets = assets;
        this.uiCamera = uiCamera;
        this.panel = assets.uiModalPatch();
    }

    public void render(LunarWorld world) {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.setColor(.012f, .021f, .027f, .91f);
        batch.draw(assets.uiWhiteTexture, 0f, 0f,
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        panel.setColor(new Color(1f, 1f, 1f, .97f));
        panel.draw(batch, 54f, 106f, 790f, 500f);
        panel.draw(batch, 872f, 106f, 354f, 500f);
        panel.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);

        text("REGISTRO LUNAR · EM ESPERA", .72f, UiTheme.CYAN, 74f, 618f);
        text("PAUSA", 2.45f, UiTheme.TEXT, 68f, 540f);
        text("A missão está congelada. Nenhum recurso será consumido.",
            .82f, UiTheme.TEXT_MUTED, 74f, 474f);
        text("RETOMAR", 1.02f, UiTheme.TEXT, 104f, 278f);
        text("ESC ou ENTER", .68f, UiTheme.CYAN, 610f, 278f);
        text("VOLTAR AO MENU", .82f, UiTheme.TEXT_MUTED, 74f, 150f);
        text("M", .74f, UiTheme.AMBER, 610f, 150f);
        text(String.format("O2 %.0f%%", world.getPlayer().getOxigenio()),
            .88f, UiTheme.TEXT, 934f, 520f);
        text(String.format("REPAROS %d/3", world.getMission().getRepairCount()),
            .88f, UiTheme.TEXT, 934f, 474f);
        text(String.format("TEMPO %.1fs", world.getPlayer().getTempoVivo()),
            .76f, UiTheme.TEXT_MUTED, 934f, 418f);
        batch.end();
        assets.font.getData().setScale(1f);
        assets.font.setColor(Color.WHITE);
    }

    private void text(String value, float scale, Color color, float x, float y) {
        assets.font.getData().setScale(scale);
        assets.font.setColor(color);
        assets.font.draw(batch, value, x, y);
    }
}
