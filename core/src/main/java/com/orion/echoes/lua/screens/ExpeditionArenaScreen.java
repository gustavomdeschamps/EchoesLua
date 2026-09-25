package com.orion.echoes.lua.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.entities.*;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.render.AtlasRegionRenderer;
import com.orion.echoes.lua.save.*;
import com.orion.echoes.lua.systems.*;
import com.orion.echoes.lua.ui.*;

/** Small dedicated boss arenas, sharing rules rather than cloning a gameplay screen. */
abstract class ExpeditionArenaScreen implements Screen {

    /**
     * Cenografia por corpo celeste.
     *
     * A arena era a mesma em todos os mundos: rochas lunares e o domo da base
     * lunar apareciam em Marte e em Calisto, so trocando a imagem de fundo. Um
     * chefe marciano lutando dentro da colonia da Lua nao e uma arena da
     * campanha, e uma sala reaproveitada. Cada fase agora traz as proprias
     * pedras, a propria estacao de reabastecimento e o proprio nome.
     */
    private enum Dressing {
        LUNAR("GUARDIÃO DA CRATERA", Inventario.CHAVE_LUA, "Posto lunar", 0),
        MARS("TITÃ-FERRUGEM", Inventario.CHAVE_MARTE, "Estação marciana", 1),
        CALLISTO("SENTINELA DE CALISTO", Inventario.CHAVE_LUZ, "Depósito de gelo", 3);
        final String bossName, key, supplyName; final int keyFrame;
        Dressing(String bossName, String key, String supplyName, int keyFrame) {
            this.bossName = bossName; this.key = key; this.supplyName = supplyName;
            this.keyFrame = keyFrame;
        }
    }

    /** Pedras da arena: x, y, largura visual. O corpo solido sai daqui. */
    private static final float[][] ROCKS = {{500f,160f,118f},{648f,528f,104f},{915f,120f,112f},{330f,452f,96f}};
    private static final Rectangle SUPPLY = new Rectangle(76f,78f,180f,150f);
    /** Area de uso da estacao: generosa, mas ancorada na propria estrutura. */
    private static final Rectangle SUPPLY_REACH = new Rectangle(40f,50f,260f,215f);
    // Measured from each frame's opaque foot baseline, not its square cell.
    // Coordenada do último pixel opaco em cada pose da folha 543×724.
    private static final int[] LUNAR_FOOT_BOTTOM = {612, 724, 700, 611};
    private static final int[] MARS_FOOT_BOTTOM = {724, 724, 706, 706};

    protected final EchoesLua game;
    protected final CampaignState campaign;
    private final CampaignState.Phase phase;
    private final Dressing dressing;
    private final ExpeditionBoss boss;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(1280f, 720f, camera);
    private final Vector2 cursor = new Vector2(), shot = new Vector2(), aim = new Vector2(), shotEnd = new Vector2();
    private final Rectangle movement = new Rectangle(), portal = new Rectangle(1116f, 196f, 104f, 168f);
    private final Rectangle keyPickup = new Rectangle();
    private final Array<Pickup> icePickups = new Array<>();
    private PhysicsWorld physics;
    private Astronauta player;
    private TerminalUi ui;
    private ExpeditionOverlay overlay;
    private ExpeditionPause pauseUi;
    private WorkshopInterior workshop;
    private com.orion.echoes.lua.managers.ParticleManager particles;
    private float time, cooldown, windup, recovery, shotTimer, mutationFlash, dashClock = 3f;
    private float bossTravel, bossMovingTimer, bossHitFlash;
    private int displayedPose;
    private float startMissionTime, messageTimer;
    private boolean paused, rewarded, keyDropped, leaving;
    private String message = "";
    private final Color tint = new Color();

    ExpeditionArenaScreen(EchoesLua game, CampaignState campaign, CampaignState.Phase phase, ExpeditionBoss boss) {
        this.game = game; this.campaign = campaign; this.phase = phase; this.boss = boss;
        this.dressing = phase == CampaignState.Phase.MARS ? Dressing.MARS
            : phase == CampaignState.Phase.CALLISTO ? Dressing.CALLISTO : Dressing.LUNAR;
    }

