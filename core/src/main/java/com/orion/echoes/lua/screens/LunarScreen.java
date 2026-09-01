package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.BaseLunar;
import com.orion.echoes.lua.entities.Item;
import com.orion.echoes.lua.entities.Enemy;
import com.orion.echoes.lua.entities.CraftingStation;
import com.orion.echoes.lua.entities.MissionCollectible;
import com.orion.echoes.lua.entities.Obstacle;
import com.orion.echoes.lua.entities.Portal;
import com.orion.echoes.lua.entities.RepairStation;
import com.orion.echoes.lua.entities.Wall;
import com.orion.echoes.lua.input.GameInputProcessor;
import com.orion.echoes.lua.factories.MissionEntityFactory;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.ParticleManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.save.GameSaveData;
import com.orion.echoes.lua.save.SaveManager;
import com.orion.echoes.lua.systems.MissionState;
import com.orion.echoes.lua.systems.CameraDirector;
import com.orion.echoes.lua.systems.JuiceSystem;
import com.orion.echoes.lua.ui.UiTheme;

public class LunarScreen implements Screen {

    private final EchoesLua game;
    private final SpriteBatch batch;
    private final AssetManager assets;

    private OrthographicCamera camera;
    private Viewport viewport;

    private OrthographicCamera pauseCamera;
    private Viewport pauseViewport;

    private ShapeRenderer shapeRenderer;
    private GlyphLayout pauseLayout;
    private NinePatch pausePanel;

    private PhysicsWorld physicsWorld;
    private GameInputProcessor input;

    private Astronauta astronauta;
    private BaseLunar baseLunar;

    private Array<Item> itens;
    private Array<Obstacle> obstacles;
    private Array<Wall> walls;
    private Array<MissionCollectible> missionCollectibles;
    private Array<RepairStation> repairStations;
    private Array<Enemy> enemies;
    private Portal portal;
    private CraftingStation craftingStation;
    private MissionState mission;
    private SaveManager saveManager;

    private Hud hud;

    private ParticleManager particleManager;
    private SoundManager sounds;

    private boolean pausado = false;
    private boolean gameOver = false;
    private boolean vitoria = false;

    private boolean oxigenioCriticoAtivado = false;
    private boolean estavaNaBase = false;

    private float tempoPoeira = 0f;
    private float tempoPasso = 0f;
    private float feedbackTimer = 0f;
    private String feedback = "Colete peças para restaurar a colônia.";
    private final Vector2 mouseWorld = new Vector2();
    private final Vector2 shotStart = new Vector2();
    private final Vector2 shotEnd = new Vector2();
    private float shotFxTimer;
    private JuiceSystem juice;
    private CameraDirector cameraDirector;
    private final Vector2 cameraTarget = new Vector2();
    private boolean portalWasUnlocked;
    private Screen nextScreen;

    public LunarScreen(
        EchoesLua game,
        SpriteBatch batch,
        AssetManager assets
    ) {

        this.game = game;
        this.batch = batch;
        this.assets = assets;
    }

    // =====================================================
    // SHOW
    // =====================================================

