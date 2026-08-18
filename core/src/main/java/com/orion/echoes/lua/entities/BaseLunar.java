package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

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

        bounds.set(x, y, width, height);
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
        if (!astronautaDentro) {
            return;
        }

        astronauta.recuperarOxigenio(20f * delta);
    }

    public boolean processarGelo(Astronauta astronauta) {
        if (!astronautaDentro) {
            return false;
        }

        if (!astronauta.removerGelo()) {
            System.out.println("Nenhuma rocha de gelo disponível.");
            return false;
        }

        astronauta.recuperarOxigenio(20f);
        astronauta.adicionarAgua(1);
        astronauta.adicionarCombustivel(1);

        EventBus.getInstance().publish(EventType.ICE_PROCESSED, astronauta);

        System.out.println("Gelo processado com sucesso!");
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
