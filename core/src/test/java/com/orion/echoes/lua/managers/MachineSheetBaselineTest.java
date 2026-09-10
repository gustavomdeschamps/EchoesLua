package com.orion.echoes.lua.managers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato geometrico das maquinas animadas.
 *
 * Estacoes e refinaria tem uma exigencia que o compilador nao ve: dentro de
 * uma mesma linha da folha, corpo, base e escala precisam ser identicos, e so
 * os componentes que de fato animam - LEDs, ventoinha, prato, esteira, vapor -
 * podem mudar. O pipeline normaliza cada celula isoladamente, entao um quadro
 * em que o painel abre ou o vapor cresce arrasta a maquina inteira alguns
 * pixels para cima ou para baixo. Em jogo isso le como a maquina pulando de
 * posicao, e nao como um mecanismo funcionando.
 *
 * A medicao usa apenas pixels opacos: vapor e brilho tem alpha baixo e nao
 * podem definir onde fica o chao da maquina. Foi exatamente essa distincao
 * que separou o defeito real (refinaria, 28px) do falso positivo.
 */
class MachineSheetBaselineTest {

    /** Corpo solido; vapor, glow e particulas ficam abaixo deste limiar. */
    private static final int OPAQUE = 200;

    /** Folga aceita: um pixel de reamostragem, nao a maquina mudando de lugar. */
    private static final int MAX_DRIFT = 3;

    private static BufferedImage sheet(String name) throws IOException {
        File file = new File("../assets/textures/" + name);
        if (!file.exists()) file = new File("assets/textures/" + name);
        assertTrue(file.exists(), "folha não encontrada: " + name);
        return ImageIO.read(file);
    }

    /** Linha mais baixa com pixel opaco na celula, ou -1 quando nao ha corpo. */
    private static int opaqueBottom(BufferedImage image, int cellX, int cellY,
                                    int cellWidth, int cellHeight) {
        for (int y = cellHeight - 1; y >= 0; y--) {
            for (int x = 0; x < cellWidth; x++) {
                int alpha = (image.getRGB(cellX + x, cellY + y) >>> 24) & 0xFF;
                if (alpha > OPAQUE) return y;
            }
        }
        return -1;
    }

    private void assertRowIsAnchored(String name, int columns, int rows, int row)
            throws IOException {
        BufferedImage image = sheet(name);
        int cellWidth = image.getWidth() / columns;
        int cellHeight = image.getHeight() / rows;

        int lowest = Integer.MIN_VALUE;
        int highest = Integer.MAX_VALUE;
        for (int column = 0; column < columns; column++) {
            int bottom = opaqueBottom(image, column * cellWidth, row * cellHeight,
                cellWidth, cellHeight);
            assertTrue(bottom >= 0,
                name + ": célula " + column + "," + row + " não tem corpo opaco");
            lowest = Math.max(lowest, bottom);
            highest = Math.min(highest, bottom);
        }
        int drift = lowest - highest;
        assertTrue(drift <= MAX_DRIFT, name + ": a base da linha " + row
            + " oscila " + drift + "px entre os quadros; a máquina muda de lugar"
            + " em vez de só animar seus componentes");
    }

    @Test
    @DisplayName("A refinaria de Titã mantém a mesma base nos quatro estados")
    void titanRefineryKeepsItsGeometry() throws IOException {
        assertRowIsAnchored("titan_refinery_sheet_v2.png", 4, 1, 0);
    }

    @Test
    @DisplayName("Cada estação de Marte mantém a base entre desligada e online")
    void marsStationsKeepTheirGeometry() throws IOException {
        for (int row = 0; row < 3; row++) {
            assertRowIsAnchored("mars_station_sheet_v2.png", 4, 3, row);
        }
    }

    @Test
    @DisplayName("Cada estação lunar mantém a base entre desligada e online")
    void lunarStationsKeepTheirGeometry() throws IOException {
        for (int row = 0; row < 4; row++) {
            assertRowIsAnchored("lunar_repair_stations_v2.png", 4, 4, row);
        }
    }

    /**
     * A linha 3 do cacador de Tita serve para a queda e tambem para a reacao
     * a dano - o codigo escolhe a mesma linha nos dois casos. O quadro 0
     * estava 37px acima do chao, entao levar um tiro fazia a criatura saltar,
     * o oposto do "nao pode flutuar" do documento.
     */
    @Test
    @DisplayName("O caçador de Titã não salta ao reagir a dano")
    void titanHunterStaysGroundedWhenHit() throws IOException {
        assertRowIsAnchored("titan_hunter_sheet_v3.png", 4, 4, 3);
    }

    /**
     * Ciclo de caminhada do chefe. A base subia 10px de forma monotonica ao
     * longo dos quatro quadros e voltava de uma vez ao reiniciar o laco: nao
     * era o sobe-e-desce de um passo, era o corpo derivando para cima e
     * pipocando de volta. A leitura do passo fica por conta das pernas.
     */
    @Test
    @DisplayName("O chefe não flutua durante o ciclo de caminhada")
    void titanBossWalkCycleDoesNotDrift() throws IOException {
        assertRowIsAnchored("titan_boss_sheet_v3.png", 4, 4, 1);
    }

    /**
     * O astronauta ja estava correto quando isto foi medido - 0 a 2px em todas
     * as linhas. O teste existe para que continue assim: e o sprite que o
     * jogador olha o tempo inteiro, e um pivo que escorrega nele e o defeito
     * mais visivel que a folha pode ter.
     */
    @Test
    @DisplayName("O astronauta não muda de pivô entre os quadros")
    void astronautKeepsItsPivot() throws IOException {
        for (int row = 0; row < 4; row++) {
            assertRowIsAnchored("astronauta_sheet.png", 4, 4, row);
        }
        for (int row = 0; row < 3; row++) {
            assertRowIsAnchored("astronaut_combat_sheet.png", 4, 3, row);
        }
    }
}