    @Override
    public void show() {

        criarCamera();
        criarCameraPause();
        juice = new JuiceSystem();
        juice.setShakeEnabled(game.getSettings().isShakeEnabled());
        cameraDirector = new CameraDirector(camera, viewport, juice,
            GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
        pausePanel = assets.uiModalPatch();

        physicsWorld =
            new PhysicsWorld();

        input =
            new GameInputProcessor();

        Gdx.input.setInputProcessor(
            input
        );

        particleManager =
            new ParticleManager(assets);

        sounds =
            game.getSounds();

        sounds.applySettings(game.getSettings());
        sounds.setVacuum(true);
        sounds.tocarMusicaLunar();

        astronauta =
            new Astronauta(
                GameConfig.PLAYER_START_X,
                GameConfig.PLAYER_START_Y,
                assets,
                physicsWorld
            );

        astronauta.setSpeed(
            GameConfig.PLAYER_SPEED
        );

        baseLunar =
            new BaseLunar(
                GameConfig.BASE_X,
                GameConfig.BASE_Y,
                GameConfig.BASE_WIDTH,
                GameConfig.BASE_HEIGHT,
                assets,
                physicsWorld
            );

        criarWalls();
        criarObstaculos();
        criarMissao();
        criarItens();

        saveManager = new SaveManager();

        hud =
            new Hud(
                assets
            );

        shapeRenderer =
            new ShapeRenderer();

        pauseLayout =
            new GlyphLayout();
    }

    private void criarMissao() {
        mission = new MissionState();
        MissionEntityFactory factory = new MissionEntityFactory(assets, physicsWorld);
        repairStations = new Array<>();
        repairStations.add(factory.station(260, 1390, MissionState.SystemType.COMUNICACAO));
        repairStations.add(factory.station(1480, 1560, MissionState.SystemType.ENERGIA));
        repairStations.add(factory.station(2500, 980, MissionState.SystemType.EXTRACAO));
        repairStations.add(factory.station(420, 560, MissionState.SystemType.ESTUFA));
        craftingStation = factory.craftingStation(1390, 740);
        portal = factory.portal(2710, 1600);

        missionCollectibles = new Array<>();
        MissionState.PartType[] parts = {
            MissionState.PartType.ANTENA, MissionState.PartType.ENERGIA,
            MissionState.PartType.EXTRACAO, MissionState.PartType.ESTUFA,
            MissionState.PartType.ARMA_A, MissionState.PartType.ARMA_B, MissionState.PartType.ARMA_C
        };
        for (MissionState.PartType part : parts) {
            Vector2 spawn = randomFreePosition(62f, 260f);
            missionCollectibles.add(factory.collectible(spawn.x, spawn.y, part));
        }

        enemies = new Array<>();
        enemies.add(factory.enemy(760, 470));
        enemies.add(factory.enemy(1500, 1320));
        enemies.add(factory.enemy(2180, 720));
        enemies.add(factory.enemy(2570, 1420));
        mission.setTotalEnemies(enemies.size);
        feedbackTimer = 5f;
    }

    // =====================================================
    // CAMERAS
    // =====================================================

    private void criarCamera() {

        camera =
            new OrthographicCamera();

        viewport =
            new FitViewport(
                GameConfig.WINDOW_WIDTH,
                GameConfig.WINDOW_HEIGHT,
                camera
            );

        camera.position.set(
            GameConfig.PLAYER_START_X,
            GameConfig.PLAYER_START_Y,
            0
        );

        camera.update();
    }

    private void criarCameraPause() {

        pauseCamera =
            new OrthographicCamera();

        pauseViewport =
            new FitViewport(
                GameConfig.WINDOW_WIDTH,
                GameConfig.WINDOW_HEIGHT,
                pauseCamera
            );

        pauseCamera.position.set(
            GameConfig.WINDOW_WIDTH / 2f,
            GameConfig.WINDOW_HEIGHT / 2f,
            0
        );

        pauseCamera.update();
    }

    // =====================================================
    // PAREDES
    // =====================================================

    private void criarWalls() {

        walls =
            new Array<>();

        float e =
            35f;

        walls.add(
            new Wall(
                -e,
                0,
                e,
                GameConfig.WORLD_HEIGHT,
                physicsWorld
            )
        );

        walls.add(
            new Wall(
                GameConfig.WORLD_WIDTH,
                0,
                e,
                GameConfig.WORLD_HEIGHT,
                physicsWorld
            )
        );

        walls.add(
            new Wall(
                0,
                -e,
                GameConfig.WORLD_WIDTH,
                e,
                physicsWorld
            )
        );

        walls.add(
            new Wall(
                0,
                GameConfig.WORLD_HEIGHT,
                GameConfig.WORLD_WIDTH,
                e,
                physicsWorld
            )
        );
    }

    // =====================================================
    // OBSTACULOS
    // =====================================================

    private void criarObstaculos() {

        obstacles =
            new Array<>();

        addObstacle(430, 220, 90, 90);
        addObstacle(650, 420, 110, 110);
        addObstacle(820, 160, 95, 95);

        addObstacle(390, 760, 100, 100);
        addObstacle(620, 1100, 115, 115);
        addObstacle(1050, 1060, 125, 105);

        addObstacle(1210, 280, 110, 100);
        addObstacle(1370, 480, 105, 105);
        addObstacle(1450, 870, 125, 115);

        addObstacle(1640, 650, 100, 100);
        addObstacle(1770, 250, 90, 90);
        addObstacle(1830, 1040, 120, 120);

        addObstacle(2020, 720, 95, 95);
        addObstacle(250, 1210, 85, 85);
        addObstacle(2260, 260, 120, 105);
        addObstacle(2440, 520, 105, 115);
        addObstacle(2680, 820, 125, 110);
        addObstacle(2320, 1240, 110, 100);
        addObstacle(1880, 1550, 125, 115);
        addObstacle(980, 1580, 105, 95);
    }

    private void addObstacle(
        float x,
        float y,
        float w,
        float h
    ) {

        obstacles.add(
            new Obstacle(
                x,
                y,
                w * 1.14f,
                h * 1.14f,
                assets.lunarObstacleRegion(obstacles.size % 6),
                physicsWorld
            )
        );
    }

    // =====================================================
    // ITENS
    // =====================================================

    private void criarItens() {

        itens =
            new Array<>();
        for (Item.TipoItem type : Item.TipoItem.values()) {
            for (int index = 0; index < 6; index++) {
                Vector2 spawn = randomFreePosition(GameConfig.ITEM_SIZE, 190f);
                adicionarItem(spawn.x, spawn.y, type);
            }
        }
    }

    private Vector2 randomFreePosition(float size, float minPlayerDistance) {
        Rectangle candidate = new Rectangle();
        for (int attempt = 0; attempt < 100; attempt++) {
            float x = MathUtils.random(110f, GameConfig.WORLD_WIDTH - size - 110f);
            float y = MathUtils.random(110f, GameConfig.WORLD_HEIGHT - size - 110f);
            candidate.set(x, y, size, size);
            if (Vector2.dst(x, y, GameConfig.PLAYER_START_X, GameConfig.PLAYER_START_Y) < minPlayerDistance) continue;
            if (baseLunar != null && candidate.overlaps(baseLunar.getBounds())) continue;
            boolean blocked = false;
            for (Obstacle obstacle : obstacles) {
                if (candidate.overlaps(obstacle.getBounds())) { blocked = true; break; }
            }
            if (blocked) continue;
            if (repairStations != null) for (RepairStation station : repairStations) {
                if (candidate.overlaps(station.getBounds())) { blocked = true; break; }
            }
            if (blocked || craftingStation != null && candidate.overlaps(craftingStation.getBounds())
                || portal != null && candidate.overlaps(portal.getBounds())) continue;
            if (itens != null) for (Item item : itens) {
                if (candidate.overlaps(item.getBounds())) { blocked = true; break; }
            }
            if (blocked) continue;
            if (missionCollectibles != null) for (MissionCollectible collectible : missionCollectibles) {
                if (candidate.overlaps(collectible.getBounds())) { blocked = true; break; }
            }
            if (!blocked) return new Vector2(x, y);
        }
        return new Vector2(MathUtils.random(120f, GameConfig.WORLD_WIDTH - size - 120f),
            MathUtils.random(120f, GameConfig.WORLD_HEIGHT - size - 120f));
    }

    private void adicionarItem(
        float x,
        float y,
        Item.TipoItem tipo
    ) {

        itens.add(
            new Item(
                x,
                y,
                GameConfig.ITEM_SIZE,
                GameConfig.ITEM_SIZE,
                tipo,
                assets,
                physicsWorld
            )
        );
    }

    // =====================================================
    // UPDATE
    // =====================================================

    /**
     * Mantem o mixer vivo fora do relogio de gameplay: a trilha nao pode
     * congelar durante hitstop nem parar na pausa.
     */
    private void atualizarMixer() {
        sounds.setListener(camera.position.x, camera.position.y);
        if (pausado) {
            sounds.atualizarIntensidade(0f, 0f);
        } else {
            sounds.atualizarIntensidade(tensaoDeCombate(),
                urgenciaDeOxigenio(astronauta.getOxigenio()));
        }
    }

    /** 0 quando nenhum hostil ameaca; 1 com hostil colado no jogador. */
    private float tensaoDeCombate() {
        float strongest = 0f;
        for (Enemy enemy : enemies) {
            if (!enemy.isAtivo()) continue;
            float dx = enemy.centerX() - astronauta.getPosition().x;
            float dy = enemy.centerY() - astronauta.getPosition().y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
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

    private void update(float delta) {

        if (
            pausado
                || gameOver
                || vitoria
        ) {

            return;
        }

        delta =
            Math.min(
                delta,
                1f / 30f
            );

        juice.update(delta);
        float gameplayDelta = juice.gameplayDelta(delta);
        if (gameplayDelta == 0f) {
            particleManager.update(delta * .35f);
            return;
        }
        delta = gameplayDelta;

        // ==========================================
        // MOVIMENTO
        // ==========================================

        Vector2 direction =
            input.getDirection();

        if (input.consumeDashPressed() && astronauta.tryDash(direction.x, direction.y)) {
            particleManager.criarPoeiraLunar(astronauta.getPosition().x + 27f,
                astronauta.getPosition().y + 4f, true, direction.x, direction.y);
            juice.trigger(JuiceSystem.Preset.DASH);
        }

        astronauta.move(
            direction.x,
            direction.y,
            input.isRunning(),
            delta
        );

        // ==========================================
        // FISICA
        // ==========================================

        physicsWorld.update(
            delta
        );

        // ==========================================
        // PLAYER
        // ==========================================

        astronauta.update(
            delta
        );
        mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouseWorld);
        astronauta.setAimTarget(mouseWorld.x, mouseWorld.y);
        astronauta.setWeaponEquipped(mission.hasWeapon());
        shotFxTimer = Math.max(0f, shotFxTimer - delta);

        // ==========================================
        // ITENS FLUTUANTES
        // ==========================================

        for (Item item : itens) {

            item.update(
                delta
            );
        }

        // ==========================================
        // BASE
        // ==========================================

        baseLunar.update(
            delta
        );

        atualizarBase();

        // ==========================================
        // COLETAS
        // ==========================================

        atualizarItens();
        atualizarMissao(delta);

        // ==========================================
        // RECARGA DA BASE
        // ==========================================

        if (
            baseLunar
                .isAstronautaDentro()
        ) {

            baseLunar
                .recarregarOxigenio(
                    astronauta,
                    delta
                );
        }

        atualizarInteracao();

        // ==========================================
        // EFEITOS
        // ==========================================

        atualizarParticulas(
            delta
        );

        atualizarSomPassos(
            delta
        );

        verificarOxigenio();

        particleManager.update(
            delta
        );

        atualizarCamera();

        verificarGameOver();

        atualizarSaveLoad();
    }

    // =====================================================
    // BASE
    // =====================================================

    private void atualizarBase() {

        boolean dentro =
            astronauta
                .getBounds()
                .overlaps(
                    baseLunar
                        .getBounds()
                );

        if (dentro) {

            baseLunar.entrar(
                astronauta
            );

            if (!estavaNaBase) {

                estavaNaBase =
                    true;

                sounds
                    .tocarBaseRecarregando();
            }

        } else {

            baseLunar.sair(
                astronauta
            );

            estavaNaBase =
                false;
        }
    }

    // =====================================================
    // COLETAS
    // =====================================================

    private void atualizarItens() {

        for (
            Item item
            : itens
        ) {

            if (
                item.isColetado()
            ) {

                continue;
            }

            if (
                !astronauta
                    .getBounds()
                    .overlaps(
                        item.getBounds()
                    )
            ) {

                continue;
            }

            float x =
                item.getCenterX();

            float y =
                item.getCenterY();

            Item.TipoItem tipo =
                item.getTipo();

            item.coletar(
                astronauta
            );
            astronauta.registrarColeta(tipo);
            juice.trigger(JuiceSystem.Preset.COLLECT);

            particleManager
                .criarEfeitoColeta(
                    x,
                    y
                );

            // Som específico
            switch (tipo) {

                case OXIGENIO:

                    sounds
                        .tocarOxigenio();

                    break;

                case COMIDA:

                    sounds
                        .tocarComida();

                    break;

                case GELO:

                    sounds
                        .tocarGelo();

                    break;
            }
        }
    }

    // =====================================================
    // PROCESSAR GELO
    // =====================================================

    private void atualizarInteracao() {

        if (
            !input
                .consumeInteractPressed()
        ) {

            return;
        }

        for (RepairStation station : repairStations) {
            if (!station.isPlayerNear(astronauta)) continue;
            if (station.repair(mission)) {
                showFeedback(station.getType().getLabel() + " reparada. Sistema ONLINE.");
                particleManager.criarProcessamento(station.getPosition().x + 63f, station.getPosition().y + 63f);
                sounds.tocarReparoConcluido();
                juice.trigger(JuiceSystem.Preset.REPAIR);
            } else if (mission.isRepaired(station.getType())) {
                showFeedback(station.getType().getLabel() + " já está online.");
            } else {
                showFeedback("Falta a peça de " + station.getType().getRequiredPart().getLabel() + ".");
            }
            return;
        }

        if (portal.isPlayerNear(astronauta)) {
            if (mission.isPortalUnlocked(astronauta.getOxigenio())) {
                vitoria = true;
                nextScreen = new MarsScreen(game, astronauta.getOxigenio(), astronauta.getEnergia(),
                    mission.getEnemiesDefeated());
            } else {
                showFeedback("Portal bloqueado. " + mission.getObjective(astronauta.getOxigenio()));
            }
            return;
        }

        if (craftingStation.isPlayerNear(astronauta)
            && mission.hasAllWeaponParts() && !mission.hasWeapon()) {
            mission.craftWeapon();
            showFeedback("Arma montada. Mire com o mouse e dispare.");
            sounds.tocarCraft();
            juice.trigger(JuiceSystem.Preset.CRAFT);
            return;
        }

        if (craftingStation.isPlayerNear(astronauta)) {
            showFeedback(mission.hasWeapon() ? "Arma equipada." : "Ainda faltam partes da arma.");
            return;
        }

        if (!baseLunar.isAstronautaDentro()) {
            showFeedback("Nada para usar aqui.");
            return;
        }

        if (mission.hasAllWeaponParts() && !mission.hasWeapon()) {
            showFeedback("Use a bancada laranja dentro da base para fabricar a arma.");
            return;
        }

        if (baseLunar.processarGelo(astronauta)) {
            sounds.tocarProcessarGelo();
            showFeedback("Gelo processado: O2, água e combustível gerados.");
            particleManager.criarProcessamento(
                baseLunar.getPosition().x + GameConfig.BASE_WIDTH / 2f,
                baseLunar.getPosition().y + GameConfig.BASE_HEIGHT / 2f);
        } else {
            showFeedback("Sem gelo. Traga gelo ou as três partes da arma.");
            sounds.tocarSemGelo();
        }
    }

    private void atualizarMissao(float delta) {
        feedbackTimer = Math.max(0f, feedbackTimer - delta);
        if (feedbackTimer == 0f) feedback = "";

        for (MissionCollectible collectible : missionCollectibles) {
            collectible.update(delta);
            if (collectible.collectIfOverlapping(astronauta, mission)) {
                showFeedback("Coletado: " + collectible.getType().getLabel());
                sounds.tocarColetaEspacial(collectible.getPosition().x + 27f,
                    collectible.getPosition().y + 27f);
                particleManager.criarEfeitoColeta(collectible.getPosition().x + 27f,
                    collectible.getPosition().y + 27f);
                juice.trigger(JuiceSystem.Preset.COLLECT);
            }
        }

        for (RepairStation station : repairStations) {
            station.update(delta);
        }

        for (Enemy enemy : enemies) {
            enemy.update(delta, astronauta, obstacles);
            if (enemy.consumeTelegraphStarted()) {
                particleManager.criarAlertaInimigo(enemy.centerX(), enemy.centerY(), false);
                sounds.tocarAlertaInimigo(enemy.centerX(), enemy.centerY());
            }
            if (enemy.canDamage(astronauta)) {
                astronauta.receberDano(12f, enemy.centerX(), enemy.centerY());
                showFeedback("Hostil atingiu o traje: -12 O2.");
                particleManager.criarImpactoTraje(
                    astronauta.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
                    astronauta.getPosition().y + GameConfig.PLAYER_HEIGHT / 2f);
                juice.trigger(JuiceSystem.Preset.PLAYER_HURT);
            }
        }

        if (input.consumeAttackPressed()) atacar();
        boolean unlocked = mission.isPortalUnlocked(astronauta.getOxigenio());
        if (unlocked && !portalWasUnlocked) {
            float portalX = portal.getPosition().x + portal.getBounds().width / 2f;
            float portalY = portal.getPosition().y + portal.getBounds().height / 2f;
            particleManager.criarPortal(portalX, portalY);
            showFeedback("Portal energizado. A passagem para Marte está aberta.");
        }
        portalWasUnlocked = unlocked;
        portal.setUnlocked(unlocked);
        portal.update(delta);
    }

    private void atacar() {
        if (!mission.hasWeapon()) {
            showFeedback("Ataque indisponível: fabrique a arma na base.");
            return;
        }
        float x = astronauta.getPosition().x + GameConfig.PLAYER_WIDTH / 2f;
        float y = astronauta.getPosition().y + GameConfig.PLAYER_HEIGHT * .48f;
        float dirX = MathUtils.cosDeg(astronauta.getAimAngle());
        float dirY = MathUtils.sinDeg(astronauta.getAimAngle());
        Enemy target = null;
        float closest = 420f;
        for (Enemy enemy : enemies) {
            if (!enemy.isAtivo()) continue;
            float dx = enemy.centerX() - x;
            float dy = enemy.centerY() - y;
            float alongRay = dx * dirX + dy * dirY;
            float perpendicular = Math.abs(dx * dirY - dy * dirX);
            if (alongRay > 0f && alongRay < closest && perpendicular < 48f) {
                closest = alongRay;
                target = enemy;
            }
        }
        shotStart.set(x + dirX * 34f, y + dirY * 34f);
        shotEnd.set(x + dirX * 420f, y + dirY * 420f);
        if (target != null) {
            shotEnd.set(target.centerX(), target.centerY());
            boolean killed = target.takeHit();
            if (killed) {
                mission.registerEnemyDefeated();
                particleManager.criarMorteInimigo(target.centerX(), target.centerY());
                sounds.tocarMorteInimigo(target.centerX(), target.centerY());
            } else {
                particleManager.criarImpactoTiro(target.centerX(), target.centerY());
                sounds.tocarImpacto(target.centerX(), target.centerY());
            }
            showFeedback("Alvo atingido");
            juice.trigger(killed ? JuiceSystem.Preset.ENEMY_KILL : JuiceSystem.Preset.SHOT_HIT);
        }
        particleManager.criarMuzzleFlash(shotStart.x, shotStart.y, astronauta.getAimAngle());
        astronauta.triggerShot();
        sounds.tocarDisparo();
        shotFxTimer = .14f;
    }

    private void showFeedback(String message) {
        feedback = message;
        feedbackTimer = 3.5f;
    }

    private void atualizarSaveLoad() {
        if (input.consumeSavePressed()) {
            GameSaveData data = astronauta.toSaveData();
            data.pecaAntena = mission.getPartCount(MissionState.PartType.ANTENA);
            data.pecaEnergia = mission.getPartCount(MissionState.PartType.ENERGIA);
            data.pecaExtracao = mission.getPartCount(MissionState.PartType.EXTRACAO);
            data.pecaEstufa = mission.getPartCount(MissionState.PartType.ESTUFA);
            data.armaParteA = mission.getPartCount(MissionState.PartType.ARMA_A);
            data.armaParteB = mission.getPartCount(MissionState.PartType.ARMA_B);
            data.armaParteC = mission.getPartCount(MissionState.PartType.ARMA_C);
            data.comunicacaoReparada = mission.isRepaired(MissionState.SystemType.COMUNICACAO);
            data.energiaReparada = mission.isRepaired(MissionState.SystemType.ENERGIA);
            data.extracaoReparada = mission.isRepaired(MissionState.SystemType.EXTRACAO);
            data.estufaReparada = mission.isRepaired(MissionState.SystemType.ESTUFA);
            data.armaCraftada = mission.hasWeapon();
            data.inimigosEliminados = mission.getEnemiesDefeated();
            saveManager.save(data);
            showFeedback("Checkpoint salvo.");
        }

        if (input.consumeLoadPressed()) {
            GameSaveData data = saveManager.load();
            if (data == null) {
                showFeedback("Nenhum checkpoint encontrado.");
                return;
            }
            astronauta.fromSaveData(data);
            mission.restore(data.pecaAntena, data.pecaEnergia, data.pecaExtracao, data.pecaEstufa,
                data.armaParteA, data.armaParteB, data.armaParteC,
                data.comunicacaoReparada, data.energiaReparada, data.extracaoReparada,
                data.estufaReparada, data.armaCraftada, data.inimigosEliminados);
            sincronizarMundoCarregado();
            showFeedback("Checkpoint carregado.");
        }
    }

    private void sincronizarMundoCarregado() {
        for (RepairStation station : repairStations) station.sync(mission);
        for (MissionCollectible collectible : missionCollectibles) {
            MissionState.PartType type = collectible.getType();
            boolean consumed = mission.getPartCount(type) > 0 || switch (type) {
                case ANTENA -> mission.isRepaired(MissionState.SystemType.COMUNICACAO);
                case ENERGIA -> mission.isRepaired(MissionState.SystemType.ENERGIA);
                case EXTRACAO -> mission.isRepaired(MissionState.SystemType.EXTRACAO);
                case ESTUFA -> mission.isRepaired(MissionState.SystemType.ESTUFA);
                case ARMA_A, ARMA_B, ARMA_C -> mission.hasWeapon();
            };
            collectible.setAtivo(!consumed);
        }
        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).setAtivo(i >= mission.getEnemiesDefeated());
        }
    }

