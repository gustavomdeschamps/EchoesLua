package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.save.GameSaveData;

public class Astronauta extends Entidade implements Interagivel {

    // ==========================================
    // TAMANHO
    // ==========================================

    private static final float WIDTH =
        GameConfig.PLAYER_WIDTH;

    private static final float HEIGHT =
        GameConfig.PLAYER_HEIGHT;

    // ==========================================
    // VISUAL
    // ==========================================

    private final Sprite sprite;

    // ==========================================
    // BOX2D
    // ==========================================

    private final Body body;

    // ==========================================
    // MOVIMENTO
    // ==========================================

    private float velocidade =
        GameConfig.PLAYER_SPEED;

    private boolean viradoEsquerda = false;

    // ==========================================
    // STATUS
    // ==========================================

    private float oxigenio =
        GameConfig.MAX_OXYGEN;

    private float energia =
        GameConfig.MAX_ENERGY;

    private float tempoVivo = 0f;

    // ==========================================
    // INVENTÁRIO
    // ==========================================

    private int gelo = 0;
    private int agua = 0;
    private int combustivel = 0;

    // ==========================================
    // BASE
    // ==========================================

    private boolean protegido = false;

    // ==========================================
    // CONSTRUTOR
    // ==========================================

    public Astronauta(
        float x,
        float y,
        AssetManager assets,
        PhysicsWorld physicsWorld
    ) {

        super(
            x,
            y,
            WIDTH,
            HEIGHT
        );

        // ======================================
        // SPRITE
        // ======================================

        sprite = new Sprite(
            assets.astronautaTexture
        );

        sprite.setSize(
            WIDTH,
            HEIGHT
        );

        sprite.setPosition(
            x,
            y
        );

        // ======================================
        // POSIÇÃO
        // ======================================

        position.set(
            x,
            y
        );

        bounds.set(
            x,
            y,
            WIDTH,
            HEIGHT
        );

        // ======================================
        // BOX2D
        // ======================================

        /*
         * A colisão física é um pouco menor
         * que o sprite para o personagem
         * não ficar preso facilmente.
         */

        float bodyWidth =
            WIDTH * 0.70f;

        float bodyHeight =
            HEIGHT * 0.75f;

        body =
            physicsWorld.createDynamicBody(
                x + WIDTH / 2f,
                y + HEIGHT / 2f,
                bodyWidth,
                bodyHeight,
                this
            );
    }

    // ==========================================
    // MOVIMENTO
    // ==========================================

    public void move(
        float dirX,
        float dirY,
        float delta
    ) {

        if (!ativo) {
            return;
        }

        // Direção visual
        if (dirX < 0) {
            viradoEsquerda = true;
        }

        if (dirX > 0) {
            viradoEsquerda = false;
        }

        sprite.setFlip(
            viradoEsquerda,
            false
        );

        // Pixels/s -> metros/s
        float velocidadeX =
            dirX
                * velocidade
                / PhysicsWorld.PPM;

        float velocidadeY =
            dirY
                * velocidade
                / PhysicsWorld.PPM;

        body.setLinearVelocity(
            velocidadeX,
            velocidadeY
        );

        // ======================================
        // ENERGIA
        // ======================================

        if (
            dirX != 0
                || dirY != 0
        ) {

            energia -=
                1.5f * delta;

            if (energia < 0) {
                energia = 0;
            }
        }
    }

    // ==========================================
    // UPDATE
    // ==========================================

    @Override
    public void update(
        float delta
    ) {

        if (!ativo) {
            return;
        }

        // Tempo de sobrevivência
        tempoVivo += delta;

        // ======================================
        // POSIÇÃO BOX2D -> SPRITE
        // ======================================

        Vector2 bodyPosition =
            body.getPosition();

        position.set(
            bodyPosition.x
                * PhysicsWorld.PPM
                - WIDTH / 2f,

            bodyPosition.y
                * PhysicsWorld.PPM
                - HEIGHT / 2f
        );

        sprite.setPosition(
            position.x,
            position.y
        );

        bounds.setPosition(
            position.x,
            position.y
        );

        // ======================================
        // OXIGÊNIO
        // ======================================

        if (!protegido) {

            oxigenio -=
                GameConfig.OXYGEN_CONSUMPTION
                    * delta;
        }

        if (oxigenio <= 0) {

            oxigenio = 0;

            ativo = false;

            body.setLinearVelocity(
                0,
                0
            );
        }

        // ======================================
        // ENERGIA
        // ======================================

        if (energia < 0) {
            energia = 0;
        }
    }

    // ==========================================
    // RENDER
    // ==========================================

