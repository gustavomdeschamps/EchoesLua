package com.orion.echoes.lua.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.systems.MissionState;
import com.orion.echoes.lua.ui.UiTheme;

/** HUD compacto que libera o centro da tela e fica translucido quando o jogador passa por baixo. */
public final class Hud implements Disposable {
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final NinePatch panelPatch;
    private final com.badlogic.gdx.graphics.g2d.TextureRegion barTrack;
    private final com.badlogic.gdx.graphics.g2d.TextureRegion barFill;
    /*
     * Caixa de mensagem.
     *
     * As medidas saem do texto ja renderizado, entao a caixa acompanha o
     * conteudo em vez de tentar adivinha-lo.
     */
    private static final float TOAST_SCALE = .82f;
    private static final float TOAST_MAX_TEXT_WIDTH = 560f;
    private static final float TOAST_MIN_WIDTH = 260f;
    private static final float TOAST_PADDING_X = 26f;
    private static final float TOAST_PADDING_Y = 16f;
    private static final float TOAST_Y = 104f;

    /** Reaproveitada a cada sombra para nao alocar Color por frame. */
    private final Color shadowColor = new Color();

    private static final float SHADOW_SPREAD = 6f;
    private static final float SHADOW_OFFSET = 3f;

    private float toastWidth;
    private float toastHeight;
    private String previousMessage = "";
    private float entrance;
    private float toastLife;
    private float toastKick;

    public Hud(AssetManager assets) {
        viewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
        camera.position.set(GameConfig.WINDOW_WIDTH / 2f, GameConfig.WINDOW_HEIGHT / 2f, 0f);
        camera.update();
        font = assets.font;
        panelPatch = assets.uiPanelPatch();
        barTrack = assets.uiBarTrackTexture;
        barFill = assets.uiBarFillTexture;
    }

    public void update(float delta, String message) {
        entrance = Math.min(1f, entrance + delta / .32f);
        String safe = message == null ? "" : message;
        if (!safe.equals(previousMessage)) {
            previousMessage = safe;
            toastLife = safe.isBlank() ? 0f : 1f;
            toastKick = safe.isBlank() ? 0f : 1f;
            medirMensagem(safe);
        }
        toastLife = Math.max(0f, toastLife - delta / 3.2f);
        toastKick = Math.max(0f, toastKick - delta / .22f);
    }

    /**
     * Mede a mensagem com a fonte real, quebrando linha se preciso.
     *
     * A largura da caixa era estimada por contagem de caracteres
     * (message.length() * 9.2f), mas Chakra Petch e proporcional: uma frase
     * com letras largas estourava a caixa, e o teto fixo cortava qualquer
     * mensagem mais longa. Agora a caixa e dimensionada pelo texto, e nao o
     * contrario.
     */
    private void medirMensagem(String value) {
        if (value.isBlank()) {
            toastWidth = 0f;
            toastHeight = 0f;
            return;
        }
        font.getData().setScale(TOAST_SCALE);
        // Primeiro sem quebra, para saber se cabe numa linha so.
        layout.setText(font, value);
        if (layout.width <= TOAST_MAX_TEXT_WIDTH) {
            toastWidth = Math.max(TOAST_MIN_WIDTH, layout.width + TOAST_PADDING_X * 2f);
            toastHeight = layout.height + TOAST_PADDING_Y * 2f;
        } else {
            // Nao cabe: quebra dentro da largura maxima e a caixa cresce em altura.
            layout.setText(font, value, font.getColor(), TOAST_MAX_TEXT_WIDTH, Align.center, true);
            toastWidth = TOAST_MAX_TEXT_WIDTH + TOAST_PADDING_X * 2f;
            toastHeight = layout.height + TOAST_PADDING_Y * 2f;
        }
        font.getData().setScale(1f);
    }

