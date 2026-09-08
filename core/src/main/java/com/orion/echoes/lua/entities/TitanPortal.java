package com.orion.echoes.lua.entities;

import com.orion.echoes.lua.managers.AssetManager;

/** Mantém o nome de domínio da fase, usando o mesmo portal animado da campanha. */
public final class TitanPortal extends Portal {
    public TitanPortal(float x, float y, AssetManager assets) {
        super(x, y, assets, true);
    }
}
