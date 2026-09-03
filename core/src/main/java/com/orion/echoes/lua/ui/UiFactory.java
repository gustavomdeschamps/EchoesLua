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
        skin.add("default", new Label.LabelStyle(assets.font, UiTheme.TEXT));
        skin.add("muted", new Label.LabelStyle(assets.font, UiTheme.TEXT_MUTED));
        skin.add("title", new Label.LabelStyle(assets.titleFont, UiTheme.TEXT));
        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.up = patch(assets.uiButtonNormalTexture, 18);
        button.over = patch(assets.uiButtonHoverTexture, 18);
        button.down = patch(assets.uiButtonPressedTexture, 18);
        button.disabled = patch(assets.uiButtonDisabledTexture, 18);
        button.font = assets.titleFont;
        button.fontColor = UiTheme.TEXT;
        button.overFontColor = UiTheme.CYAN;
        button.downFontColor = UiTheme.AMBER;
        button.disabledFontColor = UiTheme.TEXT_MUTED;
        skin.add("default", button);
        Slider.SliderStyle slider = new Slider.SliderStyle();
        slider.background = patch(assets.uiBarTrackTexture, 8);
        slider.knobBefore = patch(assets.uiBarFillTexture, 8);
        TextureRegionDrawable knob = new TextureRegionDrawable(assets.uiButtonPressedTexture);
        knob.setMinSize(20f, 28f);
        slider.knob = knob;
        skin.add("default-horizontal", slider);
        return skin;
    }
    private static NinePatchDrawable patch(com.badlogic.gdx.graphics.g2d.TextureRegion region, int split) {
        return new NinePatchDrawable(new NinePatch(region, split, split, split, split));
    }
}
