package com.orion.echoes.lua.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Orcamento de espaco da tela de opcoes.
 *
 * A tela ficou com aparencia de quebrada porque a linha de dois toggles
 * ocupava 628px numa area util de 624px. Nada no compilador reclama disso: e
 * uma conta de largura que so aparece na tela. Estes testes guardam a conta.
 */
class SettingsLayoutTest {

    /** Largura interna do painel, ja descontado o recuo dos dois lados. */
    private static float usableWidth() {
        return GameConfig.SETTINGS_PANEL_WIDTH - GameConfig.SETTINGS_PANEL_PADDING * 2f;
    }

    @Test
    @DisplayName("As duas colunas e o intervalo cabem no painel")
    void columnsFitInsideThePanel() {
        float needed = GameConfig.SETTINGS_COLUMN_WIDTH * 2f + GameConfig.SETTINGS_COLUMN_GAP;
        assertTrue(needed <= usableWidth(),
            "as colunas ocupam " + needed + "px numa área útil de " + usableWidth() + "px");
    }

    @Test
    @DisplayName("A linha de controle cabe na coluna")
    void rowFitsInsideTheColumn() {
        float needed = GameConfig.SETTINGS_LABEL_WIDTH
            + GameConfig.SETTINGS_CONTROL_WIDTH
            + GameConfig.SETTINGS_VALUE_WIDTH
            + GameConfig.SETTINGS_VALUE_PADDING;
        assertTrue(needed <= GameConfig.SETTINGS_COLUMN_WIDTH,
            "a linha ocupa " + needed + "px numa coluna de "
                + GameConfig.SETTINGS_COLUMN_WIDTH + "px");
    }

    @Test
    @DisplayName("Sobra folga, e nao apenas um encaixe raspando")
    void layoutKeepsBreathingRoom() {
        float columnSlack = usableWidth()
            - (GameConfig.SETTINGS_COLUMN_WIDTH * 2f + GameConfig.SETTINGS_COLUMN_GAP);
        assertTrue(columnSlack >= 24f,
            "folga de apenas " + columnSlack + "px entre as colunas e a borda");
    }

    @Test
    @DisplayName("O painel cabe na janela do jogo")
    void panelFitsInsideTheWindow() {
        float marginX = 52f;
        assertTrue(marginX + GameConfig.SETTINGS_PANEL_WIDTH <= GameConfig.WINDOW_WIDTH,
            "o painel ultrapassa a largura da janela");
        float marginY = 38f;
        assertTrue(marginY + GameConfig.SETTINGS_PANEL_HEIGHT <= GameConfig.WINDOW_HEIGHT,
            "o painel ultrapassa a altura da janela");
    }
}