    @Override public void show() {
        boolean resumeSavedPosition = campaign.getPhase() == phase;
        GameSaveData saved = resumeSavedPosition ? new SaveManager().load() : null;
        campaign.setPhase(phase); game.useTargetCursor(); Gdx.input.setInputProcessor(null);
        startMissionTime = campaign.getMissionTime();
        physics = new PhysicsWorld();
        new Wall(-24f,0f,24f,720f,physics); new Wall(1280f,0f,24f,720f,physics);
        new Wall(0f,-24f,1280f,24f,physics); new Wall(0f,720f,1280f,24f,physics);
        // O corpo solido cobre a base visivel da pedra, nao o quadro inteiro:
        // e o mesmo contrato dos obstaculos das fases originais.
        for (float[] rock : ROCKS) new Wall(rock[0]+rock[2]*.18f,rock[1]+rock[2]*.04f,rock[2]*.64f,rock[2]*.28f,physics);
        new Wall(SUPPLY.x+18f,SUPPLY.y+12f,SUPPLY.width-36f,SUPPLY.height*.30f,physics);
        player = new Astronauta(190f,300f,game.getAssets(),physics);
        if (saved != null && isThisArena(saved)
                && saved.posX >= 24f && saved.posX <= 1200f
                && saved.posY >= 24f && saved.posY <= 650f) {
            player.fromSaveData(saved);
        }
        player.setSurfaceProfile(phase == CampaignState.Phase.MARS
            ? Astronauta.SurfaceProfile.MARS : Astronauta.SurfaceProfile.LUNAR);
        player.setVitals(campaign.getOxygen(),campaign.getEnergy());
        player.setWeaponEquipped(campaign.hasWeapon()); player.setMunicao(campaign.getAmmo());
        camera.position.set(640f,360f,0f); camera.update();
        ui = new TerminalUi(game.getBatch(),game.getAssets());
        overlay = new ExpeditionOverlay(game,player);
        pauseUi = new ExpeditionPause(game,player);
        workshop = new WorkshopInterior(game.getBatch(), game.getAssets());
        particles = new com.orion.echoes.lua.managers.ParticleManager(game.getAssets());
        // Gelo acessível nas três arenas: a oficina só fabrica células que o
        // jogador realmente recolheu, inclusive durante uma luta longa.
        icePickups.add(new Pickup(360f, 300f, Pickup.Kind.GELO, game.getAssets()));
        icePickups.add(new Pickup(690f, 190f, Pickup.Kind.GELO, game.getAssets()));
        icePickups.add(new Pickup(1030f, 510f, Pickup.Kind.GELO, game.getAssets()));
        rewarded = campaign.getInventario().tem(dressing.key);
        boolean defeated = campaign.isBossDefeated(phase) || rewarded;
        if (defeated && boss.isAlive()) boss.receiveDamage(10000f);
        if (defeated && !rewarded) prepareKeyDrop();
        // Municao minima de entrada: o chefe nao pode virar beco sem saida por
        // o jogador ter chegado com o pente vazio e sem nada para atirar.
        if (campaign.hasWeapon() && player.getMunicao() < 12) player.setMunicao(12);
        feedback(dressing.supplyName + " a oeste: processe gelo para fabricar células. R recarrega o rifle em campo.");
    }

    private String name() { return dressing.bossName; }
    private String bossObjective() {
        return switch (dressing) {
            case LUNAR -> "Derrote o Guardião da Cratera para conquistar a chave.";
            case MARS -> "Derrote o Titã-Ferrugem para conquistar a chave.";
            case CALLISTO -> "Derrote o Sentinela de Calisto para conquistar a chave.";
        };
    }
    private void feedback(String text) { message = text; messageTimer = 6f; }

    // =====================================================
    // RENDER
    // =====================================================

