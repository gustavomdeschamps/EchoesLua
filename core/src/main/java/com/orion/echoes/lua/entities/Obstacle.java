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
    private final Sprite shadow;
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
        sprite.setColor(.9f, .92f, .96f, 1f);
        shadow = new Sprite(texture);
        shadow.setSize(width + 12f, height + 10f);
        shadow.setPosition(x - 6f, y - 8f);
        shadow.setColor(.02f, .025f, .035f, .72f);

        // Em perspectiva superior, a colisao pertence a base da rocha, nao ao topo da arte.
        float hitboxWidth = width * 0.72f;
        float hitboxHeight = height * 0.31f;

        float hitboxX = x + (width - hitboxWidth) / 2f;
        float hitboxY = y + height * 0.07f;

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
        shadow.draw(batch);
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
