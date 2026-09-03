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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.TitanEnemy;
import com.orion.echoes.lua.entities.TitanPortal;
import com.orion.echoes.lua.entities.Wall;
import com.orion.echoes.lua.input.GameInputProcessor;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.save.GameSaveData;
import com.orion.echoes.lua.save.LunarCheckpoint;
import com.orion.echoes.lua.save.SaveManager;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.systems.TitanCombatSystem;
import com.orion.echoes.lua.ui.UiTheme;

/** Terceira fase real: superfície de metano de Titã, combate e retorno. */
public final class TitanScreen implements Screen {
    private static final float WORLD_W = 2600f;
    private static final float WORLD_H = 1700f;
    private static final Color AMBER = Color.valueOf("D78A36");
    private final EchoesLua game;
    private final CampaignState campaign;
    private final Array<TitanEnemy> enemies = new Array<>();
    private final Vector2 mouseWorld = new Vector2();
    private final Vector2 shotOrigin = new Vector2();
    private final GlyphLayout layout = new GlyphLayout();
    private AssetManager assets;
    private SpriteBatch batch;
    private PhysicsWorld physics;
    private Astronauta player;
    private TitanPortal returnPortal;
    private TitanCombatSystem combat;
    private GameInputProcessor input;
    private OrthographicCamera camera;
    private OrthographicCamera uiCamera;
    private Viewport viewport;
    private Viewport uiViewport;
    private NinePatch panel;
    private String message = "A atmosfera abafa o sinal. Explore com cautela.";
    private float messageTimer = 4f;
    private boolean paused;
    private boolean changingScreen;

    public TitanScreen(EchoesLua game, CampaignState campaign) {
        this.game = game;
        this.campaign = campaign == null ? game.getCampaign() : campaign;
    }

    @Override public void show() {
        assets = game.getAssets();
        batch = game.getBatch();
        panel = assets.uiPanelPatch();
        physics = new PhysicsWorld();
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
        player = new Astronauta(260f, 260f, assets, physics);
        player.setSurfaceProfile(Astronauta.SurfaceProfile.MARS);
        player.setWeaponEquipped(true);
        player.setMunicao(campaign.getAmmo());

        // Exigência da prova: a Screen carrega o personagem via fromSaveData no show().
        GameSaveData saved = new SaveManager().load();
        if (saved != null && CampaignState.phaseFromToken(saved.fase) == CampaignState.Phase.TITAN) {
            player.fromSaveData(saved);
            player.setMunicao(saved.municao);
        } else {
            player.setVitals(Math.max(50f, campaign.getOxygen()),
                Math.max(45f, campaign.getEnergy()));
        }
        combat = new TitanCombatSystem(campaign);
        combat.setMunicao(player.getMunicao());
        returnPortal = new TitanPortal(150f, 150f, assets);
        returnPortal.setUnlocked(true);
        enemies.add(new TitanEnemy(760f, 610f, assets));
        enemies.add(new TitanEnemy(1370f, 980f, assets));
        enemies.add(new TitanEnemy(2050f, 520f, assets));
        campaign.setPhase(CampaignState.Phase.TITAN);
        campaign.setEntrouTita(true);
        camera.position.set(player.getPosition().x, player.getPosition().y, 0f);
        camera.update();
    }

