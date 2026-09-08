package com.orion.echoes.lua.managers;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Checks shipped atlas metadata without requiring an OpenGL context. */
class ProductionAtlasTest {
    private Path assets() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve("assets/atlases/game.atlas");
            if (Files.isRegularFile(candidate)) return directory.resolve("assets/atlases");
            directory = directory.getParent();
        }
        throw new AssertionError("Production assets not found from test working directory");
    }

    private TextureAtlasData atlas(String name) {
        Path directory = assets();
        return new TextureAtlasData(new FileHandle(directory.resolve(name + ".atlas").toFile()),
            new FileHandle(directory.toFile()), false);
    }

    @Test void productionPagesExistAndUseNonMipmapFiltering() {
        for (String name : new String[] {"game", "ui", "fx"}) {
            TextureAtlasData data = atlas(name);
            assertEquals(1, data.getPages().size, name + " must fit one page");
            for (TextureAtlasData.Page page : data.getPages()) {
                assertTrue(page.textureFile.exists(), page.textureFile.toString());
                assertEquals(TextureFilter.Linear, page.minFilter);
                assertEquals(TextureFilter.Linear, page.magFilter);
            }
        }
    }

    @Test void everyRegionFitsItsPageWithoutRotationOrDuplicateIndex() {
        for (String name : new String[] {"game", "ui", "fx"}) {
            Set<String> keys = new HashSet<>();
            for (TextureAtlasData.Region region : atlas(name).getRegions()) {
                assertTrue(keys.add(region.name + ":" + region.index), region.name);
                assertFalse(region.rotate, region.name);
                assertTrue(region.left >= 0 && region.top >= 0, region.name);
                assertTrue(region.width > 0 && region.height > 0, region.name);
                assertTrue(region.left + region.width <= region.page.width, region.name);
                assertTrue(region.top + region.height <= region.page.height, region.name);
            }
        }
    }

    @Test void animationGroupsContainEveryIndividuallyPaddedFrame() {
        Map<String, Integer> expected = Map.of(
            "astronauta_sheet", 16, "astronaut_combat_sheet", 12,
            "lunar_enemy_sheet", 16, "mars_drone_sheet", 16,
            "mars_crawler_sheet", 16, "titan_hunter_sheet_v3", 16,
            "titan_boss_sheet_v3", 16, "campaign_portal_sheet_v2", 16,
            "titan_portal_vertical_v2", 8, "npc_commander_ayla_sheet", 16);
        TextureAtlasData data = atlas("game");
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            Set<Integer> indices = new HashSet<>();
            for (TextureAtlasData.Region region : data.getRegions()) {
                if (region.name.equals(entry.getKey())) indices.add(region.index);
            }
            assertEquals(entry.getValue().intValue(), indices.size(), entry.getKey());
            for (int index = 0; index < entry.getValue(); index++) {
                assertTrue(indices.contains(index), entry.getKey() + " missing " + index);
            }
        }
    }
}
