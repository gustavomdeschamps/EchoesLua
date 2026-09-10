# Echoes — direção visual de produção

O `tools/source_assets/astronaut_turnaround_v2.png` é a referência aprovada do
personagem. Todo asset jogável novo deve parecer pintado pela mesma equipe e
observado pela mesma câmera.

O `tools/source_assets/world_style_reference_v3.png` é a referência aprovada
para mundo, estruturas, rochas, inimigos e materiais. O personagem continua
inalterado; essa prancha existe apenas para fixar escala e integrar todo o resto
ao traje que já está no jogo.

## Pipeline de animação do personagem

Foi escolhida a abordagem raster disciplinada (opção B), porque o projeto já usa
sprites pintados e não possui arquivos-fonte de rig compatíveis com Spine. O
turnaround fixa identidade, proporção, traje, mochila, câmera e luz antes de
qualquer pose. As fontes `astronaut_movement_candidate.png` e
`astronaut_combat_candidate.png` derivam dessa referência e são normalizadas pelo
`prepare_visual_assets.py`; nunca são consumidas diretamente pelo jogo.

- `astronauta_sheet.png`, 4×4: idle, caminhada, corrida e dash;
- `astronaut_combat_sheet.png`, 4×3: ataque, dano e queda/morte;
- quatro quadros por ação, pivô centro-base e escala visual idêntica;
- o pipeline falha se duas poses consecutivas tiverem diferença média abaixo do
  limiar ou se a grade não dividir exatamente;
- o balanço procedural que substituía a caminhada foi removido. Movimento
  secundário só pode complementar poses reais e nunca deslocar o pivô físico.

## Assinatura visual

- realismo estilizado de ficção científica, com materiais críveis e pintura
  digital limpa — não fotorrealismo colado sobre sprites;
- silhuetas grandes e reconhecíveis na escala real de jogo;
- contorno azul-grafite, nunca preto absoluto;
- volumes definidos por luz, desgaste de borda, parafusos e juntas funcionais;
- microdetalhe existe na fonte em alta resolução, mas é reduzido na escala de
  gameplay para não virar ruído;
- luz neutra vinda de cima e da esquerda, em câmera 3/4 levemente top-down.

## Paleta funcional

- `void` — `#0B0F13`: fundo absoluto de telas e céu sem atmosfera;
- `surface` — `#111820`: painel translúcido (`F2` de alpha);
- `surface-strong` — `#182129`: painel modal (`FA` de alpha);
- `border` — `#52606B`: separador e moldura secundária;
- `suit-ivory` — `#E5E0D6`: superfícies claras, bases e tecnologia humana;
- `outline-navy` — `#213244`: contorno e metal escuro;
- `deep-shadow` — `#343B46`: sombra estrutural;
- `lunar-gray` — `#787F88`: rocha e terreno lunar;
- `tech-cyan` — `#2D8BD0`: tecnologia ativa e oxigênio;
- `tech-cyan-dim` — `#1F5F83`: trilha técnica inativa;
- `energy-amber` — `#E5A43A`: energia, interação e atenção;
- `success-green` — `#67B879`: sistemas restaurados;
- `danger-red` — `#C94E55`: dano e falha;
- `lunar-core` — `#9A3BD1`: assinatura do inimigo lunar;
- `mars-oxide` — `#C95E37`: máquinas, poeira e hostis de Marte.
- `text-primary` — `#F1EEE5`: texto principal;
- `text-muted` — `#B3BDC5`: texto secundário;
- `track` — `#27323B`: trilhas de barra e estados vazios.

## Regras de forma, luz e acabamento

- Inimigos usam placas grandes, juntas simplificadas e um único núcleo emissivo; o glow não ultrapassa a silhueta mais de 2–3 px na escala final.
- Rochas usam fraturas e estratos geológicos coerentes, sempre organizados em
  3–5 massas legíveis; textura fina nunca pode apagar a silhueta.
- Lua e Marte compartilham densidade de detalhe, mas Marte tem estrias de vento e Lua tem depressões secas sem atmosfera.
- Titã usa sedimento de hidrocarboneto úmido, reflexos de metano e âmbar
  atmosférico. Seu predador tem pés em pá e bolsas de pressão translúcidas —
  nunca é uma aranha lunar ou um drone marciano recolorido.
