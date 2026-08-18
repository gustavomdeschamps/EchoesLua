package com.orion.echoes.lua.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

public class PhysicsWorld {

    // Conversão pixels -> metros
    public static final float PPM = 32f;

    /*
     * O jogo é visto de cima.
     *
     * Por isso deixamos o World sem gravidade no eixo Y.
     * A gravidade real da Lua continua disponível caso
     * seja usada depois em alguma mecânica.
     */
    public static final float GRAVITY_LUA = -1.62f;

    private final World world;

    public PhysicsWorld() {

        world = new World(
            new Vector2(0, 0),
            true
        );

        world.setContactListener(
            new CollisionListener()
        );
    }

    // =====================================================
    // UPDATE
    // =====================================================

    public void update(float delta) {

        world.step(
            delta,
            6,
            2
        );
    }

    public World getWorld() {
        return world;
    }

    // =====================================================
    // CORPO DINÂMICO
    // PLAYER
    // =====================================================

    public Body createDynamicBody(
        float x,
        float y,
        float width,
        float height,
        Object userData
    ) {

        BodyDef bodyDef =
            new BodyDef();

        bodyDef.type =
            BodyDef.BodyType.DynamicBody;

        bodyDef.position.set(
            x / PPM,
            y / PPM
        );

        bodyDef.fixedRotation = true;

        Body body =
            world.createBody(bodyDef);

        PolygonShape shape =
            new PolygonShape();

        shape.setAsBox(
            (width / 2f) / PPM,
            (height / 2f) / PPM
        );

        FixtureDef fixtureDef =
            new FixtureDef();

        fixtureDef.shape =
            shape;

        fixtureDef.density =
            1f;

        fixtureDef.friction =
            0.4f;

        fixtureDef.restitution =
            0f;

        body.createFixture(
            fixtureDef
        );

        body.setUserData(
            userData
        );

        shape.dispose();

        return body;
    }

    // =====================================================
    // CORPO ESTÁTICO
    // PAREDES / BASE / OBSTÁCULOS
    // =====================================================

    public Body createStaticBody(
        float x,
        float y,
        float width,
        float height,
        Object userData
    ) {

        BodyDef bodyDef =
            new BodyDef();

        bodyDef.type =
            BodyDef.BodyType.StaticBody;

        bodyDef.position.set(
            x / PPM,
            y / PPM
        );

        Body body =
            world.createBody(bodyDef);

        PolygonShape shape =
            new PolygonShape();

        shape.setAsBox(
            (width / 2f) / PPM,
            (height / 2f) / PPM
        );

        FixtureDef fixtureDef =
            new FixtureDef();

        fixtureDef.shape =
            shape;

        fixtureDef.friction =
            0.6f;

        body.createFixture(
            fixtureDef
        );

        body.setUserData(
            userData
        );

        shape.dispose();

        return body;
    }

    // =====================================================
    // SENSOR
    // COLETÁVEIS / BASE / ÁREAS
    // =====================================================

    public Body createSensorBody(
        float x,
        float y,
        float width,
        float height,
        Object userData
    ) {

        BodyDef bodyDef =
            new BodyDef();

        bodyDef.type =
            BodyDef.BodyType.StaticBody;

        bodyDef.position.set(
            x / PPM,
            y / PPM
        );

        Body body =
            world.createBody(bodyDef);

        PolygonShape shape =
            new PolygonShape();

        shape.setAsBox(
            (width / 2f) / PPM,
            (height / 2f) / PPM
        );

        FixtureDef fixtureDef =
            new FixtureDef();

        fixtureDef.shape =
            shape;

        fixtureDef.isSensor =
            true;

        body.createFixture(
            fixtureDef
        );

        body.setUserData(
            userData
        );

        shape.dispose();

        return body;
    }

    // =====================================================
    // REMOVER BODY
    // =====================================================

    public void destroyBody(
        Body body
    ) {

        if (body != null) {

            world.destroyBody(
                body
            );
        }
    }

    // =====================================================
    // DISPOSE
    // =====================================================

    public void dispose() {

        world.dispose();
    }
}
