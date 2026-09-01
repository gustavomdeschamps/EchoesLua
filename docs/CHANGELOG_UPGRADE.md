# Changelog do upgrade de produção

Registro do que mudou por fase, com antes e depois. Cada item aponta o arquivo
onde a mudança pode ser verificada.

---

## Fase 1 — Fundação técnica

| Antes | Depois |
|---|---|
| `world.step(delta, 6, 2)` com delta cru do frame | Acumulador de timestep fixo consumindo `GameConfig.TIME_STEP` — `physics/PhysicsWorld.java` |
| `GameConfig.TIME_STEP`, `VELOCITY_ITERATIONS`, `POSITION_ITERATIONS` e `PPM` mortos | Todos em uso; `PhysicsWorld.PPM` eliminado, fonte única em `GameConfig.PPM` |
| `getAlpha()` exposto mas nunca consumido | Corpo do jogador acompanhado e desenhado na posição interpolada entre os dois últimos passos — `PhysicsWorld.getRenderPosition()` |
| `astronauta_sheet.png` 1254×1254 (não divide por 4) com recorte de 2 px escondendo a deriva | 1252×1252 exato, sem hack de margem; o pipeline falha se qualquer folha não dividir |
| Filtro `Nearest` no personagem, com serrilhado em downscale de 3× | `MipMapLinearLinear` / `Linear` via `pack.json` |
| 20 texturas soltas, até 20 binds por frame | `game.atlas`, `ui.atlas`, `fx.atlas` + carregamento assíncrono com `LoadingScreen` real |
| 18 WAVs, 1,5 MB | 18 OGGs, 192 KB |
| `OxygenSystem`, `IceProcessingSystem`, `CollectionSystem` como cascas de 4 linhas | Removidos; `CollectionSystem` voltou depois, implementado de verdade |
| `./gradlew build` falhava ao realizar `packGameAtlas` | Corrigido: `"$group.atlas"` era lido por Groovy como acesso à propriedade `group.atlas` |

## Fase 2 — Animação

| Antes | Depois |
|---|---|
| `TextureRegion frame = idleFrame;` — a célula (0,0) desenhada sempre | Máquina de estados `IDLE, WALK, RUN, DASH, ATTACK, HURT, DEAD` — `entities/Astronauta.java` |
| Seno procedural gerando bob e squash sobre uma pose parada | Removido; as 16 células da folha e a folha de combate 4×3 são realmente percorridas |
| Dash implementado sem nenhum quadro de dash | Linha dedicada na folha de movimento |
| Ilhas de alpha soltas nas folhas de inimigo | `_remove_alpha_islands()` no pipeline |

## Fase 3 — Direção de arte

| Antes | Depois |
|---|---|
| Paleta do `ART_BIBLE.md` divergente da de `UiTheme.java` em 5 papéis | Sincronizadas; `validate_palette_sync()` falha o build se divergirem de novo |
| Bahnschrift e Segoe UI (fontes de sistema Windows, licença incerta) | Chakra Petch Regular/SemiBold com SIL OFL 1.1 versionada |
| Terrenos com microtextura fotográfica | Repintados na linguagem cartoon, costura validada por `validate_terrain_seams()` |

## Fase 4 — Game feel

| Antes | Depois |
|---|---|
| `hitStopTimer`, `cameraShakeTimer` e `damageFlashTimer` soltos dentro da tela | `systems/JuiceSystem.java` com 7 presets nomeados |
| Shake linear | Trauma decrescente ao quadrado — `JuiceSystem.java:71` |
| Follow simples de câmera | `systems/CameraDirector.java` com lookahead, resposta crítica e zoom contextual de combate |
| Lua e Marte com a mesma resposta de movimento | Perfis distintos: `PLAYER_LUNAR_ACCEL_TIME` vs `PLAYER_MARS_ACCEL_TIME` |

## Fase 5 — UI e HUD

| Antes | Depois |
|---|---|
| `ui_panel_frame.png` 96×96 gerado por código como único asset de UI | Kit autoral: painéis HUD/diálogo/modal, botões nos 4 estados, ícones, moldura de barra, vinheta, marcador e cursor |
| `ShapeRenderer` em 4 telas, alternando com `SpriteBatch` várias vezes por frame | Zero `ShapeRenderer` no projeto; tudo em textura no mesmo batch |
| Menus como retângulos hardcoded | Scene2D com skin sobre o `ui.atlas` |
| Sem tela de opções | Volume por barramento, escala de HUD, shake, modo daltônico, tela cheia — `screens/MenuScreen.java` |
| Cursor do sistema | Cursor autoral que muda de forma sobre alvo — `render/MissionOverlay.java` |

## Fase 6 — Áudio

