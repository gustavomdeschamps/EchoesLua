package com.orion.echoes.lua.systems;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.orion.echoes.lua.physics.PhysicsWorld;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SolidCollisionRegistryTest {
    @Test void enemiesUseExactSameRectangleAsStaticPhysicsBody() {
        GdxNativesLoader.load();Box2D.init();var physics=new PhysicsWorld();
        try {
            var body=physics.createStaticBody(100,80,92,34,"REPAIR_STATION");
            var rect=physics.getSolidBounds().first();
            assertEquals(54,rect.x);assertEquals(63,rect.y);
            assertEquals(92,rect.width);assertEquals(34,rect.height);
            physics.destroyBody(body);assertEquals(0,physics.getSolidBounds().size);
        } finally {physics.dispose();}
    }
    @Test void pickupSensorsAreNotSolidWallsForEnemies() {
        GdxNativesLoader.load();Box2D.init();var physics=new PhysicsWorld();
        try {
            physics.createSensorBody(100,80,32,32,"PICKUP");
            assertEquals(0,physics.getSolidBounds().size);
        } finally {physics.dispose();}
    }
}