    public void render(SpriteBatch batch, Astronauta player, MissionState mission, String message,
                       float playerScreenX, float playerScreenY) {
        float eased = Interpolation.pow3Out.apply(entrance);
        float objectiveY = 646f + (1f - eased) * 22f;
        float lowerY = 18f - (1f - eased) * 18f;
        float objectiveAlpha = near(playerScreenX, playerScreenY, 354f, objectiveY, 572f, 58f) ? .24f : .9f;
        float vitalsAlpha = near(playerScreenX, playerScreenY, 18f, lowerY, 246f, 88f) ? .24f : .88f;
        float inventoryAlpha = near(playerScreenX, playerScreenY, 940f, lowerY, 322f, 52f) ? .24f : .88f;
        boolean toastVisible = message != null && !message.isBlank() && toastLife > 0f;
        float toastAlpha = toastVisible ? Math.min(1f, toastLife * 4f) : 0f;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        panel(batch, 354f, objectiveY, 572f, 58f, objectiveAlpha, UiTheme.AMBER);
        panel(batch, 18f, lowerY, 246f, 88f, vitalsAlpha, UiTheme.CYAN);
        panel(batch, 940f, lowerY, 322f, 52f, inventoryAlpha, UiTheme.CYAN_DIM);
        if (toastVisible) {
            float kick = Interpolation.swingOut.apply(toastKick) * 4f;
            float width = toastWidth;
            float height = toastHeight + kick;
            panel(batch, 640f - width / 2f, TOAST_Y - kick / 2f, width, height,
                .92f * toastAlpha, UiTheme.GREEN);
        }
        bar(batch, 78f, lowerY + 59f, 132f, 9f, player.getOxigenio() / 100f,
            player.getOxigenio() <= 25f ? UiTheme.RED : UiTheme.CYAN, vitalsAlpha);
        bar(batch, 78f, lowerY + 37f, 132f, 8f, player.getEnergia() / 100f, UiTheme.AMBER, vitalsAlpha);
        bar(batch, 78f, lowerY + 15f, 132f, 6f, player.getMunicao() / (float) GameConfig.AMMO_MAX,
            player.getMunicao() <= GameConfig.AMMO_LOW ? UiTheme.RED : UiTheme.GREEN, vitalsAlpha);
        batch.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        centered(batch, mission.getObjective(player.getOxigenio()), 1.02f, UiTheme.TEXT, 640f,
            objectiveY + 36f, objectiveAlpha);
        text(batch, String.format("QUEST %d/%d", mission.getQuestStep(player.getOxigenio()),
                MissionState.QUEST_TOTAL_STEPS),
            .62f, UiTheme.AMBER, 372f, objectiveY + 17f, objectiveAlpha);
        centered(batch, mission.getQuestTitle(player.getOxigenio()), .64f, UiTheme.TEXT_MUTED,
            660f, objectiveY + 17f, objectiveAlpha * .9f);
        text(batch, "O2", .8f, UiTheme.TEXT_MUTED, 34f, lowerY + 71f, vitalsAlpha);
        text(batch, String.format("%.0f%%", player.getOxigenio()), .75f,
            player.getOxigenio() <= 25f ? UiTheme.RED : UiTheme.TEXT, 216f, lowerY + 69f, vitalsAlpha);
        text(batch, "EN", .8f, UiTheme.TEXT_MUTED, 34f, lowerY + 49f, vitalsAlpha);
        text(batch, String.format("%.0f%%", player.getEnergia()), .72f, UiTheme.TEXT, 216f,
            lowerY + 47f, vitalsAlpha);
        boolean lowAmmo = player.getMunicao() <= GameConfig.AMMO_LOW;
        text(batch, "MUN", .8f, UiTheme.TEXT_MUTED, 34f, lowerY + 25f, vitalsAlpha);
        text(batch, String.format("%d", player.getMunicao()), .78f,
            lowAmmo ? UiTheme.RED : UiTheme.GREEN, 216f, lowerY + 25f, vitalsAlpha);
        text(batch, "O2  " + player.getOxigenioColetado(), .8f, UiTheme.CYAN, 958f, lowerY + 33f, inventoryAlpha);
        text(batch, "COMIDA  " + player.getComidaColetada(), .8f, UiTheme.AMBER, 1035f, lowerY + 33f, inventoryAlpha);
        text(batch, "GELO  " + player.getGeloColetado(), .8f, UiTheme.TEXT, 1163f, lowerY + 33f, inventoryAlpha);
        if (toastVisible) {
            desenharToast(batch, message, toastAlpha);
        }
        batch.end();
    }