    // =====================================================
    // PASSOS
    // =====================================================

    private void atualizarSomPassos(
        float delta
    ) {

        if (!astronauta.isMoving()) {

            tempoPasso = 0f;

            return;
        }

        tempoPasso += delta;

        if (
            tempoPasso >= 0.48f
        ) {

            tempoPasso = 0f;

            sounds
                .tocarPassoLunar();
        }
    }

    // =====================================================
    // PARTICULAS
    // =====================================================

    private void atualizarParticulas(
        float delta
    ) {

        if (!astronauta.isMoving()) {

            tempoPoeira = 0f;

            return;
        }

        tempoPoeira += delta;

        float dustInterval = astronauta.isSprinting() ? .105f : .23f;
        if (tempoPoeira >= dustInterval) {

            tempoPoeira = 0f;

            Vector2 movement = input.getDirection();
            particleManager.criarPoeiraLunar(
                astronauta.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
                astronauta.getPosition().y,
                astronauta.isSprinting(), movement.x, movement.y);
        }
    }

    // =====================================================
    // OXIGENIO CRITICO
    // =====================================================

    private void verificarOxigenio() {

        if (
            astronauta.getOxigenio()
                <= 25f
                &&
                !oxigenioCriticoAtivado
        ) {

            oxigenioCriticoAtivado =
                true;

            sounds
                .tocarAlertaOxigenio();

            particleManager
                .criarAlertaOxigenio(
                    astronauta
                        .getPosition()
                        .x
                        + GameConfig.PLAYER_WIDTH / 2f,

                    astronauta
                        .getPosition()
                        .y
                        + GameConfig.PLAYER_HEIGHT
                );
        }

        if (
            astronauta.getOxigenio()
                > 25f
        ) {

            oxigenioCriticoAtivado =
                false;
        }
    }

