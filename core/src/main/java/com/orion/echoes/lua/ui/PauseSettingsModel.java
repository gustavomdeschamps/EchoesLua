package com.orion.echoes.lua.ui;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * Lista de opcoes navegavel por teclado, usada pela pausa.
 *
 * A pausa oferecia apenas retomar e voltar ao menu. Para mudar volume ou
 * desligar o tremor de camera o jogador tinha de abandonar a partida, ir ao
 * menu e comecar de novo -- ou seja, a configuracao existia mas nao estava
 * ao alcance de quem estava jogando.
 *
 * O menu principal monta suas opcoes em Scene2D. Trazer Scene2D para dentro
 * do gameplay significaria um segundo processador de input disputando o
 * teclado com o jogo, e o documento e explicito sobre nao introduzir Scene2D
 * so por causa de uma tela. Entao a pausa desenha as opcoes no mesmo modo
 * imediato do resto do overlay, e a navegacao vive aqui: sem LibGDX grafico,
 * sem Preferences, so a regra de selecao e ajuste, que e o que pode quebrar
 * em silencio.
 *
 * Os acessores sao funcoes em vez de uma referencia a AppSettings de
 * proposito: e o que deixa a regra testavel sem subir uma aplicacao.
 */
public final class PauseSettingsModel {

    /** Passo de um toque em barra continua: vinte toques cobrem a faixa. */
    public static final float STEP = .05f;

    public interface FloatAccessor {
        float get();
        void set(float value);
    }

    public interface BoolAccessor {
        boolean get();
        void set(boolean value);
    }

    public enum Kind { SLIDER, TOGGLE }

    private static final class Row {
        final String label;
        final Kind kind;
        final FloatAccessor number;
        final BoolAccessor flag;

        Row(String label, FloatAccessor number) {
            this.label = label;
            this.kind = Kind.SLIDER;
            this.number = number;
            this.flag = null;
        }

        Row(String label, BoolAccessor flag) {
            this.label = label;
            this.kind = Kind.TOGGLE;
            this.number = null;
            this.flag = flag;
        }
    }

    private final Array<Row> rows = new Array<>();
    private int selected;

    public PauseSettingsModel addSlider(String label, FloatAccessor accessor) {
        rows.add(new Row(label, accessor));
        return this;
    }

    public PauseSettingsModel addToggle(String label, BoolAccessor accessor) {
        rows.add(new Row(label, accessor));
        return this;
    }

    public int size() { return rows.size; }

    public int getSelected() { return selected; }

    public String label(int index) { return rows.get(index).label; }

    public Kind kind(int index) { return rows.get(index).kind; }

    /** Progresso 0..1 da linha, para desenhar a barra ou o marcador. */
    public float ratio(int index) {
        Row row = rows.get(index);
        return row.kind == Kind.SLIDER
            ? MathUtils.clamp(row.number.get(), 0f, 1f)
            : (row.flag.get() ? 1f : 0f);
    }

    public String valueText(int index) {
        Row row = rows.get(index);
        if (row.kind == Kind.TOGGLE) return row.flag.get() ? "LIGADO" : "DESLIGADO";
        return Math.round(MathUtils.clamp(row.number.get(), 0f, 1f) * 100f) + "%";
    }

    /**
     * Move a selecao, dando a volta nas pontas.
     *
     * Lista curta com quatro ou cinco linhas: dar a volta poupa o jogador de
     * atravessar tudo de novo para chegar na primeira opcao.
     */
    public void moveSelection(int delta) {
        if (rows.size == 0) return;
        selected = ((selected + delta) % rows.size + rows.size) % rows.size;
    }

    /**
     * Ajusta a linha selecionada.
     *
     * Direita e esquerda tem o mesmo significado nos dois tipos de linha:
     * direita aumenta ou liga, esquerda diminui ou desliga. Um liga/desliga
     * que alternasse em qualquer direcao faria o jogador precisar lembrar em
     * que estado a opcao estava para saber o que a tecla vai fazer.
     */
    public void adjust(int direction) {
        if (rows.size == 0 || direction == 0) return;
        Row row = rows.get(selected);
        if (row.kind == Kind.TOGGLE) {
            row.flag.set(direction > 0);
            return;
        }
        row.number.set(MathUtils.clamp(row.number.get() + STEP * Math.signum(direction),
            0f, 1f));
    }

    /** Enter/Espaco numa linha de liga/desliga: alterna. Em barra, nao faz nada. */
    public void activate() {
        if (rows.size == 0) return;
        Row row = rows.get(selected);
        if (row.kind == Kind.TOGGLE) row.flag.set(!row.flag.get());
    }
}
