package com.orion.echoes.lua.managers;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Art regressions that compilation cannot detect: opaque cells, trim bleed and loose pivots. */
class ReworkAssetIntegrityTest {
    private static final String[] ACTORS = {"astronauta_sheet", "astronaut_combat_sheet",
        "lunar_enemy_sheet", "mars_drone_sheet", "mars_crawler_sheet", "titan_hunter_sheet_v3",
        "titan_boss_sheet_v3", "npc_commander_ayla_sheet", "npc_colony_officer_sheet_v2",
        "npc_researcher_lira_sheet_v2"};
    private static Path assets() {
        return Files.isDirectory(Path.of("assets")) ? Path.of("assets") : Path.of("../assets");
    }
    @Test void everyActorFrameHasClearMarginsAndStableFeet() throws Exception {
        for (String name : ACTORS) {
            var image = ImageIO.read(assets().resolve("textures/"+name+".png").toFile());
            int rows = name.equals("astronaut_combat_sheet") ? 3 : 4;
            assertEquals(1252,image.getWidth(),name);
            assertEquals(rows*313,image.getHeight(),name);
            for(int row=0;row<rows;row++) for(int col=0;col<4;col++) {
                int bottom=-1, left=313,right=0,top=313;
                for(int y=0;y<313;y++) for(int x=0;x<313;x++) {
                    if ((image.getRGB(col*313+x,row*313+y)>>>24) <= 10) continue;
                    bottom=Math.max(bottom,y); top=Math.min(top,y);
                    left=Math.min(left,x); right=Math.max(right,x);
                }
                String frame=name+"["+col+","+row+"]";
                assertTrue(left>=4 && right<309 && top>=4 && bottom<309,frame+" invades border");
                assertTrue(bottom>=276 && bottom<=280,frame+" unstable floor: "+bottom);
            }
        }
    }
    @Test void everyActorFrameIsPresentInProductionAtlas() throws Exception {
        String atlas=Files.readString(assets().resolve("atlases/game.atlas"));
        for(String name:ACTORS) {
            int expected=name.equals("astronaut_combat_sheet")?12:16;
            long count=atlas.lines().filter(line->line.equals(name)).count();
            assertEquals(expected,count,name+" missing packed frames");
        }
    }

    @Test void cursorsRemainCompactAndHotspotsFit() throws Exception {
        for (String name : new String[]{"cursor_default", "cursor_target"}) {
            var image = ImageIO.read(assets().resolve("textures/ui/"+name+".png").toFile());
            assertEquals(32,image.getWidth(),name);
            assertEquals(32,image.getHeight(),name);
            assertEquals(0,image.getRGB(0,0)>>>24,name+" opaque outer corner");
        }
    }

    @Test void uiSurfaceInteriorsHaveNoBakedInStrayPixels() throws Exception {
        for (String name : new String[]{"button_normal","button_hover","button_pressed",
            "button_disabled","panel_hud","panel_dialog","panel_modal"}) {
            var image = ImageIO.read(assets().resolve("textures/ui/"+name+".png").toFile());
            int expected=image.getRGB(image.getWidth()/2,image.getHeight()/2);
            for (int y=16;y<image.getHeight()-16;y++)
                for (int x=16;x<image.getWidth()-16;x++)
                    assertEquals(expected,image.getRGB(x,y),name+" stray pixel at "+x+","+y);
        }
    }
}
