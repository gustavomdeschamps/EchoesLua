package com.orion.echoes.lua.entities;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Body;

import com.orion.echoes.lua.physics.PhysicsWorld;

public class Wall {

    private final Rectangle bounds;

    private final Body body;

    public Wall(
        float x,
        float y,
        float width,
        float height,
        PhysicsWorld physicsWorld
    ) {

        bounds =
            new Rectangle(
                x,
                y,
                width,
                height
            );

        body =
            physicsWorld.createStaticBody(
                x + width / 2f,
                y + height / 2f,
                width,
                height,
                "WALL"
            );
    }

    public Rectangle getBounds() {

        return bounds;
    }

    public Body getBody() {

        return body;
    }
}