    @Override public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);
        update(delta);
        if (changingScreen) return;
        Gdx.gl.glClearColor(.09f, .045f, .018f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(.78f, .56f, .32f, 1f);
        batch.draw(assets.titanBackgroundTexture, 0f, 0f, WORLD_W, WORLD_H);
        batch.setColor(Color.WHITE);
        returnPortal.render(batch);
        for (TitanEnemy enemy : enemies) enemy.render(batch);
        player.render(batch);
        batch.end();
        renderHud();
    }

    private void update(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) paused = !paused;
        if (paused || changingScreen) return;
        combat.update(delta);
        messageTimer = Math.max(0f, messageTimer - delta);
        Vector2 direction = input.getDirection();
        player.move(direction.x, direction.y, input.isRunning(), delta);
        physics.update(delta);
        player.update(delta);
        mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouseWorld);
        player.setAimTarget(mouseWorld.x, mouseWorld.y);
        if (input.consumeAttackPressed()) shoot();
        for (TitanEnemy enemy : enemies) {
            enemy.update(delta, player, WORLD_W, WORLD_H);
            if (enemy.canDamage(player)) {
                player.receberDano(11f, enemy.centerX(), enemy.centerY());
                feedback("Predador de metano atingiu o traje.");
            }
        }
        returnPortal.update(delta);
        if (input.consumeInteractPressed() && returnPortal.isPlayerNear(player)) returnToMars();
        if (input.consumeSavePressed()) saveTitan();
        camera.position.x = MathUtils.clamp(player.getPosition().x,
            GameConfig.WINDOW_WIDTH / 2f, WORLD_W - GameConfig.WINDOW_WIDTH / 2f);
        camera.position.y = MathUtils.clamp(player.getPosition().y,
            GameConfig.WINDOW_HEIGHT / 2f, WORLD_H - GameConfig.WINDOW_HEIGHT / 2f);
        camera.update();
    }

    private void shoot() {
        shotOrigin.set(player.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
            player.getPosition().y + GameConfig.PLAYER_HEIGHT * .48f);
        float dirX = MathUtils.cosDeg(player.getAimAngle());
        float dirY = MathUtils.sinDeg(player.getAimAngle());
        TitanEnemy target = null;
        float closest = combat.getAlcance();
        for (TitanEnemy enemy : enemies) {
            if (!enemy.isAlive()) continue;
            float dx = enemy.centerX() - shotOrigin.x;
            float dy = enemy.centerY() - shotOrigin.y;
            float along = dx * dirX + dy * dirY;
            float perpendicular = Math.abs(dx * dirY - dy * dirX);
            if (along > 0f && along < closest && perpendicular < 52f) {
                target = enemy;
                closest = along;
            }
        }
        combat.setMunicao(player.getMunicao());
        boolean fired = combat.tentarTiro(shotOrigin, target, campaign.hasWeapon());
        player.setMunicao(combat.getMunicao());
        if (fired) {
            player.triggerShot();
            feedback(target == null ? "Disparo perdido na névoa de metano."
                : !target.isAlive() ? "Predador neutralizado." : "Impacto confirmado.");
        } else if (player.getMunicao() <= 0) feedback("Sem munição.");
    }

    private void saveTitan() {
        campaign.setVitals(player.getOxigenio(), player.getEnergia());
        campaign.setAmmo(player.getMunicao());
        campaign.setPhase(CampaignState.Phase.TITAN);
        GameSaveData data = player.toSaveData();
        LunarCheckpoint.applyCampaign(data, campaign);
        new SaveManager().save(data);
        feedback("Exploração de Titã salva.");
    }

    private void returnToMars() {
        campaign.setVitals(player.getOxigenio(), player.getEnergia());
        campaign.setAmmo(player.getMunicao());
        campaign.setPhase(CampaignState.Phase.MARS);
        GameSaveData data = player.toSaveData();
        // Ao continuar depois da viagem de volta, a campanha deve reabrir em Marte.
        data.posX = 2440f;
        data.posY = 1430f;
        LunarCheckpoint.applyCampaign(data, campaign);
        new SaveManager().save(data);
        changingScreen = true;
        game.setScreen(new MarsScreen(game, campaign));
        dispose();
    }

    private void feedback(String value) { message = value; messageTimer = 3f; }

    private void renderHud() {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        panel.setColor(new Color(1f, .76f, .42f, .96f));
        panel.draw(batch, 32f, 28f, 360f, 94f);
        panel.draw(batch, 410f, 648f, 838f, 50f);
        if (messageTimer > 0f) panel.draw(batch, 430f, 130f, 420f, 48f);
        panel.setColor(Color.WHITE);
        text("TITÃ  •  LAGOS DE METANO", .78f, AMBER, 444f, 681f);
        text(String.format("O2  %.0f%%     ENERGIA  %.0f%%     MUNIÇÃO  %d",
            player.getOxigenio(), player.getEnergia(), player.getMunicao()), .74f,
            UiTheme.TEXT, 58f, 88f);
        text("E no portal retorna a Marte", .65f, UiTheme.TEXT_MUTED, 58f, 55f);
        if (messageTimer > 0f) {
            layout.setText(assets.font, message);
            text(message, .68f, UiTheme.TEXT, 455f, 160f);
        }
        batch.end();
    }

    private void text(String value, float scale, Color color, float x, float y) {
        assets.font.getData().setScale(scale);
        assets.font.setColor(color);
        assets.font.draw(batch, value, x, y);
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height, false);
        uiViewport.update(width, height, true);
    }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() {
        if (physics != null) { physics.dispose(); physics = null; }
        // SpriteBatch e AssetManager pertencem ao EchoesLua e não são descartados aqui.
    }
}