    // =====================================================
    // CAMERA
    // =====================================================

    private void atualizarCamera() {
        cameraTarget.set(astronauta.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
            astronauta.getPosition().y + GameConfig.PLAYER_HEIGHT / 2f);
        cameraDirector.update(cameraTarget, astronauta.getBody().getLinearVelocity(),
            hasNearbyHostile(), Gdx.graphics.getDeltaTime());
    }

    private boolean hasNearbyHostile() {
        float px = astronauta.getPosition().x;
        float py = astronauta.getPosition().y;
        for (Enemy enemy : enemies) {
            if (!enemy.isAtivo()) continue;
            float dx = enemy.centerX() - px;
            float dy = enemy.centerY() - py;
            if (dx * dx + dy * dy < GameConfig.CAMERA_COMBAT_RADIUS * GameConfig.CAMERA_COMBAT_RADIUS) return true;
        }
        return false;
    }

    // =====================================================
    // GAME OVER
    // =====================================================

    private void verificarGameOver() {

        if (!astronauta.isMorto()) {
            return;
        }

        gameOver =
            true;

        astronauta
            .getBody()
            .setLinearVelocity(
                0,
                0
            );

        nextScreen = new GameOverScreen(game, astronauta.getTempoVivo());
    }

    // =====================================================
    // PAUSE
    // =====================================================