- Toda célula de atlas mantém 8% de margem mínima e pivô comum no centro da base.
- Sprites animados preservam escala, ângulo, direção da luz e baseline entre quadros.
- Efeitos têm ataque curto, substância e cauda; coleta, impacto e morte nunca reutilizam a mesma silhueta.
- Tecnologia humana usa cerâmica clara, metal grafite, vidro, tecido técnico e
  desgaste localizado; tinta laranja identifica Marte e ciano identifica sistema ativo.
- Nenhum sprite pode carregar checker falso, fundo retangular, fragmento de
  célula vizinha ou sombra cortada pela grade. O pipeline valida essas bordas.

## VFX de energia

Energia é a única categoria que pode romper o contorno grafite. O núcleo do
efeito permanece definido e a cauda usa alpha suave porque representa emissão,
não matéria. O halo máximo é reservado a portal, disparo, impacto e coleta; poeira
e detritos continuam opacos, com borda pictórica. Essa exceção é funcional e não
autoriza glow decorativo na UI.

## Arte cinematográfica de abertura

A abertura usa `intro_keyart_v4.png`, compartilhada pelo carregamento, introdução
e menu. A revisão de setembro substitui a montagem de planetas por uma cena lunar
única: antena com cabo rompido, habitat distante, luz fria e espaço escuro à esquerda
para os botões. O acabamento cinematográfico realista foi solicitado explicitamente
no documento Trilha 1; não altera a direção dos sprites jogáveis.

Arte gerada com a ferramenta integrada de imagens. Briefing final: cenário lunar
16:9, antena funcional de cerâmica clara e grafite no primeiro plano direito,
cabo rompido, habitat discreto ao fundo, iluminação dura superior esquerda,
regolito azul-acinzentado, pequenos sinais ciano e âmbar; 45% da esquerda livre;
sem personagens, texto, interface, castelos, cristais, colagem de planetas ou neon
decorativo. Fonte final em `assets/textures/intro_keyart_v4.png` e cópia de execução
no atlas de UI. Ambos devem sempre ser atualizados juntos.

## Tipografia

Chakra Petch é a única família do jogo: Regular para leitura e SemiBold para
títulos, objetivos e valores críticos. Seus cortes técnicos remetem às etiquetas
dos módulos lunares sem cair em fonte monoespaçada de “terminal genérico”. Os
arquivos vieram do catálogo Google Fonts e são distribuídos sob SIL Open Font
License 1.1, preservada em `assets/fonts/OFL-ChakraPetch.txt`.

## Suprimentos de campo

Oxigênio, comida e gelo foram redesenhados como objetos completos, sem texto e
com alpha verdadeiro. Os três compartilham luz superior esquerda e acabamento
pictórico realista, mas não a mesma silhueta: `oxigenio.png` é um cartucho baixo
de cerâmica com gaiola e manômetro azul; `comida.png` é uma bolsa rígida larga
com cinta têxtil ocre; `gelo.png` é um conjunto baixo de três massas fraturadas
com base mineral escura. Essa diferença permite reconhecer o recurso pela forma
mesmo sem cor.

As fontes mestras foram geradas em 1254×1254 com margem transparente. O pipeline
recorta apenas o alpha útil, preserva quatro pixels de respiro e reduz a cópia de
atlas para no máximo 128 px. O jogo encaixa cada imagem sem deformar e calcula a
área de coleta a partir do mesmo retângulo visual. Não adicionar sombra projetada,
halo ou partículas diretamente nesses PNGs; esses sinais pertencem ao VFX de
coleta e devem continuar animados em tempo real.

## Identidade dos novos props e NPC de campanha

- **Ayla / Lua:** traje de comando em marfim e grafite, luzes ciano e mochila
  de rádio; postura direta e gestos de briefing.
- **Ayyub / Marte:** engenheiro de campo de cabelo cacheado, traje oxidado e
  cachecol contra poeira. O nome exibido é sempre Ayyub.
- **Lira / Titã:** traje científico estreito em creme e grafite, visor âmbar e
  instrumentos de amostragem. As 16 poses se dividem entre respiração, fala,
  explicação científica e inspeção do visor; não reutiliza mais o Oficial de
  Marte.
- **Estações / Marte:** solar, oxigênio e comunicação têm silhuetas físicas
  próprias. Cada uma possui quatro estados — desligada, inicialização e dois
  quadros online — e usa ferrugem estrutural como material, não como filtro
  aplicado sobre a imagem inteira.
