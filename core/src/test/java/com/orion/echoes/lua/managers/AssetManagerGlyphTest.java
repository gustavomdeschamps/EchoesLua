package com.orion.echoes.lua.managers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AssetManagerGlyphTest {

    @Test
    void gameFontContainsEveryNonAsciiHudSymbol() {
        for (char symbol : new char[] {'•', '·', '→', '←', '○', '●'}) {
            assertTrue(AssetManager.GAME_GLYPHS.indexOf(symbol) >= 0,
                () -> "Glifo ausente na fonte do HUD: " + symbol);
        }
    }
}
