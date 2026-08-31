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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.MarsEnemy;
import com.orion.echoes.lua.entities.MarsObject;
import com.orion.echoes.lua.entities.Wall;
import com.orion.echoes.lua.events.EventBus;
import com.orion.echoes.lua.events.EventType;
import com.orion.echoes.lua.input.GameInputProcessor;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.ParticleManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.ui.UiTheme;

/** Segunda missão jogável: restaura a base marciana e alcança a plataforma. */
public final class MarsScreen implements Screen {
    private static final float WORLD_W = 3000f;
    private static final float WORLD_H = 1900f;
    private static final Color MARS = Color.valueOf("E77A4E");
    private static final Color MARS_DARK = Color.valueOf("572A24");

    private final EchoesLua game;
    private final float initialOxygen;
    private final float initialEnergy;
    private final Array<MarsObject> props = new Array<>();
    private final Array<MarsObject> rocks = new Array<>();
    private final Array<MarsObject> items = new Array<>();
    private final Array<MarsObject> stations = new Array<>();
    private final Array<MarsEnemy> enemies = new Array<>();
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 mouseWorld = new Vector2();
    private final Vector2 shotStart = new Vector2();
    private final Vector2 shotEnd = new Vector2();

    private AssetManager assets;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private NinePatch panelPatch;
    private PhysicsWorld physics;
    private ParticleManager particles;
    private SoundManager sounds;
    private Astronauta player;
    private GameInputProcessor input;
    private OrthographicCamera camera;
    private OrthographicCamera uiCamera;
    private Viewport viewport;
    private Viewport uiViewport;
    private MarsObject habitat;
    private MarsObject landingPad;
    private float missionTime, reveal, dustTimer, stepTimer, shotTimer, damageFlash;
    private float messageTimer, extractionGlow;
    private float hitStopTimer, cameraShakeTimer, cameraShakePower;
    private int minerals, activeStations, hostilesDefeated;
    private boolean changingScreen;
    private boolean paused;
    private String message = "A tempestade apagou a colônia. Recupere os núcleos marcianos.";

    public MarsScreen(EchoesLua game, float oxygen, float energy, int defeatedEnemies) {
        this.game = game;
        initialOxygen = oxygen;
        initialEnergy = energy;
    }