    @Override public void render(float delta) {
        delta = MathUtils.clamp(delta,0f,1f/30f);
        if (leaving) return;
        boolean workshopBlocked = workshop.handleInput(delta);
        if (workshopBlocked) player.getBody().setLinearVelocity(0f, 0f);
        boolean blocked = workshopBlocked || !pauseUi.isPaused() && overlay.handleInput();
        if (!blocked) pauseUi.handle();
        paused = pauseUi.isPaused();
        if(pauseUi.consumeMenu()){save();leaving=true;game.setScreen(new MenuScreen(game));dispose();return;}
        if (!blocked && !paused) update(delta);
        if (leaving) return;

        ui.clear(UiTheme.VOID);
        ui.image(background(),0f,0f,1280f,720f,Color.WHITE);
        var batch = game.getBatch(); batch.setProjectionMatrix(camera.combined); batch.begin();
        drawSupply(batch);
        // Em Calisto as formacoes emprestadas de Ti(t)a sao esfriadas para nao
        // trazerem o ocre do metano para dentro da cratera de gelo.
        if (dressing == Dressing.CALLISTO) batch.setColor(.68f,.79f,.95f,1f);
        for (float[] rock : ROCKS) AtlasRegionRenderer.draw(batch,rockRegion(rock),rock[0],rock[1],rock[2],rock[2]);
        batch.setColor(Color.WHITE);
        for (Pickup ice : icePickups) ice.render(batch);
        renderBoss();
        renderKeyDrop(batch);
        float portalPulse = .77f + .17f * MathUtils.sin(time * 5f);
        batch.setColor(1f,1f,1f,rewarded ? portalPulse : .07f);
        batch.setColor(rewarded ? 1f : .34f, rewarded ? 1f : .48f,
            rewarded ? 1f : .58f, rewarded ? 1f : .72f);
        batch.draw(game.getAssets().portalEnergyReworkRegion,
            portal.x+6f,portal.y+38f,46f,46f,92f,92f,1f,1f,time*15f);
        batch.setColor(Color.WHITE);
        float portalWidth = portal.width + 52f;
        float portalHeight = portalWidth * game.getAssets().portalFrameReworkTexture.getHeight()
            / game.getAssets().portalFrameReworkTexture.getWidth();
        batch.draw(game.getAssets().portalFrameReworkTexture,
            portal.x-26f,portal.y+(portal.height-portalHeight)*.5f,portalWidth,portalHeight);
        batch.setColor(Color.WHITE); player.render(batch); renderShot(batch);
        particles.render(batch); batch.end();

        drawHud();
        overlay.render();
        pauseUi.render();
        workshop.render();
    }

    private com.badlogic.gdx.graphics.Texture background() {
        return phase == CampaignState.Phase.MARS ? game.getAssets().marsBackgroundTexture
            : phase == CampaignState.Phase.CALLISTO ? game.getAssets().callistoBackgroundTexture
            : game.getAssets().backgroundLuaTexture;
    }

    private TextureRegion rockRegion(float[] rock) {
        int index = (int)(rock[0] * 7f + rock[1]) % 3;
        return switch (dressing) {
            case MARS -> game.getAssets().marsObstacleRegion(index);
            case CALLISTO -> game.getAssets().titanFormationRegion(index == 0 ? 1 : index == 1 ? 2 : 4);
            case LUNAR -> game.getAssets().lunarObstacleRegion(index);
        };
    }

    /** Estrutura de reabastecimento, coerente com o corpo celeste da arena. */
    private void drawSupply(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
        TextureRegion region = switch (dressing) {
            case MARS -> {
                TextureRegion animated = game.getAssets().marsStationFrame(MarsObject.Kind.OXYGEN_STATION,
                    2 + (int)(time/.4f)%2);
                yield animated != null ? animated : game.getAssets().marsRegion(2,0);
            }
            case CALLISTO -> {
                TextureRegion refinery = game.getAssets().titanRefineryFrame((int)(time/.35f)%4);
                yield refinery != null ? refinery : game.getAssets().geloTexture;
            }
            case LUNAR -> game.getAssets().baseLunarTexture;
        };
        AtlasRegionRenderer.draw(batch,region,SUPPLY.x,SUPPLY.y,SUPPLY.width,SUPPLY.height);
    }

