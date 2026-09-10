package com.orion.echoes.lua.systems;

import com.orion.echoes.lua.config.GameConfig;

/**
 * Trava com histerese para a corrida.
 *
 * Quando a reserva chega ao fim, manter Shift pressionado não pode alternar
 * WALK/RUN a cada quadro. A corrida só volta depois de o jogador soltar Shift
 * ou recuperar uma reserva mínima de energia.
 */
public final class SprintGate {
    private boolean exhausted;

    public boolean resolve(boolean wantsToRun, boolean moving, float energy) {
        if (!wantsToRun) exhausted = false;
        if (energy <= 1f) exhausted = true;
        if (exhausted && energy >= GameConfig.PLAYER_RUN_RESUME_ENERGY) exhausted = false;
        return wantsToRun && moving && !exhausted;
    }

    public boolean isExhausted() {
        return exhausted;
    }
}
