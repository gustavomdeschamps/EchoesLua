package com.orion.echoes.lua.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import com.orion.echoes.lua.ui.HudLabel;
import com.orion.echoes.lua.ui.UiTheme;

/** Interface de campo inspirada na telemetria do traje, sem cobrir o centro da acao. */
public final class Hud implements Disposable {
    private static final float OBJECTIVE_X = 24f, OBJECTIVE_Y = 642f;
    private static final float OBJECTIVE_W = 620f, OBJECTIVE_H = 62f;
    private static final float VITALS_X = 24f, VITALS_Y = 18f;
    private static final float VITALS_W = 318f, VITALS_H = 94f;
    private static final float CARGO_X = 1006f, CARGO_Y = 18f;
    private static final float CARGO_W = 250f, CARGO_H = 76f;
    private static final float TOAST_Y = 126f, TOAST_SCALE = .78f;
    private static final float TOAST_MAX_TEXT_WIDTH = 520f;
    private static final float TOAST_PADDING_X = 24f, TOAST_PADDING_Y = 14f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final NinePatch panelPatch;
    private final com.orion.echoes.lua.render.GameplayStatusHud statusHud;
    private final TextureRegion barTrack, barFill, white;
    private final TextureRegion[] resourceIcons = new TextureRegion[3];
    private final Color tint = new Color();
    private final HudLabel oxygenLabel = new HudLabel("", "%");
    private final HudLabel energyLabel = new HudLabel("", "%");
    private final HudLabel ammoLabel = new HudLabel("", "");
    private final HudLabel oxygenStock = new HudLabel("", "");
    private final HudLabel foodStock = new HudLabel("", "");
    private final HudLabel iceStock = new HudLabel("", "");

    private String previousMessage = "";
    private float toastWidth, toastHeight, toastLife, toastKick, entrance;

    public Hud(AssetManager assets) {
        viewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
        camera.position.set(GameConfig.WINDOW_WIDTH / 2f, GameConfig.WINDOW_HEIGHT / 2f, 0f);
        camera.update();
        font = assets.font;
        panelPatch = assets.uiPanelPatch();
        statusHud = new com.orion.echoes.lua.render.GameplayStatusHud(assets);
        barTrack = assets.uiBarTrackTexture;
        barFill = assets.uiBarFillTexture;
        white = assets.uiWhiteTexture;
        for (int i = 0; i < resourceIcons.length; i++) resourceIcons[i] = assets.resourceIcon(i);
    }

    public void update(float delta, String message) {
        entrance = Math.min(1f, entrance + delta / .36f);
        String safe = message == null ? "" : message.strip();
        if (!safe.equals(previousMessage)) {
            previousMessage = safe;
            toastLife = safe.isEmpty() ? 0f : 1f;
            toastKick = safe.isEmpty() ? 0f : 1f;
            measureToast(safe);
        }
        toastLife = Math.max(0f, toastLife - delta / 3.1f);
        toastKick = Math.max(0f, toastKick - delta / .2f);
    }

    public void render(SpriteBatch batch, Astronauta player, MissionState mission, String message,
                       float playerScreenX, float playerScreenY) {
        float eased = Interpolation.pow3Out.apply(entrance);
        float objectiveY = OBJECTIVE_Y + (1f - eased) * 18f;
        float lowerY = VITALS_Y - (1f - eased) * 18f;
        float objectiveAlpha = overlap(playerScreenX, playerScreenY,
            OBJECTIVE_X, objectiveY, OBJECTIVE_W, OBJECTIVE_H) ? .58f : .94f;
        float vitalsAlpha = overlap(playerScreenX, playerScreenY,
            VITALS_X, lowerY, VITALS_W, VITALS_H) ? .58f : .94f;
        float cargoAlpha = overlap(playerScreenX, playerScreenY,
            CARGO_X, lowerY, CARGO_W, CARGO_H) ? .58f : .94f;
        boolean showToast = message != null && !message.isBlank() && toastLife > 0f;
        float toastAlpha = showToast ? Math.min(1f, toastLife * 4f) : 0f;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        panel(batch, OBJECTIVE_X, objectiveY, OBJECTIVE_W, OBJECTIVE_H, objectiveAlpha);
        statusHud.render(batch,player);

        text(batch, "MISSÃO  " + mission.getQuestStep(player.getOxigenio()) + "/"
            + MissionState.QUEST_TOTAL_STEPS, .57f, UiTheme.AMBER,
            OBJECTIVE_X + 31f, objectiveY + 50f, objectiveAlpha);
        text(batch, mission.getObjective(player.getOxigenio()), .77f, UiTheme.TEXT,
            OBJECTIVE_X + 15f, objectiveY + 25f, objectiveAlpha);


        if (showToast) drawToast(batch, message, toastAlpha);
        batch.setColor(Color.WHITE);
        font.getData().setScale(1f);
        batch.end();
    }