    @Override public void show() {
        assets = game.getAssets();
        batch = game.getBatch();
        shapes = new ShapeRenderer();
        panelPatch = assets.uiPanelPatch();
        physics = new PhysicsWorld();
        particles = new ParticleManager(assets);
        sounds = SoundManager.getInstance();
        input = new GameInputProcessor();
        Gdx.input.setInputProcessor(input);
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, uiCamera);
        uiCamera.position.set(640f, 360f, 0f);
        uiCamera.update();
        new Wall(-24f, 0f, 24f, WORLD_H, physics);
        new Wall(WORLD_W, 0f, 24f, WORLD_H, physics);
        new Wall(0f, -24f, WORLD_W, 24f, physics);
        new Wall(0f, WORLD_H, WORLD_W, 24f, physics);
        player = new Astronauta(230f, 250f, assets, physics);
        player.setVitals(Math.max(45f, initialOxygen), Math.max(38f, initialEnergy));
        player.setWeaponEquipped(true);
        buildColony();
        camera.position.set(640f, 360f, 0f);
        camera.update();
        EventBus.getInstance().publish(EventType.MARS_ENTERED);
    }

    private void buildColony() {
        habitat = prop(160f, 1020f, 330f, 275f, MarsObject.Kind.HABITAT);
        stations.add(prop(700f, 1420f, 225f, 190f, MarsObject.Kind.SOLAR_STATION));
        stations.add(prop(1550f, 1330f, 225f, 190f, MarsObject.Kind.OXYGEN_STATION));
        stations.add(prop(2440f, 1130f, 225f, 190f, MarsObject.Kind.COMMS_STATION));
        landingPad = prop(2570f, 1530f, 250f, 190f, MarsObject.Kind.LANDING_PAD);
        prop(2400f, 1230f, 120f, 140f, MarsObject.Kind.BEACON);
        float[][] data = {{530,760,130},{850,420,115},{1080,930,145},{1320,580,125},
            {1710,810,150},{1980,430,120},{2350,650,145},{2750,320,110},{610,1660,120},
            {1150,1610,105},{2600,900,130},{2250,1660,115}};
        for (float[] r : data) {
            MarsObject rock = new MarsObject(r[0], r[1], r[2] * 1.04f, r[2] * .86f,
                MarsObject.Kind.ROCK, assets, physics);
            props.add(rock);
            rocks.add(rock);
        }
        for (int i = 0; i < 4; i++) addRandomItem(MarsObject.Kind.MINERAL);
        addRandomItem(MarsObject.Kind.MEDKIT);
        addRandomItem(MarsObject.Kind.POWER_CELL);
        enemies.add(new MarsEnemy(920f, 1120f, true, assets, WORLD_W, WORLD_H));
        enemies.add(new MarsEnemy(1600f, 520f, false, assets, WORLD_W, WORLD_H));
        enemies.add(new MarsEnemy(2120f, 1250f, true, assets, WORLD_W, WORLD_H));
        enemies.add(new MarsEnemy(2420f, 420f, false, assets, WORLD_W, WORLD_H));
    }

    private MarsObject prop(float x, float y, float w, float h, MarsObject.Kind kind) {
        MarsObject object = new MarsObject(x, y, w, h, kind, assets, physics);
        props.add(object);
        return object;
    }

    private void addItem(float x, float y, MarsObject.Kind kind) {
        MarsObject item = new MarsObject(x, y, 72f, 72f, kind, assets, physics);
        props.add(item);
        items.add(item);
    }

    private void addRandomItem(MarsObject.Kind kind) {
        Rectangle candidate = new Rectangle();
        for (int attempt = 0; attempt < 120; attempt++) {
            float x = MathUtils.random(120f, WORLD_W - 192f);
            float y = MathUtils.random(120f, WORLD_H - 192f);
            candidate.set(x + 10f, y + 10f, 52f, 52f);
            if (Vector2.dst(x, y, 230f, 250f) < 250f) continue;
            boolean blocked = candidate.overlaps(habitat.getBounds())
                || candidate.overlaps(landingPad.getBounds());
            for (MarsObject object : rocks) if (candidate.overlaps(object.getBounds())) { blocked = true; break; }
            if (!blocked) for (MarsObject station : stations) {
                if (candidate.overlaps(station.getBounds())) { blocked = true; break; }
            }
            if (!blocked) { addItem(x, y, kind); return; }
        }
        addItem(MathUtils.random(180f, WORLD_W - 220f), MathUtils.random(180f, WORLD_H - 220f), kind);
    }

    @Override public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);
        update(delta);
        if (changingScreen) return;
        Gdx.gl.glClearColor(.15f, .045f, .025f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(assets.marsBackgroundTexture, 0f, 0f, WORLD_W, WORLD_H);
        renderLandmarks();
        for (MarsObject object : props) object.render(batch);
        for (MarsEnemy enemy : enemies) enemy.render(batch);
        player.render(batch);
        particles.render(batch);
        batch.end();
        renderShot();
        renderEnemyHealth();
        renderHud();
        renderDamage();
        if (paused) renderPause();
        renderTransition();
    }

    private void update(float delta) {
        if (changingScreen) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || paused && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            paused = !paused;
            player.getBody().setLinearVelocity(0f, 0f);
            return;
        }
        if (paused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                changingScreen = true;
                game.setScreen(new MenuScreen(game));
                dispose();
            }
            return;
        }
        if (hitStopTimer > 0f) {
            hitStopTimer = Math.max(0f, hitStopTimer - delta);
            particles.update(delta * .35f);
            return;
        }
        missionTime += delta;
        reveal = Math.min(1f, reveal + delta / .65f);
        messageTimer = Math.max(0f, messageTimer - delta);
        shotTimer = Math.max(0f, shotTimer - delta);
        damageFlash = Math.max(0f, damageFlash - delta);
        extractionGlow += delta;
        Vector2 direction = input.getDirection();
        if (input.consumeDashPressed() && player.tryDash(direction.x, direction.y)) {
            particles.criarPoeiraMarte(player.getPosition().x + 27f, player.getPosition().y,
                true, direction.x, direction.y);
            shakeCamera(.16f, 5f);
        }
        player.move(direction.x, direction.y, input.isRunning(), delta);
        physics.update(delta);
        player.update(delta);
        player.setProtegido(player.getBounds().overlaps(habitat.getBounds()));
        if (player.isProtegido()) {
            player.recuperarOxigenio(9f * delta);
            player.recuperarEnergia(5f * delta);
        }
        mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouseWorld);
        player.setAimTarget(mouseWorld.x, mouseWorld.y);
        if (input.consumeAttackPressed()) shoot();
        if (input.consumeInteractPressed()) interact();
        for (MarsObject object : props) object.update(delta);
        collectItems();
        for (MarsEnemy enemy : enemies) {
            enemy.update(delta, player, rocks);
            if (enemy.canDamage(player)) {
                player.receberDano(10f, enemy.centerX(), enemy.centerY());
                damageFlash = .28f;
                particles.criarImpactoTraje(player.getPosition().x + 27f, player.getPosition().y + 38f);
                feedback("Impacto no traje: perda de oxigênio.");
                shakeCamera(.24f, 8f);
            }
        }
        particles.update(delta);
        updateFootFx(delta, direction);
        updateCamera();
        if (missionComplete() && player.getBounds().overlaps(landingPad.getBounds())) {
            changingScreen = true;
            game.setScreen(new VictoryScreen(game, missionTime));
            dispose();
        } else if (player.isMorto()) {
            changingScreen = true;
            game.setScreen(new GameOverScreen(game, missionTime));
            dispose();
        }
    }

    private void collectItems() {
        for (MarsObject item : items) {
            if (!item.isAtivo() || !player.getBounds().overlaps(item.getBounds())) continue;
            item.collect();
            particles.criarEfeitoColeta(item.getPosition().x + 36f, item.getPosition().y + 36f);
            sounds.tocarColeta();
            switch (item.getKind()) {
                case MINERAL -> { minerals++; feedback("Núcleo marciano recuperado  •  " + minerals); }
                case MEDKIT -> { player.recuperarOxigenio(28f); feedback("Selagem de emergência aplicada  •  O2 restaurado"); }
                case POWER_CELL -> { player.recuperarEnergia(35f); feedback("Célula de energia integrada"); }
                default -> { }
            }
        }
    }

    private void interact() {
        for (MarsObject station : stations) {
            if (station.isEnabled() || distanceTo(station) > 135f) continue;
            if (minerals <= 0) {
                feedback("Esta estação precisa de um núcleo marciano.");
                return;
            }
            minerals--;
            activeStations++;
            station.activate();
            particles.criarProcessamento(station.getBounds().x + station.getBounds().width / 2f,
                station.getBounds().y + station.getBounds().height * .55f);
            feedback(activeStations == 3 ? "Colônia sincronizada. Neutralize os hostis e alcance a plataforma."
                : "Estação reativada  •  " + activeStations + "/3");
            return;
        }
        feedback(missionComplete() ? "Sinal de extração disponível na plataforma."
            : "Nenhum sistema ao alcance.");
    }

    private float distanceTo(MarsObject object) {
        return Vector2.dst(player.getPosition().x + 27f, player.getPosition().y + 38f,
            object.getBounds().x + object.getBounds().width / 2f,
            object.getBounds().y + object.getBounds().height / 2f);
    }

    private void shoot() {
        float x = player.getPosition().x + 27f;
        float y = player.getPosition().y + GameConfig.PLAYER_HEIGHT * .48f;
        float dx = MathUtils.cosDeg(player.getAimAngle());
        float dy = MathUtils.sinDeg(player.getAimAngle());
        MarsEnemy target = null;
        float closest = 480f;
        for (MarsEnemy enemy : enemies) {
            if (!enemy.isAtivo()) continue;
            float ex = enemy.centerX() - x;
            float ey = enemy.centerY() - y;
            float along = ex * dx + ey * dy;
            float perpendicular = Math.abs(ex * dy - ey * dx);
            if (along > 0f && along < closest && perpendicular < 48f) {
                target = enemy;
                closest = along;
            }
        }
        shotStart.set(x + dx * 33f, y + dy * 33f);
        shotEnd.set(x + dx * 480f, y + dy * 480f);
        if (target != null) {
            shotEnd.set(target.centerX(), target.centerY());
            if (target.takeHit()) {
                hostilesDefeated++;
                particles.criarMorteInimigo(target.centerX(), target.centerY());
            } else particles.criarImpactoTiro(target.centerX(), target.centerY());
            hitStopTimer = .05f;
            shakeCamera(.12f, 4.5f);
        }
        particles.criarMuzzleFlash(shotStart.x, shotStart.y, player.getAimAngle());
        player.triggerShot();
        sounds.tocarDisparo();
        shotTimer = .14f;
    }

    private void updateFootFx(float delta, Vector2 direction) {
        if (!player.isMoving()) { dustTimer = 0f; stepTimer = 0f; return; }
        dustTimer += delta;
        stepTimer += delta;
        float interval = player.isSprinting() ? .11f : .24f;
        if (dustTimer >= interval) {
            dustTimer = 0f;
            particles.criarPoeiraMarte(player.getPosition().x + 27f, player.getPosition().y,
                player.isSprinting(), direction.x, direction.y);
        }
        if (stepTimer >= (player.isSprinting() ? .32f : .48f)) {
            stepTimer = 0f;
            sounds.tocarPassoLunar();
        }
    }

    private void updateCamera() {
        float halfW = viewport.getWorldWidth() / 2f;
        float halfH = viewport.getWorldHeight() / 2f;
        Vector2 velocity = player.getBody().getLinearVelocity();
        float x = MathUtils.clamp(player.getPosition().x + 27f + velocity.x * PhysicsWorld.PPM * .18f,
            halfW, WORLD_W - halfW);
        float y = MathUtils.clamp(player.getPosition().y + 38f + velocity.y * PhysicsWorld.PPM * .14f,
            halfH, WORLD_H - halfH);
        camera.position.x = MathUtils.lerp(camera.position.x, x, .11f);
        camera.position.y = MathUtils.lerp(camera.position.y, y, .11f);
        cameraShakeTimer = Math.max(0f, cameraShakeTimer - Gdx.graphics.getDeltaTime());
        if (cameraShakeTimer > 0f) {
            float falloff = cameraShakeTimer / .24f;
            camera.position.x += MathUtils.random(-cameraShakePower, cameraShakePower) * falloff;
            camera.position.y += MathUtils.random(-cameraShakePower, cameraShakePower) * falloff;
        }
        camera.update();
    }

    private void shakeCamera(float duration, float power) {
        cameraShakeTimer = Math.max(cameraShakeTimer, duration);
        cameraShakePower = Math.max(cameraShakePower * .65f, power);
    }

    private boolean missionComplete() {
        return activeStations == 3 && hostilesDefeated == enemies.size;
    }
    private void feedback(String value) { message = value; messageTimer = 3.4f; }

    private void renderShot() {
        if (shotTimer <= 0f) return;
        float alpha = shotTimer / .14f;
        float progress = 1f - alpha;
        float px = MathUtils.lerp(shotStart.x, shotEnd.x, progress);
        float py = MathUtils.lerp(shotStart.y, shotEnd.y, progress);
        float dx = shotEnd.x - shotStart.x, dy = shotEnd.y - shotStart.y;
        float length = Math.max(.001f, (float)Math.sqrt(dx * dx + dy * dy));
        dx /= length;
        dy /= length;
        shapes.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, .45f, .25f, alpha * .35f);
        shapes.rectLine(px - dx * 36f, py - dy * 36f, px, py, 8f * alpha);
        shapes.setColor(1f, .86f, .62f, alpha);
        shapes.rectLine(px - dx * 44f, py - dy * 44f, px, py, 2f);
        shapes.circle(px, py, 6f, 12);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderEnemyHealth() {
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (MarsEnemy enemy : enemies) {
            if (!enemy.isAtivo() || enemy.getHealthRatio() >= 1f) continue;
            shapes.setColor(MARS_DARK);
            shapes.rect(enemy.centerX() - 28f, enemy.centerY() + 48f, 56f, 6f);
            shapes.setColor(MARS);
            shapes.rect(enemy.centerX() - 28f, enemy.centerY() + 48f,
                56f * enemy.getHealthRatio(), 6f);
        }
        shapes.end();
    }

    private void renderHud() {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        drawUiPanel(32f, 28f, 294f, 82f, MARS, .91f);
        drawUiPanel(914f, 28f, 334f, 82f, MARS, .91f);
        drawUiPanel(330f, 646f, 620f, 52f, MARS, .94f);
        if (messageTimer > 0f) drawUiPanel(398f, 128f, 484f, 48f, UiTheme.AMBER,
            Math.min(.94f, messageTimer * 2f));
        batch.end();
        shapes.setProjectionMatrix(uiCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        panel(32f, 28f, 294f, 82f, MARS, .91f);
        bar(88f, 75f, 178f, 9f, player.getOxigenio() / 100f,
            player.getOxigenio() < 25f ? UiTheme.RED : UiTheme.CYAN);
        bar(88f, 49f, 178f, 8f, player.getEnergia() / 100f, UiTheme.AMBER);
        panel(914f, 28f, 334f, 82f, MARS, .91f);
        panel(330f, 646f, 620f, 52f, MARS, .94f);
        if (messageTimer > 0f) panel(398f, 128f, 484f, 48f, UiTheme.AMBER,
            Math.min(.94f, messageTimer * 2f));
        if (missionComplete()) {
            float pulse = .35f + MathUtils.sin(extractionGlow * 4f) * .15f;
            shapes.setColor(MARS.r, MARS.g, MARS.b, pulse);
            shapes.rect(330f, 642f, 620f, 3f);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        text("O2", .72f, UiTheme.TEXT_MUTED, 50f, 88f, 1f);
        text(String.format("%.0f%%", player.getOxigenio()), .7f, UiTheme.TEXT, 275f, 87f, 1f);
        text("EN", .72f, UiTheme.TEXT_MUTED, 50f, 61f, 1f);
        text(String.format("%.0f%%", player.getEnergia()), .7f, UiTheme.TEXT, 275f, 60f, 1f);
        text("NÚCLEOS  " + minerals, .74f, UiTheme.AMBER, 944f, 84f, 1f);
        text("ESTAÇÕES  " + activeStations + "/3", .74f, UiTheme.CYAN, 1050f, 84f, 1f);
        text("HOSTIS  " + hostilesDefeated + "/" + enemies.size, .72f, UiTheme.TEXT_MUTED, 944f, 55f, 1f);
        text(missionComplete() ? "SINAL VERDE  •  ALCANCE A PLATAFORMA DE EXTRAÇÃO"
            : "MARTE 01  •  REATIVE 3 ESTAÇÕES E NEUTRALIZE OS HOSTIS",
            .77f, missionComplete() ? UiTheme.GREEN : UiTheme.TEXT, 384f, 680f, 1f);
        if (messageTimer > 0f) centered(message, .72f, UiTheme.TEXT, 152f,
            Math.min(1f, messageTimer * 2f));
        batch.end();
    }

    private void renderDamage() {
        if (damageFlash <= 0f) return;
        float alpha = damageFlash / .28f;
        shapes.setProjectionMatrix(uiCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.78f, .03f, .02f, alpha * .5f);
        float edge = 34f * alpha;
        shapes.rect(0f, 0f, 1280f, edge);
        shapes.rect(0f, 720f - edge, 1280f, edge);
        shapes.rect(0f, 0f, edge, 720f);
        shapes.rect(1280f - edge, 0f, edge, 720f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderLandmarks() {
        batch.setColor(1f, .9f, .82f, .88f);
        batch.draw(assets.landmarkRegion(0, 1), 1120f, 1480f, 260f, 180f);
        batch.draw(assets.landmarkRegion(1, 1), 2640f, 680f, 220f, 165f);
        batch.draw(assets.landmarkRegion(2, 1), 380f, 720f, 235f, 145f);
        batch.draw(assets.landmarkRegion(3, 1), 1900f, 1510f, 180f, 205f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderPause() {
        shapes.setProjectionMatrix(uiCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.055f, .016f, .012f, .88f);
        shapes.rect(0f, 0f, 1280f, 720f);
        shapes.setColor(MARS.r, MARS.g, MARS.b, .92f);
        shapes.rect(72f, 572f, 214f, 8f);
        shapes.rect(72f, 188f, 760f, 2f);
        shapes.setColor(.12f, .045f, .03f, .9f);
        shapes.rect(72f, 220f, 760f, 92f);
        shapes.setColor(.94f, .52f, .29f, .15f);
        shapes.rect(872f, 0f, 408f, 720f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        text("REGISTRO MARCIANO · EM ESPERA", .75f, MARS, 74f, 616f, 1f);
        text("PAUSA", 2.25f, UiTheme.TEXT, 68f, 540f, 1f);
        text("A poeira parou. A telemetria também.", .88f, UiTheme.TEXT_MUTED, 74f, 475f, 1f);
        text("RETOMAR", 1.02f, UiTheme.TEXT, 104f, 278f, 1f);
        text("ESC ou ENTER", .68f, MARS, 610f, 278f, 1f);
        text("VOLTAR AO MENU", .82f, UiTheme.TEXT_MUTED, 74f, 150f, 1f);
        text("M", .74f, MARS, 610f, 150f, 1f);
        text(String.format("O2 %.0f%%\nESTAÇÕES %d/3\nHOSTIS %d/%d",
            player.getOxigenio(), activeStations, hostilesDefeated, enemies.size),
            .82f, UiTheme.TEXT, 934f, 520f, 1f);
        batch.end();
    }

    private void renderTransition() {
        if (reveal >= 1f) return;
        float alpha = 1f - Interpolation.pow3Out.apply(reveal);
        shapes.setProjectionMatrix(uiCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.07f, .012f, .008f, alpha);
        shapes.rect(0f, 0f, 1280f, 720f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void panel(float x, float y, float w, float h, Color accent, float alpha) {
        shapes.setColor(UiTheme.SURFACE.r, UiTheme.SURFACE.g, UiTheme.SURFACE.b, alpha);
        shapes.rect(x + 4f, y + 4f, w - 8f, h - 8f);
        shapes.setColor(accent.r, accent.g, accent.b, alpha);
        shapes.rect(x + 10f, y + h - 3f, 48f, 3f);
        shapes.rect(x + w - 18f, y, 8f, 3f);
    }

    private void drawUiPanel(float x, float y, float w, float h, Color accent, float alpha) {
        panelPatch.setColor(new Color(accent.r * .38f + .62f, accent.g * .38f + .62f,
            accent.b * .38f + .62f, alpha));
        panelPatch.draw(batch, x, y, w, h);
        panelPatch.setColor(Color.WHITE);
    }

    private void bar(float x, float y, float w, float h, float ratio, Color color) {
        shapes.setColor(UiTheme.TRACK);
        shapes.rect(x, y, w, h);
        shapes.setColor(color);
        shapes.rect(x, y, w * MathUtils.clamp(ratio, 0f, 1f), h);
    }

    private void text(String value, float scale, Color color, float x, float y, float alpha) {
        assets.font.getData().setScale(scale);
        assets.font.setColor(color.r, color.g, color.b, alpha);
        assets.font.draw(batch, value, x, y);
    }

    private void centered(String value, float scale, Color color, float y, float alpha) {
        assets.font.getData().setScale(scale);
        assets.font.setColor(color.r, color.g, color.b, alpha);
        layout.setText(assets.font, value);
        assets.font.draw(batch, layout, (1280f - layout.width) / 2f, y);
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height, false);
        uiViewport.update(width, height, true);
    }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() {
        if (shapes != null) { shapes.dispose(); shapes = null; }
        if (particles != null) { particles.dispose(); particles = null; }
        if (physics != null) { physics.dispose(); physics = null; }
    }
}
