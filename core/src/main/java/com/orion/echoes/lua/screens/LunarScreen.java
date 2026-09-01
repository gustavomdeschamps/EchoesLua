package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.Enemy;
import com.orion.echoes.lua.entities.Item;
import com.orion.echoes.lua.entities.RepairStation;
import com.orion.echoes.lua.input.GameInputProcessor;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.ParticleManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.render.MissionOverlay;
import com.orion.echoes.lua.render.PauseOverlay;
import com.orion.echoes.lua.render.WorldRenderer;
import com.orion.echoes.lua.save.GameSaveData;
import com.orion.echoes.lua.save.LunarCheckpoint;
import com.orion.echoes.lua.save.SaveManager;
import com.orion.echoes.lua.systems.CameraDirector;
import com.orion.echoes.lua.systems.CollectionSystem;
import com.orion.echoes.lua.systems.CombatSystem;
import com.orion.echoes.lua.systems.FeedbackSystem;
import com.orion.echoes.lua.systems.InteractionSystem;
import com.orion.echoes.lua.systems.JuiceSystem;
import com.orion.echoes.lua.systems.MissionState;
import com.orion.echoes.lua.world.LunarWorld;

/**
 * Fase lunar.
 *
 * A tela orquestra: quem constroi o mundo e {@link LunarWorld}, quem desenha e
 * {@link WorldRenderer}, quem resolve combate e {@link CombatSystem}, quem fala
 * com o jogador e {@link FeedbackSystem}. Aqui ficam apenas o ciclo de vida, a
 * ordem do frame e as regras que ligam esses pedacos.
 */
public class LunarScreen implements Screen {

    private static final float MAX_STEP = 1f / 30f;
    private static final float OXYGEN_CRITICAL = 25f;
    private static final float FOOTSTEP_INTERVAL = .48f;
    private static final float ENEMY_CONTACT_MESSAGE_TIME = 5f;

    private final EchoesLua game;
    private final SpriteBatch batch;
    private final AssetManager assets;
    private final long seed;

    private OrthographicCamera camera;
    private Viewport viewport;
    private OrthographicCamera uiCamera;
    private Viewport uiViewport;

    private PhysicsWorld physicsWorld;
    private GameInputProcessor input;
    private ParticleManager particleManager;
    private SoundManager sounds;
    private SaveManager saveManager;
    private Hud hud;

    private JuiceSystem juice;
    private CameraDirector cameraDirector;
    private FeedbackSystem feedback;
    private CombatSystem combat;
    private InteractionSystem interactions;
    private CollectionSystem collection;
    private WorldRenderer worldRenderer;
    private MissionOverlay overlay;
    private PauseOverlay pauseOverlay;

    private LunarWorld world;
    private Astronauta astronauta;
    private MissionState mission;

    private boolean pausado, gameOver, vitoria;
    private boolean oxigenioCriticoAtivado, estavaNaBase, portalWasUnlocked;
    private float tempoPoeira, tempoPasso;

    private final Vector2 mouseWorld = new Vector2();
    private final Vector2 cameraTarget = new Vector2();
    private Screen nextScreen;

    public LunarScreen(EchoesLua game, SpriteBatch batch, AssetManager assets) {
        this(game, batch, assets, System.nanoTime());
    }

    /** Semente fixa reproduz exatamente o mesmo layout de fase. */
    public LunarScreen(EchoesLua game, SpriteBatch batch, AssetManager assets, long seed) {
        this.game = game;
        this.batch = batch;
        this.assets = assets;
        this.seed = seed;
    }

    // =====================================================
    // CICLO DE VIDA
    // =====================================================

