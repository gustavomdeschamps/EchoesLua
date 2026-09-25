package com.orion.echoes.lua.systems;

import java.util.LinkedHashSet;
import java.util.Set;

/** Persistent expedition equipment. Keys can only be awarded by boss completion. */
public final class Inventario {
    public enum Difficulty {
        FACIL("FÁCIL", .72f, .80f, .90f),
        NORMAL("NORMAL", 1f, 1f, 1f),
        DIFICIL("DIFÍCIL", 1.35f, 1.25f, 1.15f);
        private final String label;
        private final float damageMultiplier;
        private final float oxygenMultiplier;
        private final float enemySpeedMultiplier;
        Difficulty(String label, float damageMultiplier, float oxygenMultiplier,
                   float enemySpeedMultiplier) {
            this.label = label;
            this.damageMultiplier = damageMultiplier;
            this.oxygenMultiplier = oxygenMultiplier;
            this.enemySpeedMultiplier = enemySpeedMultiplier;
        }
        public String label() { return label; }
        public float damageMultiplier() { return damageMultiplier; }
        public float oxygenMultiplier() { return oxygenMultiplier; }
        public float enemySpeedMultiplier() { return enemySpeedMultiplier; }
        public static Difficulty fromSave(String value) {
            if (value != null) for (Difficulty option : values())
                if (option.name().equals(value)) return option;
            return NORMAL;
        }
    }
    public static final String CHAVE_LUA = "CHAVE_LUA";
    public static final String CHAVE_MARTE = "CHAVE_MARTE";
    public static final String CHAVE_TITA = "CHAVE_TITA";
    public static final String CHAVE_LUZ = "CHAVE_LUZ";
    private final Set<String> chaves = new LinkedHashSet<>();
    private int comida, nivelArma, nivelArmadura;
    private int reserveAmmo;
    private Difficulty difficulty = Difficulty.NORMAL;
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty value) { difficulty = value == null ? Difficulty.NORMAL : value; }
    /** Visual placement only: item identity/quantities remain in their own fields. */
    private final int[] slots = new int[32];

    public Inventario() {
        // Ração, células, gelo, rifle and four boss keys.
        for (int i = 0; i < 8; i++) slots[i] = i + 1;
    }

    public int itemAt(int slot) { return slot >= 0 && slot < slots.length ? slots[slot] : 0; }
    public void swapSlots(int a, int b) {
        if (a < 0 || b < 0 || a >= slots.length || b >= slots.length) return;
        int item = slots[a]; slots[a] = slots[b]; slots[b] = item;
    }

    public boolean tem(String id) { return chaves.contains(id); }
    public void add(String id) {
        if (CHAVE_LUA.equals(id) || CHAVE_MARTE.equals(id)
            || CHAVE_TITA.equals(id) || CHAVE_LUZ.equals(id)) chaves.add(id);
        else if ("COMIDA".equals(id)) comida++;
    }
    public int getComida() { return comida; }
    public int getReserveAmmo() { return reserveAmmo; }
    public int addReserveAmmo(int amount) {
        int previous = reserveAmmo;
        reserveAmmo = Math.max(0, Math.min(240, reserveAmmo + Math.max(0, amount)));
        return reserveAmmo - previous;
    }
    /** Returns the new magazine count after moving crafted cells into it. */
    public int reload(int loaded, int capacity) {
        int moved = Math.min(Math.max(0, capacity - loaded), reserveAmmo);
        reserveAmmo -= moved;
        return loaded + moved;
    }
    public void restoreReserveAmmo(int value) { reserveAmmo = Math.max(0, Math.min(240, value)); }
    public int[] exportSlots() { return slots.clone(); }
    public void restoreSlots(int[] saved) {
        if (saved == null || saved.length != slots.length) return;
        boolean[] seen = new boolean[9];
        for (int item : saved) {
            if (item < 0 || item > 8 || (item != 0 && seen[item])) return;
            if (item != 0) seen[item] = true;
        }
        for (int item = 1; item <= 8; item++) if (!seen[item]) return;
        System.arraycopy(saved, 0, slots, 0, slots.length);
    }
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
    public float getMultiplicadorDanoRecebido() {
        return (1f - .15f * nivelArmadura) * difficulty.damageMultiplier();
    }
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
