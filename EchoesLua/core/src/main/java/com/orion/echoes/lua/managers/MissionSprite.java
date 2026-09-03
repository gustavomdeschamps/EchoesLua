package com.orion.echoes.lua.managers;

/** Enderecos semanticos do atlas 4x4 de entidades da missao. */
public enum MissionSprite {
    PART_ANTENNA(0, 0),
    PART_ENERGY(1, 0),
    PART_EXTRACTION(2, 0),
    PART_GREENHOUSE(3, 0),
    WEAPON_A(0, 1),
    WEAPON_B(1, 1),
    WEAPON_C(2, 1),
    ENEMY(3, 1),
    STATION_COMMUNICATION(0, 2),
    STATION_ENERGY(1, 2),
    STATION_EXTRACTION(2, 2),
    STATION_GREENHOUSE(3, 2),
    PORTAL_OFFLINE(0, 3),
    PORTAL_ONLINE(1, 3),
    MARS_BEACON(2, 3),
    CRAFTING_TERMINAL(3, 3);

    private final int column;
    private final int row;

    MissionSprite(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public int column() { return column; }
    public int row() { return row; }
}
