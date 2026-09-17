import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.orion.echoes.lua.entities.*;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.render.*;

/** Real GPU captures using production atlas offsets, filters and player renderer. */
public final class AssetVisualCapture extends ApplicationAdapter {
    private AssetManager assets;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private PhysicsWorld physics;
    private Astronauta player;
    private int capture;
    public static void main(String[] args) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("ECHOES asset verification");
        config.setWindowedMode(1280,720);
        config.setInitialVisible(false);
        config.disableAudio(true);
        config.setForegroundFPS(30);
        new Lwjgl3Application(new AssetVisualCapture(),config);
    }
    @Override public void create() {
        assets = new AssetManager(); assets.queue();
        batch = new SpriteBatch();
        camera = new OrthographicCamera(1280,720);
        camera.position.set(640,360,0); camera.update();
    }
    private void sprite(TextureRegion region,float x,float y,float w,float h) {
        SpriteFit.draw(batch,region,x,y,w,h);
    }
    @Override public void render() {
        if (!assets.update()) return;
        if (physics == null) {
            physics = new PhysicsWorld();
            player = new Astronauta(580,270,assets,physics);
            player.setWeaponEquipped(true);
        }
        if (capture >= 19) { Gdx.app.exit(); return; }
        ScreenUtils.clear(.045f,.06f,.075f,1);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (capture < 3) scene(capture);
        else gallery((capture-3)/4,(capture-3)%4);
        batch.end();
        Pixmap pixels = ScreenUtils.getFrameBufferPixmap(0,0,1280,720);
        var output = Gdx.files.local("../build/asset-qa/runtime/capture-"+String.format("%02d",capture)+".png");
        output.parent().mkdirs();
        PixmapIO.writePNG(output,pixels,-1,true); pixels.dispose();
        System.out.println("Captured "+output.path());
        capture++;
    }
    private void scene(int world) {
        Texture ground = world==0 ? assets.backgroundLuaTexture : world==1
            ? assets.marsBackgroundTexture : assets.titanBackgroundTexture;
        TerrainRenderer.draw(batch,ground,1280,720);
        if (world==0) {
            sprite(assets.baseLunarTexture,780,355,420,350);
            for(int i=0;i<3;i++) sprite(assets.repairStationFrame(i,2),55+i*245,420,190,190);
            sprite(assets.portalFrame(2,1),920,60,230,230);
        } else if(world==1) {
            sprite(assets.marsRegion(0,0),780,355,420,350);
            var kinds = new MarsObject.Kind[]{MarsObject.Kind.SOLAR_STATION,MarsObject.Kind.OXYGEN_STATION,MarsObject.Kind.COMMS_STATION};
            for(int i=0;i<3;i++) sprite(assets.marsStationFrame(kinds[i],2),55+i*245,420,190,190);
            sprite(assets.portalFrame(2,1),920,60,230,230);
        } else {
            sprite(assets.titanRefineryFrame(2),80,425,240,220);
            sprite(assets.titanPortalFrame(2,1),925,380,240,240);
            sprite(assets.titanBossFrame(0,0),740,150,270,270);
        }
        for(int i=0;i<6;i++) {
            var rock = world==0 ? assets.lunarObstacleRegion(i) : world==1
                ? assets.marsObstacleRegion(i) : assets.titanFormationRegion(i);
            sprite(rock,80+i*180,(i%2)*70+40,120,120);
        }
        sprite(assets.oxigenioTexture,370,270,32,48);
        sprite(assets.comidaTexture,425,290,38,30);
        sprite(assets.geloTexture,470,270,38,38);
        sprite(world==0?assets.lunarEnemyFrame(1,1):world==1?assets.marsEnemyFrame(false,1,1)
            :assets.titanEnemyFrame(1,1),740,280,110,110);
        sprite(assets.npcVisualFrame(world==0?Npc.Visual.AYLA:world==1?Npc.Visual.MARS_OFFICER:Npc.Visual.LIRA,0,0),
            280,275,105,105);
        player.setAimDirection(world==1?-1:1,0); player.render(batch);
        assets.titleFont.setColor(Color.WHITE);
        assets.titleFont.draw(batch,new String[]{"LUA","MARTE","TITÃ"}[world],40,690);
    }
    private void gallery(int row,int column) {
        TextureRegion[] frames = {assets.astronautFrame(column,row),
            assets.astronautCombatFrame(column,Math.min(row,2)),assets.lunarEnemyFrame(column,row),
            assets.marsEnemyFrame(true,column,row),assets.marsEnemyFrame(false,column,row),
            assets.titanEnemyFrame(column,row),assets.titanBossFrame(column,row),
            assets.npcVisualFrame(Npc.Visual.LIRA,column,row),assets.repairStationFrame(row,column),
            assets.portalFrame(column,row),assets.titanPortalFrame(column,Math.min(row,1)),
            assets.titanRefineryFrame(column)};
        for(int i=0;i<frames.length;i++) {
            float x=20+(i%6)*210, y=i<6?375:80;
            sprite(frames[i],x,y,190,190);
            var flipped = new TextureAtlas.AtlasRegion((TextureAtlas.AtlasRegion)frames[i]);
            flipped.flip(true,false);
            sprite(flipped,x+125,y,70,70);
            batch.setColor(.2f,.6f,.5f,1f);
            batch.draw(assets.uiWhiteTexture,x,y+190*.11f,190,1);
            batch.setColor(Color.WHITE);
        }
        assets.font.setColor(Color.WHITE);
        assets.font.draw(batch,"Linha "+row+" | Quadro "+column+" | original e espelhado",40,690);
    }
    @Override public void dispose() {
        if(physics!=null) physics.dispose();
        if(batch!=null) batch.dispose();
        if(assets!=null) assets.dispose();
    }
}