    /** Desenha a mensagem centralizada na caixa, em uma ou duas linhas. */
    private void desenharToast(SpriteBatch batch, String value, float alpha) {
        font.getData().setScale(TOAST_SCALE);
        font.setColor(UiTheme.TEXT.r, UiTheme.TEXT.g, UiTheme.TEXT.b, alpha);
        float textWidth = Math.min(toastWidth - TOAST_PADDING_X * 2f, TOAST_MAX_TEXT_WIDTH);
        layout.setText(font, value, font.getColor(), textWidth, Align.center, true);
        float kick = Interpolation.swingOut.apply(toastKick) * 4f;
        float baseline = TOAST_Y - kick / 2f + toastHeight + kick - TOAST_PADDING_Y;
        font.draw(batch, layout, 640f - textWidth / 2f, baseline);
        font.getData().setScale(1f);
    }

    private boolean near(float px, float py, float x, float y, float width, float height) {
        return px > x - 46f && px < x + width + 46f && py > y - 44f && py < y + height + 44f;
    }

    /**
     * Painel do HUD com sombra de mesma silhueta.
     *
     * A sombra usa o proprio 9-patch, em duas camadas, para acompanhar os
     * cantos arredondados; e escala com o alpha do painel, senao ficaria uma
     * mancha escura sob um painel que esta esmaecendo.
     */
    private void panel(SpriteBatch batch, float x, float y, float width, float height,
                       float alpha, Color accent) {
        sombra(batch, x, y, width, height, SHADOW_SPREAD, SHADOW_OFFSET, .16f * alpha);
        sombra(batch, x, y, width, height, SHADOW_SPREAD * .45f, SHADOW_OFFSET * .55f, .24f * alpha);
        panelPatch.setColor(new Color(accent.r * .42f + .58f, accent.g * .42f + .58f,
            accent.b * .42f + .58f, alpha));
        panelPatch.draw(batch, x, y, width, height);
        panelPatch.setColor(Color.WHITE);
    }

    private void sombra(SpriteBatch batch, float x, float y, float width, float height,
                        float spread, float offset, float alpha) {
        shadowColor.set(0f, 0f, 0f, alpha);
        panelPatch.setColor(shadowColor);
        panelPatch.draw(batch, x - spread + offset, y - spread - offset,
            width + spread * 2f, height + spread * 2f);
        panelPatch.setColor(Color.WHITE);
    }

    private void bar(SpriteBatch batch, float x, float y, float width, float height,
                     float ratio, Color color, float alpha) {
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(barTrack, x, y, width, height);
        batch.setColor(color.r, color.g, color.b, alpha);
        batch.draw(barFill, x, y, width * MathUtils.clamp(ratio, 0f, 1f), height);
        batch.setColor(Color.WHITE);
    }

    private void text(SpriteBatch batch, String value, float scale, Color color, float x, float y, float alpha) {
        font.getData().setScale(scale);
        font.setColor(color.r, color.g, color.b, alpha);
        font.draw(batch, value, x, y);
    }

    private void centered(SpriteBatch batch, String value, float scale, Color color,
                          float centerX, float y, float alpha) {
        font.getData().setScale(scale);
        font.setColor(color.r, color.g, color.b, alpha);
        layout.setText(font, value);
        font.draw(batch, layout, centerX - layout.width / 2f, y);
    }

    public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void dispose() { }
}