    private void vital(SpriteBatch batch, String name, String value, float ratio, Color color,
                       float x, float y, float alpha) {
        text(batch, name, .52f, UiTheme.TEXT_MUTED, x, y + 10f, alpha);
        rightText(batch, value, .62f, color, x + 286f, y + 10f, alpha);
        bar(batch, x + 75f, y + 2f, 164f, 7f, ratio, color, alpha);
    }

    private void cargo(SpriteBatch batch, TextureRegion icon, String value,
                       float x, float y, Color color, float alpha) {
        batch.setColor(1f, 1f, 1f, alpha);
        com.orion.echoes.lua.render.SpriteFit.draw(batch, icon, x, y, 34f, 34f);
        text(batch, value, .72f, color, x + 42f, y + 24f, alpha);
    }

    private void panel(SpriteBatch batch, float x, float y, float width, float height, float alpha) {
        tint.set(0f, 0f, 0f, .28f * alpha);
        panelPatch.setColor(tint);
        panelPatch.draw(batch, x + 3f, y - 4f, width, height);
        tint.set(1f, 1f, 1f, alpha);
        panelPatch.setColor(tint);
        panelPatch.draw(batch, x, y, width, height);
        panelPatch.setColor(Color.WHITE);
        screw(batch, x + 9f, y + 8f, alpha);
        screw(batch, x + width - 12f, y + height - 11f, alpha);
    }

    private void screw(SpriteBatch batch, float x, float y, float alpha) {
        batch.setColor(UiTheme.BORDER.r, UiTheme.BORDER.g, UiTheme.BORDER.b, alpha * .7f);
        batch.draw(white, x, y, 3f, 3f);
        batch.setColor(Color.WHITE);
    }

    private void statusMark(SpriteBatch batch, float x, float y, Color color, float alpha) {
        batch.setColor(color.r, color.g, color.b, alpha);
        batch.draw(white, x, y, 8f, 8f);
        batch.setColor(Color.WHITE);
    }

    private void bar(SpriteBatch batch, float x, float y, float width, float height,
                     float ratio, Color color, float alpha) {
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(barTrack, x, y, width, height);
        batch.setColor(color.r, color.g, color.b, alpha);
        batch.draw(barFill, x, y, width * MathUtils.clamp(ratio, 0f, 1f), height);
        batch.setColor(Color.WHITE);
    }

    private void measureToast(String value) {
        if (value.isEmpty()) {
            toastWidth = toastHeight = 0f;
            return;
        }
        font.getData().setScale(TOAST_SCALE);
        layout.setText(font, value, UiTheme.TEXT, TOAST_MAX_TEXT_WIDTH, Align.center, true);
        toastWidth = Math.min(TOAST_MAX_TEXT_WIDTH, Math.max(230f, layout.width)) + TOAST_PADDING_X * 2f;
        toastHeight = layout.height + TOAST_PADDING_Y * 2f;
        font.getData().setScale(1f);
    }

    private void drawToast(SpriteBatch batch, String value, float alpha) {
        float kick = Interpolation.swingOut.apply(toastKick) * 5f;
        float x = GameConfig.WINDOW_WIDTH / 2f - toastWidth / 2f;
        panel(batch, x, TOAST_Y - kick, toastWidth, toastHeight, .92f * alpha);
        font.getData().setScale(TOAST_SCALE);
        font.setColor(UiTheme.TEXT.r, UiTheme.TEXT.g, UiTheme.TEXT.b, alpha);
        float textWidth = toastWidth - TOAST_PADDING_X * 2f;
        layout.setText(font, value, font.getColor(), textWidth, Align.center, true);
        font.draw(batch, layout, x + TOAST_PADDING_X,
            TOAST_Y - kick + toastHeight - TOAST_PADDING_Y);
    }

    private boolean overlap(float px, float py, float x, float y, float width, float height) {
        return px > x - 36f && px < x + width + 36f && py > y - 36f && py < y + height + 36f;
    }

    private void text(SpriteBatch batch, String value, float scale, Color color,
                      float x, float y, float alpha) {
        font.getData().setScale(scale);
        font.setColor(color.r, color.g, color.b, alpha);
        font.draw(batch, value, x, y);
    }

    private void rightText(SpriteBatch batch, String value, float scale, Color color,
                           float right, float y, float alpha) {
        font.getData().setScale(scale);
        font.setColor(color.r, color.g, color.b, alpha);
        layout.setText(font, value);
        font.draw(batch, layout, right - layout.width, y);
    }

    public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void dispose() { }
}
