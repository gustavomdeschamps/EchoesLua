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

    public enum SurfaceProfile { LUNAR, MARS }

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

    private static final float FOOTPRINT_WIDTH = 30f;
    private static final float FOOTPRINT_HEIGHT = 22f;
    private static final float BODY_CENTER_Y = 17f;
    private enum AnimationState { IDLE, WALK, RUN, DASH, ATTACK, HURT, DEAD }
    private final TextureRegion[][] movementFrames =
        new TextureRegion[4][GameConfig.PLAYER_ANIMATION_FRAMES];
    private final TextureRegion[][] movementFramesLeft =
        new TextureRegion[4][GameConfig.PLAYER_ANIMATION_FRAMES];
    private final TextureRegion[][] combatFrames =
        new TextureRegion[3][GameConfig.PLAYER_ANIMATION_FRAMES];
    private final TextureRegion[][] combatFramesLeft =
        new TextureRegion[3][GameConfig.PLAYER_ANIMATION_FRAMES];
    private final Sprite weaponSprite;
    private float animationTime;
    private AnimationState animationState = AnimationState.IDLE;
    private float recoilTimer;
    private float damageTimer;
    private float aimAngle;
    private boolean weaponEquipped;
    private boolean sprinting;
    private float invulnerabilityTimer;
    private float dashTimer;
    private float dashCooldown;
    private final Vector2 dashDirection = new Vector2(1f, 0f);

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
    private SurfaceProfile surfaceProfile = SurfaceProfile.LUNAR;

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

        for (int row = 0; row < movementFrames.length; row++) {
            for (int column = 0; column < GameConfig.PLAYER_ANIMATION_FRAMES; column++) {
                movementFrames[row][column] = assets.astronautFrame(column, row);
                movementFramesLeft[row][column] = mirrored(movementFrames[row][column]);
            }
        }
        for (int row = 0; row < combatFrames.length; row++) {
            for (int column = 0; column < GameConfig.PLAYER_ANIMATION_FRAMES; column++) {
                combatFrames[row][column] = assets.astronautCombatFrame(column, row);
                combatFramesLeft[row][column] = mirrored(combatFrames[row][column]);
            }
        }
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

    private TextureRegion mirrored(TextureRegion source) {
        TextureRegion copy = new TextureRegion(source);
        copy.flip(true, false);
        return copy;
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
                / GameConfig.PPM;

        float velocidadeY =
            dirY
                * currentSpeed
                / GameConfig.PPM;

        if (dashTimer > 0f) {
            body.setLinearVelocity(dashDirection.x * GameConfig.PLAYER_DASH_SPEED / GameConfig.PPM,
                dashDirection.y * GameConfig.PLAYER_DASH_SPEED / GameConfig.PPM);
        } else {
            boolean stopping = dirX == 0f && dirY == 0f;
            float responseTime = surfaceProfile == SurfaceProfile.LUNAR
                ? (stopping ? GameConfig.PLAYER_LUNAR_DECEL_TIME : GameConfig.PLAYER_LUNAR_ACCEL_TIME)
                : (stopping ? GameConfig.PLAYER_MARS_DECEL_TIME : GameConfig.PLAYER_MARS_ACCEL_TIME);
            float response = 1f - (float)Math.exp(-delta / responseTime);
            Vector2 velocity = body.getLinearVelocity();
            body.setLinearVelocity(MathUtils.lerp(velocity.x, velocidadeX, response),
                MathUtils.lerp(velocity.y, velocidadeY, response));
        }

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

    public void setSurfaceProfile(SurfaceProfile profile) {
        surfaceProfile = profile == null ? SurfaceProfile.LUNAR : profile;
    }

    // ==========================================
    // UPDATE
    // ==========================================

    @Override
    public void update(
        float delta
    ) {

        if (!ativo) return;

        recoilTimer = Math.max(0f, recoilTimer - delta);
        damageTimer = Math.max(0f, damageTimer - delta);
        invulnerabilityTimer = Math.max(0f, invulnerabilityTimer - delta);
        dashTimer = Math.max(0f, dashTimer - delta);
        dashCooldown = Math.max(0f, dashCooldown - delta);
        AnimationState nextState = resolveAnimationState();
        if (nextState != animationState) {
            animationState = nextState;
            animationTime = 0f;
        } else {
            animationTime += delta;
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
                * GameConfig.PPM
                - WIDTH / 2f,

            bodyPosition.y
                * GameConfig.PPM
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

        TextureRegion frame = currentAnimationFrame();
        float visualX = position.x + WIDTH / 2f - GameConfig.PLAYER_VISUAL_SIZE / 2f;
        float visualY = position.y - 5f;

        float blink = invulnerabilityTimer > 0f && ((int)(invulnerabilityTimer * 24f) & 1) == 0 ? .45f : 1f;
        if (damageTimer > 0f) batch.setColor(1f, .58f, .58f, blink);
        else batch.setColor(1f, 1f, 1f, blink);
        batch.draw(frame, visualX, visualY,
            GameConfig.PLAYER_VISUAL_SIZE, GameConfig.PLAYER_VISUAL_SIZE);
        batch.setColor(1f, 1f, 1f, 1f);
        if (weaponEquipped) drawWeapon(batch);
    }

    private AnimationState resolveAnimationState() {
        if (!ativo) return AnimationState.DEAD;
        if (damageTimer > 0f) return AnimationState.HURT;
        if (recoilTimer > 0f) return AnimationState.ATTACK;
        if (dashTimer > 0f) return AnimationState.DASH;
        if (isMoving()) return sprinting ? AnimationState.RUN : AnimationState.WALK;
        return AnimationState.IDLE;
    }

    private TextureRegion currentAnimationFrame() {
        AnimationState state = ativo ? animationState : AnimationState.DEAD;
        int row;
        float frameTime;
        boolean combat;
        switch (state) {
            case WALK -> { row = 1; frameTime = GameConfig.PLAYER_WALK_FRAME_TIME; combat = false; }
            case RUN -> { row = 2; frameTime = GameConfig.PLAYER_RUN_FRAME_TIME; combat = false; }
            case DASH -> { row = 3; frameTime = GameConfig.PLAYER_DASH_FRAME_TIME; combat = false; }
            case ATTACK -> { row = 0; frameTime = GameConfig.PLAYER_ATTACK_FRAME_TIME; combat = true; }
            case HURT -> { row = 1; frameTime = GameConfig.PLAYER_HURT_FRAME_TIME; combat = true; }
            case DEAD -> { row = 2; frameTime = GameConfig.PLAYER_DEATH_FRAME_TIME; combat = true; }
            default -> { row = 0; frameTime = GameConfig.PLAYER_IDLE_FRAME_TIME; combat = false; }
        }
        int frameIndex = state == AnimationState.DEAD
            ? GameConfig.PLAYER_ANIMATION_FRAMES - 1
            : (int) (animationTime / frameTime) % GameConfig.PLAYER_ANIMATION_FRAMES;
        if (combat) return viradoEsquerda
            ? combatFramesLeft[row][frameIndex] : combatFrames[row][frameIndex];
        return viradoEsquerda
            ? movementFramesLeft[row][frameIndex] : movementFrames[row][frameIndex];
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

    public void triggerShot() {
        recoilTimer = .12f;
    }

    public boolean tryDash(float dirX, float dirY) {
        if (!ativo || dashCooldown > 0f || energia < GameConfig.PLAYER_DASH_ENERGY_COST) return false;
        if (dirX == 0f && dirY == 0f) dirX = viradoEsquerda ? -1f : 1f;
        dashDirection.set(dirX, dirY).nor();
        dashTimer = GameConfig.PLAYER_DASH_DURATION;
        dashCooldown = GameConfig.PLAYER_DASH_COOLDOWN;
        invulnerabilityTimer = Math.max(invulnerabilityTimer, .24f);
        energia -= GameConfig.PLAYER_DASH_ENERGY_COST;
        return true;
    }

    public boolean isDashing() { return dashTimer > 0f; }
    public boolean isInvulnerable() { return invulnerabilityTimer > 0f; }

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
        receberDano(quantidade, position.x + WIDTH / 2f, position.y + HEIGHT / 2f);
    }

    public void receberDano(float quantidade, float sourceX, float sourceY) {
        if (!ativo || protegido || quantidade <= 0f || invulnerabilityTimer > 0f) {
            return;
        }
        oxigenio = Math.max(0f, oxigenio - quantidade);
        damageTimer = .22f;
        invulnerabilityTimer = .48f;
        Vector2 away = new Vector2(position.x + WIDTH / 2f - sourceX,
            position.y + HEIGHT / 2f - sourceY);
        if (away.len2() < .01f) away.set(viradoEsquerda ? 1f : -1f, .25f);
        away.nor().scl(155f / GameConfig.PPM);
        body.setLinearVelocity(away);
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
                / GameConfig.PPM,

            (
                data.posY
                    + BODY_CENTER_Y
            )
                / GameConfig.PPM,

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
