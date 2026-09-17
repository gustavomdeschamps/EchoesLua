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
}
