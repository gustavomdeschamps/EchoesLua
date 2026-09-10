package com.orion.echoes.lua.managers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorldIntroAssetTest {
    private Path textures() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve("assets/textures");
            if (Files.isDirectory(candidate)) return candidate;
            directory = directory.getParent();
        }
        throw new AssertionError("Production textures not found from test working directory");
    }

    @Test void everyWorldHasExactWidescreenKeyArt() throws IOException {
        for (String world : new String[] {"lunar", "mars", "titan"}) {
            Path path = textures().resolve("world_intro_" + world + "_v1.png");
            BufferedImage image = ImageIO.read(path.toFile());
            assertNotNull(image, path.toString());
            assertEquals(1280, image.getWidth(), path.toString());
            assertEquals(720, image.getHeight(), path.toString());
        }
    }
}
