# Novos assets visuais — especificação para geração

Este documento é o contrato entre o código já preparado e os PNGs que ainda
precisam ser gerados. Cada seção descreve um arquivo completo: resolução,
grade, pivô, paleta, iluminação e onde ele entra no jogo. Coloque o arquivo
final em `assets/textures/<nome>` exatamente como especificado, rode

```
python tools/prepare_visual_assets.py --validate-only
.\gradlew.bat packVisualAssets
```

e o jogo passa a usá-lo automaticamente — nenhum código precisa mudar. Até lá,
o jogo roda com o fallback descrito em cada seção; nada quebra por falta de um
arquivo.

Todos os assets seguem a direção geral de `docs/ART_BIBLE.md`: realismo
estilizado de ficção científica, câmera 3/4 levemente top-down, luz vinda de
cima e da esquerda, contorno azul-grafite (nunca preto absoluto), 8% de
margem segura mínima por célula, fundo transparente real (sem checker, sem
retângulo de cor sólida por trás do sujeito).

## Reaproveitados sem alteração

Estes assets já cumprem o contrato pedido e **não precisam ser regenerados**:

- `npc_colony_officer_sheet_v2.png` — já é a folha dedicada do Oficial da
  Colônia (Marte), 4×4, com identidade própria e distinta de Ayla.
- `npc_commander_ayla_sheet.png` — já é a folha dedicada da Comandante Ayla
  (Lua).
- `lunar_repair_stations_v2.png` — a estrutura de 4 colunas × 4 linhas já
  está correta; o problema era a máquina de estados no código (RepairStation),
  não a arte. Já corrigido (ver seção "Estações lunares" abaixo).

---

## 1. `npc_researcher_lira_sheet_v2.png` — Pesquisadora Lira (Titã)

**Por quê**: hoje Titã reaproveita a folha do Oficial de Marte para Lira —
elas aparecem como a mesma pessoa. Este é o asset de maior prioridade do
manifesto.

```
FILE:      npc_researcher_lira_sheet_v2.png
SIZE:      1252×1252
GRID:      4×4
CELL:      313×313 (recuo de 2px já é absorvido pelo pipeline de recorte)
BACKGROUND: transparente (RGBA real, sem checker)
SAFE MARGIN: 8% mínimo por célula
CAMERA:    3/4 levemente top-down, igual às outras folhas de NPC
LIGHT:     superior-esquerda, neutra
PIVOT:     centro-base (pés no piso da célula)
PALETA:    creme/grafite com detalhes âmbar controlados (nunca laranja de
           Marte, nunca ciano de Ayla) — ver "energy-amber" no ART_BIBLE
```

**Direção de personagem**: pesquisadora científica, não militar. Traje
isolado térmico cor creme/osso com painéis grafite, visor de sensores no
capacete (diferente do capacete técnico de Ayla e do capacete robusto do
Oficial de Marte), equipamento de amostragem preso ao cinto/mochila.
Silhueta mais estreita e postura mais curva/analítica que a do Oficial de
Marte — precisa ser reconhecível como *outra pessoa* mesmo em silhueta, não
apenas em cor.

**Linhas (contrato idêntico ao dos outros NPCs)**:
- **Linha 0 — idle**: 4 quadros de respiração/postura sutil, sem os pés
  deslizando, sem a cabeça mudando de tamanho.
- **Linha 1 — talking**: 4 quadros de gesto de fala contido (leve inclinação
  de cabeça, mão erguendo um instrumento de leitura).
- **Linha 2 — scientific gestures**: 4 quadros de gesto explicativo com um
  equipamento de sensor/tablet, reforçando a leitura "cientista".
- **Linha 3 — reaction/alternate idle**: 4 quadros de reação/idle alternativo
  (ex.: ajustando o visor, olhando para um instrumento).

**Onde é usada**: `Npc.Visual.LIRA` em `AssetManager.npcVisualFrame`;
consumida por `TitanScreen` (constrói `Npc` com `Visual.LIRA`) e por
`DialogBox`/`NpcConversation` para o portrait de diálogo.

**Fallback atual**: `npc_colony_officer_sheet_v2.png` (Oficial de Marte). O
jogo compila e roda normalmente; Lira só passa a ter identidade própria
quando este arquivo for adicionado.

