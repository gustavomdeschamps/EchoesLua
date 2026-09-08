package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.ui.UiTheme;
import java.util.function.Supplier;

/**
 * Abertura curta de cada mundo, reaproveitada entre Lua, Marte e Titã.
 *
 * As três aberturas compartilham a mesma arquitetura — texto escalonado,
 * fundo com deriva lenta, vinheta, letreiro de missão — mas cada mundo tem
 * paleta e um motivo de efeito próprio (secção 16 em diante do briefing de
 * produção), então a diferença fica na direção de arte, não em três classes
 * quase idênticas.
 *
 * Só aparece na primeira travessia de cada mundo: {@link CampaignState}
 * guarda a flag correspondente, e os métodos estáticos {@code routeTo*} são o
 * único jeito de chegar aqui — eles decidem sozinhos se a cinematic é
 * necessária ou se o jogo deve ir direto para a Screen de jogo.
 */
public final class WorldIntroScreen implements Screen {

    private static final float DURATION = 3.4f;
    private static final float SKIP_DELAY = .35f;
    private static final float FADE_OUT = .5f;

    private final EchoesLua game;
    private final SpriteBatch batch;
    private final AssetManager assets;
    private final CampaignState.Phase world;
    private final Supplier<Screen> nextScreenFactory;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport =
        new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);

    private float elapsed;
    private boolean finished;

    private WorldIntroScreen(EchoesLua game, CampaignState.Phase world, Supplier<Screen> nextScreenFactory) {
        this.game = game;
        this.batch = game.getBatch();
        this.assets = game.getAssets();
        this.world = world;
        this.nextScreenFactory = nextScreenFactory;
        camera.position.set(GameConfig.WINDOW_WIDTH / 2f, GameConfig.WINDOW_HEIGHT / 2f, 0f);
        camera.update();
    }

    // =====================================================
    // ROTEAMENTO — o único jeito de se chegar a esta tela
    // =====================================================

    /** Lua só é apresentada uma vez por campanha: é sempre o primeiro mundo. */
    public static Screen routeToLunar(EchoesLua game, CampaignState campaign) {
        Screen next = new LunarScreen(game, game.getBatch(), game.getAssets(), campaign);
        if (campaign.isLunarIntroShown()) return next;
        campaign.setLunarIntroShown(true);
        return new WorldIntroScreen(game, CampaignState.Phase.LUNAR, () -> next);
    }

    public static Screen routeToMars(EchoesLua game, CampaignState campaign) {
        Screen next = new MarsScreen(game, campaign);
        if (campaign.isMarsIntroShown()) return next;
        campaign.setMarsIntroShown(true);
        return new WorldIntroScreen(game, CampaignState.Phase.MARS, () -> next);
    }

    public static Screen routeToTitan(EchoesLua game, CampaignState campaign) {
        Screen next = new TitanScreen(game, campaign);
        if (campaign.isTitanIntroShown()) return next;
        campaign.setTitanIntroShown(true);
        return new WorldIntroScreen(game, CampaignState.Phase.TITAN, () -> next);
    }

    // =====================================================
    // CONTEÚDO POR MUNDO
    // =====================================================

    private Color accent() {
        return switch (world) {
            case LUNAR -> UiTheme.CYAN;
            case MARS -> Color.valueOf("C95E37");
            case TITAN -> UiTheme.AMBER;
        };
    }

    private String[] titleLines() {
        return switch (world) {
            case LUNAR -> new String[] {"MISSÃO 01", "LUA", "SILÊNCIO NO ENLACE"};
            case MARS -> new String[] {"MISSÃO 02", "MARTE", "COLÔNIA NA TEMPESTADE"};
            case TITAN -> new String[] {"MISSÃO 03", "TITÃ", "SOB A NÉVOA DE METANO"};
        };
    }

    private String subtitle() {
        return switch (world) {
            case LUNAR -> "Restabeleça os sistemas da colônia.";
            case MARS -> "Reative a instalação e localize o novo portal.";
            case TITAN -> "Encontre a equipe e atravesse o vale.";
        };
    }

    /** Fundo dedicado quando existir (docs/NEW_VISUAL_ASSETS.md); senão, o terreno do mundo. */
    private Texture background() {
        Texture dedicated = assets.worldIntroTexture(world);
        if (dedicated != null) return dedicated;
        return switch (world) {
            case LUNAR -> assets.backgroundLuaTexture;
            case MARS -> assets.marsBackgroundTexture;
            case TITAN -> assets.titanBackgroundTexture;
        };
    }

    private Color backgroundTint() {
        return switch (world) {
            case LUNAR -> new Color(.16f, .19f, .24f, 1f);
            case MARS -> new Color(.42f, .22f, .15f, 1f);
            case TITAN -> new Color(.28f, .21f, .13f, 1f);
        };
    }

    // =====================================================
    // FRAME
    // =====================================================

    @Override
    public void render(float delta) {
        elapsed += Math.min(delta, 1f / 30f);
        if (elapsed > SKIP_DELAY && (Gdx.input.justTouched()
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))) {
            finish();
            return;
        }
        if (elapsed >= DURATION) {
            finish();
            return;
        }

        Gdx.gl.glClearColor(.01f, .012f, .016f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawBackground();
        batch.end();

        switch (world) {
            case LUNAR -> renderLunarMotif();
            case MARS -> renderMarsMotif();
            case TITAN -> renderTitanMotif();
        }

        renderVignette();
        renderText();
        renderFade();
    }

    private void drawBackground() {
        float drift = Interpolation.sine.apply(MathUtils.clamp(elapsed / DURATION, 0f, 1f)) * 34f;
        batch.setColor(backgroundTint());
        batch.draw(background(), -drift, -drift * .5f,
            GameConfig.WINDOW_WIDTH + drift * 2f, GameConfig.WINDOW_HEIGHT + drift);
        batch.setColor(Color.WHITE);
    }

    // =====================================================
    // MOTIVOS POR MUNDO
    // =====================================================

    /** Lua: silêncio e sinal perdido — arcos de rádio incompletos, poeira mínima. */
    private void renderLunarMotif() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Color cyan = new Color(.24f, .66f, .78f, 1f);
        for (int pulse = 0; pulse < 3; pulse++) {
            float age = elapsed - pulse * .7f;
            if (age <= 0f) continue;
            float life = MathUtils.clamp(age / 2.4f, 0f, 1f);
            if (life >= 1f) continue;
            float radius = 30f + Interpolation.pow2Out.apply(life) * 300f;
            float alpha = (1f - life) * .38f;
            arc(150f, 120f, radius, -20f, 130f, 2.2f, new Color(cyan.r, cyan.g, cyan.b, alpha));
        }
        // Poeira lunar rara subindo: sem atmosfera, ela não se dispersa — sobe reta e some.
        for (int i = 0; i < 10; i++) {
            float seed = i * 91.7f;
            float x = Math.floorMod((int) (seed * 13f), 1280);
            float y = (elapsed * (14f + i % 5) + seed) % 760f - 20f;
            batch.setColor(.7f, .82f, .86f, .12f * (1f - Math.abs(y - 360f) / 400f));
            batch.draw(assets.uiWhiteTexture, x, y, 2f, 2f);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Marte: poeira lateral em camadas e interferência curta de tempestade. */
    private void renderMarsMotif() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Color oxide = new Color(.79f, .48f, .27f, 1f);
        for (int layer = 0; layer < 3; layer++) {
            float speed = 46f + layer * 34f;
            float bandY = 90f + layer * 190f;
            float bandH = 70f - layer * 12f;
            float offset = (elapsed * speed + layer * 260f) % (1280f + 320f) - 320f;
            batch.setColor(oxide.r, oxide.g, oxide.b, .1f + layer * .03f);
            batch.draw(assets.uiWhiteTexture, offset, bandY, 320f, bandH);
        }
        // Interferência: linhas finas que piscam por instantes, como estática de tempestade.
        float glitch = MathUtils.sin(elapsed * 23f) > .93f ? 1f : 0f;
        if (glitch > 0f) {
            batch.setColor(1f, .86f, .74f, .1f);
            float y = MathUtils.random(80f, 640f);
            batch.draw(assets.uiWhiteTexture, 0f, y, GameConfig.WINDOW_WIDTH, 2f);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Titã: névoa de metano em várias velocidades e um sinal distante, sem revelar o chefe. */
    private void renderTitanMotif() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Color fog = new Color(.55f, .42f, .27f, 1f);
        for (int layer = 0; layer < 4; layer++) {
            float speed = 10f + layer * 7f;
            float y = 40f + layer * 150f;
            float offset = (elapsed * speed * (layer % 2 == 0 ? 1f : -1f)) % 1280f;
            batch.setColor(fog.r, fog.g, fog.b, .16f - layer * .022f);
            batch.draw(assets.uiWhiteTexture, offset - 1280f, y, 2560f, 96f);
        }
        // Sinal distante e ameaçador: um pulso fraco no horizonte, nunca a silhueta do chefe.
        float pulse = .3f + MathUtils.sin(elapsed * 2.1f) * .18f;
        batch.setColor(1f, .55f, .28f, .22f * pulse);
        batch.draw(assets.uiWhiteTexture, 1090f, 470f, 26f, 26f);
        batch.setColor(1f, .55f, .28f, .5f * pulse);
        batch.draw(assets.uiWhiteTexture, 1099f, 479f, 8f, 8f);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Arco incompleto — a mesma assinatura de rádio da abertura geral (ver ART_BIBLE). */
    private void arc(float centerX, float centerY, float radius,
                     float startDegrees, float sweepDegrees, float thickness, Color color) {
        int segments = Math.max(8, (int) (sweepDegrees / 4f));
        float step = sweepDegrees / segments;
        float chord = 2f * radius * MathUtils.sinDeg(step / 2f) + 1f;
        batch.setColor(color);
        for (int index = 0; index < segments; index++) {
            float angle = startDegrees + step * (index + .5f);
            float x = centerX + MathUtils.cosDeg(angle) * radius;
            float y = centerY + MathUtils.sinDeg(angle) * radius;
            batch.draw(assets.uiWhiteTexture, x, y - thickness / 2f,
                0f, thickness / 2f, chord, thickness, 1f, 1f, angle + 90f);
        }
    }

    // =====================================================
    // TEXTO E MOLDURA
    // =====================================================

    private void renderVignette() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(.03f, .035f, .045f, .58f);
        batch.draw(assets.uiDamageVignetteTexture, 0f, 0f,
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        batch.setColor(.01f, .012f, .016f, .5f);
        batch.draw(assets.uiWhiteTexture, 0f, 0f, GameConfig.WINDOW_WIDTH, 190f);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void renderText() {
        String[] lines = titleLines();
        Color accent = accent();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawAt(assets.font, lines[0], .78f, .1f, DURATION, 78f, 190f, accent);
        drawAt(assets.titleFont, lines[1], 2.55f, .28f, DURATION, 74f, 118f, Color.WHITE);
        drawAt(assets.font, lines[2], .82f, .5f, DURATION, 78f, 74f,
            new Color(.86f, .87f, .84f, 1f));
        drawAt(assets.font, subtitle(), .66f, .78f, DURATION, 78f, 42f, UiTheme.TEXT_MUTED);
        drawAt(assets.font, "ESPAÇO / ENTER  PULAR", .56f, .7f, DURATION, 1010f, 22f,
            new Color(.72f, .76f, .75f, 1f));
        batch.end();
    }

    private void drawAt(com.badlogic.gdx.graphics.g2d.BitmapFont font, String text, float scale,
                        float start, float end, float x, float y, Color color) {
        float alpha = envelope(start, end, .32f);
        if (alpha <= 0f) return;
        font.getData().setScale(scale);
        font.setColor(color.r, color.g, color.b, alpha);
        font.draw(batch, text, x, y);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private float envelope(float start, float end, float edge) {
        if (elapsed <= start || elapsed >= end) return 0f;
        return Math.min(MathUtils.clamp((elapsed - start) / edge, 0f, 1f),
            MathUtils.clamp((end - elapsed) / edge, 0f, 1f));
    }

    private void renderFade() {
        float fadeIn = 1f - Interpolation.pow2Out.apply(MathUtils.clamp(elapsed / .4f, 0f, 1f));
        float fadeOut = Interpolation.pow2In.apply(
            MathUtils.clamp((elapsed - (DURATION - FADE_OUT)) / FADE_OUT, 0f, 1f));
        float alpha = Math.max(fadeIn, fadeOut);
        if (alpha <= 0f) return;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(0f, 0f, 0f, alpha);
        batch.draw(assets.uiWhiteTexture, 0f, 0f, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void finish() {
        if (finished) return;
        finished = true;
        game.setScreen(nextScreenFactory.get());
        dispose();
    }

    @Override public void show() { }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { }
}
