package com.orion.echoes.lua.world;

import com.badlogic.gdx.math.Vector2;

/** Seleciona um ponto seguro de NPC por campanha, sem nascer sobre obstáculos. */
public final class NpcSpawnSelector {
    private NpcSpawnSelector() { }

    public static Vector2 choose(long campaignSeed, String npcId, float[][] safeAnchors) {
        if (npcId == null || npcId.isBlank()) throw new IllegalArgumentException("npcId vazio");
        if (safeAnchors == null || safeAnchors.length == 0) {
            throw new IllegalArgumentException("NPC sem pontos seguros");
        }
        long mixed = campaignSeed ^ ((long) npcId.hashCode() * 0x9E3779B97F4A7C15L);
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        int index = Math.floorMod(mixed, safeAnchors.length);
        float[] anchor = safeAnchors[index];
        if (anchor == null || anchor.length < 2) throw new IllegalArgumentException("Ponto de NPC inválido");
        return new Vector2(anchor[0], anchor[1]);
    }
}
