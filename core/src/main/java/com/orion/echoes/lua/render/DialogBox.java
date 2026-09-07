package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

    private static final float X = 168f;
    private static final float WIDTH = 944f;
    private static final float Y = 214f;
    private static final float HEIGHT = 190f;
    private static final float PADDING = 36f;
    private static final float TEXT_SCALE = .84f;
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
        this.panel = assets.uiDialogPatch();
        this.font = assets.font;
    }

    public void update(float delta, boolean open) {
        float target = open ? 1f : 0f;
        appear = MathUtils.clamp(appear + (target - appear) * Math.min(1f, delta * 14f), 0f, 1f);
        if (!open && appear < .02f) appear = 0f;
    }

    /** Desenha dentro de um batch já aberto, em coordenadas de tela. */
    public void render(DialogueController dialog, String falante) {
        if (appear <= 0f || !dialog.isOpen()) return;
        float eased = Interpolation.pow3Out.apply(appear);
        float y = Y - (1f - eased) * 24f;

        // Escurece a cena para a fala virar o foco, sem esconder o jogo.
        batch.setColor(0f, 0f, 0f, .42f * eased);
        batch.draw(assets.uiWhiteTexture, 0f, 0f,
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        sombra(X, y, WIDTH, HEIGHT, SHADOW_SPREAD, SHADOW_OFFSET, .18f * eased);
        sombra(X, y, WIDTH, HEIGHT, SHADOW_SPREAD * .45f, SHADOW_OFFSET * .55f, .26f * eased);
        panel.setColor(new Color(1f, 1f, 1f, eased));
        panel.draw(batch, X, y, WIDTH, HEIGHT);
        panel.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);

        float textWidth = WIDTH - PADDING * 2f;
        font.getData().setScale(.66f);
        font.setColor(UiTheme.AMBER.r, UiTheme.AMBER.g, UiTheme.AMBER.b, eased);
        font.draw(batch, falante, X + PADDING, y + HEIGHT - 26f);

        // Quebra dentro da largura útil: é isto que impede o texto de vazar.
        font.getData().setScale(TEXT_SCALE);
        font.setColor(UiTheme.TEXT.r, UiTheme.TEXT.g, UiTheme.TEXT.b, eased);
        layout.setText(font, dialog.line(), font.getColor(), textWidth, Align.left, true);
        font.draw(batch, layout, X + PADDING, y + HEIGHT - 72f);

        font.getData().setScale(.62f);
        font.setColor(UiTheme.TEXT_MUTED.r, UiTheme.TEXT_MUTED.g, UiTheme.TEXT_MUTED.b, eased);
        String dica = "ESPAÇO para continuar";
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
