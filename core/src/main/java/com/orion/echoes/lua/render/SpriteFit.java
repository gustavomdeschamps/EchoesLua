package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * Desenho que preserva a proporcao da arte.
 *
 * As celulas dos atlas sao quadradas (313x313), mas varios props eram
 * desenhados em retangulos de outra proporcao - a plataforma de pouso
 * esticava 32%, o portal achatava 15%. Esticar sprite e o que faz a arte
 * parecer "bugada" mesmo quando o arquivo esta correto.
 *
 * Aqui a arte e encaixada dentro do espaco pedido sem deformar, apoiada na
 * base (os props tocam o chao) e centrada na horizontal. O retangulo
 * resultante fica disponivel para a hitbox usar a MESMA medida, que e o que
 * mantem colisao e desenho juntos.
 */
public final class SpriteFit {

    private SpriteFit() { }

    /**
     * Calcula o retangulo de desenho que cabe no espaco pedido sem deformar.
     *
     * @param out recebe o resultado e e devolvido, para evitar alocacao por frame
     */
    public static Rectangle fit(TextureRegion region, float x, float y,
                                float boxWidth, float boxHeight, Rectangle out) {
        float regionWidth = region.getRegionWidth();
        float regionHeight = region.getRegionHeight();
        if (regionWidth <= 0f || regionHeight <= 0f) {
            return out.set(x, y, boxWidth, boxHeight);
        }
        float scale = Math.min(boxWidth / regionWidth, boxHeight / regionHeight);
        float drawWidth = regionWidth * scale;
        float drawHeight = regionHeight * scale;
        // Centrado na horizontal, apoiado na base: prop nao flutua nem afunda.
        return out.set(x + (boxWidth - drawWidth) / 2f, y, drawWidth, drawHeight);
    }

    /** Desenha ja encaixado; devolve o retangulo usado. */
    public static Rectangle draw(SpriteBatch batch, TextureRegion region,
                                 float x, float y, float boxWidth, float boxHeight,
                                 Rectangle out) {
        fit(region, x, y, boxWidth, boxHeight, out);
        batch.draw(region, out.x, out.y, out.width, out.height);
        return out;
    }
}
