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

    /** Altura util das colunas, ja descontados titulo, rodape e recuos. */
    private static float usableColumnHeight() {
        return GameConfig.SETTINGS_PANEL_HEIGHT
            - GameConfig.SETTINGS_PANEL_PADDING * 2f
            - GameConfig.SETTINGS_TITLE_BLOCK
            - GameConfig.SETTINGS_FOOTER_BLOCK;
    }

    private static float sliderRow() {
        return GameConfig.SETTINGS_SLIDER_HEIGHT + GameConfig.SETTINGS_ROW_GAP;
    }

    private static float toggleRow() {
        return GameConfig.SETTINGS_TOGGLE_HEIGHT + GameConfig.SETTINGS_ROW_GAP;
    }

    /** Cabecalho de secao; o primeiro da coluna nao paga o respiro de cima. */
    private static float sectionRow(boolean first) {
        return GameConfig.SETTINGS_SECTION_HEIGHT
            + (first ? 0f : GameConfig.SETTINGS_SECTION_SPACING);
    }

    /**
     * Coluna de audio: musica, efeitos, interface e ambiente.
     *
     * O barramento de ambiente era o unico sem controle proprio - herdava o
     * volume dos efeitos -, e passos e vento subiam junto com o rifle.
     */
    @Test
    @DisplayName("A coluna de áudio cabe na altura do painel")
    void audioColumnFitsInsideThePanel() {
        float needed = sectionRow(true) + sliderRow() * 4f;
        assertTrue(needed <= usableColumnHeight(),
            "a coluna de áudio ocupa " + needed + "px numa altura útil de "
                + usableColumnHeight() + "px");
    }

    /**
     * Coluna da direita: video, interface, acessibilidade e controles.
     *
     * A opcao de tela cheia ja existia em AppSettings e era gravada, mas nao
     * tinha controle nenhum na tela nem era aplicada.
     */
    @Test
    @DisplayName("A coluna de vídeo e acessibilidade cabe na altura do painel")
    void systemColumnFitsInsideThePanel() {
        float needed = sectionRow(true) + toggleRow()
            + sectionRow(false) + sliderRow()
            + sectionRow(false) + toggleRow() * 3f
            + sectionRow(false) + toggleRow();
        assertTrue(needed <= usableColumnHeight(),
            "a coluna da direita ocupa " + needed + "px numa altura útil de "
                + usableColumnHeight() + "px");
    }

    @Test
    @DisplayName("Sobra altura para mais um controle sem refazer o painel")
    void columnsKeepVerticalHeadroom() {
        float tallest = sectionRow(true) + toggleRow()
            + sectionRow(false) + sliderRow()
            + sectionRow(false) + toggleRow() * 3f
            + sectionRow(false) + toggleRow();
        assertTrue(usableColumnHeight() - tallest >= toggleRow(),
            "a coluna mais alta deixa só " + (usableColumnHeight() - tallest)
                + "px de folga");
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
