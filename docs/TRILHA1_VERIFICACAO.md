# Trilha 1 — registro de verificação, 9 de setembro de 2026

Este registro não substitui o checklist de aceite do documento do usuário.
Não houve commit: o documento exige a travessia e os ensaios antes dele.

## Verificado nesta rodada

- Compilação Gradle de core e launcher com dependências locais.
- 122 testes JUnit passando, incluindo CampaignTransferTest: recursos parciais,
  tempo, fase, posição e flags de diálogo sobrevivem à serialização da campanha.
- Menu com nova arte lunar, conferido em janela 1280×720.
- Configurações: controles de áudio sem o botão comprimido usado como puxador.
  A escala do HUD usa a escala real, não a posição normalizada da barra.
- Diálogo de Ayla: três falas distintas, quebra de linha dentro da caixa,
  oxigênio congelado durante a conversa, fechamento e Espaço extra sem crash.
- HUD lunar com objetivo e subtítulo separados; indicação de planeta LUA.
- Pausa lunar após o diálogo sem crash.
- Tela de derrota exibindo oxigênio final 0%, corrigindo o relatório antigo.
- Atlas de jogo em página única, frames separados e filtro Linear.
- QA rigoroso dos assets com Pillow e NumPy, sem violações de alpha ou grade.
- Novos suprimentos de oxigênio, comida e gelo com alpha real, silhuetas
  distintas, cópia de atlas limitada a 128 px e desenho sem deformação.
- Lira dedicada (16 quadros), três estações marcianas (12 quadros) e refinaria
  de Titã (4 quadros) substituindo os fallbacks reaproveitados.
- Key arts dedicadas 1280×720 para as entradas de Lua, Marte e Titã,
  substituindo ampliações do terreno nas transições de campanha.
- Portal de Titã separado do portal da campanha, com oito estados próprios,
  bloqueio avermelhado e sem inversão brusca no meio da transição.
- NPCs nomeados Ayla, Ayyub e Lira, com nascimento variável entre pontos
  seguros e estável dentro da mesma campanha.
- Corrida com trava de exaustão: manter Shift com energia vazia não alterna
  mais os estados WALK/RUN; a caminhada recupera energia mais rapidamente.
- Titã refeito com piso, seis formações, caçador, refinaria, portal e chefe da
  mesma família visual; fase final com quatro caçadores e chefe mais agressivo.
- Aberturas de mundo recompostas em painel alto, texto com quebra controlada,
  entrada escalonada e key art sem escurecimento destrutivo.
- IntelliJ reindexado: os 98 falsos erros de símbolos em `MenuScreen` sumiram;
  a classe ficou apenas com avisos e sugestões.
- `git diff --check` sem erros.

## Rodada de correções de controle, mixagem e desempenho

Quatro defeitos reais encontrados por auditoria do código em uso, todos
corrigidos e cobertos por teste. A suíte passou de 77 para 95 testes.

- **Rifle atravessado nas costas.** A orientação do corpo era escrita por duas
  fontes: `move()` decidia pelo eixo do movimento e a arma girava pela mira.
  Andando para a direita e mirando à esquerda o traje ficava com o rifle
  cruzado. Agora a mira é a fonte de verdade enquanto a arma está equipada, e o
  movimento só decide quando ela não está. `WeaponGeometry.resolveFacingLeft`
  tem zona morta perto da vertical: varrer o cursor por cima do personagem
  troca o lado uma vez, não a cada grau.
- **Origem do tiro fora do cano.** O sprite da arma era desenhado na altura
  `.44` do traje e o projétil nascia em `.48`, com avanço de 34 px na Lua,
  33 px em Marte e 30 px em Titã. Desenho, mira, traço e muzzle flash agora
  leem `WeaponGeometry.muzzle`, com distância constante em qualquer ângulo.
