package com.orion.echoes.lua.factories;

import com.orion.echoes.lua.entities.Enemy;
import com.orion.echoes.lua.entities.CraftingStation;
import com.orion.echoes.lua.entities.MissionCollectible;
import com.orion.echoes.lua.entities.Portal;
import com.orion.echoes.lua.entities.RepairStation;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.systems.MissionState;
import com.orion.echoes.lua.physics.PhysicsWorld;

/** Centraliza a configuracao visual e a criacao das entidades de missao. */
public class MissionEntityFactory {
    private final AssetManager assets;
    private final PhysicsWorld physics;

    public MissionEntityFactory(AssetManager assets, PhysicsWorld physics) {
        this.assets = assets;
        this.physics = physics;
    }

    public MissionCollectible collectible(float x, float y, MissionState.PartType type) {
        return new MissionCollectible(x, y, type, assets);
    }

    public RepairStation station(float x, float y, MissionState.SystemType type) {
        return new RepairStation(x, y, type, assets, physics);
    }

    public Enemy enemy(float x, float y) {
        return new Enemy(x, y, assets);
    }

    public Enemy enemy(float x, float y, Enemy.Behavior behavior) {
        return new Enemy(x, y, behavior, assets);
    }

    public Portal portal(float x, float y) {
        return new Portal(x, y, assets);
    }

    public CraftingStation craftingStation(float x, float y) {
        return new CraftingStation(x, y, assets, physics);
    }
}
