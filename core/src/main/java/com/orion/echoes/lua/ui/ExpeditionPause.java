package com.orion.echoes.lua.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.render.PauseOverlay;

/** New worlds use the established pause kit and settings instead of a second design. */
public final class ExpeditionPause {
    private final EchoesLua game;
    private final Astronauta player;
    private final PauseOverlay overlay;
    private boolean paused, menu;
    private final String[] labels={"OXIGÊNIO","ENERGIA","MUNIÇÃO"};
    private final String[] values=new String[3];
    public ExpeditionPause(EchoesLua game,Astronauta player) {
        this.game=game;this.player=player;overlay=new PauseOverlay(game.getBatch(),game.getAssets());
        overlay.setReduceMotion(game.getSettings().isReduceMotion());
        overlay.setSettings(new PauseSettingsModel()
            .addSlider("Música",new PauseSettingsModel.FloatAccessor(){
                public float get(){return game.getSettings().getMusicVolume();}
                public void set(float value){game.getSettings().setMusicVolume(value);game.aplicarPreferenciasDeAudio();}})
            .addSlider("Efeitos",new PauseSettingsModel.FloatAccessor(){
                public float get(){return game.getSettings().getSfxVolume();}
                public void set(float value){game.getSettings().setSfxVolume(value);game.aplicarPreferenciasDeAudio();}}));
    }
    public void handle() {
        if(paused) {
            boolean consumed=overlay.handlePauseKeys();
            if(overlay.consumeQuitRequested())Gdx.app.exit();
            if(overlay.consumeMenuRequested())menu=true;
            if(overlay.consumeResumeRequested()){paused=false;game.getSounds().tocarUnpause();return;}
            if(consumed)return;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)||paused&&Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            paused=!paused;player.getBody().setLinearVelocity(0f,0f);
            if(paused){overlay.open();game.getSounds().tocarPause();}else game.getSounds().tocarUnpause();
        }
    }
    public boolean isPaused(){return paused;}
    public void pause(){paused=true;overlay.open();}
    public boolean consumeMenu(){boolean result=menu;menu=false;return result;}
    public void render(){
        if(!paused)return;
        values[0]=(int)player.getOxigenio()+"%";values[1]=(int)player.getEnergia()+"%";values[2]=""+player.getMunicao();
        overlay.render(UiTheme.CYAN,"ECHOES · "+game.getCampaign().phaseToken(),game.getCampaign().missaoAtual(),
            "A expedição aguarda. Seu progresso permanece salvo.",labels,values);
    }
    public void resize(int w,int h){overlay.resize(w,h);}
    public void dispose(){overlay.dispose();}
}
