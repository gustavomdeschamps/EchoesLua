package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.render.AtlasSpriteFactory;
import com.orion.echoes.lua.render.SpriteFit;

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
        this(x, y, width, height, new TextureRegion(texture), physicsWorld);
    }

    public Obstacle(float x, float y, float width, float height,
                    TextureRegion region, PhysicsWorld physicsWorld) {
        this.position = new Vector2(x, y);
        Rectangle draw = SpriteFit.fit(region, x, y, width, height, new Rectangle());
        this.width = draw.width;
        this.height = draw.height;

        sprite = AtlasSpriteFactory.create(region);
        sprite.setSize(draw.width, draw.height);
        sprite.setPosition(draw.x, draw.y);
        sprite.setColor(.9f, .92f, .96f, 1f);
        shadow = AtlasSpriteFactory.create(region);
        shadow.setSize(draw.width + 12f, draw.height + 10f);
        shadow.setPosition(draw.x - 6f, draw.y - 8f);
        shadow.setColor(.02f, .025f, .035f, .72f);

        // Em perspectiva superior, a colisao pertence a base da rocha, nao ao topo da arte.
        float hitboxWidth = draw.width * 0.72f;
        float hitboxHeight = draw.height * 0.31f;

        float hitboxX = draw.x + (draw.width - hitboxWidth) / 2f;
        float hitboxY = draw.y + draw.height * 0.11f;

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