- **Refinaria / Titã:** máquina humana blindada para frio, com coletor de gelo,
  mangueiras reforçadas e câmara âmbar. Seus quatro quadros comunicam standby,
  boot e duas fases de processamento sem alterar o volume externo da máquina.

As fontes de alta resolução ficam em `tools/source_assets` e as folhas finais
em `assets/textures`. `prepare_fitted_grid` recorta e encaixa cada sujeito em
uma célula 313×313 separadamente; isso impede que uma folha larga seja
esticada para caber no contrato do atlas.

Todo sujeito ocupa no máximo cerca de 62% da célula final. A QA rejeita alpha
na faixa externa; braços, antenas, pernas, fumaça e efeitos não podem depender
da célula vizinha. Os NPCs usam a mesma altura de base e a mesma câmera entre
os três mundos, mudando materiais e função, não o estilo de renderização.

As transições de mundo usam três composições 16:9 próprias: silêncio técnico e
antena rompida na Lua, colônia parcialmente engolida por poeira em Marte e
refinaria sob névoa de metano em Titã. Todas deixam a metade esquerda escura e
sem assunto principal para a tipografia do jogo; nenhuma contém personagem,
texto ou interface gravados na imagem.

## Trilha e desenho de som

O som segue a mesma lógica funcional da paleta: cada camada tem um papel e não
concorre com as outras.

- **Camadas adaptativas.** Cada mundo tem três loops de 32 s tocando juntos:
  base (sempre presente), tensão (entra com hostil próximo) e urgência (entra
  com O2 abaixo de 25%). A intensidade só cruza volumes — o transporte nunca
  reinicia, então as camadas permanecem em fase.
- **Loops sem emenda.** Toda frequência e todo LFO em `tools/generate_music.py`
  é múltiplo exato do fundamental do loop (1/32 Hz). Isso torna o último sample
  contínuo com o primeiro, condição para o crossfade não denunciar o corte.
- **Barramentos.** Música, SFX, interface e ambiente têm volume próprio,
  persistido em `AppSettings` e exposto na tela de opções.
- **Vácuo lunar.** A Lua não tem atmosfera: som externo chega atenuado e mais
  grave, como se conduzido pelo traje. Marte, com atmosfera fina, não aplica
  essa correção.
- **Espaço.** Fontes do mundo atenuam com a distância e panoramizam pelo lado da
  tela. O telegraph do hostil é posicionado de propósito: o jogador precisa
  localizar de onde vem o ataque.
- **Ducking.** Stingers de reparo, craft e alerta abaixam a trilha por um
  instante em vez de disputar espaço com ela.

## Leitura dos hostis

Os três comportamentos dividem o mesmo rig e a mesma folha 4×4; a diferença é de
cor e de distância de reação, não de arte nova.

- **Perseguidor** — tinta neutra. Reage de longe e avança em linha reta.
- **Emboscador** — desvio violeta (`lunar-core`). Fica parado até o jogador
  encostar, então avança rápido com telegraph curto.
- **Atirador** — desvio âmbar (`energy-amber`). Recua para manter distância e
  ataca com pulsos desviáveis; não causa dano por contato.
- **Soberano do Metano** — chefe de Titã com folha 4×4 exclusiva. A carapaça
  grafite, as placas de gelo e as fissuras âmbar criam uma silhueta muito mais
  larga que a dos predadores comuns; a terceira linha da folha é reservada ao
  aviso e ao impacto do golpe no chão, e a quarta à reação e à queda.

## Layout por semente

O desenho geral da fase é autoral e fixo — base, estações, bancada e portal têm
posição fixa. A semente desloca as rochas dentro de uma folga curta e sorteia
onde caem peças e recursos. Antes de entrar em jogo, um flood-fill
(`world/ReachabilityGrid`) confirma que todo ponto obrigatório é alcançável a
partir do início; sem essa checagem um sorteio infeliz fecharia uma peça atrás
de um anel de rochas e travaria a missão.

## Assinatura de Echoes

O sinal de rádio é o motivo recorrente: arcos concêntricos incompletos aparecem apenas em portal, transmissão, objetivo concluído e molduras principais. Ele não deve virar decoração repetida em todos os painéis.
