package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.render.SpriteFit;
import com.orion.echoes.lua.render.AtlasSpriteFactory;

public class Item extends Entidade implements Interagivel {

    public enum TipoItem {
        OXIGENIO,
        COMIDA,
        GELO
    }

    private final TipoItem tipo;
    private final Sprite sprite;
    private final Rectangle visualBounds = new Rectangle();

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
                    AtlasSpriteFactory.create(
                        assets.oxigenioTexture
                    );

                break;

            case COMIDA:

                sprite =
                    AtlasSpriteFactory.create(
                        assets.comidaTexture
                    );

                break;

            case GELO:

                sprite =
                    AtlasSpriteFactory.create(
                        assets.geloTexture
                    );

                break;

            default:

                throw new IllegalArgumentException(
                    "Tipo de item invalido."
                );
        }

        // Cada arte tem uma silhueta diferente. O encaixe preserva a proporcao
        // original em vez de esmagar cartucho, racao e gelo no mesmo quadrado.
        SpriteFit.fit(sprite, x, y, width, height, visualBounds);
        sprite.setSize(visualBounds.width, visualBounds.height);

        sprite.setOriginCenter();

        sprite.setPosition(
            visualBounds.x,
            y
        );

        sincronizarHitbox();
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
            position.x + (width - sprite.getWidth()) / 2f,
            yOriginal + offsetY
        );

        sprite.setScale(
            escala
        );

        sprite.setRotation(
            rotacao
        );

        sincronizarHitbox();
    }

    /**
     * A area de coleta acompanha o sprite.
     *
     * O comentario antigo dizia "area de coleta fica parada", mas o item sobe
     * 12px, pulsa e gira: o jogador via o recurso no ar e coletava no chao.
     * A caixa agora e o retangulo do proprio sprite, ja com a escala aplicada,
     * mais uma folga curta.
     */
    private void sincronizarHitbox() {
        float padding = GameConfig.PICKUP_HITBOX_PADDING;
        float scaledWidth = sprite.getWidth() * sprite.getScaleX();
        float scaledHeight = sprite.getHeight() * sprite.getScaleY();
        float centerX = sprite.getX() + sprite.getWidth() / 2f;
        float centerY = sprite.getY() + sprite.getHeight() / 2f;
        bounds.set(centerX - scaledWidth / 2f - padding, centerY - scaledHeight / 2f - padding,
            scaledWidth + padding * 2f, scaledHeight + padding * 2f);
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

                  astronauta.guardarComida();

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
        return sprite.getX() + sprite.getWidth() / 2f;
    }

    public float getCenterY() {
        return sprite.getY() + sprite.getHeight() / 2f;
    }

    @Override
    public void dispose() {

    }
}