| Antes | Depois |
|---|---|
| Lua e Marte jogadas em silêncio, só SFX | Trilha adaptativa em 3 camadas por mundo — `managers/MusicDirector.java` |
| Sliders de música/efeitos/interface salvavam preferência mas não mexiam em nada | `SoundManager.applySettings()` liga os barramentos ao `AppSettings` |
| Um único `volumeGeral` | Barramentos MUSIC, SFX, UI e AMBIENT |
| Todo som centralizado e no mesmo volume | `tocarEspacial()` com atenuação por distância e pan pelo lado da tela |
| Sem ducking | Stingers de reparo, craft e alerta abaixam a trilha |
| `tocarVariado()` usado só em passo e disparo | Rodízio de pitch em todo SFX repetitivo |
| Sem tratamento de vácuo | Som externo na Lua chega atenuado e mais grave |

Trilha gerada por `tools/generate_music.py`: 6 loops OGG de 32 s, 1,1 MB no
total, com frequências múltiplas do fundamental do loop para fechar sem clique.

## Fase 7 — Conteúdo e design

| Antes | Depois |
|---|---|
| Reparar só contava para destravar o portal | Cada sistema dá um benefício distinto e cumulativo — `systems/MissionState.java` |
| 3 hostis idênticos com 3 HP | Perseguidor, emboscador e atirador, com pulso desviável — `entities/Enemy.java`, `entities/EnemyPulse.java` |
| Layout inteiramente fixo | Layout por semente com validação de alcance por flood-fill — `world/LunarWorld.java`, `world/ReachabilityGrid.java` |
| Sem indicação de para onde ir | Marcador direcional preso à borda, liberado ao reparar a Comunicação |
| Tutorial por texto | O primeiro hostil aparece isolado e no comportamento mais simples |

Benefícios por sistema:

- **Comunicação** → objetivos marcados no visor;
- **Energia** → base recarrega o traje em dobro;
- **Extração** → cada rocha de gelo rende o dobro;
- **Estufa** → oxigênio volta sozinho em campo aberto.

## Fase 8 — Arquitetura e QA

| Antes | Depois |
|---|---|
| `LunarScreen` com 1.742 linhas concentrando tudo | 476 linhas, apenas orquestração |
| — | `world/LunarWorld`, `world/ReachabilityGrid`, `render/WorldRenderer`, `render/MissionOverlay`, `render/PauseOverlay`, `systems/CombatSystem`, `systems/CollectionSystem`, `systems/InteractionSystem`, `systems/FeedbackSystem`, `save/LunarCheckpoint` |
| Zero testes | 31 testes JUnit 5 cobrindo `MissionState`, `SaveManager`, `EventBus` e `ReachabilityGrid` |
| Sem CI | GitHub Actions rodando QA de assets, testes e build a cada push |
| Pipeline só transformava assets | 5 validações que falham o build: grade, movimento entre quadros, paleta, costura de terreno e escala de célula |

---

## Prova final — as 7 melhorias da campanha

Requisitos do enunciado do Módulo 07 (Aula 09), verificados um a um.

| # | Requisito | Antes | Depois |
|---|---|---|---|
| 01 | **Quest** com progressão clara | Só texto de objetivo | Passo numerado `QUEST n/7` com título próprio no HUD — `MissionState.getQuestStep()` |
| 02 | **Combate com munição** | Tiro infinito | Rifle com carga limitada, recarregado por gelo processado e células marcianas — `Astronauta.consumirMunicao()` |
| 03 | **Dois tipos de inimigo** | 3 hostis idênticos | Perseguidor, emboscador e atirador na Lua; drone e crawler em Marte |
| 04 | **Save da campanha** | Só a fase lunar | `GameSaveData` v3 grava fase, semente, munição e progresso marciano; F5/F9 funcionam nas duas fases |
| 05 | **HUD fixa** | Sem munição nem progresso | Barra e contador de munição, alerta em vermelho, passo da quest — nas duas fases |
| 06 | **Portal bidirecional** | Só ida | Portal de volta em Marte; a semente reconstrói a mesma Lua com o mesmo progresso |
| 07 | **Vitória** | Existia | Mantida: 3 estações online, hostis neutralizados e chegada à plataforma |

### Como a ida e volta funciona

`CampaignState` é o estado que atravessa o portal nos dois sentidos: semente,
fase atual, vitais, munição, progresso lunar e marciano. Voltar de Marte não
recomeça a Lua — a semente reproduz o layout e `LunarCheckpoint.syncWorld()`
remove do chão o que já foi coletado e mantém abatidos os hostis já derrotados.

O mesmo objeto é o que o save grava, então persistência e portal usam um
caminho só. Saves da versão 2 continuam carregando: os campos novos ficam nos
valores padrão.

### Munição como decisão

O rifle nasce com 12 células e o teto é 30. Cada rocha de gelo processada na
base rende 4 (8 com a Extração reparada) e cada célula de energia marciana
rende 6. Ficar sem munição em Marte é motivo real para voltar pelo portal —
é o que dá função à ida e volta em vez de deixá-la decorativa.
