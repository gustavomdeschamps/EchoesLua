package com.orion.echoes.lua.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.ObjectMap;
import com.orion.echoes.lua.config.GameConfig;

public class PhysicsWorld {

    /*
     * O jogo é visto de cima.
     *
     * Por isso deixamos o World sem gravidade no eixo Y.
     * A gravidade real da Lua continua disponível caso
     * seja usada depois em alguma mecânica.
     */
    public static final float GRAVITY_LUA = -1.62f;

    private final World world;
    private final ObjectMap<Body, Vector2> previousPositions = new ObjectMap<>();
    private float accumulator;

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
        accumulator += Math.min(delta, GameConfig.MAX_FRAME_DELTA);
        while (accumulator >= GameConfig.TIME_STEP) {
            /*
             * O estado anterior e guardado antes de cada passo. Como o
             * acumulador quase nunca fecha exatamente no fim do frame, e a
             * interpolacao entre esses dois estados que tira o micro-stutter
             * de um timestep fixo desenhado num frame rate variavel.
             */
            for (ObjectMap.Entry<Body, Vector2> entry : previousPositions) {
                entry.value.set(entry.key.getPosition());
            }
            world.step(GameConfig.TIME_STEP,
                GameConfig.VELOCITY_ITERATIONS,
                GameConfig.POSITION_ITERATIONS);
            accumulator -= GameConfig.TIME_STEP;
        }
    }

    /** Fracao de passo ainda nao simulada, em 0..1. */
    public float getAlpha() {
        return accumulator / GameConfig.TIME_STEP;
    }

    /** Passa a acompanhar este corpo para poder interpolar o render dele. */
    public void trackForRender(Body body) {
        if (body == null || previousPositions.containsKey(body)) return;
        previousPositions.put(body, new Vector2(body.getPosition()));
    }

    public void untrackForRender(Body body) {
        previousPositions.remove(body);
    }

    /**
     * Posicao de desenho do corpo: mistura o estado anterior com o atual pela
     * fracao de passo pendente. Corpos nao acompanhados voltam a posicao crua.
     */
    public Vector2 getRenderPosition(Body body, Vector2 out) {
        Vector2 current = body.getPosition();
        Vector2 previous = previousPositions.get(body);
        if (previous == null) return out.set(current);
        float alpha = getAlpha();
        return out.set(previous.x + (current.x - previous.x) * alpha,
            previous.y + (current.y - previous.y) * alpha);
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
            x / GameConfig.PPM,
            y / GameConfig.PPM
        );

        bodyDef.fixedRotation = true;

        Body body =
            world.createBody(bodyDef);

        PolygonShape shape =
            new PolygonShape();

        shape.setAsBox(
            (width / 2f) / GameConfig.PPM,
            (height / 2f) / GameConfig.PPM
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
            x / GameConfig.PPM,
            y / GameConfig.PPM
        );

        Body body =
            world.createBody(bodyDef);

        PolygonShape shape =
            new PolygonShape();

        shape.setAsBox(
            (width / 2f) / GameConfig.PPM,
            (height / 2f) / GameConfig.PPM
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
            x / GameConfig.PPM,
            y / GameConfig.PPM
        );

        Body body =
            world.createBody(bodyDef);

        PolygonShape shape =
            new PolygonShape();

        shape.setAsBox(
            (width / 2f) / GameConfig.PPM,
            (height / 2f) / GameConfig.PPM
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
