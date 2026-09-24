package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.systems.DialogueController;
import com.orion.echoes.lua.ui.UiTheme;

/**
 * Caixa de fala.
 *
 * Dois defeitos que ela resolve: o texto era desenhado sem largura máxima,
 * então frases longas passavam por fora do painel; e a sombra era um bloco
 * quadrado atrás de um painel de cantos arredondados.
 *
 * Aqui o texto é medido com GlyphLayout e quebrado dentro da largura útil, e
 * a sombra usa o próprio 9-patch em duas camadas, acompanhando a silhueta.
 */
public final class DialogBox {

    /** Reaproveitada por quadro enquanto a caixa esta aberta. */
    private final Color panelColor = new Color();

    private static final float X = 100f;
    private static final float WIDTH = 1080f;
    private static final float Y = 78f;
    private static final float HEIGHT = 270f;
    private static final float PADDING = 36f;
    private static final float TEXT_SCALE = .68f;
    private static final float SHADOW_SPREAD = 8f;
    private static final float SHADOW_OFFSET = 5f;

    private final SpriteBatch batch;
    private final AssetManager assets;
    private final NinePatch panel;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final Color shadowColor = new Color();
    private float appear;

    public DialogBox(SpriteBatch batch, AssetManager assets) {
        this.batch = batch;
        this.assets = assets;
        this.panel = assets.uiPanelPatch();
        this.font = assets.font;
    }

    public void update(float delta, boolean open) {
        float target = open ? 1f : 0f;
        appear = MathUtils.clamp(appear + (target - appear) * Math.min(1f, delta * 14f), 0f, 1f);
        if (!open && appear < .02f) appear = 0f;
    }

    /** Desenha dentro de um batch já aberto, em coordenadas de tela. */
    public void render(DialogueController dialog, String falante) {
        render(dialog, falante, assets.npcCommanderFrame(0, 1));
    }

    public void render(DialogueController dialog, String falante,
                       com.badlogic.gdx.graphics.g2d.TextureRegion portrait) {
        if (appear <= 0f || !dialog.isOpen()) return;
        float eased = Interpolation.pow3Out.apply(appear);
        float y = Y - (1f - eased) * 24f;

        // Escurece a cena para a fala virar o foco, sem esconder o jogo.
        batch.setColor(0f, 0f, 0f, .42f * eased);
        batch.draw(assets.uiWhiteTexture, 0f, 0f,
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        batch.setColor(Color.WHITE);

        sombra(X, y, WIDTH, HEIGHT, SHADOW_SPREAD, SHADOW_OFFSET, .18f * eased);
        sombra(X, y, WIDTH, HEIGHT, SHADOW_SPREAD * .45f, SHADOW_OFFSET * .55f, .26f * eased);
        panelColor.set(1f, 1f, 1f, eased);
        panel.setColor(panelColor);
        panel.draw(batch, X, y, WIDTH, HEIGHT);
        panel.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);

        boolean playerTurn = dialog.isPlayerTurn();
        float left = X + 20f, right = X + WIDTH - 208f;
        float portraitY = y + 45f;
        batch.setColor(.025f, .055f, .08f, .9f * eased);
        batch.draw(assets.uiWhiteTexture, left, portraitY, 188f, 180f);
        batch.draw(assets.uiWhiteTexture, right, portraitY, 188f, 180f);
        batch.setColor(1f, 1f, 1f, (playerTurn ? .65f : 1f) * eased);
        SpriteFit.draw(batch, portrait, left + 3f, portraitY + 3f, 182f, 174f);
        batch.setColor(1f, 1f, 1f, (playerTurn ? 1f : .65f) * eased);
        SpriteFit.draw(batch, new TextureRegion(assets.playerPortraitTexture),
            right + 3f, portraitY + 3f, 182f, 174f);
        batch.setColor(Color.WHITE);

        float textX = X + 232f;
        float textWidth = WIDTH - 464f;
        font.getData().setScale(.68f);
        font.setColor(UiTheme.AMBER.r, UiTheme.AMBER.g, UiTheme.AMBER.b, eased);
        font.draw(batch, playerTurn ? "EXPLORADOR" : falante, textX, y + HEIGHT - 42f);

        // Quebra dentro da largura útil: é isto que impede o texto de vazar.
        font.getData().setScale(TEXT_SCALE);
        font.setColor(UiTheme.TEXT.r, UiTheme.TEXT.g, UiTheme.TEXT.b, eased);
        layout.setText(font, dialog.line(), font.getColor(), textWidth, Align.left, true);
        font.draw(batch, layout, textX, y + HEIGHT - 91f);

        batch.setColor(UiTheme.CYAN.r, UiTheme.CYAN.g, UiTheme.CYAN.b, .55f * eased);
        for (int i = 0; i < dialog.lineCount(); i++)
            batch.draw(assets.uiWhiteTexture, textX + i * 36f, y + 23f,
                24f, i < dialog.lineNumber() ? 4f : 2f);
        batch.setColor(Color.WHITE);
        font.getData().setScale(.54f);
        font.setColor(UiTheme.TEXT_MUTED.r, UiTheme.TEXT_MUTED.g, UiTheme.TEXT_MUTED.b, eased);
        String dica = dialog.lineNumber() + "/" + dialog.lineCount();
        layout.setText(font, dica);
        font.draw(batch, dica, X + WIDTH - PADDING - layout.width, y + 32f);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private void sombra(float x, float y, float width, float height,
                        float spread, float offset, float alpha) {
        shadowColor.set(0f, 0f, 0f, alpha);
        panel.setColor(shadowColor);
        panel.draw(batch, x - spread + offset, y - spread - offset,
            width + spread * 2f, height + spread * 2f);
        panel.setColor(Color.WHITE);
    }
}
