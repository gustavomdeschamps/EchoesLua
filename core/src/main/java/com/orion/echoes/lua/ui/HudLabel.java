package com.orion.echoes.lua.ui;

/**
 * Rotulo numerico do HUD, remontado so quando o valor inteiro muda.
 *
 * Os tres HUDs de gameplay montavam os mesmos numeros com String.format ou
 * concatenacao dentro do render: sete strings por quadro na Lua, cinco em
 * Marte e mais em Tita. Sao valores que mudam poucas vezes por partida sendo
 * reconstruidos 60 vezes por segundo, direto na pressao de GC do combate.
 *
 * O rotulo e montado a partir de pedacos fixos intercalados com os valores:
 * {@code new HudLabel("O2  ", "%")} produz {@code O2  87%}. Nenhum caminho
 * aloca quando o valor repete - nem o array de argumentos.
 */
public final class HudLabel {

    private final String[] parts;
    private final int[] values;
    private final StringBuilder builder = new StringBuilder(48);
    private String text;
    private boolean primed;

    /**
     * @param parts pedacos fixos; o numero de valores e {@code parts.length - 1}
     */
    public HudLabel(String... parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("um rótulo precisa de pelo menos dois pedaços");
        }
        this.parts = parts.clone();
        this.values = new int[parts.length - 1];
        this.text = String.join("", parts);
    }

    public String of(int first) {
        requireSlots(1);
        if (primed && values[0] == first) return text;
        values[0] = first;
        return rebuild();
    }

    public String of(int first, int second) {
        requireSlots(2);
        if (primed && values[0] == first && values[1] == second) return text;
        values[0] = first;
        values[1] = second;
        return rebuild();
    }

    public String of(int first, int second, int third) {
        requireSlots(3);
        if (primed && values[0] == first && values[1] == second && values[2] == third) return text;
        values[0] = first;
        values[1] = second;
        values[2] = third;
        return rebuild();
    }

    private void requireSlots(int expected) {
        if (values.length != expected) {
            throw new IllegalArgumentException("o rótulo espera " + values.length + " valores");
        }
    }

    private String rebuild() {
        primed = true;
        builder.setLength(0);
        for (int index = 0; index < values.length; index++) {
            builder.append(parts[index]).append(values[index]);
        }
        builder.append(parts[parts.length - 1]);
        text = builder.toString();
        return text;
    }
}
