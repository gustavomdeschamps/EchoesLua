package com.orion.echoes.lua.systems;

import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.Item;
import com.orion.echoes.lua.entities.MissionCollectible;
import com.orion.echoes.lua.managers.ParticleManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.world.LunarWorld;

/**
 * Coleta de pecas de missao e de recursos soltos.
 *
 * Toda coleta responde em quatro canais - som posicionado, particula, pop no
 * HUD via mensagem e um toque de camera - porque pegar algo e a acao mais
 * repetida da fase e a que mais precisa parecer solida.
 */
public final class CollectionSystem {

    private static final float COLLECTIBLE_CENTER = 27f;

    private final SoundManager sounds;
    private final ParticleManager particles;
    private final JuiceSystem juice;
    private final FeedbackSystem feedback;

    public CollectionSystem(SoundManager sounds, ParticleManager particles,
                            JuiceSystem juice, FeedbackSystem feedback) {
        this.sounds = sounds;
        this.particles = particles;
        this.juice = juice;
        this.feedback = feedback;
    }

    public void update(float delta, LunarWorld world) {
        collectMissionParts(delta, world);
        collectResources(world);
    }

    private void collectMissionParts(float delta, LunarWorld world) {
        for (MissionCollectible collectible : world.getCollectibles()) {
            collectible.update(delta);
            if (!collectible.collectIfOverlapping(world.getPlayer(), world.getMission())) continue;
            float x = collectible.getPosition().x + COLLECTIBLE_CENTER;
            float y = collectible.getPosition().y + COLLECTIBLE_CENTER;
            feedback.show("Coletado: " + collectible.getType().getLabel());
            sounds.tocarColetaEspacial(x, y);
            particles.criarEfeitoColeta(x, y);
            juice.trigger(JuiceSystem.Preset.COLLECT);
        }
    }

    private void collectResources(LunarWorld world) {
        Astronauta player = world.getPlayer();
        for (Item item : world.getItems()) {
            if (item.isColetado()) continue;
            if (!player.getBounds().overlaps(item.getBounds())) continue;

            float x = item.getCenterX();
            float y = item.getCenterY();
            Item.TipoItem type = item.getTipo();

            item.coletar(player);
            player.registrarColeta(type);
            juice.trigger(JuiceSystem.Preset.COLLECT);
            particles.criarEfeitoColeta(x, y);

            switch (type) {
                case OXIGENIO -> sounds.tocarOxigenio();
                case COMIDA -> sounds.tocarComida();
                case GELO -> sounds.tocarGelo();
            }
        }
    }
}
