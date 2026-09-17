package com.orion.echoes.lua.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.ui.UiTheme;
import com.orion.echoes.lua.ui.HudLabel;

/** Identical vitals and resource layout in all three worlds; caller owns the batch. */
public final class GameplayStatusHud {
    private final AssetManager assets;
    private final NinePatch panel;
    private final HudLabel oxygen = new HudLabel("","%");
    private final HudLabel energy = new HudLabel("","%");
    private final HudLabel ammo = new HudLabel("","");
    private final HudLabel[] stock = {new HudLabel("",""),new HudLabel("",""),new HudLabel("","")};
    public GameplayStatusHud(AssetManager assets) { this.assets=assets; panel=assets.uiPanelPatch(); }
    public void render(SpriteBatch batch,Astronauta player) {
        batch.setColor(Color.WHITE);
        panel.draw(batch,24,18,318,94);
        panel.draw(batch,970,18,286,76);
        row(batch,"O2",oxygen.of(Math.round(player.getOxigenio())),player.getOxigenio()/100f,
            player.getOxigenio()<25?UiTheme.RED:UiTheme.CYAN,83);
        row(batch,"ENERGIA",energy.of(Math.round(player.getEnergia())),player.getEnergia()/100f,UiTheme.AMBER,57);
        row(batch,"MUNIÇÃO",ammo.of(player.getMunicao()),player.getMunicao()/(float)GameConfig.AMMO_MAX,
            player.getMunicao()<=GameConfig.AMMO_LOW?UiTheme.RED:UiTheme.GREEN,31);
        text(batch,"CARGA",.53f,UiTheme.TEXT_MUTED,986,79);
        for(int i=0;i<3;i++) {
            float x=986+i*86;
            batch.setColor(Color.WHITE);
            SpriteFit.draw(batch,assets.resourceIcon(i),x,30,34,34);
            int count=i==0?player.getOxigenioColetado():i==1?player.getComidaColetada():player.getGelo();
            text(batch,stock[i].of(count),.72f,i==0?UiTheme.CYAN:i==1?UiTheme.AMBER:UiTheme.TEXT,x+42,54);
        }
        batch.setColor(Color.WHITE); assets.font.setColor(Color.WHITE); assets.font.getData().setScale(1);
    }
    private void row(SpriteBatch batch,String label,String value,float ratio,Color color,float y) {
        text(batch,label,.52f,UiTheme.TEXT_MUTED,40,y+10);
        text(batch,value,.62f,color,290,y+10);
        batch.setColor(.08f,.15f,.2f,1); batch.draw(assets.uiWhiteTexture,115,y+2,164,6);
        batch.setColor(color); batch.draw(assets.uiWhiteTexture,115,y+2,164*Math.max(0,Math.min(1,ratio)),6);
    }
    private void text(SpriteBatch batch,String value,float scale,Color color,float x,float y) {
        batch.setColor(Color.WHITE); assets.font.getData().setScale(scale); assets.font.setColor(color);
        assets.font.draw(batch,value,x,y);
    }
}
