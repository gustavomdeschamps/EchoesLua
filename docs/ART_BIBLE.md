# Echoes — direção visual de produção

O `tools/source_assets/astronaut_turnaround_v2.png` é a referência aprovada do
personagem. Todo asset jogável novo deve parecer pintado pela mesma equipe e
observado pela mesma câmera.

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

- cartoon robusto com pintura digital limpa;
- silhuetas grandes e reconhecíveis na escala real de jogo;
- contorno azul-grafite, nunca preto absoluto;
- dois níveis principais de sombra suave e detalhe concentrado no foco funcional;
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
- Rochas usam 3–5 massas principais e poucos poros desenhados; não usam microtextura fotográfica.
- Lua e Marte compartilham densidade de detalhe, mas Marte tem estrias de vento e Lua tem depressões secas sem atmosfera.
- Toda célula de atlas mantém 8% de margem mínima e pivô comum no centro da base.
- Sprites animados preservam escala, ângulo, direção da luz e baseline entre quadros.
- Efeitos têm ataque curto, substância e cauda; coleta, impacto e morte nunca reutilizam a mesma silhueta.

## VFX de energia

Energia é a única categoria que pode romper o contorno grafite. O núcleo do
efeito permanece definido e a cauda usa alpha suave porque representa emissão,
não matéria. O halo máximo é reservado a portal, disparo, impacto e coleta; poeira
e detritos continuam opacos, com borda pictórica. Essa exceção é funcional e não
autoriza glow decorativo na UI.

## Arte cinematográfica de abertura

A abertura usa composição mais ampla, mas a mesma pintura, paleta, materiais,
câmera e direção de luz do gameplay. `intro_keyart_v2.png` deriva do astronauta,
dos props e do terreno aprovados; não usa microtextura fotográfica. O receptor
danificado e a fita em arco conectam a imagem diretamente ao motivo de rádio de
Echoes.

## Tipografia

Chakra Petch é a única família do jogo: Regular para leitura e SemiBold para
títulos, objetivos e valores críticos. Seus cortes técnicos remetem às etiquetas
dos módulos lunares sem cair em fonte monoespaçada de “terminal genérico”. Os
arquivos vieram do catálogo Google Fonts e são distribuídos sob SIL Open Font
License 1.1, preservada em `assets/fonts/OFL-ChakraPetch.txt`.

## Assinatura de Echoes

O sinal de rádio é o motivo recorrente: arcos concêntricos incompletos aparecem apenas em portal, transmissão, objetivo concluído e molduras principais. Ele não deve virar decoração repetida em todos os painéis.
