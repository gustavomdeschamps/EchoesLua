package com.orion.echoes.lua.systems;

/** Alvo aceito pelo contrato de tiro das fases Marte/Titã. */
public interface CombatTarget {
    float centerX();
    float centerY();
    boolean isAlive();
    boolean receiveDamage(float damage);
}