    private void verificarPause() {

        if (
            Gdx.input
                .isKeyJustPressed(
                    Input.Keys.ESCAPE
                )
        ) {

            pausado =
                !pausado;

            astronauta
                .getBody()
                .setLinearVelocity(
                    0,
                    0
                );

            if (pausado) {

                sounds
                    .tocarPause();

            } else {

                sounds
                    .tocarUnpause();
            }
        }

        if (!pausado) {
            return;
        }

        // ENTER CONTINUA
        if (
            Gdx.input
                .isKeyJustPressed(
                    Input.Keys.ENTER
                )
        ) {

            pausado =
                false;

            sounds
                .tocarUnpause();
        }

        // M VOLTA PARA MENU
        if (
            Gdx.input
                .isKeyJustPressed(
                    Input.Keys.M
                )
        ) {

            nextScreen = new MenuScreen(game);
        }

    }

    // =====================================================
    // RENDER
    // =====================================================

    @Override
    public void render(
        float delta
    ) {

        verificarPause();

        atualizarMixer();

        if (trocarTelaSePendente()) {
            return;
        }

        update(
            delta
        );

        if (trocarTelaSePendente()) {
            return;
        }

        Gdx.gl.glClearColor(
            0.02f,
            0.025f,
            0.04f,
            1f
        );

        Gdx.gl.glClear(
            GL20.GL_COLOR_BUFFER_BIT
        );

        batch.setProjectionMatrix(
            camera.combined
        );

        batch.begin();

        // BACKGROUND
        batch.draw(
            assets.backgroundLuaTexture,
            0,
            0,
            GameConfig.WORLD_WIDTH,
            GameConfig.WORLD_HEIGHT,
            0f,
            0f,
            GameConfig.WORLD_WIDTH / assets.backgroundLuaTexture.getWidth(),
            GameConfig.WORLD_HEIGHT / assets.backgroundLuaTexture.getHeight()
        );

        renderLandmarks();

        // BASE
        baseLunar.render(
            batch
        );

        // ITENS
        for (
            Item item
            : itens
        ) {

            item.render(
                batch
            );
        }

        for (MissionCollectible collectible : missionCollectibles) {
            collectible.render(batch);
        }

        for (RepairStation station : repairStations) {
            station.render(batch);
        }

        craftingStation.render(batch);

        portal.render(batch);

        for (Enemy enemy : enemies) {
            enemy.render(batch);
        }

        // OBSTACULOS
        for (
            Obstacle obstacle
            : obstacles
        ) {

            obstacle.render(
                batch
            );
        }

        // PLAYER
        astronauta.render(
            batch
        );

        // PARTICULAS
        particleManager.render(
            batch
        );

        batch.end();

        renderShotEffect();
        renderEnemyHealthBars();

        // HUD NÃO APARECE DURANTE PAUSE
        if (!pausado) {
            String hudMessage = feedback == null || feedback.isBlank() ? getContextHint() : feedback;
            hud.update(Math.min(delta, 1f / 30f), hudMessage);
            hud.render(
                batch,
                astronauta,
                mission,
                hudMessage,
                astronauta.getPosition().x + GameConfig.PLAYER_WIDTH / 2f - camera.position.x + GameConfig.WINDOW_WIDTH / 2f,
                astronauta.getPosition().y + GameConfig.PLAYER_HEIGHT / 2f - camera.position.y + GameConfig.WINDOW_HEIGHT / 2f
            );
        }

        renderDamageOverlay();

        if (pausado) {

            renderPause();
        }
    }