    @Override
    public void show() {
        criarCameras();

        physicsWorld = new PhysicsWorld();
        input = new GameInputProcessor();
        Gdx.input.setInputProcessor(input);
        particleManager = new ParticleManager(assets);
        saveManager = new SaveManager();
        hud = new Hud(assets);

        sounds = game.getSounds();
        sounds.applySettings(game.getSettings());
        sounds.setVacuum(true);
        sounds.tocarMusicaLunar();

        juice = new JuiceSystem();
        juice.setShakeEnabled(game.getSettings().isShakeEnabled());
        cameraDirector = new CameraDirector(camera, viewport, juice,
            GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

        world = new LunarWorld(seed, assets, physicsWorld);
        astronauta = world.getPlayer();
        mission = world.getMission();

        feedback = new FeedbackSystem();
        feedback.showFor("Colete peças para restaurar a colônia.", ENEMY_CONTACT_MESSAGE_TIME);
        combat = new CombatSystem(batch, assets, camera, particleManager, sounds, juice, feedback);
        interactions = new InteractionSystem(sounds, particleManager, juice, feedback);
        collection = new CollectionSystem(sounds, particleManager, juice, feedback);
        worldRenderer = new WorldRenderer(batch, assets, camera);
        overlay = new MissionOverlay(batch, assets, camera, uiCamera);
        pauseOverlay = new PauseOverlay(batch, assets, uiCamera);
    }

    private void criarCameras() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
        camera.position.set(GameConfig.PLAYER_START_X, GameConfig.PLAYER_START_Y, 0f);
        camera.update();

        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, uiCamera);
        uiCamera.position.set(GameConfig.WINDOW_WIDTH / 2f, GameConfig.WINDOW_HEIGHT / 2f, 0f);
        uiCamera.update();
    }

    // =====================================================
    // FRAME
    // =====================================================

    @Override
    public void render(float delta) {
        verificarPause();
        atualizarMixer();
        if (trocarTelaSePendente()) return;

        update(delta);
        if (trocarTelaSePendente()) return;

        Gdx.gl.glClearColor(.02f, .025f, .04f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldRenderer.render(world, particleManager);
        combat.render(world);

        if (!pausado) {
            String hudMessage = feedback.resolveHudText(world);
            hud.update(Math.min(delta, MAX_STEP), hudMessage);
            hud.render(batch, astronauta, mission, hudMessage,
                astronauta.getPosition().x + GameConfig.PLAYER_WIDTH / 2f
                    - camera.position.x + GameConfig.WINDOW_WIDTH / 2f,
                astronauta.getPosition().y + GameConfig.PLAYER_HEIGHT / 2f
                    - camera.position.y + GameConfig.WINDOW_HEIGHT / 2f);
            overlay.renderObjectiveMarker(world);
        }

        overlay.renderDamageVignette(juice.getDamageFlashAlpha());

        if (pausado) pauseOverlay.render(world);
        else overlay.renderCursor(world, mouseWorld);
    }

    private void update(float delta) {
        if (pausado || gameOver || vitoria) return;

        delta = Math.min(delta, MAX_STEP);
        juice.update(delta);

        float gameplayDelta = juice.gameplayDelta(delta);
        if (gameplayDelta == 0f) {
            // Hitstop: o mundo congela, mas as particulas ainda respiram.
            particleManager.update(delta * .35f);
            return;
        }
        delta = gameplayDelta;

        atualizarJogador(delta);
        physicsWorld.update(delta);
        astronauta.update(delta);
        apontarMira();

        for (Item item : world.getItems()) item.update(delta);
        world.getBase().update(delta);
        atualizarBase();
        atualizarMissao(delta);

        if (world.getBase().isAstronautaDentro()) {
            world.getBase().recarregarOxigenio(astronauta, delta, mission.getRechargeMultiplier());
        }

        atualizarInteracao();
        atualizarParticulas(delta);
        atualizarSomPassos(delta);
        aplicarOxigenioPassivo(delta);
        verificarOxigenio();

        particleManager.update(delta);
        atualizarCamera();
        verificarGameOver();
        atualizarSaveLoad();
    }

    private void atualizarJogador(float delta) {
        Vector2 direction = input.getDirection();
        if (input.consumeDashPressed() && astronauta.tryDash(direction.x, direction.y)) {
            particleManager.criarPoeiraLunar(astronauta.getPosition().x + 27f,
                astronauta.getPosition().y + 4f, true, direction.x, direction.y);
            juice.trigger(JuiceSystem.Preset.DASH);
        }
        astronauta.move(direction.x, direction.y, input.isRunning(), delta);
    }

    private void apontarMira() {
        mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouseWorld);
        astronauta.setAimTarget(mouseWorld.x, mouseWorld.y);
        astronauta.setWeaponEquipped(mission.hasWeapon());
    }

    // =====================================================
    // MIXAGEM ADAPTATIVA
    // =====================================================

    /**
     * Mantem o mixer vivo fora do relogio de gameplay: a trilha nao pode
     * congelar durante hitstop nem parar na pausa.
     */
    private void atualizarMixer() {
        overlay.update(Gdx.graphics.getDeltaTime());
        sounds.setListener(camera.position.x, camera.position.y);
        if (pausado) sounds.atualizarIntensidade(0f, 0f);
        else sounds.atualizarIntensidade(tensaoDeCombate(),
            urgenciaDeOxigenio(astronauta.getOxigenio()));
    }

    /** 0 quando nenhum hostil ameaca; 1 com hostil colado no jogador. */
    private float tensaoDeCombate() {
        float strongest = 0f;
        for (Enemy enemy : world.getEnemies()) {
            if (!enemy.isAtivo()) continue;
            float distance = Vector2.dst(enemy.centerX(), enemy.centerY(),
                astronauta.getPosition().x, astronauta.getPosition().y);
            strongest = Math.max(strongest,
                1f - MathUtils.clamp(distance / GameConfig.MUSIC_TENSION_RADIUS, 0f, 1f));
        }
        return strongest;
    }

    /** Cresce conforme o oxigenio cai abaixo do limiar critico. */
    private float urgenciaDeOxigenio(float oxygen) {
        if (oxygen >= GameConfig.MUSIC_URGENCY_OXYGEN) return 0f;
        return MathUtils.clamp(1f - oxygen / GameConfig.MUSIC_URGENCY_OXYGEN, 0f, 1f);
    }

    // =====================================================
    // MISSAO
    // =====================================================

    private void atualizarMissao(float delta) {
        feedback.update(delta);

        collection.update(delta, world);
        for (RepairStation station : world.getRepairStations()) station.update(delta);

        combat.update(delta, world);
        if (input.consumeAttackPressed()) combat.fire(world);

        boolean unlocked = mission.isPortalUnlocked(astronauta.getOxigenio());
        if (unlocked && !portalWasUnlocked) {
            particleManager.criarPortal(
                world.getPortal().getPosition().x + world.getPortal().getBounds().width / 2f,
                world.getPortal().getPosition().y + world.getPortal().getBounds().height / 2f);
            feedback.show("Portal energizado. A passagem para Marte está aberta.");
        }
        portalWasUnlocked = unlocked;
        world.getPortal().setUnlocked(unlocked);
        world.getPortal().update(delta);
    }

    private void atualizarBase() {
        boolean dentro = astronauta.getBounds().overlaps(world.getBase().getBounds());
        if (!dentro) {
            world.getBase().sair(astronauta);
            estavaNaBase = false;
            return;
        }
        world.getBase().entrar(astronauta);
        if (estavaNaBase) return;
        estavaNaBase = true;
        sounds.tocarBaseRecarregando();
    }

    private void atualizarInteracao() {
        if (!input.consumeInteractPressed()) return;
        if (interactions.interact(world) != InteractionSystem.Result.PORTAL_CROSSED) return;
        vitoria = true;
        nextScreen = new MarsScreen(game, astronauta.getOxigenio(),
            astronauta.getEnergia(), mission.getEnemiesDefeated());
    }

    // =====================================================
    // SOBREVIVENCIA E APRESENTACAO
    // =====================================================

    /** Estufa reparada devolve oxigenio devagar em campo aberto. */
    private void aplicarOxigenioPassivo(float delta) {
        float perSecond = mission.getPassiveOxygenPerSecond();
        if (perSecond <= 0f || world.getBase().isAstronautaDentro()) return;
        astronauta.recuperarOxigenio(perSecond * delta);
    }

    private void verificarOxigenio() {
        float oxygen = astronauta.getOxigenio();
        if (oxygen > OXYGEN_CRITICAL) {
            oxigenioCriticoAtivado = false;
            return;
        }
        if (oxigenioCriticoAtivado) return;
        oxigenioCriticoAtivado = true;
        sounds.tocarAlertaOxigenio();
        particleManager.criarAlertaOxigenio(
            astronauta.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
            astronauta.getPosition().y + GameConfig.PLAYER_HEIGHT);
    }

    private void atualizarSomPassos(float delta) {
        if (!astronauta.isMoving()) {
            tempoPasso = 0f;
            return;
        }
        tempoPasso += delta;
        if (tempoPasso < FOOTSTEP_INTERVAL) return;
        tempoPasso = 0f;
        sounds.tocarPassoLunar();
    }

    private void atualizarParticulas(float delta) {
        if (!astronauta.isMoving()) {
            tempoPoeira = 0f;
            return;
        }
        tempoPoeira += delta;
        float dustInterval = astronauta.isSprinting() ? .105f : .23f;
        if (tempoPoeira < dustInterval) return;
        tempoPoeira = 0f;
        Vector2 movement = input.getDirection();
        particleManager.criarPoeiraLunar(
            astronauta.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
            astronauta.getPosition().y,
            astronauta.isSprinting(), movement.x, movement.y);
    }

    private void atualizarCamera() {
        cameraTarget.set(astronauta.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
            astronauta.getPosition().y + GameConfig.PLAYER_HEIGHT / 2f);
        cameraDirector.update(cameraTarget, astronauta.getBody().getLinearVelocity(),
            hasNearbyHostile(), Gdx.graphics.getDeltaTime());
    }

    private boolean hasNearbyHostile() {
        float radius = GameConfig.CAMERA_COMBAT_RADIUS;
        for (Enemy enemy : world.getEnemies()) {
            if (!enemy.isAtivo()) continue;
            if (Vector2.dst2(enemy.centerX(), enemy.centerY(),
                astronauta.getPosition().x, astronauta.getPosition().y) < radius * radius) {
                return true;
            }
        }
        return false;
    }

    private void verificarGameOver() {
        if (!astronauta.isMorto()) return;
        gameOver = true;
        astronauta.getBody().setLinearVelocity(0f, 0f);
        nextScreen = new GameOverScreen(game, astronauta.getTempoVivo());
    }

    private void verificarPause() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            pausado = !pausado;
            astronauta.getBody().setLinearVelocity(0f, 0f);
            if (pausado) sounds.tocarPause();
            else sounds.tocarUnpause();
        }
        if (!pausado) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            pausado = false;
            sounds.tocarUnpause();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) nextScreen = new MenuScreen(game);
    }

    // =====================================================
    // SAVE E LOAD
    // =====================================================

    private void atualizarSaveLoad() {
        if (input.consumeSavePressed()) {
            saveManager.save(LunarCheckpoint.capture(world));
            feedback.show("Checkpoint salvo.");
        }

        if (!input.consumeLoadPressed()) return;
        GameSaveData data = saveManager.load();
        if (data == null) {
            feedback.show("Nenhum checkpoint encontrado.");
            return;
        }
        LunarCheckpoint.apply(data, world);
        feedback.show("Checkpoint carregado.");
    }

    private boolean trocarTelaSePendente() {
        if (nextScreen == null) return false;
        Screen destination = nextScreen;
        nextScreen = null;
        game.setScreen(destination);
        dispose();
        return true;
    }

    // =====================================================
    // JANELA
    // =====================================================

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        uiViewport.update(width, height, true);
        hud.resize(width, height);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (hud != null) hud.dispose();
        if (particleManager != null) particleManager.dispose();
        if (physicsWorld != null) physicsWorld.dispose();
        if (overlay != null) overlay.dispose();
    }
}
