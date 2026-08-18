package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.physics.PhysicsWorld;

public class Item extends Entidade implements Interagivel {

    public enum TipoItem {
        OXIGENIO,
        COMIDA,
        GELO
    }

    private final TipoItem tipo;
    private final Sprite sprite;

    private boolean coletado = false;

    // ==========================================
    // ANIMAÇÃO DE FLUTUAÇÃO
    // ==========================================

    private float tempoAnimacao = 0f;

    private final float yOriginal;
    private final float fase;

    private float offsetY = 0f;

    public Item(
        float x,
        float y,
        float width,
        float height,
        TipoItem tipo,
        AssetManager assets,
        PhysicsWorld physicsWorld
    ) {

        super(
            x,
            y,
            width,
            height
        );

        this.tipo = tipo;

        this.yOriginal = y;

        // Faz cada item flutuar em um tempo diferente
        this.fase =
            (x * 0.021f + y * 0.017f)
                % MathUtils.PI2;

        switch (tipo) {

            case OXIGENIO:

                sprite =
                    new Sprite(
                        assets.oxigenioTexture
                    );

                break;

            case COMIDA:

                sprite =
                    new Sprite(
                        assets.comidaTexture
                    );

                break;

            case GELO:

                sprite =
                    new Sprite(
                        assets.geloTexture
                    );

                break;

            default:

                throw new IllegalArgumentException(
                    "Tipo de item invalido."
                );
        }

        sprite.setSize(
            width,
            height
        );

        sprite.setOriginCenter();

        sprite.setPosition(
            x,
            y
        );

        // Área de coleta fica parada.
        bounds.set(
            x,
            y,
            width,
            height
        );
    }

    // ==========================================
    // UPDATE
    // ==========================================

    @Override
    public void update(float delta) {

        if (coletado) {
            return;
        }

        tempoAnimacao += delta;

        // Movimento vertical bem visível
        offsetY =
            MathUtils.sin(
                tempoAnimacao * 2.4f + fase
            ) * 12f;

        // Pulsação
        float escala =
            1f
                + MathUtils.sin(
                tempoAnimacao * 2f + fase
            ) * 0.055f;

        // Pequena rotação
        float rotacao =
            MathUtils.sin(
                tempoAnimacao * 1.35f + fase
            ) * 4f;

        sprite.setPosition(
            position.x,
            yOriginal + offsetY
        );

        sprite.setScale(
            escala
        );

        sprite.setRotation(
            rotacao
        );
    }

    // ==========================================
    // RENDER
    // ==========================================

    @Override
    public void render(
        SpriteBatch batch
    ) {

        if (!coletado) {
            sprite.draw(batch);
        }
    }

    // ==========================================
    // COLETA
    // ==========================================

    public void coletar(
        Astronauta astronauta
    ) {

        if (coletado) {
            return;
        }

        switch (tipo) {

            case OXIGENIO:

                astronauta.recuperarOxigenio(
                    30f
                );

                break;

            case COMIDA:

                astronauta.recuperarEnergia(
                    30f
                );

                break;

            case GELO:

                astronauta.adicionarGelo();

                break;
        }

        coletado = true;
        ativo = false;
    }

    @Override
    public void interagir(
        Entidade outra
    ) {

        if (
            outra instanceof Astronauta
                && !coletado
        ) {

            coletar(
                (Astronauta) outra
            );
        }
    }

    @Override
    public boolean podeInteragir() {

        return !coletado
            && ativo;
    }

    public TipoItem getTipo() {

        return tipo;
    }

    public boolean isColetado() {

        return coletado;
    }

    @Override
    public Rectangle getBounds() {

        return bounds;
    }

    public float getCenterX() {

        return position.x
            + width / 2f;
    }

    public float getCenterY() {

        return yOriginal
            + offsetY
            + height / 2f;
    }

    @Override
    public void dispose() {

    }
}