    /**
     * HUD da arena.
     *
     * A versao anterior espalhava texto solto sobre o mundo: a linha de
     * equipamento caia atras do painel de vitais, o nome do chefe encostava na
     * barra de vida e o estado do portal era uma palavra de 10px no canto. Aqui
     * cada leitura tem painel, contraste e tamanho proprios.
     */
    private void drawHud() {
        Inventario inventory = campaign.getInventario();
        boolean bossVisible = boss.isAlive() && !rewarded;
        // O estado vital ocupa apenas um canto; muda de altura quando o
        // astronauta se aproxima, sem tapar o personagem na arena.
        float vitalsX = 24f;
        float vitalsY = player.getPosition().x < 440f && player.getPosition().y > 430f
            ? 18f : 468f;
        ui.beginShapes();
        ui.panel(24f,620f,426f,82f,UiTheme.CYAN);
        ui.panel(vitalsX,vitalsY,398f,108f,UiTheme.CYAN);
        ui.rect(vitalsX+136f,vitalsY+74f,188f,7f,UiTheme.TRACK);
        ui.rect(vitalsX+136f,vitalsY+74f,188f * MathUtils.clamp(player.getOxigenio()/100f,0f,1f),7f,UiTheme.CYAN);
        ui.rect(vitalsX+136f,vitalsY+48f,188f,7f,UiTheme.TRACK);
        ui.rect(vitalsX+136f,vitalsY+48f,188f * MathUtils.clamp(player.getEnergia()/100f,0f,1f),7f,UiTheme.AMBER);
        if (bossVisible) {
            ui.panel(468f,620f,430f,82f,UiTheme.RED);
            ui.rect(484f,635f,398f,9f,UiTheme.TRACK);
            ui.rect(484f,635f,398f * MathUtils.clamp(boss.getHp()/boss.getHpMax(),0f,1f),9f,UiTheme.RED);
        }
        ui.panel(1064f,372f,196f,50f,rewarded ? UiTheme.GREEN : UiTheme.RED);
        if (messageTimer > 0f) ui.panel(440f,18f,816f,72f,UiTheme.CYAN_DIM);
        ui.endShapes();

        ui.beginText();
        if (bossVisible) {
            ui.text(name(), .57f, UiTheme.TEXT, 484f, 682f);
            ui.text(phase == CampaignState.Phase.CALLISTO ? "FORMA " + boss.getForma() + " / 3" : "CHEFE DA FASE",
                .46f, UiTheme.TEXT_MUTED, 484f, 660f);
            ui.text("HP " + (int)Math.ceil(boss.getHp()) + " / " + (int)boss.getHpMax(),
                .46f, UiTheme.TEXT, 772f, 660f);
        }
        ui.textWrapped(rewarded ? "Chave conquistada. Leve-a ao portal leste."
            : keyDropped ? "A chave caiu na arena. Aproxime-se para recolhê-la."
            : bossObjective(),
            .53f, UiTheme.TEXT, 40f, 680f, 394f);
        ui.textWrapped(campaign.missaoAtual(), .43f, UiTheme.TEXT_MUTED, 40f, 645f, 394f);

        ui.text("OXIGÊNIO", .49f, UiTheme.TEXT, vitalsX+16f, vitalsY+83f);
        ui.text((int)player.getOxigenio() + "%", .46f, UiTheme.CYAN, vitalsX+336f, vitalsY+83f);
        ui.text("ENERGIA", .49f, UiTheme.TEXT, vitalsX+16f, vitalsY+57f);
        ui.text((int)player.getEnergia() + "%", .46f, UiTheme.AMBER, vitalsX+336f, vitalsY+57f);
        ui.text("MUNIÇÃO  " + player.getMunicao() + " / " + com.orion.echoes.lua.config.GameConfig.AMMO_MAX
                + "  ·  RESERVA " + inventory.getReserveAmmo(),
            .46f, player.getMunicao() <= 3 ? UiTheme.RED : UiTheme.TEXT, vitalsX+16f, vitalsY+32f);
        ui.text("GELO " + player.getGelo() + "  ·  DIF: " + inventory.getDifficulty().label(),
            .42f, UiTheme.TEXT_MUTED, vitalsX+16f, vitalsY+21f);

        ui.centered(rewarded ? "PORTAL ONLINE" : "PORTAL BLOQUEADO", .56f,
            rewarded ? UiTheme.GREEN : UiTheme.RED, 1162f, 390f);
        if (messageTimer > 0f) ui.textWrapped(message, .52f, UiTheme.TEXT, 458f, 70f, 778f);
        ui.endText();
    }

    // =====================================================
    // SIMULACAO
    // =====================================================