- **Drone marciano passando pela quina da rocha.** A caixa de colisão com o
  cenário era derivada do sprite, incluindo a flutuação senoidal do drone e o
  tremor de 25 Hz do telegraph: passar ou não pela quina dependia da fase da
  senoide. A pegada de colisão ficou estável e ancorada no chão; a caixa de
  dano continua acompanhando o voo — que é visível —, mas não o tremor.
- **Tela cheia e ambiente sem efeito.** `AppSettings.fullscreen` era gravada e
  nunca lida por ninguém, e o barramento de ambiente recebia o volume dos
  efeitos (`ambientVolume = settings.getSfxVolume()`), então passos e vento
  subiam junto com o rifle. Ambos ganharam controle próprio em Configurações;
  a tela cheia é aplicada em `EchoesLua.aplicarModoDeTela`, respeitando a flag
  `echoes.windowed` usada no QA.
- **Alocação por quadro nos três HUDs.** Sete `String.format`/concatenações por
  quadro na Lua, cinco em Marte e duas compostas em Titã, mais um `new Color`
  por painel. Trocados por `ui.HudLabel`, que só remonta o texto quando o
  inteiro muda, e por uma `Color` reaproveitada.

Testes adicionados: `WeaponGeometryTest` (8), `HudLabelTest` (6) e três novos
casos de orçamento vertical em `SettingsLayoutTest`, que agora guarda a altura
das colunas — a conta de largura já era guardada, a de altura não, e cada
controle novo empurrava a coluna para o rodapé sem ninguém reclamar.

## Segunda rodada: fase final, acessibilidade e pipeline

- **Titã não tinha game feel nenhum.** Era a única fase sem `JuiceSystem` e
  sem `CameraDirector`: a câmera grudava na posição do jogador com um clamp
  cru, sem suavização nem lookahead, não havia hit-stop nem tremor, e a opção
  "Tremor de câmera" das configurações não tinha efeito ali — justamente na
  fase do chefe. Agora Titã usa os mesmos dois sistemas das outras fases,
  com zoom de combate que abre antes para o chefe caber na tela, vinheta de
  dano e partículas de impacto e abate que também faltavam.
- **Presets próprios do chefe.** `BOSS_SLAM` e `BOSS_DEATH` cumprem a
  hierarquia pedida no documento — "chefe: muito forte", "vitória final:
  máxima" — acima do golpe comum e abaixo do teto de tremor.
- **Redução de movimento não existia.** O documento pede a opção junto do
  tremor de câmera; não havia nada. Foi implementada de ponta a ponta:
  preferência persistida, controle em Configurações, e efeito real em
  `JuiceSystem` (sem empurrão de zoom, sem câmera lenta), `CameraDirector`
  (sem lookahead, sem zoom contextual) e nas entradas de painel da pausa e
  das telas de resultado. O hit-stop e a vinheta de dano ficam de propósito:
  seguram o quadro e piscam, mas não deslocam a imagem — tirá-los junto
  trocaria acessibilidade por perda de informação.
- **Passada da Lua ignorava a corrida.** A poeira já lia `isSprinting` e o som
  não: correndo, o traje levantava poeira a cada 105 ms e pisava a cada
  480 ms. Passo e partícula passaram a ler o mesmo estado.
- **QA de assets estava pulando em silêncio.** `validateVisualAssets` procura
  um Python com Pillow e NumPy e, não achando, apenas avisa e segue. Nesta
  máquina o NumPy não estava instalado, então a verificação que o documento
  exige nunca rodava. Com o NumPy instalado, a QA roda por padrão e passou em
  modo estrito (`-PstrictAssetQa=true`); os três atlas foram reempacotados.
- **Cópia aninhada removida.** `EchoesLua/` — 123 MB e 230 arquivos
  versionados — saiu do índice e do disco. Nada no `settings.gradle` ou no
  código a referenciava; os únicos dois arquivos exclusivos eram
  `assets/atlases/game2.png`, página do atlas antigo de duas páginas, e um
  `gradle-daemon-jvm.properties` gerado.
- **Alocação por quadro fora do gameplay.** `PauseOverlay`, `DialogBox` e
  `MissionResultScreen` pediam um `Color` novo por rótulo desenhado.

