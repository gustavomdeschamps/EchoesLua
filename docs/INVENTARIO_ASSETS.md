# Inventário de assets

**Gerado por `tools/generate_asset_inventory.py`. Não edite à mão.**

Cada linha é lida de três fontes reais: o `AssetManager`, para saber de
qual atlas a textura sai e qual método entrega os quadros dela; o PNG em
disco, para medir grade, célula, quadros e margem; e o resto de
`core/src/main`, para descobrir quem chama aquele método.

A última coluna é a que importa para decidir uso: o documento de arte diz
que nome de arquivo não decide se um asset está em uso, e sim a busca por
referência. Um método sem chamador é um asset que o jogo carrega e
ninguém desenha.

`Margem` é a **menor** folga transparente entre o conteúdo e a borda,
considerando todas as células da folha e os dois eixos. É um número mais
severo que a folga média que `tools/audit_spritesheets.py` reporta, e os
dois medem coisas diferentes de propósito: aquele descreve a folha, este
encontra a célula mais apertada dela.

O contrato de sprites pede 18% para personagens e NPCs e 16% para estações
compactas. Vários valores abaixo aparecem aqui, e isso **não** significa
quadro cortado: a auditoria confirma que nenhum conteúdo encosta na borda
da célula em folha nenhuma. Significa que a folga é mais apertada que a
meta na célula mais cheia. Fechar essa diferença não é reescala mecânica:
encolher o conteúdo dentro da célula encolheria o sprite em jogo, porque o
tamanho de desenho é o tamanho da célula, e desalinharia as hitboxes
derivadas dele. É trabalho de refazer a fonte com mais respiro, não de
reprocessar o que existe.

## Personagem

| Região | Atlas | Acessor | Grade | Quadros | Célula | Margem | Alpha | Desenhado em |
|---|---|---|---|---|---|---|---|---|
| `astronaut_combat_sheet` | game | `astronautCombatFrame()` | 4x3 | 12 | 313x313 | 11% | sim | `Astronauta` |
| `astronauta_sheet` | game | `astronautFrame()` | 4x4 | 16 | 313x313 | 11% | sim | `Astronauta` |

## NPCs

| Região | Atlas | Acessor | Grade | Quadros | Célula | Margem | Alpha | Desenhado em |
|---|---|---|---|---|---|---|---|---|
| `npc_colony_officer_sheet_v2` | game | `npcCommanderFrame()` | 4x4 | 16 | 313x313 | 11% | sim | `DialogBox` |

## Inimigos e chefe

| Região | Atlas | Acessor | Grade | Quadros | Célula | Margem | Alpha | Desenhado em |
|---|---|---|---|---|---|---|---|---|
| `lunar_enemy_sheet` | game | `lunarEnemyFrame()` | 4x4 | 16 | 313x313 | 11% | sim | `Enemy` |
| `titan_boss_sheet_v3` | game | `titanBossFrame()` | 4x4 | 16 | 313x313 | 7% | sim | `TitanBoss` |
| `titan_hunter_sheet_v3` | game | `titanEnemyFrame()` | 4x4 | 16 | 313x313 | 4% | sim | `TitanEnemy` |

## Estações e refinaria

| Região | Atlas | Acessor | Grade | Quadros | Célula | Margem | Alpha | Desenhado em |
|---|---|---|---|---|---|---|---|---|
| `mars_station_sheet_v2` | game (opcional) | `marsStationFrame()` | 4x3 | 12 | 313x313 | 9% | sim | `MarsObject`, `SpriteQaScreen` |
| `lunar_repair_stations_v2` | game | `repairStationFrame()` | 4x4 | 16 | 313x313 | 7% | sim | `RepairStation`, `SpriteQaScreen` |
| `titan_refinery_sheet_v2` | game (opcional) | `titanRefineryFrame()` | 4x1 | 4 | 313x313 | 16% | sim | `TitanScreen` |

## Portais

| Região | Atlas | Acessor | Grade | Quadros | Célula | Margem | Alpha | Desenhado em |
|---|---|---|---|---|---|---|---|---|
| `campaign_portal_sheet_v2` | game | `portalFrame()` | 4x4 | 16 | 313x313 | 4% | sim | `Portal` |
| `titan_portal_vertical_v2` | game | `titanPortalFrame()` | 4x2 | 8 | 313x313 | 10% | sim | `Portal` |

## Itens e missão

| Região | Atlas | Acessor | Grade | Quadros | Célula | Margem | Alpha | Desenhado em |
|---|---|---|---|---|---|---|---|---|
| `mission_atlas_unified` | game | `missionRegion()` | 4x4 | 16 | 313x313 | 9% | sim | `CraftingStation`, `MissionCollectible`, `TitanScreen` |
| `resource_icons` | ui | `resourceIcon()` | 4x1 | 4 | — | — | — | `Hud`, `TitanScreen` |

## Obstáculos e terreno

| Região | Atlas | Acessor | Grade | Quadros | Célula | Margem | Alpha | Desenhado em |
|---|---|---|---|---|---|---|---|---|
| `landmarks` | game | `landmarkRegion()` | 4x2 | 8 | 313x313 | 5% | sim | `MarsScreen`, `TitanScreen`, `WorldRenderer` |
| `lunar_obstacles` | game | `lunarObstacleRegion()` | 3x2 | 6 | 313x313 | 6% | sim | `LunarWorld` |
| `mars_obstacles` | game | `marsObstacleRegion()` | 3x2 | 6 | 313x313 | 6% | sim | `MarsObject` |
| `titan_formations_v2` | game | `titanFormationRegion()` | 3x2 | 6 | 313x313 | 9% | sim | `TitanScreen` |

## VFX

| Região | Atlas | Acessor | Grade | Quadros | Célula | Margem | Alpha | Desenhado em |
|---|---|---|---|---|---|---|---|---|
| `action_fx_sheet` | fx | `actionFxFrame()` | 6x4 | 24 | 256x256 | 5% | sim | `ParticleManager` |
| `energy_fx_sheet` | fx | `energyFxFrame()` | 6x4 | 24 | 256x256 | 7% | sim | `CombatSystem`, `EnemyPulse`, `MarsScreen`, `ParticleManager`, `TitanScreen` |

## Outros

| Região | Atlas | Acessor | Grade | Quadros | Célula | Margem | Alpha | Desenhado em |
|---|---|---|---|---|---|---|---|---|
| `mars_atlas_v4` | game | `marsRegion()` | 4x3 | 12 | 313x313 | 5% | sim | `MarsObject` |
| `?` | ? | `npcVisualFrame()` | 4x4 | 16 | — | — | — | `Npc`, `SpriteQaScreen` |

## Acessores sem chamador

Nenhum. Todo acessor de textura tem pelo menos um chamador.
