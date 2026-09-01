package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.orion.echoes.lua.entities.EnemyPulse;
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
    private final Vector2 objectiveTarget = new Vector2();
    private final Array<EnemyPulse> enemyPulses = new Array<>();
    private float markerPulse;
    private Cursor blankCursor;

    /** Distancia da borda em que o marcador de objetivo encosta. */
    private static final float MARKER_MARGIN = 58f;
    private static final float MARKER_SIZE = 46f;
    private static final float CURSOR_SIZE = 34f;
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

        pauseLayout =
            new GlyphLayout();

        esconderCursorDoSistema();
    }

    /**
     * O jogo desenha o proprio cursor; o do sistema operacional sairia
     * duplicado por cima. Substitui-lo por um cursor transparente e a forma
     * de esconde-lo sem capturar o ponteiro dentro da janela.
     */
    private void esconderCursorDoSistema() {
        if (blankCursor != null) return;
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        blankCursor = Gdx.graphics.newCursor(pixmap, 0, 0);
        pixmap.dispose();
        if (blankCursor != null) Gdx.graphics.setCursor(blankCursor);
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
        /*
         * A ordem ensina sem texto: o primeiro hostil aparece isolado e no
         * comportamento mais simples; emboscador e atirador so entram depois,
         * quando o jogador ja leu o telegraph uma vez.
         */
        enemies.add(factory.enemy(760, 470, Enemy.Behavior.STALKER));
        enemies.add(factory.enemy(1500, 1320, Enemy.Behavior.AMBUSHER));
        enemies.add(factory.enemy(2180, 720, Enemy.Behavior.RANGED));
        enemies.add(factory.enemy(2570, 1420, Enemy.Behavior.AMBUSHER));
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
        markerPulse += Gdx.graphics.getDeltaTime();
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
                    delta,
                    mission.getRechargeMultiplier()
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

        aplicarOxigenioPassivo(delta);

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
                showFeedback(station.getType().getLabel() + " ONLINE  •  "
                    + MissionState.getPerkLabel(station.getType()));
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

        if (baseLunar.processarGelo(astronauta, mission.getIceYieldMultiplier())) {
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
            if (enemy.consumeRangedShot()) {
                enemyPulses.add(new EnemyPulse(enemy.centerX(), enemy.centerY(),
                    enemy.getAimDirection().x, enemy.getAimDirection().y, assets));
                sounds.tocarEspacial("disparo_pulso", SoundManager.Bus.SFX, .5f,
                    enemy.centerX(), enemy.centerY());
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

        atualizarPulsosHostis(delta);

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

    /** Pulsos do hostil atirador: viajam, batem em rocha ou acertam o traje. */
    private void atualizarPulsosHostis(float delta) {
        for (int index = enemyPulses.size - 1; index >= 0; index--) {
            EnemyPulse pulse = enemyPulses.get(index);
            pulse.update(delta);
            if (pulse.collideWith(obstacles)) {
                particleManager.criarImpactoTiro(pulse.centerX(), pulse.centerY());
            } else if (pulse.hits(astronauta)) {
                astronauta.receberDano(GameConfig.ENEMY_PULSE_DAMAGE,
                    pulse.centerX(), pulse.centerY());
                showFeedback("Pulso hostil atingiu o traje.");
                particleManager.criarImpactoTraje(
                    astronauta.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
                    astronauta.getPosition().y + GameConfig.PLAYER_HEIGHT / 2f);
                juice.trigger(JuiceSystem.Preset.PLAYER_HURT);
            }
            if (!pulse.isAtivo()) enemyPulses.removeIndex(index);
        }
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

    /** Estufa reparada devolve oxigenio devagar em campo aberto. */
    private void aplicarOxigenioPassivo(float delta) {
        float perSecond = mission.getPassiveOxygenPerSecond();
        if (perSecond <= 0f || baseLunar.isAstronautaDentro()) return;
        astronauta.recuperarOxigenio(perSecond * delta);
    }

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

        for (EnemyPulse pulse : enemyPulses) {
            pulse.render(batch);
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

        renderObjectiveMarker();

        renderDamageOverlay();

        if (pausado) {

            renderPause();
        }

        if (!pausado) renderCursor();
    }

    /**
     * Marcador direcional do objetivo atual.
     *
     * So aparece com a Comunicacao reparada: e o beneficio jogavel daquele
     * reparo. Fica preso a borda quando o alvo esta fora do enquadramento e
     * pousa sobre ele quando entra em tela.
     */
    private void renderObjectiveMarker() {
        if (pausado || !mission.isMapRevealed()) return;
        if (!encontrarAlvoDeObjetivo(objectiveTarget)) return;

        float screenX = objectiveTarget.x - camera.position.x + GameConfig.WINDOW_WIDTH / 2f;
        float screenY = objectiveTarget.y - camera.position.y + GameConfig.WINDOW_HEIGHT / 2f;
        float centerX = GameConfig.WINDOW_WIDTH / 2f;
        float centerY = GameConfig.WINDOW_HEIGHT / 2f;
        float angle = MathUtils.atan2(screenY - centerY, screenX - centerX) * MathUtils.radiansToDegrees;
        boolean offScreen = screenX < MARKER_MARGIN
            || screenX > GameConfig.WINDOW_WIDTH - MARKER_MARGIN
            || screenY < MARKER_MARGIN
            || screenY > GameConfig.WINDOW_HEIGHT - MARKER_MARGIN;

        float drawX = MathUtils.clamp(screenX, MARKER_MARGIN, GameConfig.WINDOW_WIDTH - MARKER_MARGIN);
        float drawY = MathUtils.clamp(screenY, MARKER_MARGIN, GameConfig.WINDOW_HEIGHT - MARKER_MARGIN);
        float pulse = .82f + MathUtils.sin(markerPulse * 4.2f) * .18f;

        batch.setProjectionMatrix(pauseCamera.combined);
        batch.begin();
        batch.setColor(1f, 1f, 1f, offScreen ? .95f : .55f);
        float size = MARKER_SIZE * pulse;
        batch.draw(assets.uiObjectiveMarkerTexture,
            drawX - size / 2f, drawY - size / 2f,
            size / 2f, size / 2f, size, size, 1f, 1f,
            offScreen ? angle : 0f);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Alvo do marcador, na mesma ordem de prioridade do texto de objetivo. */
    private boolean encontrarAlvoDeObjetivo(Vector2 out) {
        if (mission.getRepairCount() < 3) {
            if (alvoMaisProximo(out)) return true;
            for (RepairStation station : repairStations) {
                if (mission.isRepaired(station.getType())) continue;
                if (mission.getPartCount(station.getType().getRequiredPart()) < 1) continue;
                out.set(station.getPosition().x + 63f, station.getPosition().y + 63f);
                return true;
            }
            return false;
        }
        if (!mission.hasWeapon()) {
            if (!mission.hasAllWeaponParts() && alvoMaisProximo(out)) return true;
            out.set(craftingStation.getPosition().x + 32f, craftingStation.getPosition().y + 32f);
            return true;
        }
        if (mission.getEnemiesDefeated() < mission.getTotalEnemies()) {
            Enemy nearest = null;
            float best = Float.MAX_VALUE;
            for (Enemy enemy : enemies) {
                if (!enemy.isAtivo()) continue;
                float distance = Vector2.dst(enemy.centerX(), enemy.centerY(),
                    astronauta.getPosition().x, astronauta.getPosition().y);
                if (distance < best) {
                    best = distance;
                    nearest = enemy;
                }
            }
            if (nearest == null) return false;
            out.set(nearest.centerX(), nearest.centerY());
            return true;
        }
        out.set(portal.getPosition().x + portal.getBounds().width / 2f,
            portal.getPosition().y + portal.getBounds().height / 2f);
        return true;
    }

    private boolean alvoMaisProximo(Vector2 out) {
        MissionCollectible nearest = null;
        float best = Float.MAX_VALUE;
        for (MissionCollectible collectible : missionCollectibles) {
            if (!collectible.isAtivo()) continue;
            float distance = Vector2.dst(collectible.getPosition().x, collectible.getPosition().y,
                astronauta.getPosition().x, astronauta.getPosition().y);
            if (distance < best) {
                best = distance;
                nearest = collectible;
            }
        }
        if (nearest == null) return false;
        out.set(nearest.getPosition().x + 27f, nearest.getPosition().y + 27f);
        return true;
    }

    /** Cursor autoral: muda de forma quando a mira encosta em um hostil. */
    private void renderCursor() {
        float x = Gdx.input.getX() * GameConfig.WINDOW_WIDTH / (float) Gdx.graphics.getWidth();
        float y = GameConfig.WINDOW_HEIGHT
            - Gdx.input.getY() * GameConfig.WINDOW_HEIGHT / (float) Gdx.graphics.getHeight();
        boolean onTarget = false;
        for (Enemy enemy : enemies) {
            if (!enemy.isAtivo()) continue;
            if (Vector2.dst(enemy.centerX(), enemy.centerY(), mouseWorld.x, mouseWorld.y) < 60f) {
                onTarget = true;
                break;
            }
        }
        batch.setProjectionMatrix(pauseCamera.combined);
        batch.begin();
        batch.setColor(1f, 1f, 1f, onTarget ? 1f : .8f);
        batch.draw(onTarget ? assets.uiCursorTargetTexture : assets.uiCursorDefaultTexture,
            x - CURSOR_SIZE / 2f, y - CURSOR_SIZE / 2f, CURSOR_SIZE, CURSOR_SIZE);
        batch.setColor(Color.WHITE);
        batch.end();
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
        /*
         * Texturas em vez de ShapeRenderer: o traco entra no mesmo batch das
         * entidades, sem alternar begin/end e sem o flush de GPU que isso custa.
         */
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(.45f, .95f, 1f, alpha * .35f);
        desenharTraco(px - dx * 34f, py - dy * 34f, px, py, 7f * alpha);
        batch.setColor(.9f, 1f, 1f, alpha);
        desenharTraco(px - dx * 42f, py - dy * 42f, px, py, 2f);
        float glow = (5f + 4f * alpha) * 2.6f;
        batch.draw(assets.energyFxFrame(1, 0), px - glow / 2f, py - glow / 2f, glow, glow);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Segmento texturizado: substitui rectLine sem sair do SpriteBatch. */
    private void desenharTraco(float x1, float y1, float x2, float y2, float thickness) {
        float lineX = x2 - x1;
        float lineY = y2 - y1;
        float length = (float) Math.sqrt(lineX * lineX + lineY * lineY);
        if (length <= .001f) return;
        float angle = MathUtils.atan2(lineY, lineX) * MathUtils.radiansToDegrees;
        batch.draw(assets.uiWhiteTexture, x1, y1 - thickness / 2f,
            0f, thickness / 2f, length, thickness, 1f, 1f, angle);
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

        if (blankCursor != null) {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            blankCursor.dispose();
            blankCursor = null;
        }
    }
}
