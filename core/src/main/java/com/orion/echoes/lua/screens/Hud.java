package com.orion.echoes.lua.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
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
        }
        toastLife = Math.max(0f, toastLife - delta / 3.2f);
        toastKick = Math.max(0f, toastKick - delta / .22f);
    }

    public void render(SpriteBatch batch, Astronauta player, MissionState mission, String message,
                       float playerScreenX, float playerScreenY) {
        float eased = Interpolation.pow3Out.apply(entrance);
        float objectiveY = 646f + (1f - eased) * 22f;
        float lowerY = 18f - (1f - eased) * 18f;
        float objectiveAlpha = near(playerScreenX, playerScreenY, 354f, objectiveY, 572f, 58f) ? .24f : .9f;
        float vitalsAlpha = near(playerScreenX, playerScreenY, 18f, lowerY, 246f, 68f) ? .24f : .88f;
        float inventoryAlpha = near(playerScreenX, playerScreenY, 940f, lowerY, 322f, 52f) ? .24f : .88f;
        boolean toastVisible = message != null && !message.isBlank() && toastLife > 0f;
        float toastAlpha = toastVisible ? Math.min(1f, toastLife * 4f) : 0f;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        panel(batch, 354f, objectiveY, 572f, 58f, objectiveAlpha, UiTheme.AMBER);
        panel(batch, 18f, lowerY, 246f, 68f, vitalsAlpha, UiTheme.CYAN);
        panel(batch, 940f, lowerY, 322f, 52f, inventoryAlpha, UiTheme.CYAN_DIM);
        if (toastVisible) {
            float width = Math.min(590f, Math.max(250f, message.length() * 9.2f));
            float height = 44f + Interpolation.swingOut.apply(toastKick) * 4f;
            panel(batch, 640f - width / 2f, 108f, width, height, .92f * toastAlpha, UiTheme.GREEN);
        }
        bar(batch, 78f, lowerY + 39f, 132f, 9f, player.getOxigenio() / 100f,
            player.getOxigenio() <= 25f ? UiTheme.RED : UiTheme.CYAN, vitalsAlpha);
        bar(batch, 78f, lowerY + 17f, 132f, 8f, player.getEnergia() / 100f, UiTheme.AMBER, vitalsAlpha);
        batch.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        centered(batch, mission.getObjective(player.getOxigenio()), 1.02f, UiTheme.TEXT, 640f,
            objectiveY + 36f, objectiveAlpha);
        text(batch, "O2", .8f, UiTheme.TEXT_MUTED, 34f, lowerY + 51f, vitalsAlpha);
        text(batch, String.format("%.0f%%", player.getOxigenio()), .75f,
            player.getOxigenio() <= 25f ? UiTheme.RED : UiTheme.TEXT, 216f, lowerY + 49f, vitalsAlpha);
        text(batch, "EN", .8f, UiTheme.TEXT_MUTED, 34f, lowerY + 29f, vitalsAlpha);
        text(batch, String.format("%.0f%%", player.getEnergia()), .72f, UiTheme.TEXT, 216f,
            lowerY + 27f, vitalsAlpha);
        text(batch, "O2  " + player.getOxigenioColetado(), .8f, UiTheme.CYAN, 958f, lowerY + 33f, inventoryAlpha);
        text(batch, "COMIDA  " + player.getComidaColetada(), .8f, UiTheme.AMBER, 1035f, lowerY + 33f, inventoryAlpha);
        text(batch, "GELO  " + player.getGeloColetado(), .8f, UiTheme.TEXT, 1163f, lowerY + 33f, inventoryAlpha);
        if (toastVisible) {
            centered(batch, message, .82f, UiTheme.TEXT, 640f, 137f, toastAlpha);
        }
        batch.end();
    }

    private boolean near(float px, float py, float x, float y, float width, float height) {
        return px > x - 46f && px < x + width + 46f && py > y - 44f && py < y + height + 44f;
    }

    private void panel(SpriteBatch batch, float x, float y, float width, float height,
                       float alpha, Color accent) {
        panelPatch.setColor(new Color(accent.r * .42f + .58f, accent.g * .42f + .58f,
            accent.b * .42f + .58f, alpha));
        panelPatch.draw(batch, x, y, width, height);
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
