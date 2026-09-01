package com.orion.echoes.lua.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.IntArray;

/**
 * Grade grosseira de navegacao usada para garantir que um layout gerado por
 * semente e completavel.
 *
 * Sem esta checagem, um sorteio infeliz pode encurralar uma peca atras de um
 * anel de rochas e travar a missao. O flood-fill parte da posicao inicial do
 * jogador e responde, para qualquer ponto, se ele esta do lado de fora.
 *
 * A classe e propositalmente pura: nao conhece LibGDX alem de Rectangle, o que
 * a torna testavel sem abrir janela.
 */
public final class ReachabilityGrid {

    private final float cellSize;
    private final int columns;
    private final int rows;
    private final boolean[] blocked;
    private final boolean[] reachable;

    public ReachabilityGrid(float worldWidth, float worldHeight, float cellSize) {
        if (cellSize <= 0f) throw new IllegalArgumentException("cellSize deve ser positivo.");
        this.cellSize = cellSize;
        this.columns = Math.max(1, (int) Math.ceil(worldWidth / cellSize));
        this.rows = Math.max(1, (int) Math.ceil(worldHeight / cellSize));
        this.blocked = new boolean[columns * rows];
        this.reachable = new boolean[columns * rows];
    }

    /** Marca como bloqueada toda celula tocada pelo retangulo. */
    public void block(Rectangle rectangle) {
        block(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    public void block(float x, float y, float width, float height) {
        int fromColumn = clampColumn((int) Math.floor(x / cellSize));
        int toColumn = clampColumn((int) Math.floor((x + width) / cellSize));
        int fromRow = clampRow((int) Math.floor(y / cellSize));
        int toRow = clampRow((int) Math.floor((y + height) / cellSize));
        for (int row = fromRow; row <= toRow; row++) {
            for (int column = fromColumn; column <= toColumn; column++) {
                blocked[row * columns + column] = true;
            }
        }
    }

    /** Propaga a partir do ponto de origem em quatro direcoes. */
    public void floodFrom(float x, float y) {
        java.util.Arrays.fill(reachable, false);
        int start = indexOf(x, y);
        if (blocked[start]) {
            start = nearestFreeIndex(start);
            if (start < 0) return;
        }
        IntArray queue = new IntArray();
        queue.add(start);
        reachable[start] = true;
        while (queue.size > 0) {
            int index = queue.pop();
            int column = index % columns;
            int row = index / columns;
            visit(queue, column - 1, row);
            visit(queue, column + 1, row);
            visit(queue, column, row - 1);
            visit(queue, column, row + 1);
        }
    }

    private void visit(IntArray queue, int column, int row) {
        if (column < 0 || column >= columns || row < 0 || row >= rows) return;
        int index = row * columns + column;
        if (blocked[index] || reachable[index]) return;
        reachable[index] = true;
        queue.add(index);
    }

    public boolean isReachable(float x, float y) {
        return reachable[indexOf(x, y)];
    }

    public boolean isBlocked(float x, float y) {
        return blocked[indexOf(x, y)];
    }

    /** Quantas celulas o flood-fill alcancou; util para medir o mapa util. */
    public int getReachableCount() {
        int total = 0;
        for (boolean value : reachable) if (value) total++;
        return total;
    }

    public int getColumns() { return columns; }

    public int getRows() { return rows; }

    private int indexOf(float x, float y) {
        int column = clampColumn((int) Math.floor(x / cellSize));
        int row = clampRow((int) Math.floor(y / cellSize));
        return row * columns + column;
    }

    /** Busca em espiral simples pela celula livre mais proxima da origem. */
    private int nearestFreeIndex(int origin) {
        int originColumn = origin % columns;
        int originRow = origin / columns;
        for (int radius = 1; radius < Math.max(columns, rows); radius++) {
            for (int row = originRow - radius; row <= originRow + radius; row++) {
                for (int column = originColumn - radius; column <= originColumn + radius; column++) {
                    if (column < 0 || column >= columns || row < 0 || row >= rows) continue;
                    int index = row * columns + column;
                    if (!blocked[index]) return index;
                }
            }
        }
        return -1;
    }

    private int clampColumn(int column) { return Math.max(0, Math.min(columns - 1, column)); }

    private int clampRow(int row) { return Math.max(0, Math.min(rows - 1, row)); }
}
