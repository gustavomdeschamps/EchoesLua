package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.ui.*;

/** PDF's documented fallback: four timed lines, fade, and an automatic return to Earth. */
public final class EndingScreen implements Screen {
    private static final String[] LINES={"O sinal deixou de ser um mistério.","Lua, Marte, Titã e Calisto voltaram a se ouvir.",
        "Da luz de Aharin, trouxemos conhecimento.","A Terra ainda pode escolher uma nova era."};
    private final EchoesLua game;
    private final CampaignState campaign;
    private final Color textColor=new Color();
    private TerminalUi ui;
    private float time;
    private boolean finished;
    public EndingScreen(EchoesLua game,CampaignState campaign){this.game=game;this.campaign=campaign;}
    @Override public void show(){ui=new TerminalUi(game.getBatch(),game.getAssets());game.getSounds().tocarMusicaMenu();}
    @Override public void render(float delta){
        if(finished)return;
        time+=Math.max(0f,delta);
        if(time>=10f){finished=true;game.setScreen(new VictoryScreen(game,campaign.getMissionTime()));dispose();return;}
        ui.clear(UiTheme.VOID);
        ui.image(game.getAssets().victoryReturnTexture,0f,0f,1280f,720f,new Color(.42f,.5f,.66f,1f));
        float fade=MathUtils.clamp(time/.8f,0f,1f)*MathUtils.clamp((10f-time)/.8f,0f,1f);
        int line=Math.min(3,(int)(time/2.5f));
        float local=time-line*2.5f;
        float opacity=fade*MathUtils.clamp(local/.35f,0f,1f)*MathUtils.clamp((2.5f-local)/.35f,0f,1f);
        ui.beginShapes();ui.rect(0f,0f,1280f,150f,new Color(0f,0f,0f,.85f));ui.endShapes();
        ui.beginText();ui.title("RETORNO À TERRA",1.25f,UiTheme.TEXT,72f,644f);
        ui.text(LINES[line],.86f,textColor.set(1f,1f,1f,opacity),72f,91f);ui.endText();
    }
    @Override public void resize(int w,int h){if(ui!=null)ui.resize(w,h);}
    @Override public void pause(){}
    @Override public void resume(){}
    @Override public void hide(){}
    @Override public void dispose(){if(ui!=null){ui.dispose();ui=null;}}
}
