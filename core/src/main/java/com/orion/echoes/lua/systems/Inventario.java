package com.orion.echoes.lua.systems;

import java.util.LinkedHashSet;
import java.util.Set;

/** Persistent expedition equipment. Keys can only be awarded by boss completion. */
public final class Inventario {
    public static final String CHAVE_LUA = "CHAVE_LUA";
    public static final String CHAVE_MARTE = "CHAVE_MARTE";
    public static final String CHAVE_TITA = "CHAVE_TITA";
    public static final String CHAVE_LUZ = "CHAVE_LUZ";
    private final Set<String> chaves = new LinkedHashSet<>();
    private int comida, nivelArma, nivelArmadura;

    public boolean tem(String id) { return chaves.contains(id); }
    public void add(String id) {
        if (CHAVE_LUA.equals(id) || CHAVE_MARTE.equals(id)
            || CHAVE_TITA.equals(id) || CHAVE_LUZ.equals(id)) chaves.add(id);
        else if ("COMIDA".equals(id)) comida++;
    }
    public int getComida() { return comida; }
    public boolean consumirComida() { if (comida <= 0) return false; comida--; return true; }
    public int getNivelArma() { return nivelArma; }
    public int getNivelArmadura() { return nivelArmadura; }
    /**
     * Dano do rifle por nivel de arma.
     *
     * O guia da prova sugere {@code 10 + 5 * nivelArma}, calibrado para
     * inimigos de exemplo com poucos pontos de vida. Neste projeto o cacador de
     * Ti(t)a tem 125 HP e os chefes vao de 120 a 220, com pente de 30 balas: com
     * base 10 uma unica forma de Calisto custaria 22 acertos e a fase travaria
     * por municao. A base virou 20 e o incremento de 5 por nivel do guia foi
     * preservado, que e o que a HUD mostra e a banca confere.
     */
    public float getDano() { return 20f + 5f * nivelArma; }
    public float getMultiplicadorDanoRecebido() { return 1f - .15f * nivelArmadura; }
    public boolean melhorarArma() { if (nivelArma >= 3) return false; nivelArma++; return true; }
    public boolean melhorarArmadura() { if (nivelArmadura >= 3) return false; nivelArmadura++; return true; }
    public String[] exportarChaves() { return chaves.toArray(new String[0]); }
    public void restaurar(String[] keys, int food, int weaponLevel, int armorLevel) {
        chaves.clear();
        if (keys != null) for (String key : keys) add(key);
        comida = Math.max(0, food);
        nivelArma = Math.max(0, Math.min(3, weaponLevel));
        nivelArmadura = Math.max(0, Math.min(3, armorLevel));
    }
}