---

## 2. `mars_station_sheet_v2.png` — Estações marcianas animadas

**Por quê**: hoje as três estações de Marte (solar, oxigênio, comunicação)
são a mesma região estática do `mars_atlas_v4`, "animada" só por um
`Sprite.setScale()` procedural. Isso não é uma animação de estação, e as três
não são visualmente diferentes o bastante.

```
FILE:      mars_station_sheet_v2.png
SIZE:      1252×939
GRID:      4×3
CELL:      313×313
BACKGROUND: transparente
SAFE MARGIN: 8% mínimo
CAMERA:    3/4 levemente top-down (mesma câmera do mars_atlas_v4)
LIGHT:     superior-esquerda
PIVOT:     centro-base
PALETA:    óxido/grafite de Marte (ver ART_BIBLE, "mars-oxide" #C95E37),
           cada linha com um acento de cor funcional próprio (ver abaixo)
```

**Contrato de coluna, igual para as três linhas** (mesma máquina de estados
de `RepairStation`):
- **Coluna 0 — offline**: estrutura apagada, sem luzes ativas.
- **Coluna 1 — ativando/boot**: luzes acendendo, painel em transição.
- **Coluna 2 — online A**: operação normal, quadro A.
- **Coluna 3 — online B**: operação normal, quadro B (alterna com A no loop).

A estrutura física **não muda** de tamanho, câmera, posição ou pivô entre as
4 colunas — só luzes, painéis, ventoinhas e indicadores.

**Linha 0 — SOLAR_STATION**: painéis captadores inclinados, célula
fotovoltaica com leve brilho ciano-esverdeado quando online; colunas 2/3
alternam o ângulo de um pequeno rastreador solar.

**Linha 1 — OXYGEN_STATION**: tanques cilíndricos e tubulação, indicador de
pressão; colunas 2/3 alternam o nível de um medidor e um leve vapor saindo de
uma válvula.

**Linha 2 — COMMS_STATION**: antena parabólica pequena + painel transmissor;
colunas 2/3 alternam a posição de um LED de sinal e um leve giro da antena.

As três precisam ser claramente diferentes já na silhueta — nunca a mesma
estrutura recolorida.

**Onde é usada**: `AssetManager.marsStationFrame(MarsObject.Kind, int)`;
consumida por `MarsObject` (estados `OFFLINE_IDLE`/`ACTIVATING`/`ONLINE_LOOP`,
já implementados).

**Fallback atual**: região estática de `mars_atlas_v4` + pulso procedural de
escala (comportamento anterior, preservado). `AssetManager.hasMarsStationSheet()`
retorna `false` até o arquivo existir.

---

## 3. `titan_refinery_sheet_v2.png` — Refinaria de campo de Titã

**Por quê**: a refinaria (gelo → munição) hoje usa a região genérica
`CRAFTING_TERMINAL` do atlas de missão lunar — importante demais na economia
da fase para parecer um prop reciclado.

```
FILE:      titan_refinery_sheet_v2.png
SIZE:      1252×313
GRID:      4×1
CELL:      313×313
BACKGROUND: transparente
SAFE MARGIN: 8% mínimo
CAMERA:    3/4 levemente top-down
LIGHT:     superior-esquerda
PIVOT:     centro-base
PALETA:    tecnologia humana (cerâmica clara + metal grafite, ver ART_BIBLE)
           com acentos âmbar de Titã — nunca ciano (isso é Lua) nem laranja
           puro (isso é Marte)
```

**Direção**: tecnologia humana adaptada ao frio de Titã — mangueiras
reforçadas, isolamento visível, um pequeno coletor de gelo acoplado. Deve
parecer parente da bancada lunar (mesma linguagem de manufatura humana), mas
com blindagem extra e reforços de baixa temperatura — não uma estação lunar
apenas repintada de âmbar.

**Quadros**:
- **0 — idle**: refinaria parada, luz de standby fraca.
- **1 — boot/interação**: luz de processamento acendendo no instante em que
  o jogador interage.
- **2 — processing A**: câmara de processamento com brilho ativo, variante A.
- **3 — processing B**: variante B (alterna com A durante o refino).