Um NPE foi introduzido e corrigido dentro desta rodada: em `LunarScreen` a
chamada de `pauseOverlay.setReduceMotion` ficou antes da criação do overlay,
o que derrubaria a fase lunar ao carregar.

Testes: 107 (eram 77 no início da trilha). `JuiceSystemTest` cobre a
hierarquia de impacto, o teto de tremor, o respeito às duas opções de
acessibilidade e o decaimento até o repouso.

## Terceira rodada: auditoria medida dos assets

O QA automatizado só passou a rodar nesta máquina depois de instalar o NumPy,
então as folhas nunca tinham sido medidas aqui. `tools/audit_spritesheets.py`
corta cada folha pela grade que o `AssetManager` usa em runtime e mede célula
vazia, alpha real, conteúdo encostado na borda, oscilação de baseline e
variação de escala dentro da linha.

**Resultado das regras duras do §12: todas passam.** Nenhum quadro cortado,
nenhuma célula vazia, nenhum conteúdo encostando na borda, nenhuma folha sem
alpha. As margens vão de 12% a 33%. O pipeline anterior funcionou.

O que sobrou foi deriva de baseline, e a primeira medição estava errada: usar
a caixa cheia fazia vapor e brilho — que têm alpha baixo — contarem como corpo
da máquina. Medindo só pixels opacos (alpha > 200), o quadro mudou:

| Folha | Antes | Depois | Motivo |
|---|---|---|---|
| Refinaria de Titã, linha 0 | 28 px | 0 px | §19 exige geometria externa idêntica |
| Estações de Marte, linha 2 | 23 px | 0 px | §18; linhas 0 e 1 já estavam em 0–2 px |
| Estações lunares, linhas 1–3 | 7–13 px | 0 px | §17: corpo, base e escala idênticos na linha |
| Caçador de Titã, linha 3 | 37 px | 0 px | a linha serve também para reação a dano |
| Chefe de Titã, linha 1 | 10 px | 0 px | ciclo de caminhada derivando, não bob |

O caçador merece nota: o código escolhe a linha 3 tanto para a queda quanto
para a reação a dano, e o quadro 0 estava 37 px acima do chão — levar um tiro
fazia a criatura saltar, o oposto do "não pode flutuar" do §21. O chefe subia
10 px de forma monotônica ao longo dos quatro quadros da caminhada e voltava
de uma vez ao reiniciar o laço: não era o sobe-e-desce de um passo.

**O que foi deixado de propósito.** A linha 2 do chefe (27 px) e a linha 2 do
caçador (18 px) são telegraph: a criatura se ergue antes do golpe, e isso é
animação, não defeito. Folhas de formações, landmarks e obstáculos têm um
objeto diferente por célula, e as folhas de VFX crescem por natureza — medir
deriva nelas não significa nada. O astronauta já estava correto: 0 a 2 px em
todas as linhas, nas duas folhas.

A correção é `tools/anchor_machine_rows.py`, deliberadamente conservadora:
apenas translação vertical, nunca reescala, alvo na mediana da linha medida em
pixels opacos, e aborta se algum deslocamento fosse encostar na borda da
célula em vez de estragar a folha. As folhas originais ficaram preservadas em
`tools/source_assets/*_pre_anchor.png`.

`MachineSheetBaselineTest` lê os PNGs com ImageIO e trava as sete linhas
corrigidas mais as do astronauta. Os três atlas foram reempacotados.

## Quarta rodada: áudio medido

`tools/audit_audio.py` mede pico, degrau de borda, continuidade de laço,
atraso inicial e balanço de mix. `tools/repair_audio.py` corrige. A tarefa
`validateAudio` roda o relatório no build e falha em modo estrito.

**Três arquivos decodificavam acima de 1.0** — `alerta_oxigenio` em 1.121,
`colisao_rocha` em 1.067, `passo_lunar` em 1.036. Isso é distorção antes de
somar com qualquer outra coisa. Outros oito estavam gravados abaixo do papel
que ocupam: o rugido do chefe final tinha pico 0.47, com metade da escala
sobrando.

