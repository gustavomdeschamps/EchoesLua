package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import com.orion.echoes.lua.physics.PhysicsWorld;

public class Obstacle {

    private final Vector2 position;
    private final Sprite sprite;
    private final Rectangle bounds;
    private final Body body;

    private final float width;
    private final float height;

    public Obstacle(
        float x,
        float y,
        float width,
        float height,
        Texture texture,
        PhysicsWorld physicsWorld
    ) {
        this.position = new Vector2(x, y);
        this.width = width;
        this.height = height;

        sprite = new Sprite(texture);
        sprite.setSize(width, height);
        sprite.setPosition(x, y);

        // hitbox menor que a arte
        float hitboxWidth = width * 0.70f;
        float hitboxHeight = height * 0.70f;

        float hitboxX = x + (width - hitboxWidth) / 2f;
        float hitboxY = y + (height - hitboxHeight) / 2f;

        bounds = new Rectangle(
            hitboxX,
            hitboxY,
            hitboxWidth,
            hitboxHeight
        );

        body = physicsWorld.createStaticBody(
            hitboxX + hitboxWidth / 2f,
            hitboxY + hitboxHeight / 2f,
            hitboxWidth,
            hitboxHeight,
            "OBSTACLE"
        );
    }

    public Obstacle(
        float x,
        float y,
        Texture texture,
        PhysicsWorld physicsWorld
    ) {
        this(x, y, 64f, 64f, texture, physicsWorld);
    }

    public void render(SpriteBatch batch) {
        sprite.draw(batch);
    }

    public Vector2 getPosition() {
        return position;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public Body getBody() {
        return body;
    }

    public void dispose() {
        // textura pertence ao AssetManager
    }
}
