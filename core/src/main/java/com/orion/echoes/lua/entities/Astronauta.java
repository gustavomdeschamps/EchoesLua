package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
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

    private static final float VISUAL_SIZE = 104f;
    private static final float FOOTPRINT_WIDTH = 30f;
    private static final float FOOTPRINT_HEIGHT = 22f;
    private static final float BODY_CENTER_Y = 17f;
    private static final float COLLECT_DURATION = .56f;
    private static final int[] CYCLE = {0, 1, 2, 1};
    private static final float[] IDLE_CENTER_X = {190.5f};
    private static final float[] IDLE_BOTTOM_PAD = {28f};
    private static final float[] WALK_CENTER_X = {186f, 160.5f, 143.5f};
    private static final float[] WALK_BOTTOM_PAD = {40f, 40f, 41f};
    private static final float[] COLLECT_CENTER_X = {184.5f, 152f, 133.5f};
    private static final float[] COLLECT_BOTTOM_PAD = {0f, 0f, 0f};
    private final TextureRegion idleFrame;
    private final TextureRegion[] walkFrames;
    private final TextureRegion[] collectFrames;
    private final Sprite weaponSprite;
    private final float frameCellWidth;
    private final float frameCellHeight;
    private float animationTime;
    private float collectTimer;
    private float recoilTimer;
    private float damageTimer;
    private float aimAngle;
    private boolean weaponEquipped;
    private boolean sprinting;
    private boolean movingLastFrame;

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
    private int oxigenioColetado;
    private int comidaColetada;
    private int geloColetado;

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

        int cellWidth = assets.astronautaSheetTexture.getWidth() / 4;
        int cellHeight = assets.astronautaSheetTexture.getHeight() / 4;
        // Dois pixels de respiro impedem que a borda da célula vizinha apareça
        // durante escala/flip, principalmente na pose de coleta que toca o grid.
        frameCellWidth = cellWidth - 4f;
        frameCellHeight = cellHeight - 4f;
        idleFrame = frame(assets, 0, 0, cellWidth, cellHeight);
        walkFrames = new TextureRegion[] {
            frame(assets, 0, 1, cellWidth, cellHeight),
            frame(assets, 1, 1, cellWidth, cellHeight),
            frame(assets, 2, 1, cellWidth, cellHeight)
        };
        collectFrames = new TextureRegion[] {
            frame(assets, 0, 2, cellWidth, cellHeight),
            frame(assets, 1, 2, cellWidth, cellHeight),
            frame(assets, 2, 2, cellWidth, cellHeight)
        };
        weaponSprite = new Sprite(assets.pulseRifleTexture);
        weaponSprite.setSize(62f, 41f);
        weaponSprite.setOrigin(13f, 20.5f);

        // ======================================
        // POSIÇÃO
        // ======================================

        position.set(
            x,
            y
        );

        bounds.set(x + (WIDTH - FOOTPRINT_WIDTH) / 2f, y + 6f,
            FOOTPRINT_WIDTH, FOOTPRINT_HEIGHT);

        // ======================================
        // BOX2D
        // ======================================

        /*
         * A colisão física é um pouco menor
         * que o sprite para o personagem
         * não ficar preso facilmente.
         */

        body =
            physicsWorld.createDynamicBody(
                x + WIDTH / 2f,
                y + BODY_CENTER_Y,
                FOOTPRINT_WIDTH,
                FOOTPRINT_HEIGHT,
                this
            );
    }

    private TextureRegion frame(AssetManager assets, int column, int row, int cellWidth, int cellHeight) {
        return new TextureRegion(assets.astronautaSheetTexture,
            column * cellWidth + 2, row * cellHeight + 2, cellWidth - 4, cellHeight - 4);
    }

    // ==========================================
    // MOVIMENTO
    // ==========================================

    public void move(
        float dirX,
        float dirY,
        boolean wantsToRun,
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

        // Pixels/s -> metros/s
        sprinting = wantsToRun && energia > 1f && (dirX != 0f || dirY != 0f);
        float currentSpeed = velocidade * (sprinting ? GameConfig.PLAYER_RUN_MULTIPLIER : 1f);

        float velocidadeX =
            dirX
                * currentSpeed
                / PhysicsWorld.PPM;

        float velocidadeY =
            dirY
                * currentSpeed
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

            energia -= (sprinting ? 3.4f : .9f) * delta;

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

        boolean movingNow = isMoving();
        if (movingNow) {
            animationTime += delta;
        } else if (movingLastFrame) {
            animationTime = 0f;
        }
        movingLastFrame = movingNow;
        collectTimer = Math.max(0f, collectTimer - delta);
        recoilTimer = Math.max(0f, recoilTimer - delta);
        damageTimer = Math.max(0f, damageTimer - delta);

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
                - BODY_CENTER_Y
        );

        bounds.setPosition(position.x + (WIDTH - FOOTPRINT_WIDTH) / 2f, position.y + 6f);

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

        int frameIndex = 0;
        TextureRegion frame;
        float anchorCenterX;
        float bottomPad;

        if (collectTimer > 0f) {
            float progress = 1f - collectTimer / COLLECT_DURATION;
            frameIndex = progress < .24f ? 0 : progress < .55f ? 1 : progress < .82f ? 2 : 1;
            frame = collectFrames[frameIndex];
            anchorCenterX = COLLECT_CENTER_X[frameIndex];
            bottomPad = COLLECT_BOTTOM_PAD[frameIndex];
        } else if (isMoving()) {
            float frameDuration = sprinting ? .075f : .115f;
            frameIndex = CYCLE[((int) (animationTime / frameDuration)) % CYCLE.length];
            frame = walkFrames[frameIndex];
            anchorCenterX = WALK_CENTER_X[frameIndex];
            bottomPad = WALK_BOTTOM_PAD[frameIndex];
        } else {
            frame = idleFrame;
            anchorCenterX = IDLE_CENTER_X[0];
            bottomPad = IDLE_BOTTOM_PAD[0];
        }
        boolean flip = viradoEsquerda;
        if (weaponEquipped) flip = Math.abs(aimAngle) > 90f;
        if (frame.isFlipX() != flip) frame.flip(true, false);
        float effectiveCenter = flip ? frameCellWidth - anchorCenterX : anchorCenterX;
        float visualX = position.x + WIDTH / 2f - effectiveCenter / frameCellWidth * VISUAL_SIZE;
        float visualY = position.y - bottomPad / frameCellHeight * VISUAL_SIZE - 5f;

        if (damageTimer > 0f) batch.setColor(1f, .58f, .58f, 1f);
        batch.draw(frame, visualX, visualY, VISUAL_SIZE, VISUAL_SIZE);
        batch.setColor(1f, 1f, 1f, 1f);
        if (weaponEquipped && collectTimer <= 0f) drawWeapon(batch);
    }

    private void drawWeapon(SpriteBatch batch) {
        float centerX = position.x + WIDTH / 2f;
        float centerY = position.y + HEIGHT * .44f;
        float recoil = recoilTimer > 0f ? recoilTimer / .12f * 5f : 0f;
        float radians = aimAngle * MathUtils.degreesToRadians;
        weaponSprite.setPosition(centerX - 13f - MathUtils.cos(radians) * recoil,
            centerY - 20.5f - MathUtils.sin(radians) * recoil);
        weaponSprite.setRotation(aimAngle);
        boolean upsideDown = aimAngle > 90f || aimAngle < -90f;
        weaponSprite.setFlip(false, upsideDown);
        weaponSprite.draw(batch);
    }

    public void triggerCollectAnimation() {
        collectTimer = COLLECT_DURATION;
        animationTime = 0f;
    }

    public void triggerShot() {
        recoilTimer = .12f;
    }

    public void setWeaponEquipped(boolean weaponEquipped) {
        this.weaponEquipped = weaponEquipped;
    }

    public void setAimTarget(float worldX, float worldY) {
        float centerX = position.x + WIDTH / 2f;
        float centerY = position.y + HEIGHT * .48f;
        aimAngle = MathUtils.atan2(worldY - centerY, worldX - centerX) * MathUtils.radiansToDegrees;
    }

    public float getAimAngle() { return aimAngle; }

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

    public void receberDano(float quantidade) {
        if (!ativo || protegido || quantidade <= 0f) {
            return;
        }
        oxigenio = Math.max(0f, oxigenio - quantidade);
        damageTimer = .22f;
        if (oxigenio == 0f) {
            ativo = false;
            body.setLinearVelocity(0f, 0f);
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

    public void registrarColeta(Item.TipoItem tipo) {
        switch (tipo) {
            case OXIGENIO -> oxigenioColetado++;
            case COMIDA -> comidaColetada++;
            case GELO -> geloColetado++;
        }
        triggerCollectAnimation();
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

        data.versao = 2;

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

        bounds.setPosition(
            data.posX + (WIDTH - FOOTPRINT_WIDTH) / 2f,
            data.posY + 6f
        );

        body.setTransform(
            (
                data.posX
                    + WIDTH / 2f
            )
                / PhysicsWorld.PPM,

            (
                data.posY
                    + BODY_CENTER_Y
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

    public int getOxigenioColetado() { return oxigenioColetado; }
    public int getComidaColetada() { return comidaColetada; }
    public int getGeloColetado() { return geloColetado; }

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

    public boolean isSprinting() {
        return sprinting;
    }

    public void setVitals(float oxygen, float power) {
        oxigenio = MathUtils.clamp(oxygen, 0f, GameConfig.MAX_OXYGEN);
        energia = MathUtils.clamp(power, 0f, GameConfig.MAX_ENERGY);
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
