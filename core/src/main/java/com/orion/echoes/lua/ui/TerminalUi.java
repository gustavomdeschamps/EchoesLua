package com.orion.echoes.lua.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;

/** Componente compartilhado pelas telas de menu, pausa e resultado. */
public class TerminalUi implements Disposable {
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final GlyphLayout layout = new GlyphLayout();
    private final BitmapFont font;
    private final BitmapFont titleFont;
    private final SpriteBatch batch;

    public TerminalUi(SpriteBatch batch, AssetManager assets) {
        this.batch = batch;
        this.font = assets.font;
        this.titleFont = assets.titleFont;
        viewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
        camera.position.set(GameConfig.WINDOW_WIDTH / 2f, GameConfig.WINDOW_HEIGHT / 2f, 0f);
        camera.update();
    }

    public void clear(Color color) {
        Gdx.gl.glClearColor(color.r, color.g, color.b, color.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    public void beginShapes() {
        shapes.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    public void endShapes() {
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void panel(float x, float y, float width, float height, Color accent) {
        shapes.setColor(0f, 0f, 0f, .42f);
        shapes.rect(x + 5f, y - 5f, width, height);
        shapes.setColor(UiTheme.SURFACE_STRONG);
        shapes.rect(x, y, width, height);
        // Marcas de calibração nos cantos substituem a barra lateral genérica.
        shapes.setColor(accent);
        shapes.rect(x + 12f, y + height - 3f, 54f, 3f);
        shapes.rect(x + 12f, y + height - 15f, 3f, 12f);
        shapes.rect(x + width - 28f, y, 16f, 3f);
        shapes.rect(x + width - 15f, y, 3f, 12f);
    }

    public void rect(float x, float y, float width, float height, Color color) {
        shapes.setColor(color);
        shapes.rect(x, y, width, height);
    }

    public void beginText() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
    }

    public void image(Texture texture, float x, float y, float width, float height, Color tint) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(tint);
        batch.draw(texture, x, y, width, height);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    public void region(TextureRegion region, float x, float y, float width, float height, Color tint) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(tint);
        batch.draw(region, x, y, width, height);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    public Vector2 unproject(Vector2 point) {
        return viewport.unproject(point);
    }

    public void endText() { batch.end(); }

    public void text(String value, float scale, Color color, float x, float y) {
        font.getData().setScale(scale);
        font.setColor(color);
        font.draw(batch, value, x, y);
    }

    public void centered(String value, float scale, Color color, float centerX, float y) {
        font.getData().setScale(scale);
        font.setColor(color);
        layout.setText(font, value);
        font.draw(batch, layout, centerX - layout.width / 2f, y);
    }

    public void title(String value, float scale, Color color, float x, float y) {
        titleFont.getData().setScale(scale);
        titleFont.setColor(color);
        titleFont.draw(batch, value, x, y);
    }

    public void centeredTitle(String value, float scale, Color color, float centerX, float y) {
        titleFont.getData().setScale(scale);
        titleFont.setColor(color);
        layout.setText(titleFont, value);
        titleFont.draw(batch, layout, centerX - layout.width / 2f, y);
    }

    public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void dispose() { shapes.dispose(); }
}
