# Echoes - Integracao Lua -> Marte

Jogo 2D em Java 17 com LibGDX 1.14.2. A missao comeca na Lua: o astronauta precisa administrar oxigenio e energia, coletar recursos e restaurar a colonia antes de atravessar o portal para Marte.

## Fluxo da missao

1. Colete as quatro pecas de reparo e as partes A, B e C da arma.
2. Aproxime-se das estacoes da base e pressione `E` para reparar comunicacao, energia, extracao e estufa.
3. Dentro da base, pressione `E` com as tres partes para fabricar a arma.
4. Use `ESPACO` perto dos inimigos para atacar. Cada inimigo possui 3 pontos de vida.
5. O portal libera quando pelo menos 3 sistemas estao reparados, a arma foi fabricada, os 3 hostis foram eliminados e o oxigenio esta acima de 25%.
6. Entre no portal com `E`; o oxigenio, a energia e o progresso de combate seguem para Marte.
7. Na fase de Marte, alcance o sinalizador ciano para concluir a missao.

As pecas de reparo usam as cores ciano (antena), amarela (energia), azul (extracao) e verde (estufa). As partes da arma usam vermelho, laranja e violeta. Estacoes vermelhas estao offline; estacoes verdes estao reparadas.

## Direcao visual

- `mission_atlas.png`: atlas 4x4 exclusivo com modulos, partes da arma, hostil, estacoes, portal, beacon e bancada.
- `mars_background.png`: terreno marciano jogavel com rota luminosa para a base.
- HUDs usam a mesma linguagem de telemetria: superficies grafite, informacao primaria branca, tecnologia ciano, energia ambar, sucesso verde e perigo vermelho.
- A janela desktop abre em 1280x720, a mesma resolucao logica dos `FitViewport`, evitando texto reduzido e letterbox desnecessario.

## Controles

- `WASD` ou setas: mover
- `E`: reparar, fabricar, processar gelo ou usar o portal
- `ESPACO`: atacar apos fabricar a arma
- `F5`: salvar checkpoint lunar
- `F9`: carregar checkpoint lunar
- `ESC`: pausar; na pausa, `M` volta ao menu

## Arquitetura

- `MissionState` centraliza inventario, reparos, arma, inimigos e a regra do portal (State).
- `EventBus` publica coletas, reparos, craft, eliminacoes e transicao (Observer).
- `MissionEntityFactory` concentra a criacao de pecas, estacoes, inimigos e portal (Factory).
- `ParticleManager` reutiliza efeitos por meio de `ParticleEffectPool`, evitando recargas e alocacoes em cada impacto.
- `LunarScreen` e `MarsScreen` mantem as fases separadas no mesmo executavel.
- Entidades especificas representam pecas, estacoes, hostis e portal.
- `SaveManager` serializa o astronauta e o progresso da missao em JSON via Preferences.

## Executar e validar

No Windows:

```powershell
.\gradlew.bat lwjgl3:run
```

Compilacao sem abrir janela:

```powershell
.\gradlew.bat build
```