    private void renderDamageOverlay() {
        float alpha = juice.getDamageFlashAlpha();
        if (alpha <= 0f) return;
        batch.setProjectionMatrix(pauseCamera.combined);
        batch.begin();
        batch.setColor(1f, 1f, 1f, alpha * .82f);
        batch.draw(assets.uiDamageVignetteTexture, 0f, 0f,
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void renderLandmarks() {
        batch.setColor(.78f, .82f, .88f, .86f);
        batch.draw(assets.landmarkRegion(0, 0), 1540f, 1440f, 250f, 185f);
        batch.draw(assets.landmarkRegion(1, 0), 2440f, 1080f, 205f, 165f);
        batch.draw(assets.landmarkRegion(2, 0), 330f, 1510f, 230f, 150f);
        batch.draw(assets.landmarkRegion(3, 0), 2100f, 250f, 180f, 205f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderShotEffect() {
        if (shotFxTimer <= 0f) return;
        float alpha = shotFxTimer / .14f;
        float progress = 1f - alpha;
        float px = MathUtils.lerp(shotStart.x, shotEnd.x, progress);
        float py = MathUtils.lerp(shotStart.y, shotEnd.y, progress);
        float dx = shotEnd.x - shotStart.x;
        float dy = shotEnd.y - shotStart.y;
        float length = Math.max(.001f, (float)Math.sqrt(dx * dx + dy * dy));
        dx /= length;
        dy /= length;
        shapeRenderer.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(.45f, .95f, 1f, alpha * .35f);
        shapeRenderer.rectLine(px - dx * 34f, py - dy * 34f, px, py, 7f * alpha);
        shapeRenderer.setColor(.9f, 1f, 1f, alpha);
        shapeRenderer.rectLine(px - dx * 42f, py - dy * 42f, px, py, 2f);
        shapeRenderer.circle(px, py, 5f + 4f * alpha, 12);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderEnemyHealthBars() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Enemy enemy : enemies) {
            if (!enemy.isAtivo() || enemy.getHealthRatio() >= 1f) continue;
            float x = enemy.centerX() - 34f;
            float y = enemy.centerY() + 46f;
            batch.setColor(Color.WHITE);
            batch.draw(assets.uiBarTrackTexture, x, y, 68f, 7f);
            batch.setColor(UiTheme.MAGENTA);
            batch.draw(assets.uiBarFillTexture, x, y, 68f * enemy.getHealthRatio(), 7f);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private String getContextHint() {
        for (RepairStation station : repairStations) {
            if (station.isPlayerNear(astronauta)) {
                return mission.isRepaired(station.getType())
                    ? station.getType().getLabel() + ": sistema ativo"
                    : station.getType().getLabel() + " pronta para reparo";
            }
        }
        if (craftingStation.isPlayerNear(astronauta)) {
            if (mission.hasWeapon()) return "Bancada: arma pronta";
            return mission.hasAllWeaponParts() ? "Arma pronta para montagem" : "Bancada: faltam partes A, B e C";
        }
        if (portal.isPlayerNear(astronauta)) {
            return mission.isPortalUnlocked(astronauta.getOxigenio())
                ? "Portal pronto para travessia"
                : "Portal inativo: conclua a missão";
        }
        if (baseLunar.isAstronautaDentro()) return "Base pressurizada: O2 recarregando";
        return "";
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
    // PAUSE VISUAL
    // =====================================================

    private void renderPause() {
        batch.setProjectionMatrix(pauseCamera.combined);
        batch.begin();
        batch.setColor(.012f, .021f, .027f, .91f);
        batch.draw(assets.uiWhiteTexture, 0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        pausePanel.setColor(new Color(1f, 1f, 1f, .97f));
        pausePanel.draw(batch, 54f, 106f, 790f, 500f);
        pausePanel.draw(batch, 872f, 106f, 354f, 500f);
        pausePanel.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);
        desenharPauseTexto("REGISTRO LUNAR · EM ESPERA", .72f, UiTheme.CYAN, 74f, 618f);
        desenharPauseTexto("PAUSA", 2.45f, UiTheme.TEXT, 68f, 540f);
        desenharPauseTexto("A missão está congelada. Nenhum recurso será consumido.",
            .82f, UiTheme.TEXT_MUTED, 74f, 474f);
        desenharPauseTexto("RETOMAR", 1.02f, UiTheme.TEXT, 104f, 278f);
        desenharPauseTexto("ESC ou ENTER", .68f, UiTheme.CYAN, 610f, 278f);
        desenharPauseTexto("VOLTAR AO MENU", .82f, UiTheme.TEXT_MUTED, 74f, 150f);
        desenharPauseTexto("M", .74f, UiTheme.AMBER, 610f, 150f);
        desenharPauseTexto(String.format("O2 %.0f%%", astronauta.getOxigenio()),
            .88f, UiTheme.TEXT, 934f, 520f);
        desenharPauseTexto(String.format("REPAROS %d/3", mission.getRepairCount()),
            .88f, UiTheme.TEXT, 934f, 474f);
        desenharPauseTexto(String.format("TEMPO %.1fs", astronauta.getTempoVivo()),
            .76f, UiTheme.TEXT_MUTED, 934f, 418f);
        batch.end();
    }

    private void desenharPauseTexto(String texto, float escala, Color cor, float x, float y) {
        assets.font.getData().setScale(escala);
        assets.font.setColor(cor);
        assets.font.draw(batch, texto, x, y);
    }

    private void desenharPauseCentralizado(
        String texto,
        float escala,
        Color cor,
        float y
    ) {

        assets.font
            .getData()
            .setScale(
                escala
            );

        assets.font.setColor(
            cor
        );

        pauseLayout.setText(
            assets.font,
            texto
        );

        float x =
            (
                GameConfig.WINDOW_WIDTH
                    - pauseLayout.width
            ) / 2f;

        assets.font.draw(
            batch,
            pauseLayout,
            x,
            y
        );
    }

    // =====================================================
    // RESIZE
    // =====================================================

    @Override
    public void resize(
        int width,
        int height
    ) {

        viewport.update(
            width,
            height,
            true
        );

        pauseViewport.update(
            width,
            height,
            true
        );

        hud.resize(
            width,
            height
        );
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {

        if (hud != null) {
            hud.dispose();
        }

        if (particleManager != null) {
            particleManager.dispose();
        }

        if (physicsWorld != null) {
            physicsWorld.dispose();
        }

        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
