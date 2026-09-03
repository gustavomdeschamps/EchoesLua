package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.ui.UiTheme;

/**
 * Sobreposicao de depuracao: desenha as hitboxes por cima dos sprites.
 *
 * Existe porque desalinhamento de hitbox e invisivel no codigo e obvio na
 * tela. Cada cor identifica um papel, entao da para ver de relance se a caixa
 * do inimigo acompanha o desenho ou se um item flutuou para fora da sua area
 * de coleta.
 *
 * Ligada e desligada com F3; nao desenha nada quando esta desativada.
 */
public final class HitboxDebugRenderer {

    private static final float LINE = 2f;

    private final SpriteBatch batch;
    private final AssetManager assets;
    private boolean enabled;

    public HitboxDebugRenderer(SpriteBatch batch, AssetManager assets) {
        this.batch = batch;
        this.assets = assets;
    }

    public void toggle() { enabled = !enabled; }

    public boolean isEnabled() { return enabled; }

    /** Abre o lote de desenho; devolve false quando o modo esta desligado. */
    public boolean begin(OrthographicCamera camera) {
        if (!enabled) return false;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        return true;
    }

    public void end() {
        if (!enabled) return;
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Contorno da hitbox de uma entidade. */
    public void box(Rectangle bounds, Color color) {
        if (!enabled || bounds == null) return;
        outline(bounds.x, bounds.y, bounds.width, bounds.height, color);
    }

    /**
     * Retangulo do sprite, para comparar com a hitbox.
     *
     * Quando os dois contornos nao compartilham o centro horizontal, ou quando
     * a base do sprite nao encosta na base da caixa, o desalinhamento aparece.
     */
    public void sprite(float x, float y, float width, float height) {
        if (!enabled) return;
        outline(x, y, width, height, UiTheme.TEXT_MUTED);
    }

    /** Cruz no ponto usado como centro para mira, barra de vida e particulas. */
    public void center(float x, float y, Color color) {
        if (!enabled) return;
        batch.setColor(color.r, color.g, color.b, .95f);
        batch.draw(assets.uiWhiteTexture, x - 7f, y - LINE / 2f, 14f, LINE);
        batch.draw(assets.uiWhiteTexture, x - LINE / 2f, y - 7f, LINE, 14f);
    }

    private void outline(float x, float y, float width, float height, Color color) {
        batch.setColor(color.r, color.g, color.b, .8f);
        batch.draw(assets.uiWhiteTexture, x, y, width, LINE);
        batch.draw(assets.uiWhiteTexture, x, y + height - LINE, width, LINE);
        batch.draw(assets.uiWhiteTexture, x, y, LINE, height);
        batch.draw(assets.uiWhiteTexture, x + width - LINE, y, LINE, height);
    }
}
