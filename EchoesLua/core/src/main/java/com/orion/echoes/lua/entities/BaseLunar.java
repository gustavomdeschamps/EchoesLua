package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.events.EventBus;
import com.orion.echoes.lua.events.EventType;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.physics.PhysicsWorld;

public class BaseLunar extends Entidade implements Interagivel {

    private final Sprite sprite;

    private boolean astronautaDentro = false;

    public BaseLunar(
        float x,
        float y,
        float width,
        float height,
        AssetManager assets,
        PhysicsWorld physicsWorld
    ) {
        super(x, y, width, height);

        sprite = new Sprite(assets.baseLunarTexture);
        sprite.setSize(width, height);
        sprite.setPosition(x, y);

        // Área útil no solo: antenas e teto não devem funcionar como zona pressurizada.
        bounds.set(x + width * .09f, y + height * .06f, width * .82f, height * .5f);
    }

    @Override
    public void update(float delta) {
        // base parada
    }

    @Override
    public void render(SpriteBatch batch) {
        sprite.draw(batch);
    }

    public void entrar(Astronauta astronauta) {
        if (astronautaDentro) {
            return;
        }

        astronautaDentro = true;
        astronauta.setProtegido(true);

        EventBus.getInstance().publish(EventType.BASE_ENTERED, this);
    }

    public void sair(Astronauta astronauta) {
        if (!astronautaDentro) {
            return;
        }

        astronautaDentro = false;
        astronauta.setProtegido(false);

        EventBus.getInstance().publish(EventType.BASE_EXITED, this);
    }

    public void recarregarOxigenio(Astronauta astronauta, float delta) {
        recarregarOxigenio(astronauta, delta, 1f);
    }

    /**
     * Recarrega o traje dentro da base.
     *
     * O multiplicador vem do sistema de energia da colonia: com ele online,
     * a base deixa de ser so um abrigo e vira um posto de reabastecimento.
     */
    public void recarregarOxigenio(Astronauta astronauta, float delta, float multiplier) {
        if (!astronautaDentro) {
            return;
        }

        astronauta.recuperarOxigenio(GameConfig.BASE_OXYGEN_RECHARGE * multiplier * delta);
        astronauta.recuperarEnergia(GameConfig.BASE_ENERGY_RECHARGE * multiplier * delta);
    }

    public boolean processarGelo(Astronauta astronauta) {
        return processarGelo(astronauta, 1f);
    }

    /** O rendimento dobra quando a extracao esta reparada. */
    public boolean processarGelo(Astronauta astronauta, float yieldMultiplier) {
        if (!astronautaDentro) {
            return false;
        }

        if (!astronauta.removerGelo()) {
            return false;
        }

        int units = Math.max(1, Math.round(yieldMultiplier));
        astronauta.recuperarOxigenio(GameConfig.BASE_OXYGEN_RECHARGE * yieldMultiplier);
        astronauta.adicionarAgua(units);
        astronauta.adicionarCombustivel(units);

        EventBus.getInstance().publish(EventType.ICE_PROCESSED, astronauta);
        return true;
    }

    @Override
    public void interagir(Entidade outra) {
        if (outra instanceof Astronauta) {
            processarGelo((Astronauta) outra);
        }
    }

    @Override
    public boolean podeInteragir() {
        return astronautaDentro;
    }

    public boolean isAstronautaDentro() {
        return astronautaDentro;
    }

    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    @Override
    public void dispose() {
    }
}