**Duas medições minhas estavam erradas, e vale registrar.** Julgar clique pelo
pico dos primeiros 64 samples acusa o transiente de ataque, que um passo deve
ter; a métrica certa é a distância da primeira amostra até o zero, e por ela
nenhum arquivo tem clique. E julgar laço por amplitude de borda acusa
justamente o que prova que o laço está certo — conteúdo em laço começa em
amplitude não-nula porque continua de onde parou. O degrau real de laço de
todas as músicas é menor que 0.006; nenhuma foi tocada.

**Reencodar Vorbis faz o pico crescer**, e não pouco: escrever com pico 0.89
devolveu 1.026 na leitura de volta. O ganho agora é verificado — o arquivo é
regravado, lido de volta e medido, e o ganho reduzido até o pico decodificado
ficar sob o teto.

### Mixagem

Os ganhos estavam espalhados como literais, cada um escolhido isolado, e a
escala tinha saído invertida. Medindo sonoridade de curto prazo (janela de
300 ms, próxima do que o ouvido julga num disparo único) vezes o ganho:

| | antes | depois |
|---|---|---|
| Alarme de oxigênio | 0.373 | **0.419** |
| Rugido do chefe | 0.149 | 0.212 |
| Pausa | **0.390** | 0.180 |
| Hover da interface | 0.190 | 0.071 |
| Passo lunar / Titã / Marte | 0.062 / 0.035 / 0.023 | 0.045 / 0.045 / 0.045 |

O blip de pausa tocava mais alto que o alarme de suporte de vida, e o hover do
menu mais alto que o chefe final. Os passos são o mesmo evento e soavam 2.7×
diferente entre a Lua e Marte.

### Sons órfãos

`boss_rugido.ogg` e `boss_morte.ogg` estavam carregados e nunca eram tocados
por ninguém: o chefe preparava o ataque em silêncio e morria sem som.
`TitanBoss` ganhou `consumeRoar` e `consumeDeath`.

Resultado: 34 → 0 problemas no relatório de áudio.

## Quinta rodada: configurações e saída na pausa

A pausa oferecia apenas retomar e voltar ao menu. Para mudar volume ou
desligar o tremor de câmera o jogador tinha de abandonar a partida, ir ao
menu e começar de novo — a configuração existia mas não estava ao alcance de
quem estava jogando. O documento pede continuar, configurações, menu e sair.

O menu principal monta suas opções em Scene2D. Trazer Scene2D para dentro do
gameplay significaria um segundo processador de input disputando o teclado
com o jogo, e o documento é explícito sobre não introduzir Scene2D só por
causa de uma tela. Então a pausa desenha as opções no mesmo modo imediato do
resto do overlay, e a navegação vive em `ui/PauseSettingsModel` — sem LibGDX
gráfico, sem `Preferences`, só a regra de seleção e ajuste, que é o que pode
quebrar em silêncio.

Atalhos: `O` abre as opções, setas navegam e ajustam, `ESC` volta ao painel da
missão, `M` vai ao menu, `Q` sai. O tratamento das teclas ficou em
`PauseOverlay.handlePauseKeys`, num lugar só, em vez de triplicado — já havia
duplicação no `ESC` e no `M` entre as três fases. Com o painel aberto o
overlay consome tudo, então `ESC` fecha as opções em vez de despausar e
nenhuma tecla vaza para o gameplay.

As opções da pausa são um subconjunto do menu de propósito — volume de
música, efeitos e ambiente, tremor de câmera e redução de movimento. Escala
de HUD, tela cheia e controles continuam só no menu, onde há espaço para
explicar. Mudar acessibilidade na pausa reaplica na hora, sem esperar o
próximo carregamento.

