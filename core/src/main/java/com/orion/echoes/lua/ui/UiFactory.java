package com.orion.echoes.lua.ui;

import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.orion.echoes.lua.managers.AssetManager;

/** Skin Scene2D construída exclusivamente com o ui.atlas de produção. */
public final class UiFactory {
    private UiFactory() { }
    public static Skin create(AssetManager assets) {
        Skin skin = new Skin();
        com.badlogic.gdx.graphics.g2d.BitmapFont copy = assets.createInterfaceFont(24, false);
        com.badlogic.gdx.graphics.g2d.BitmapFont heading = assets.createInterfaceFont(28, true);
        skin.add("copy-owned", copy);
        skin.add("heading-owned", heading);
        skin.add("default", new Label.LabelStyle(copy, UiTheme.TEXT));
        skin.add("muted", new Label.LabelStyle(copy, UiTheme.TEXT_MUTED));
        skin.add("title", new Label.LabelStyle(heading, UiTheme.TEXT));
        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.up = patch(assets.uiButtonNormalTexture, 18);
        button.over = patch(assets.uiButtonHoverTexture, 18);
        button.down = patch(assets.uiButtonPressedTexture, 18);
        button.disabled = patch(assets.uiButtonDisabledTexture, 18);
        button.font = copy;
        button.fontColor = UiTheme.TEXT;
        button.overFontColor = UiTheme.CYAN;
        button.downFontColor = UiTheme.AMBER;
        button.disabledFontColor = UiTheme.TEXT_MUTED;
        skin.add("default", button);
        Slider.SliderStyle slider = new Slider.SliderStyle();
        slider.background = solid(assets, UiTheme.TEXT_MUTED.cpy().mul(.35f), 4f, 6f);
        slider.knobBefore = solid(assets, UiTheme.CYAN, 4f, 6f);
        slider.knob = solid(assets, UiTheme.TEXT, 12f, 22f);
        slider.knobOver = solid(assets, UiTheme.CYAN, 12f, 22f);
        slider.knobDown = solid(assets, UiTheme.AMBER, 12f, 22f);
        skin.add("default-horizontal", slider);
        return skin;
    }
    private static NinePatchDrawable patch(com.badlogic.gdx.graphics.g2d.TextureRegion region, int split) {
        return new NinePatchDrawable(new NinePatch(region, split, split, split, split));
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable solid(AssetManager assets,
            com.badlogic.gdx.graphics.Color color, float width, float height) {
        com.badlogic.gdx.scenes.scene2d.utils.Drawable result =
            new TextureRegionDrawable(assets.uiWhiteTexture).tint(color);
        result.setMinWidth(width);
        result.setMinHeight(height);
        return result;
    }
}