**Onde é usada**: `AssetManager.titanRefineryFrame(int)`; consumida por
`TitanScreen.desenharRefinaria()` — já implementado, com máquina de estados
`idle → boot → processing A/B → idle` disparada a cada refino bem-sucedido.

**Fallback atual**: `MissionSprite.CRAFTING_TERMINAL` (região estática), sem
animação.

---

## 4–6. Key art das aberturas de mundo (`WorldIntroScreen`)

Três arquivos, um por mundo, todos com a mesma especificação técnica:

```
SIZE:      1280×720 (16:9 exato — não redimensionar depois)
FORMAT:    RGB ou RGBA (sem necessidade de transparência)
TEXTO:     nenhum — todo texto é renderizado pelo jogo por cima da imagem
UI:        nenhuma — sem HUD, sem logo, sem moldura
PERSONAGENS: nenhum cortado nas bordas; preferencialmente nenhum personagem
           em primeiro plano (a cena é o cenário, não um retrato)
SAFE ZONE: 45% da faixa esquerda e a faixa inferior (~190px) livres de
           elementos essenciais — é onde o jogo desenha título e status
```

Cada imagem precisa ser reconhecível como o mundo certo mesmo sem o texto.

### 4. `world_intro_lunar_v1.png`
Silêncio, isolamento, frio, tecnologia danificada. Regolito cinza-azulado,
preto espacial, antena com cabo rompido ao fundo, luz fria vinda de cima à
esquerda, pequenos sinais técnicos ciano discretos. Reaproveita a mesma
direção de `intro_keyart_v4.png` (ver ART_BIBLE) em uma composição nova.
**Fallback atual**: `textures/lunar_ground.png` tingido de ciano-frio e com
leve deriva de câmera (já implementado em `WorldIntroScreen`).

### 5. `world_intro_mars_v1.png`
Óxido, laranja queimado, vermelho escuro, grafite. Colônia marciana entre
camadas de poeira em movimento, silhueta de estrutura ao fundo, luz difusa
por trás da tempestade. **Fallback atual**: `textures/mars_ground.png`
tingido de óxido, com bandas de poeira procedurais (já implementado).

### 6. `world_intro_titan_v1.png`
Âmbar, marrom profundo, grafite, névoa fria de metano, contraste baixo no
horizonte. Silhuetas distantes muito discretas — **nunca** revelar a forma do
Soberano do Metano, só sugerir uma presença longínqua. **Fallback atual**:
`textures/titan_ground_v2.png` tingido de âmbar, com névoa em camadas e um
sinal pulsante distante (já implementado).

**Onde são usadas**: `AssetManager.worldIntroTexture(CampaignState.Phase)`;
consumidas por `WorldIntroScreen`, que já implementa toda a apresentação
(parallax, motivo por mundo, texto escalonado, skip, fade) e funciona hoje
com o fallback de terreno tingido.

---

## Assets que NÃO precisam ser gerados agora

- Nova folha da Ayla: a atual já cumpre o contrato (identidade própria,
  grade 4×4, sem folga de animação). Não solicitada.
- Novo `npc_colony_officer_sheet_v2.png`: idem — já é a folha dedicada certa
  para o Oficial de Marte.
- Novos assets de portal: `campaign_portal_sheet_v2.png` e
  `titan_portal_vertical_v2.png` já passam na QA de grade, bordas e
  quantidade de regiões (`ProductionAtlasTest`); nenhum corte encontrado.

---

## Checklist de integração (por arquivo)

1. Salvar o PNG em `assets/textures/<nome>` com o tamanho exato acima.
2. `python tools/prepare_visual_assets.py --validate-only` — confirma grade,
   quadros vazios, baseline e escala.
3. `.\gradlew.bat packVisualAssets` — reempacota `game.atlas`/`ui.atlas`.
4. `python tools/prepare_visual_assets.py --validate-only` de novo (o atlas
   não afeta a validação, mas confirma que nada mudou nas fontes).
5. `.\gradlew.bat :core:test` — `ProductionAtlasTest` confirma que o atlas
   final não tem região rotacionada, fora da página ou duplicada.
6. Jogar a fase correspondente e confirmar visualmente.

Nenhum passo de código é necessário — `AssetManager`, `Npc`, `MarsObject` e
`TitanScreen`/`WorldIntroScreen` já resolvem o asset novo assim que ele existe
no atlas.