`PauseSettingsModelTest` cobre a volta na seleção, o limite da barra nas
pontas, esquerda/direita significando a mesma coisa nos dois tipos de linha,
e o valor escrito ao lado da barra — sem ele a leitura dependeria só da cor.

Testes: 122.

## Sexta rodada: inventário gerado

`docs/INVENTARIO_ASSETS.md` passou a existir, gerado por
`tools/generate_asset_inventory.py` em vez de escrito à mão. Um inventário
escrito envelhece na primeira renomeação e passa a afirmar coisas que o código
não faz mais — e o próprio documento avisa para não usar documentação como
substituto de implementação.

As três fontes são lidas de verdade: o `AssetManager`, para saber de qual
atlas cada textura sai e qual método entrega os quadros; o PNG em disco, para
medir grade, célula, quadros e margem; e o resto de `core/src/main`, para
descobrir quem chama cada método. A regra do documento é que nome de arquivo
não decide uso — a busca por referência decide.

**Achado: dois acessores sem chamador.** `npcAylaFrame` era código morto — a
folha da Ayla era carregada duas vezes, uma pelo campo dedicado e outra pelo
enum `Visual.AYLA`, que veio depois no refactor. Campo, carregamento e acessor
foram removidos. `resourceIcon` continua sem chamador: o HUD mostra o
inventário como texto, não como ícone. Não removi — o documento é explícito
sobre não apagar asset só por parecer órfão —, e agora ele aparece listado.

**Correção de um número que eu havia relatado melhor do que é.** A rodada
anterior falou em "margens de 12% a 33%": era a folga medida no nível da
folha. A folga da célula mais apertada é bem menor — de 3,5% a 16%, ou 11 a
49 px. Isso não é quadro cortado: nenhuma célula encosta na borda em folha
nenhuma, e isso continua verificado. É a folga real ficando abaixo da meta de
18% do contrato de sprites na célula mais cheia. Fechar a diferença não é
reescala mecânica: encolher o conteúdo dentro da célula encolheria o sprite em
jogo, porque o tamanho de desenho é o tamanho da célula, e desalinharia as
hitboxes derivadas dele. É trabalho de refazer a fonte com mais respiro.

**Um risco checado e descartado.** Os acessores aplicam `inset` de 2 a 3 px,
que recorta a borda da célula antes de entregar a região. Se a margem em
pixels fosse menor que o inset, o jogo desenharia o sprite cortado e o corte
não apareceria olhando a folha. Medido: nenhuma folha tem esse problema, e a
mais apertada é o portal da campanha, com 8 px de sobra. A checagem entrou em
`audit_spritesheets.py` para não voltar em silêncio.

## Implementado, mas ainda exige verificação visual completa

- Conversa da pesquisadora em Titã, HUD de missão e áudio dessa fase.
- Transferências reais por portal com recursos parciais nos três mundos.
- Fix de fonte própria nas interfaces e escala estável em todas as sequências.
- Ataques do chefe preservados quando recebe tiros, sem interrupção contínua.
- Itens renováveis e refinamento sem consumir gelo quando a munição está cheia.
- Animações sem bleeding: pipeline atualizado, mas não conferidas todas as poses.
- Retrato de Ayla passou a usar a arte do próprio NPC; compilado após a sessão.

## Pendências do documento original

- Todas as hitboxes e animações nos três mundos, em janela com debug ligado.
- Validação de continuidade de carregamento/abertura em dez inicializações.
- Vitória e pausa em fullscreen e na sequência completa de retornos.
- Três sessões reais de cinco minutos cruzando Lua, Marte e Titã.
- Suíte final e commit somente depois do aceite acima.

## Ambiente

O erro `Unable to establish loopback connection` vinha do caminho curto de TEMP
usado pelos sockets Unix do JDK no Windows. `tools/gradle-local.ps1` define um
diretório canônico dentro de `build/socket-tmp` para o wrapper e para o daemon;
Gradle, TexturePacker, compilação e JUnit voltaram a funcionar normalmente.
Saves de teste usam `build/test-home`, separados do save do jogador.
