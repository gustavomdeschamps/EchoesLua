# Echoes — Integração Lua → Marte

Jogo 2D em Java 17 com LibGDX 1.14.2. A missão começa na Lua: o astronauta
administra oxigênio e energia, restaura os sistemas da colônia e atravessa o
portal para Marte.

```
Menu → Intro → Fase Lunar ⇄ Portal ⇄ Fase Marciana → Vitória
```

O portal é **bidirecional**: dá para voltar à Lua a qualquer momento para
reabastecer oxigênio e munição. A Lua é reconstruída pela mesma semente, com o
progresso preservado.

## Como jogar

1. Colete as quatro peças de reparo e as partes A, B e C da arma.
2. Repare as estações com `E`. **Cada sistema reparado dá um ganho diferente**,
   então a ordem é uma decisão sua:
   - **Comunicação** → objetivos passam a aparecer marcados no visor;
   - **Energia** → a base recarrega o traje em dobro;
   - **Extração** → cada rocha de gelo rende o dobro;
   - **Estufa** → o traje volta a gerar oxigênio sozinho em campo aberto.
3. Monte a arma na bancada dentro da base.
4. Neutralize os hostis. São três comportamentos distintos:
   - **perseguidor** avança em linha reta de longe;
   - **emboscador** (violeta) fica parado até você encostar;
   - **atirador** (âmbar) recua e dispara pulsos — dá para desviar.
5. O portal abre com 3 sistemas online, arma montada, hostis eliminados e O2
   acima de 25%.
6. Em Marte, reative as três estações com núcleos marcianos, neutralize os
   hostis e alcance a plataforma de extração.
7. Ficou sem munição ou oxigênio? Use o portal ao lado do ponto de chegada
   para voltar à Lua, processar gelo na base e retornar.

### Munição

O rifle tem carga limitada — 12 células ao ser fabricado, teto de 30. Recarrega
processando gelo na base lunar (4 por rocha, 8 com a Extração reparada) e
recolhendo células de energia em Marte (6 cada). O HUD mostra o contador e a
barra, que ficam vermelhos quando a carga está baixa.

### Controles

| Tecla | Ação |
|---|---|
| `WASD` / setas | mover |
| `Shift` | correr (gasta mais energia) |
| `Espaço` | dash (gasta energia) |
| Mouse | mirar · botão esquerdo dispara |
| `E` | reparar, montar, processar gelo, usar o portal |
| `F5` / `F9` | salvar e carregar a campanha (nas duas fases) |
| `Esc` | pausar · `M` volta ao menu |

## Build e execução

Requer **JDK 17** e **Python 3.11+** (o build valida os assets antes de compilar).

```bash
pip install pillow numpy soundfile
```

**Linux e macOS**

```bash
./gradlew lwjgl3:run      # rodar
./gradlew build           # compilar e testar
./gradlew :core:test      # só os testes
```

**Windows**

```powershell
.\gradlew.bat lwjgl3:run
.\gradlew.bat build
```

### Tarefas de asset

```bash
python tools/prepare_visual_assets.py                 # regenera sprites e kit de UI
python tools/prepare_visual_assets.py --validate-only # só o QA (é o que o build roda)
python tools/generate_music.py                        # regera as camadas de trilha
./gradlew packVisualAssets                            # reempacota os atlas
```

O QA de assets falha o build se uma folha não dividir pela grade, se dois quadros
consecutivos forem quase idênticos, se a paleta divergir do `ART_BIBLE.md`, se a
costura de um terreno abrir ou se uma célula de atlas fugir da régua de escala.

## Arquitetura

```
core/src/main/java/com/orion/echoes/lua/
├── config/      GameConfig (constantes) · AppSettings (preferências)
├── entities/    Astronauta, Enemy, EnemyPulse, Item, Portal, ...
├── events/      EventBus / EventType / GameEvent  (Observer)
├── factories/   MissionEntityFactory              (Factory)
├── managers/    AssetManager, SoundManager, MusicDirector, ParticleManager
├── physics/     PhysicsWorld (timestep fixo + interpolação de render)
├── render/      WorldRenderer, MissionOverlay, PauseOverlay
├── save/        SaveManager, GameSaveData, LunarCheckpoint
├── screens/     LunarScreen, MarsScreen, MenuScreen, LoadingScreen, Hud, ...
├── systems/     MissionState, CampaignState, JuiceSystem, CameraDirector,
│                CombatSystem, CollectionSystem, InteractionSystem, FeedbackSystem
├── ui/          UiTheme, UiFactory, TerminalUi
└── world/       LunarWorld (layout por semente), ReachabilityGrid (flood-fill)
```

- **`MissionState`** centraliza inventário, reparos, arma, hostis, a regra do
  portal, os benefícios passivos e os passos da quest.
- **`CampaignState`** é o estado que atravessa o portal nos dois sentidos e o
  que o save grava: semente, fase, vitais, munição e progresso das duas fases.
- **`LunarScreen`** apenas orquestra a ordem do frame — construção, combate,
  coleta, interação e render moram nos sistemas acima.
- **`PhysicsWorld`** roda Box2D em timestep fixo e devolve a posição
  interpolada para o render, o que elimina o micro-stutter.
- **`MusicDirector`** mantém três camadas por mundo em fase e só cruza volumes.

## Documentação

- [`docs/ART_BIBLE.md`](docs/ART_BIBLE.md) — direção visual e sonora, fonte única
  de verdade da paleta.
- [`docs/CHANGELOG_UPGRADE.md`](docs/CHANGELOG_UPGRADE.md) — o que mudou por fase.
- [`docs/TECHNICAL_DEBT.md`](docs/TECHNICAL_DEBT.md) — o que ficou de fora e por quê.

## Créditos

- **Código e arte** — projeto Echoes.
- **Tipografia** — [Chakra Petch](https://fonts.google.com/specimen/Chakra+Petch),
  SIL Open Font License 1.1 (`assets/fonts/OFL-ChakraPetch.txt`).
- **Trilha** — sintetizada por `tools/generate_music.py` (NumPy + libsndfile).
- **Engine** — [libGDX](https://libgdx.com) 1.14.2, LWJGL3.
