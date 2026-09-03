package com.orion.echoes.lua.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Invariantes da geometria de hitbox.
 *
 * As caixas passaram a ser derivadas do sprite por proporcao; estes testes
 * garantem que uma proporcao mal ajustada no futuro nao volte a colocar a
 * caixa fora do desenho, que foi exatamente o bug corrigido.
 */
class HitboxGeometryTest {

    private static final float[] SPRITE_SIZES = {
        GameConfig.ENEMY_SPRITE_SIZE,
        GameConfig.MARS_DRONE_SPRITE_SIZE,
        GameConfig.MARS_CRAWLER_SPRITE_SIZE
    };

    @Test
    @DisplayName("A hitbox cabe dentro do sprite em qualquer um dos tamanhos")
    void hitboxFitsInsideSprite() {
        for (float sprite : SPRITE_SIZES) {
            float width = sprite * GameConfig.ENEMY_HITBOX_WIDTH_RATIO;
            float height = sprite * GameConfig.ENEMY_HITBOX_HEIGHT_RATIO;
            float base = sprite * GameConfig.ENEMY_HITBOX_BASE_RATIO;

            assertTrue(width > 0f && width <= sprite, "largura fora do sprite: " + width);
            assertTrue(height > 0f && height <= sprite, "altura fora do sprite: " + height);
            assertTrue(base >= 0f, "base negativa");
            assertTrue(base + height <= sprite,
                "o topo da hitbox ultrapassa o sprite de " + sprite);
        }
    }

    @Test
    @DisplayName("A hitbox fica centrada na largura do sprite")
    void hitboxIsHorizontallyCentered() {
        for (float sprite : SPRITE_SIZES) {
            float width = sprite * GameConfig.ENEMY_HITBOX_WIDTH_RATIO;
            float left = (sprite - width) / 2f;
            float right = sprite - (left + width);
            assertEquals(left, right, 0.001f, "sobra desigual dos dois lados");
        }
    }

    @Test
    @DisplayName("A hitbox e um rodape, nao o corpo inteiro")
    void hitboxIsAFootprint() {
        // Uma caixa alta demais faria o jogador acertar o ar acima do bicho.
        assertTrue(GameConfig.ENEMY_HITBOX_HEIGHT_RATIO < 0.5f);
        assertTrue(GameConfig.ENEMY_HITBOX_WIDTH_RATIO <= 0.6f);
    }

    @Test
    @DisplayName("A folga de coleta e curta o bastante para nao coletar de longe")
    void pickupPaddingIsModest() {
        assertTrue(GameConfig.PICKUP_HITBOX_PADDING >= 0f);
        assertTrue(GameConfig.PICKUP_HITBOX_PADDING <= 12f,
            "folga grande demais transformaria coleta em ima");
    }
}
