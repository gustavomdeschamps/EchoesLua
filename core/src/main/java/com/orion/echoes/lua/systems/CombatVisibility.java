package com.orion.echoes.lua.systems;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
/** Solid cover blocks contact attacks as well as movement. */
public final class CombatVisibility {
    private CombatVisibility() { }
    public static boolean blocked(float x,float y,float tx,float ty,Array<Rectangle> solids) {
        for(Rectangle solid:solids) if(Intersector.intersectSegmentRectangle(x,y,tx,ty,solid))return true;
        return false;
    }

    /** Primeiro contato ao longo de um raio unitário; infinito quando não há contato. */
    public static float firstHit(float x, float y, float dx, float dy,
                                 float maxDistance, Rectangle box) {
        float entry = 0f, exit = maxDistance;
        if (Math.abs(dx) < .00001f) {
            if (x < box.x || x > box.x + box.width) return Float.POSITIVE_INFINITY;
        } else {
            float a = (box.x - x) / dx, b = (box.x + box.width - x) / dx;
            entry = Math.max(entry, Math.min(a, b));
            exit = Math.min(exit, Math.max(a, b));
        }
        if (Math.abs(dy) < .00001f) {
            if (y < box.y || y > box.y + box.height) return Float.POSITIVE_INFINITY;
        } else {
            float a = (box.y - y) / dy, b = (box.y + box.height - y) / dy;
            entry = Math.max(entry, Math.min(a, b));
            exit = Math.min(exit, Math.max(a, b));
        }
        return exit >= entry ? entry : Float.POSITIVE_INFINITY;
    }

    public static float firstSolidHit(float x, float y, float dx, float dy,
                                      float maxDistance, Array<Rectangle> solids) {
        float nearest = Float.POSITIVE_INFINITY;
        for (Rectangle solid : solids)
            nearest = Math.min(nearest, firstHit(x, y, dx, dy, maxDistance, solid));
        return nearest;
    }
}