    @Override
    public void render(
        SpriteBatch batch
    ) {

        if (!ativo) {
            return;
        }

        sprite.draw(
            batch
        );
    }

    // ==========================================
    // OXIGÊNIO
    // ==========================================

    public void recuperarOxigenio(
        float quantidade
    ) {

        oxigenio += quantidade;

        if (
            oxigenio
                > GameConfig.MAX_OXYGEN
        ) {

            oxigenio =
                GameConfig.MAX_OXYGEN;
        }
    }

    // ==========================================
    // ENERGIA
    // ==========================================

    public void recuperarEnergia(
        float quantidade
    ) {

        energia += quantidade;

        if (
            energia
                > GameConfig.MAX_ENERGY
        ) {

            energia =
                GameConfig.MAX_ENERGY;
        }
    }

    // ==========================================
    // GELO
    // ==========================================

    public void adicionarGelo() {

        gelo++;
    }

    public boolean removerGelo() {

        if (gelo <= 0) {
            return false;
        }

        gelo--;

        return true;
    }

    // ==========================================
    // ÁGUA
    // ==========================================

    public void adicionarAgua(
        int quantidade
    ) {

        agua += quantidade;
    }

    // ==========================================
    // COMBUSTÍVEL
    // ==========================================

    public void adicionarCombustivel(
        int quantidade
    ) {

        combustivel += quantidade;
    }

    // ==========================================
    // BASE LUNAR
    // ==========================================

    public void setProtegido(
        boolean protegido
    ) {

        this.protegido =
            protegido;
    }

    public boolean isProtegido() {

        return protegido;
    }

    // ==========================================
    // SAVE
    // ==========================================

    public GameSaveData toSaveData() {

        GameSaveData data =
            new GameSaveData();

        data.posX =
            position.x;

        data.posY =
            position.y;

        data.oxigenio =
            oxigenio;

        data.energia =
            energia;

        data.tempoVivo =
            tempoVivo;

        data.gelo =
            gelo;

        data.agua =
            agua;

        data.combustivel =
            combustivel;

        data.versao = 1;

        return data;
    }

    // ==========================================
    // LOAD
    // ==========================================

    public void fromSaveData(
        GameSaveData data
    ) {

        if (data == null) {
            return;
        }

        // ======================================
        // POSIÇÃO
        // ======================================

        position.set(
            data.posX,
            data.posY
        );

        sprite.setPosition(
            data.posX,
            data.posY
        );

        bounds.setPosition(
            data.posX,
            data.posY
        );

        body.setTransform(
            (
                data.posX
                    + WIDTH / 2f
            )
                / PhysicsWorld.PPM,

            (
                data.posY
                    + HEIGHT / 2f
            )
                / PhysicsWorld.PPM,

            0
        );

        body.setLinearVelocity(
            0,
            0
        );

        // ======================================
        // STATUS
        // ======================================

        oxigenio =
            data.oxigenio;

        energia =
            data.energia;

        tempoVivo =
            data.tempoVivo;

        // ======================================
        // INVENTÁRIO
        // ======================================

        gelo =
            data.gelo;

        agua =
            data.agua;

        combustivel =
            data.combustivel;

        // ======================================
        // ESTADO
        // ======================================

        ativo =
            oxigenio > 0;
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public Body getBody() {

        return body;
    }

    @Override
    public Vector2 getPosition() {

        return position;
    }

    @Override
    public Rectangle getBounds() {

        return bounds;
    }

    public float getOxigenio() {

        return oxigenio;
    }

    public float getEnergia() {

        return energia;
    }

    public float getTempoVivo() {

        return tempoVivo;
    }

    public int getGelo() {

        return gelo;
    }

    public int getAgua() {

        return agua;
    }

    public int getCombustivel() {

        return combustivel;
    }

    public boolean isMoving() {

        return body
            .getLinearVelocity()
            .len2() > 0.01f;
    }

    public boolean isMorto() {

        return !ativo;
    }

    public float getSpeed() {

        return velocidade;
    }

    public void setSpeed(
        float velocidade
    ) {

        this.velocidade =
            velocidade;
    }

    // ==========================================
    // INTERAÇÃO
    // ==========================================

    @Override
    public void interagir(
        Entidade outra
    ) {

        /*
         * A interação é tratada
         * pelos itens e pela BaseLunar.
         */
    }

    @Override
    public boolean podeInteragir() {

        return ativo;
    }

    // ==========================================
    // DISPOSE
    // ==========================================

    @Override
    public void dispose() {

        /*
         * A Texture pertence ao AssetManager.
         * O Body pertence ao PhysicsWorld.
         */
    }
}
