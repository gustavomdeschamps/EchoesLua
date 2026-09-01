package com.orion.echoes.lua.systems;

import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.RepairStation;
import com.orion.echoes.lua.managers.ParticleManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.world.LunarWorld;

/**
 * Tudo que a tecla de interacao resolve: reparo, montagem da arma,
 * processamento de gelo e travessia do portal.
 *
 * A ordem de teste e a ordem de prioridade que o jogador espera - o que esta
 * embaixo da mao ganha do que esta em volta.
 */
public final class InteractionSystem {

    /** O que a interacao produziu neste frame. */
    public enum Result { NOTHING, HANDLED, PORTAL_CROSSED }

    private final SoundManager sounds;
    private final ParticleManager particles;
    private final JuiceSystem juice;
    private final FeedbackSystem feedback;

    public InteractionSystem(SoundManager sounds, ParticleManager particles,
                             JuiceSystem juice, FeedbackSystem feedback) {
        this.sounds = sounds;
        this.particles = particles;
        this.juice = juice;
        this.feedback = feedback;
    }

    public Result interact(LunarWorld world) {
        Astronauta player = world.getPlayer();
        MissionState mission = world.getMission();

        for (RepairStation station : world.getRepairStations()) {
            if (!station.isPlayerNear(player)) continue;
            repair(station, mission);
            return Result.HANDLED;
        }

        if (world.getPortal().isPlayerNear(player)) {
            if (mission.isPortalUnlocked(player.getOxigenio())) return Result.PORTAL_CROSSED;
            feedback.show("Portal bloqueado. " + mission.getObjective(player.getOxigenio()));
            return Result.HANDLED;
        }

        if (world.getCraftingStation().isPlayerNear(player)) {
            craft(mission, player);
            return Result.HANDLED;
        }

        if (!world.getBase().isAstronautaDentro()) {
            feedback.show("Nada para usar aqui.");
            return Result.NOTHING;
        }

        if (mission.hasAllWeaponParts() && !mission.hasWeapon()) {
            feedback.show("Use a bancada laranja dentro da base para fabricar a arma.");
            return Result.HANDLED;
        }

        processIce(world, mission);
        return Result.HANDLED;
    }

    private void repair(RepairStation station, MissionState mission) {
        if (station.repair(mission)) {
            feedback.show(station.getType().getLabel() + " ONLINE  •  "
                + MissionState.getPerkLabel(station.getType()));
            particles.criarProcessamento(station.getPosition().x + 63f,
                station.getPosition().y + 63f);
            sounds.tocarReparoConcluido();
            juice.trigger(JuiceSystem.Preset.REPAIR);
            return;
        }
        if (mission.isRepaired(station.getType())) {
            feedback.show(station.getType().getLabel() + " já está online.");
            return;
        }
        feedback.show("Falta a peça de " + station.getType().getRequiredPart().getLabel() + ".");
    }

    private void craft(MissionState mission, Astronauta player) {
        if (mission.hasAllWeaponParts() && !mission.hasWeapon()) {
            mission.craftWeapon();
            player.adicionarMunicao(GameConfig.AMMO_ON_CRAFT);
            feedback.show("Arma montada com " + player.getMunicao()
                + " células. Mire com o mouse e dispare.");
            sounds.tocarCraft();
            juice.trigger(JuiceSystem.Preset.CRAFT);
            return;
        }
        feedback.show(mission.hasWeapon() ? "Arma equipada." : "Ainda faltam partes da arma.");
    }

    private void processIce(LunarWorld world, MissionState mission) {
        if (!world.getBase().processarGelo(world.getPlayer(), mission.getIceYieldMultiplier())) {
            feedback.show("Sem gelo. Traga gelo ou as três partes da arma.");
            sounds.tocarSemGelo();
            return;
        }
        int cells = world.getPlayer().adicionarMunicao(
            Math.round(GameConfig.AMMO_PER_ICE * mission.getIceYieldMultiplier()));
        sounds.tocarProcessarGelo();
        feedback.show(cells > 0
            ? "Gelo processado: O2, água e +" + cells + " células de pulso."
            : "Gelo processado: O2 e água. Munição já está no limite.");
        particles.criarProcessamento(
            world.getBase().getPosition().x + GameConfig.BASE_WIDTH / 2f,
            world.getBase().getPosition().y + GameConfig.BASE_HEIGHT / 2f);
    }
}
