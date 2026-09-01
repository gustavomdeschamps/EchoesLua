# Dívida técnica

O que ficou de fora do upgrade de produção, por quê, e o que seria preciso para
fechar cada ponto. A lista é honesta: nada aqui está "quase pronto".

---

## Assets

### 27 MB de texturas duplicadas na distribuição
`assets/textures/` continua com os PNGs originais **e** `assets/atlases/` traz os
mesmos sprites empacotados (32 MB). Só `lunar_ground.png` e `mars_ground.png`
ainda são carregados direto — os outros existem apenas como entrada do
TexturePacker, mas são distribuídos junto do jogo.

**Correção:** mover as fontes para `tools/source_textures/`, ajustar
`atlasGroups` em `build.gradle` e os caminhos de `prepare_visual_assets.py`.
Não foi feito porque move ~27 MB de binários no histórico do Git e merece um
commit isolado.

### `ui_panel_frame.png` continua no atlas
Placeholder de 96×96 gerado por código, hoje substituído por `panel_hud`. Segue
empacotado por compatibilidade com `uiPanelPatch()`. Remover exige varrer as
chamadas restantes.

### Atlas de jogo em duas páginas
`game.png` (4058×3774) mais `game2.png` (3780×2520). São 2 binds em vez de 1.
Reduzir exigiria diminuir a resolução das folhas — que é generosa demais para
1280×720, mas mexer nisso é decisão de arte, não de engenharia.

### Draw calls nunca medidos
A Fase 8 pedia medir antes e depois do atlas, alvo de menos de 20 por frame.
Não foi medido: exige rodar o jogo com `GLProfiler` numa máquina com display, e
este ambiente é headless.

---

## Arquitetura

### `MarsScreen` não foi refatorada
Continua com 598 linhas e duplica câmera, tiro, partículas, HUD e pausa da fase
lunar. A `AbstractMissionScreen` compartilhada entre os dois mundos não existe —
o que existe são os sistemas extraídos (`CombatSystem`, `FeedbackSystem`,
`WorldRenderer`), que hoje só a Lua usa.

**Correção:** generalizar `LunarWorld` para uma interface de mundo de missão e
fazer Marte consumir os mesmos sistemas. É a próxima refatoração natural.

### Marte não recebeu os ganhos da Lua
Sem benefícios de reparo, sem marcador de objetivo, sem cursor autoral, sem
comportamentos de hostil variados (drone e crawler diferem só em números).

### Interpolação de render só no jogador
`PhysicsWorld.trackForRender()` está genérico, mas só o corpo do astronauta é
registrado. Inimigos usam matemática manual de posição e `Rectangle.overlaps` —
eles atravessam paredes que o jogador respeita. Unificar tudo em Box2D era o
item 2.5 do plano e continua aberto.

---

## Animação

### Abordagem raster, não esquelética
Escolha registrada no `ART_BIBLE.md`: opção B. Spine resolveria consistência de
silhueta de forma estrutural e permitiria blending entre estados, mas o projeto
não tem arquivos-fonte de rig e a migração seria um trabalho de arte inteiro.

### Quatro quadros por ação
Suficiente para leitura, abaixo do ideal para corrida e ataque. Aumentar exige
regerar as folhas a partir do turnaround.

---

## Áudio

### Trilha é sintetizada, não composta
`tools/generate_music.py` produz pads e pulsos corretos e sem emenda, mas é
música procedural, não composição. Serve como base de produção; um compositor
substituiria os seis arquivos sem tocar em uma linha de código — o
`MusicDirector` só depende dos nomes e da duração comum.

### Espacialização é pan e atenuação, sem filtro
Não há filtro passa-baixa por distância nem reverb. O "vácuo" lunar é um ganho
menor com pitch mais grave, não uma simulação. Resolver bem exigiria um
backend de áudio com DSP, que a API `Sound` da LibGDX não oferece.

### Sem pools de samples alternativos
A variação anti-fadiga é por rodízio de pitch sobre o mesmo arquivo. Dois ou
três samples reais por passo e impacto soariam melhor.

---

## Entrada e acessibilidade

### Sem gamepad e sem remapeamento
`GameInputProcessor` continua com bindings fixos em `switch`. Existe apenas o
alternador de setas/WASD na tela de opções. Falta `gdx-controllers` e uma tela
de remapeamento.

### Modo daltônico é uma preferência sem efeito
`AppSettings.isColorblindEnabled()` é salvo e exposto na UI, mas nenhuma tela lê
o valor. Implementar exige um segundo conjunto de cores em `UiTheme` e a
substituição dos tintes de hostil, que hoje são a leitura primária de
comportamento.

### Sem legendas para áudio
Alertas críticos (oxigênio, telegraph) são apenas sonoros e visuais, sem
transcrição textual.

---

## Distribuição

### `jpackage` nunca executado
`lwjgl3/nativeimage.gradle` existe desde o início do projeto e não foi rodado
nem validado neste upgrade. Não há executável testado para Windows, Linux ou
macOS.

### CI não gera artefato jogável
O workflow roda QA de assets, testes e build, e publica só o relatório de
testes. Falta um job que empacote e anexe o build.

---

## Verificação

### Nada foi testado rodando o jogo
Todo o upgrade foi validado por compilação (`./gradlew build`) e por 31 testes de
regras puras. O ambiente de trabalho é headless: não há display para abrir a
janela. Mudanças visuais e de game feel — animação, câmera, mixagem, marcador,
cursor — precisam de uma passada com o jogo aberto antes de qualquer release.

### Cobertura concentrada em regras puras
`MissionState`, `SaveManager`, `EventBus` e `ReachabilityGrid` têm testes.
`JuiceSystem`, `CameraDirector`, `MusicDirector` e `CombatSystem` não — são
testáveis (dependem pouco de GL), só não foram cobertos ainda.
