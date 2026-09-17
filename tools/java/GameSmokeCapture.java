import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.ScreenUtils;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.screens.*;

/** Visits the actual three gameplay Screens without changing production startup. */
public final class GameSmokeCapture extends EchoesLua {
    private int world = -1;
    private int ticks;
    private int uiView;
    public static void main(String[] args) {
        System.setProperty("echoes.windowed","true");
        var config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280,720);
        config.setInitialVisible(false);
        config.disableAudio(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new GameSmokeCapture(),config);
    }
    @Override public void render() {
        super.render();
        if(!getAssets().isReady() || getScreen() instanceof LoadingScreen) return;
        if (uiView < 3) {
            if (ticks == 0) {
                Screen previous = getScreen();
                setScreen(uiView == 0 ? new IntroScreen(this) : new MenuScreen(this));
                previous.dispose();
                if (uiView == 2) {
                    try {
                        var field = MenuScreen.class.getDeclaredField("stage");
                        field.setAccessible(true);
                        var stage = (com.badlogic.gdx.scenes.scene2d.Stage)field.get(getScreen());
                        stage.act(.5f);
                        hoverFirstButton(stage.getRoot());
                    } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
                }
            }
            ticks++;
            if (ticks >= 100) {
                Pixmap pixels = ScreenUtils.getFrameBufferPixmap(0,0,1280,720);
                var file = Gdx.files.local("../build/asset-qa/runtime/ui-clean-"+uiView+".png");
                file.parent().mkdirs(); PixmapIO.writePNG(file,pixels,-1,true); pixels.dispose();
                uiView++; ticks=0;
            }
            return;
        }
        if(world < 0 || ticks >= 90) {
            if(world >= 0) {
                Pixmap pixels = ScreenUtils.getFrameBufferPixmap(0,0,1280,720);
                var file = Gdx.files.local("../build/asset-qa/runtime/game-world-"+world+".png");
                file.parent().mkdirs(); PixmapIO.writePNG(file,pixels,-1,true); pixels.dispose();
                System.out.println("Gameplay screen captured: "+world);
                if(world==0) captureConversationsAndPause();
                if(world==2) captureBossSequence();
            }
            world++; ticks=0;
            if(world==3) captureExpansion();
            if(world==3) {Gdx.app.exit();return;}
            Screen previous = getScreen();
            setScreen(world==0 ? new LunarScreen(this,getBatch(),getAssets())
                : world==1 ? new MarsScreen(this,getCampaign()) : new TitanScreen(this,getCampaign()));
            previous.dispose();
        }
        ticks++;
    }

    private void captureExpansion() {
        var campaign=getCampaign();
        campaign.fromLunarArray(new int[]{0,0,0,0,0,0,0,1,1,1,1,1,4,4});
        campaign.setAmmo(30);campaign.setVitals(100,100);
        campaign.setMarsProgress(0,3,4,true);campaign.setDialogoTita(true);campaign.setCombateOk(true);
        var oldInput=Gdx.input;
        var keys=new java.util.HashSet<Integer>();
        var proxy=(com.badlogic.gdx.Input)java.lang.reflect.Proxy.newProxyInstance(
            com.badlogic.gdx.Input.class.getClassLoader(),new Class[]{com.badlogic.gdx.Input.class},
            (object,method,args)->method.getName().equals("isKeyJustPressed")
                ? keys.contains((Integer)args[0]) : method.invoke(oldInput,args));
        Gdx.input=proxy;
        try {
            Screen[] scenes={new PhaseBossScreen(this,campaign,false),new PhaseBossScreen(this,campaign,true),
                new CallistoScreen(this,campaign),new AharinScreen(this,campaign),new VictoryScreen(this,160f),new EndingScreen(this,campaign)};
            for(int i=0;i<scenes.length;i++) {
                if(i==2)campaign.getInventario().add(com.orion.echoes.lua.systems.Inventario.CHAVE_TITA);
                if(i==3)campaign.getInventario().add(com.orion.echoes.lua.systems.Inventario.CHAVE_LUZ);
                Screen previous=getScreen();setScreen(scenes[i]);previous.dispose();
                for(int tick=0;tick<90;tick++)scenes[i].render(1f/60f);
                saveOverlay("expansion-"+i);
                if(i<4){keys.add(com.badlogic.gdx.Input.Keys.I);scenes[i].render(1f/60f);keys.clear();saveOverlay("inventory-"+i);
                    keys.add(com.badlogic.gdx.Input.Keys.I);scenes[i].render(1f/60f);keys.clear();}
                if(i==2) {
                    var field=scenes[i].getClass().getSuperclass().getDeclaredField("boss");field.setAccessible(true);
                    var boss=(com.orion.echoes.lua.entities.BossCalisto)field.get(scenes[i]);
                    for(int form=2;form<=3;form++) {
                        boss.receiveDamage(10000f);boss.update(1.3f);scenes[i].render(1f/60f);saveOverlay("callisto-form-"+form);
                    }
                    boss.receiveDamage(10000f);scenes[i].render(1f/60f);
                    if(!campaign.getInventario().tem(com.orion.echoes.lua.systems.Inventario.CHAVE_LUZ))throw new AssertionError("No earned light key");
                }
                if(i==3) {
                    var field=AharinScreen.class.getDeclaredField("player");field.setAccessible(true);
                    var player=(com.orion.echoes.lua.entities.Astronauta)field.get(scenes[i]);
                    player.getBody().setTransform(700f/com.orion.echoes.lua.config.GameConfig.PPM,
                        260f/com.orion.echoes.lua.config.GameConfig.PPM,0f);
                    var physicsField=AharinScreen.class.getDeclaredField("physics");physicsField.setAccessible(true);
                    ((com.orion.echoes.lua.physics.PhysicsWorld)physicsField.get(scenes[i])).untrackForRender(player.getBody());
                    player.update(0f);
                    keys.add(com.badlogic.gdx.Input.Keys.E);scenes[i].render(0f);keys.clear();
                    for(int line=0;line<3;line++){keys.add(com.badlogic.gdx.Input.Keys.SPACE);scenes[i].render(1f/60f);keys.clear();}
                    if(!(getScreen() instanceof EndingScreen))throw new AssertionError("Aharin did not start ending");
                }
            }
            verifyAimIndependence();
        } catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        finally {Gdx.input=oldInput;}
    }
    private void verifyAimIndependence() {
        var physics=new com.orion.echoes.lua.physics.PhysicsWorld();
        var player=new com.orion.echoes.lua.entities.Astronauta(300,250,getAssets(),physics);
        try {
            player.setWeaponEquipped(true);player.move(-1,0,false,1f/60f);player.setAimDirection(1,0);
            if(!player.isViradoEsquerda()||player.getBody().getLinearVelocity().x>=0)throw new AssertionError("Mouse changed left locomotion");
            player.move(1,0,false,1f/60f);player.setAimDirection(-1,0);
            if(player.isViradoEsquerda()||player.getBody().getLinearVelocity().x<=0)throw new AssertionError("Mouse changed right locomotion");
            System.out.println("Armed locomotion independent of aim: passed");
        } finally {player.dispose();physics.dispose();}
    }
    private boolean hoverFirstButton(com.badlogic.gdx.scenes.scene2d.Group group) {
        for (var actor : group.getChildren()) {
            if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.TextButton) {
                var event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
                event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.enter);
                event.setPointer(-1); actor.fire(event); return true;
            }
            if (actor instanceof com.badlogic.gdx.scenes.scene2d.Group child && hoverFirstButton(child)) return true;
        }
        return false;
    }

    private void captureConversationsAndPause() {
        var camera = new com.badlogic.gdx.graphics.OrthographicCamera(1280,720);
        camera.position.set(640,360,0); camera.update();
        var portraits = new com.badlogic.gdx.graphics.g2d.TextureRegion[]{
            getAssets().npcCommanderFrame(0,0),
            getAssets().npcVisualFrame(com.orion.echoes.lua.entities.Npc.Visual.MARS_OFFICER,0,0),
            getAssets().npcVisualFrame(com.orion.echoes.lua.entities.Npc.Visual.LIRA,0,0)};
        String[] names={"Ayla","Ayyub","Lira"};
        var dialog = new com.orion.echoes.lua.systems.DialogueController();
        dialog.start(new String[]{"Esta colônia mantém o enlace entre a Terra e nossas expedições. Sem as antenas, ninguém ouve um pedido de resgate."});
        var box = new com.orion.echoes.lua.render.DialogBox(getBatch(),getAssets());
        box.update(1,true);
        for(int i=0;i<3;i++) {
            getScreen().render(0); getBatch().setProjectionMatrix(camera.combined);
            getBatch().begin(); box.render(dialog,names[i],portraits[i]); getBatch().end();
            saveOverlay("dialog-clean-"+i);
        }
        getScreen().render(0);
        var pause = new com.orion.echoes.lua.render.PauseOverlay(getBatch(),getAssets());
        pause.resize(1280,720); pause.open(); pause.setReduceMotion(true);
        pause.render(com.orion.echoes.lua.ui.UiTheme.CYAN,"ECHOES · LUA","Reative três sistemas e abra o portal.",
            "Sua expedição aguarda.",new String[]{"O2","ENERGIA","MUNIÇÃO"},new String[]{"89%","90%","4"});
        saveOverlay("pause-clean"); pause.dispose();
    }
    private void saveOverlay(String name) {
        Pixmap pixels=ScreenUtils.getFrameBufferPixmap(0,0,1280,720);
        PixmapIO.writePNG(Gdx.files.local("../build/asset-qa/runtime/"+name+".png"),pixels,-1,true);
        pixels.dispose();
    }
    private void captureBossSequence() {
        var physics=new com.orion.echoes.lua.physics.PhysicsWorld();
        var player=new com.orion.echoes.lua.entities.Astronauta(660,350,getAssets(),physics);
        var boss=new com.orion.echoes.lua.entities.TitanBoss(470,300,getAssets());
        var camera=new com.badlogic.gdx.graphics.OrthographicCamera(1280,720);
        camera.position.set(640,360,0);camera.update();
        try {
            for(int i=0;i<160;i++) {
                boss.update(1f/60f,player,1280,720,physics.getSolidBounds());
                if(i==0 || i==24 || i==48 || i==64 || i==84 || i==110 || i==159) {
                    Gdx.gl.glClearColor(.025f,.04f,.055f,1);
                    Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);
                    getBatch().setProjectionMatrix(camera.combined);getBatch().begin();
                    boss.render(getBatch());player.render(getBatch());getBatch().end();
                    saveOverlay("boss-motion-"+i);
                }
            }
        } finally {boss.dispose();player.dispose();physics.dispose();}
    }
}