    private void update(float delta) {
        time += delta; cooldown = Math.max(0f,cooldown-delta); recovery = Math.max(0f,recovery-delta);
        shotTimer = Math.max(0f,shotTimer-delta); mutationFlash = Math.max(0f,mutationFlash-delta);
        bossMovingTimer = Math.max(0f,bossMovingTimer-delta);
        bossHitFlash = Math.max(0f,bossHitFlash-delta);
        messageTimer = Math.max(0f,messageTimer-delta);
        float dx = (Gdx.input.isKeyPressed(Input.Keys.D)||Gdx.input.isKeyPressed(Input.Keys.RIGHT)?1f:0f)
            -(Gdx.input.isKeyPressed(Input.Keys.A)||Gdx.input.isKeyPressed(Input.Keys.LEFT)?1f:0f);
        float dy = (Gdx.input.isKeyPressed(Input.Keys.W)||Gdx.input.isKeyPressed(Input.Keys.UP)?1f:0f)
            -(Gdx.input.isKeyPressed(Input.Keys.S)||Gdx.input.isKeyPressed(Input.Keys.DOWN)?1f:0f);
        cursor.set(dx,dy); if (cursor.len2()>1f) cursor.nor();
        boolean running = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.Q))
            && player.tryDash(cursor.x, cursor.y)) {
            if (phase == CampaignState.Phase.MARS)
                particles.criarPoeiraMarte(player.getPosition().x + 27f, player.getPosition().y + 8f,
                    true, cursor.x, cursor.y);
            else
                particles.criarPoeiraLunar(player.getPosition().x + 27f, player.getPosition().y + 8f,
                    true, cursor.x, cursor.y);
        }
        player.move(cursor.x,cursor.y,running,delta);
        physics.update(delta); player.update(delta); particles.update(delta);
        for (Pickup ice : icePickups) {
            ice.update(delta);
            if (ice.coletar(player)) {
                game.getSounds().tocarColeta();
                feedback("Gelo coletado. Processe na estação a oeste para fabricar células.");
            }
        }
        // Mira: mundo, nao tela. O corpo ja foi orientado pelo movimento acima.
        cursor.set(Gdx.input.getX(),Gdx.input.getY()); viewport.unproject(cursor); player.setAimTarget(cursor.x,cursor.y);
        if (boss instanceof BossCalisto calisto) calisto.update(delta);
        if (!rewarded && boss.isAlive() && !(boss instanceof BossCalisto c && c.isMutating())) updateBoss(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            int before = player.getMunicao();
            player.setMunicao(campaign.getInventario().reload(before, com.orion.echoes.lua.config.GameConfig.AMMO_MAX));
            feedback(player.getMunicao() > before ? "Rifle recarregado." : "Sem células fabricadas na reserva.");
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) shoot();
        if (!boss.isAlive() && !rewarded && !keyDropped) dropKey();
        if (keyDropped && keyPickup.overlaps(player.getBounds())) collectKey();
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) interact();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) save();
        if (player.getOxigenio()<=0f) {
            leaving=true; game.setScreen(new GameOverScreen(game,startMissionTime+time)); dispose();
        }
    }

    private void prepareKeyDrop() {
        keyDropped = true;
        keyPickup.set(boss.centerX()-42f, boss.bounds.y+8f, 84f, 84f);
    }

    private void dropKey() {
        campaign.setBossDefeated(phase, true);
        prepareKeyDrop();
        particles.criarMorteInimigo(boss.centerX(),boss.centerY());
        feedback("O chefe caiu. A chave materializou-se no centro da arena — aproxime-se para recolher.");
        save();
    }

    private void collectKey() {
        rewarded = true;
        keyDropped = false;
        campaign.getInventario().add(dressing.key);
        campaign.getInventario().melhorarArma(); campaign.getInventario().melhorarArmadura();
        game.getSounds().tocarColeta();
        particles.criarPortal(portal.x+portal.width/2f,portal.y+portal.height/2f);
        feedback("Chave conquistada e equipamento melhorado. O portal leste está ONLINE.");
        save();
    }

    private void renderKeyDrop(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
        if (!keyDropped) return;
        float pulse = .78f + MathUtils.sin(time * 4.5f) * .12f;
        float bob = MathUtils.sin(time * 3.2f) * 7f;
        batch.setColor(1f,1f,1f,pulse);
        AtlasRegionRenderer.draw(batch,game.getAssets().bossKeyFrame(dressing.keyFrame),
            keyPickup.x,keyPickup.y+bob,keyPickup.width,keyPickup.height);
        batch.setColor(Color.WHITE);
    }

    private void interact() {
        if (SUPPLY_REACH.overlaps(player.getBounds())) {
            workshop.open(dressing.supplyName.toUpperCase() + " · INTERIOR", new WorkshopInterior.Actions() {
                @Override public String status() {
                    return "O2  " + (int)player.getOxigenio() + "%   ·   ENERGIA  "
                        + (int)player.getEnergia() + "%   ·   MUNIÇÃO  " + player.getMunicao();
                }
                @Override public void recharge() {
                    player.recuperarOxigenio(100f); player.recuperarEnergia(100f);
                    feedback(dressing.supplyName + ": suporte vital reabastecido.");
                    game.getSounds().tocarBaseRecarregando();
                }
                @Override public void craft() { feedback("Equipamento de expedição calibrado."); }
                @Override public void processIce() {
                    if (!player.removerGelo()) { feedback("Sem gelo armazenado."); return; }
                    player.recuperarOxigenio(35f);
                    int cells = campaign.getInventario().addReserveAmmo(com.orion.echoes.lua.config.GameConfig.AMMO_PER_ICE);
                    game.getSounds().tocarProcessarGelo();
                    feedback("Gelo processado: +" + cells + " células na reserva. R recarrega.");
                }
            });
            return;
        }
        if (portal.overlaps(player.getBounds())) {
            if (campaign.getInventario().tem(dressing.key)) travel();
            else feedback("BLOQUEADO — o portal só aceita a chave do chefe desta fase.");
        }
    }

    private void updateBoss(float delta) {
        aim.set(player.getHurtbox().getCenter(aim));
        float distance = Vector2.dst(boss.centerX(),boss.centerY(),aim.x,aim.y);
        if (windup > 0f) {
            windup -= delta;
            if (windup <= 0f) {
                // O golpe so alcanca quem esta no raio E com linha livre: rocha
                // e estacao bloqueiam de verdade, nao so atrapalham o caminho.
                if (distance < 145f && !CombatVisibility.blocked(boss.centerX(),boss.centerY(),aim.x,aim.y,
                        physics.getSolidBounds())) {
                    // Astronauta aplica o multiplicador da armadura. Passar o dano
                    // já reduzido aqui aplicava a proteção duas vezes nas arenas.
                    player.receberDano(24f, boss.centerX(),boss.centerY());
                    particles.criarImpactoTraje(aim.x,aim.y);
                }
                game.getSounds().tocarBoss("impacto",boss.centerX(),boss.centerY()); recovery = .7f;
            }
            return;
        }
        if (recovery > 0f) return;
        // Cada mundo aumenta a pressão, mas conserva uma janela visível para
        // sair do círculo antes do impacto. A arena ainda oferece recarga.
        float attackRange = phase == CampaignState.Phase.LUNAR ? 125f : 137f;
        if (distance < attackRange && cooldown == 0f) {
            windup = phase == CampaignState.Phase.LUNAR ? .78f : .70f;
            cooldown = phase == CampaignState.Phase.LUNAR ? 2.3f : 2.05f;
            game.getSounds().tocarBoss("rugido",boss.centerX(),boss.centerY()); return;
        }
        aim.sub(boss.centerX(),boss.centerY()).nor();
        // Fecha distância fora do alcance do golpe, sem encurtar o aviso de ataque.
        float speed = boss.getSpeed() * campaign.getInventario().getDifficulty().enemySpeedMultiplier()
            * (distance > 230f ? 1.15f : 1f);
        dashClock -= delta;
        if (boss.getForma()==3 && dashClock<.22f && dashClock>0f) speed *= 2.3f;
        if (dashClock<=0f) dashClock=phase == CampaignState.Phase.CALLISTO ? 2.65f : 3f;
        moveBoss(aim.x*speed*delta,aim.y*speed*delta);
    }

    /**
     * Avanco do chefe em passos curtos.
     *
     * O dash da terceira forma multiplica a velocidade por 2.3: num unico
     * deslocamento ele pularia por cima de uma pedra sem nunca sobrepo-la
     * (tunneling). Dividir o movimento mantem a colisao honesta sem um segundo
     * sistema de fisica.
     */
    private void moveBoss(float dx,float dy) {
        float oldX = boss.bounds.x, oldY = boss.bounds.y;
        int steps = 1 + (int)(Math.max(Math.abs(dx),Math.abs(dy)) / 8f);
        float stepX = dx/steps, stepY = dy/steps;
        for (int i=0;i<steps;i++) {
            movement.set(boss.bounds.x+stepX,boss.bounds.y,boss.bounds.width,boss.bounds.height);
            if (free()) boss.bounds.x=MathUtils.clamp(boss.bounds.x+stepX,25f,1150f);
            movement.set(boss.bounds.x,boss.bounds.y+stepY,boss.bounds.width,boss.bounds.height);
            if (free()) boss.bounds.y=MathUtils.clamp(boss.bounds.y+stepY,25f,565f);
        }
        float travelled = Vector2.dst(oldX,oldY,boss.bounds.x,boss.bounds.y);
        if (travelled > .01f) {
            bossTravel += travelled;
            bossMovingTimer = .14f;
        }
    }

    private boolean free() { for (Rectangle solid : physics.getSolidBounds()) if (solid.overlaps(movement)) return false; return true; }

    private void shoot() {
        if (!campaign.hasWeapon()) { feedback("Sem rifle equipado: monte a arma antes de enfrentar o chefe."); return; }
        if (player.getMunicao()<=0) { feedback("Pente vazio. Reabasteça na " + dressing.supplyName.toLowerCase() + "."); return; }
        if (shotTimer>0f) return;
        shotTimer=.15f; player.setMunicao(player.getMunicao()-1); player.muzzle(shot); player.triggerShot();
        float angle=player.getAimAngle()*MathUtils.degreesToRadians;
        float rayX = MathUtils.cos(angle), rayY = MathUtils.sin(angle);
        float range = 760f;
        float wallHit = CombatVisibility.firstSolidHit(shot.x,shot.y,rayX,rayY,
            range,physics.getSolidBounds());
        // Movement uses a feet-sized collider; shooting must target the visible torso.
        Rectangle target = boss.shotBounds();
        float bossHit = boss.isAlive() && !rewarded
            ? CombatVisibility.firstHit(shot.x,shot.y,rayX,rayY,range,target)
            : Float.POSITIVE_INFINITY;
        float distance = Math.min(range,Math.min(wallHit,bossHit));
        shotEnd.set(shot.x+rayX*distance,shot.y+rayY*distance);
        if (bossHit < wallHit && bossHit <= range) {
            int previous = boss.getForma(); boss.receiveDamage(campaign.getInventario().getDano());
            bossHitFlash = .10f;
            particles.criarImpactoTiro(shotEnd.x,shotEnd.y);
            if (boss.getForma()!=previous) {
                mutationFlash=1.25f; windup=0f; recovery=1.25f;
                feedback("O Sentinela renasceu: forma " + boss.getForma() + " de 3.");
                game.getSounds().tocarBoss("rugido",boss.centerX(),boss.centerY()); save();
            }
        }
        particles.criarMuzzleFlash(shot.x,shot.y,player.getAimAngle());
        game.getSounds().tocarDisparo();
    }

    /** O dano da arena já existia, mas o disparo não era desenhado. */
    private void renderShot(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
        if (shotTimer <= 0f) return;
        float alpha = MathUtils.clamp(shotTimer / .15f, 0f, 1f);
        float dx = shotEnd.x - shot.x, dy = shotEnd.y - shot.y;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length < 1f) return;
        float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        batch.setColor(.16f, .72f, 1f, alpha * .38f);
        batch.draw(game.getAssets().uiWhiteTexture, shot.x, shot.y - 5f,
            0f, 5f, length, 10f, 1f, 1f, angle);
        batch.setColor(.89f, .98f, 1f, alpha);
        batch.draw(game.getAssets().uiWhiteTexture, shot.x, shot.y - 1.5f,
            0f, 1.5f, length, 3f, 1f, 1f, angle);
        batch.setColor(Color.WHITE);
    }

    /**
     * Desenho do chefe.
     *
     * Cada arena usa a própria folha de poses. Os pés de cada quadro são
     * alinhados ao mesmo chão para não parecer que o chefe teleporta ao atacar.
     */
    private void renderBoss() {
        if (!boss.isAlive() || rewarded) return;
        var batch=game.getBatch();
        if(windup>0f) {
            batch.setColor(1f,.25f,.15f,.45f);
            for(int i=0;i<48;i++) {
                float a=i*MathUtils.PI2/48f,b=(i+1)*MathUtils.PI2/48f;
                float x=boss.centerX()+MathUtils.cos(a)*145f,y=boss.centerY()+MathUtils.sin(a)*145f;
                float dx=(MathUtils.cos(b)-MathUtils.cos(a))*145f,dy=(MathUtils.sin(b)-MathUtils.sin(a))*145f;
                batch.draw(game.getAssets().uiWhiteTexture,x,y,0f,0f,(float)Math.sqrt(dx*dx+dy*dy),2f,
                    1f,1f,MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees);
            }
            batch.setColor(Color.WHITE);
        }
        int pose = windup>0f ? 1 : recovery>.52f ? 2 : recovery>0f ? 3 : 0;
        displayedPose = pose;
        float size = dressing == Dressing.CALLISTO ? 210f + (boss.getForma()-1)*34f : 252f;
        TextureRegion frame = bossFrame(displayedPose);
        float scale = size / frame.getRegionWidth();
        float drawHeight = frame.getRegionHeight() * scale;
        boolean reducedMotion = game.getSettings().isReduceMotion();
        float step = bossMovingTimer > 0f && !reducedMotion ? MathUtils.sin(bossTravel * .085f) : 0f;
        // A massa comprime antes do ataque e recupera depois. O pé opaco
        // continua preso ao chão; nenhuma dessas transformações altera colisão.
        float widthScale = windup > 0f ? 1.055f : recovery > .52f ? .96f : 1f;
        float heightScale = windup > 0f ? .92f : recovery > .52f ? 1.045f : 1f;
        if (!reducedMotion && windup <= 0f && recovery <= 0f)
            heightScale += MathUtils.sin(time * 2.4f) * .007f;
        float drawWidth = size * widthScale;
        float spriteHeight = drawHeight * heightScale;
        float drawX = boss.centerX() - drawWidth / 2f + step * 2.2f;
        float drawY = boss.bounds.y - bossFootPadding(displayedPose) * scale * heightScale
            + Math.abs(step) * (reducedMotion ? 0f : 2.5f);

        // Sombra de apoio em faixas, para assentar a figura no chao sem virar
        // um retangulo solido embaixo do chefe.
        for (int i=0;i<4;i++) {
            float k = 1f - i*.22f;
            batch.setColor(0f,0f,0f,.10f);
            AtlasRegionRenderer.draw(batch,game.getAssets().uiWhiteTexture,
                boss.centerX()-size*.20f*k,boss.bounds.y+2f+i*3f,size*.40f*k,4f);
        }

        // No cross-fade between different silhouettes: overlapping limbs looked like a glitch.
        if (mutationFlash>0f) batch.setColor(1f,1f,1f,.55f+MathUtils.sin(time*6f)*.15f);
        else if (bossHitFlash > 0f) batch.setColor(1f,.79f,.68f,1f);
        else { Color base=bossTint(); batch.setColor(base.r,base.g,base.b,1f); }
        AtlasRegionRenderer.draw(batch,frame,drawX,drawY,drawWidth,spriteHeight);
        batch.setColor(Color.WHITE);
    }

    /** Distância da borda da textura ao pé opaco, sempre na escala da fonte. */
    private int bossFootPadding(int pose) {
        int index = MathUtils.clamp(pose, 0, 3);
        return switch (dressing) {
            case LUNAR -> 724 - LUNAR_FOOT_BOTTOM[index];
            case MARS -> 724 - MARS_FOOT_BOTTOM[index];
            case CALLISTO -> 40;
        };
    }

    private TextureRegion bossFrame(int pose) {
        return switch (dressing) {
            case CALLISTO -> game.getAssets().callistoBossFrame(boss.getForma(),pose);
            case LUNAR -> game.getAssets().lunarBossFrame(pose);
            case MARS -> game.getAssets().marsBossFrame(pose);
        };
    }

    /** Os chefes planetários agora possuem folhas próprias; a tinta fica neutra. */
    private Color bossTint() {
        return switch (dressing) {
            case LUNAR, MARS -> tint.set(1f,1f,1f,1f);
            case CALLISTO -> tint.set(1f,1f,1f,1f);
        };
    }

    // =====================================================
    // PERSISTENCIA E SAIDA
    // =====================================================

    private void save() {
        campaign.setVitals(player.getOxigenio(),player.getEnergia()); campaign.setAmmo(player.getMunicao());
        campaign.setMissionTime(startMissionTime + time);
        if (boss instanceof BossCalisto) campaign.setBossCalisto(boss.getForma(),boss.getHp());
        GameSaveData data=player.toSaveData(); LunarCheckpoint.applyCampaign(data,campaign);
        data.cena = switch (phase) {
            case LUNAR -> "CHEFE_LUA";
            case MARS -> "CHEFE_MARTE";
            default -> "ARENA";
        };
        new SaveManager().save(data);
    }

    private boolean isThisArena(GameSaveData data) {
        if (data.campanhaConcluida || data.semente != campaign.getSeed()
            || CampaignState.phaseFromToken(data.fase) != phase) return false;
        return switch (phase) {
            case LUNAR -> "CHEFE_LUA".equals(data.cena);
            case MARS -> "CHEFE_MARTE".equals(data.cena);
            default -> "ARENA".equals(data.cena);
        };
    }

    private void travel() {
        save(); leaving=true;
        if (phase==CampaignState.Phase.LUNAR) game.setScreen(WorldIntroScreen.routeToMars(game,campaign));
        else if (phase==CampaignState.Phase.MARS) game.setScreen(WorldIntroScreen.routeToTitan(game,campaign));
        else game.setScreen(new AharinScreen(game,campaign));
        dispose();
    }

    @Override public void resize(int w,int h) { viewport.update(w,h); if(ui!=null)ui.resize(w,h); if(overlay!=null)overlay.resize(w,h);if(pauseUi!=null)pauseUi.resize(w,h);if(workshop!=null)workshop.resize(w,h); }
    @Override public void pause() { if(pauseUi!=null)pauseUi.pause(); }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() {
        if(overlay!=null){overlay.dispose();overlay=null;} if(ui!=null){ui.dispose();ui=null;}
        if(pauseUi!=null){pauseUi.dispose();pauseUi=null;}
        if(workshop!=null){workshop.dispose();workshop=null;}
        if(particles!=null){particles.dispose();particles=null;} if(physics!=null){physics.dispose();physics=null;}
    }
}
