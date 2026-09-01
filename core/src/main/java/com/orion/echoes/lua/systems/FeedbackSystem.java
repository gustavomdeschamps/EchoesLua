package com.orion.echoes.lua.systems;

import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.CraftingStation;
import com.orion.echoes.lua.entities.Portal;
import com.orion.echoes.lua.entities.RepairStation;
import com.orion.echoes.lua.world.LunarWorld;

/**
 * Camada de texto do jogo: mensagem momentanea e, quando ela expira, a dica
 * do que esta ao alcance da mao.
 *
 * Separado da tela porque HUD, pausa e tela de resultado leem a mesma frase.
 */
public final class FeedbackSystem {

    private static final float MESSAGE_DURATION = 3.5f;

    private String message = "";
    private float timer;

    public void show(String text) {
        message = text == null ? "" : text;
        timer = MESSAGE_DURATION;
    }

    /** Mantem a mensagem inicial por mais tempo, antes do jogador agir. */
    public void showFor(String text, float seconds) {
        message = text == null ? "" : text;
        timer = seconds;
    }

    public void update(float delta) {
        timer = Math.max(0f, timer - delta);
        if (timer == 0f) message = "";
    }

    public String getMessage() { return message; }

    public boolean hasMessage() { return !message.isBlank(); }

    /** A frase que o HUD mostra: a mensagem ativa ou a dica de contexto. */
    public String resolveHudText(LunarWorld world) {
        return hasMessage() ? message : contextHint(world);
    }

    private String contextHint(LunarWorld world) {
        Astronauta player = world.getPlayer();
        MissionState mission = world.getMission();
        for (RepairStation station : world.getRepairStations()) {
            if (!station.isPlayerNear(player)) continue;
            return mission.isRepaired(station.getType())
                ? station.getType().getLabel() + ": sistema ativo"
                : station.getType().getLabel() + " pronta para reparo";
        }
        CraftingStation bench = world.getCraftingStation();
        if (bench.isPlayerNear(player)) {
            if (mission.hasWeapon()) return "Bancada: arma pronta";
            return mission.hasAllWeaponParts()
                ? "Arma pronta para montagem"
                : "Bancada: faltam partes A, B e C";
        }
        Portal portal = world.getPortal();
        if (portal.isPlayerNear(player)) {
            return mission.isPortalUnlocked(player.getOxigenio())
                ? "Portal pronto para travessia"
                : "Portal inativo: conclua a missão";
        }
        if (world.getBase().isAstronautaDentro()) return "Base pressurizada: O2 recarregando";
        return "";
    }
}
