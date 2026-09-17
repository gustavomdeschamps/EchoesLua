# Combate e abertura — 17/09/2026

## Implementação

- As barreiras sólidas registradas pelo Box2D são consultadas pelos inimigos das três fases, incluindo estações e limites do mapa. Sensores continuam sem bloquear movimento.
- Ataques de contato usam a área vulnerável do jogador e verificam cobertura; inimigos mortos ou preparando ataques não causam dano de contato indevido.
- Pulsos lunares colidem com estações e não exibem dano quando o traje está invulnerável ou protegido.
- O chefão possui sequência de animação por estado: preparação, impacto, recuperação, perseguição e morte. A direção fica travada durante o golpe. O centro do ataque acompanha a hitbox e a colisão de movimento usa a região dos pés.
- A abertura usa um novo panorama de Lua, Marte e Titã, título com contraste suave, movimento de câmera e arcos de sinal. O HUD existente foi preservado.

## Verificação

Compilação do núcleo e launcher; 130 testes aprovados, incluindo cobertura de contato e registro/destruição de barreiras físicas. Capturas com o jogo executando nas três fases e sequência visual do chefão. Essas verificações não substituem uma partida completa até o final.

## Arte da abertura

Modo: geração de imagem nova. Fonte preservada em `tools/source_assets/rework_20260914/intro_panorama.png`; versão instalada em `assets/textures/intro_keyart_v4.png` e atlas de interface. Redimensionamento mecânico para 1280 × 720. A arte anterior permanece no backup fora dos assets ativos.

Prompt utilizado:

Use case: stylized-concept. Asset type: premium opening key art for ECHOES, a 2D space survival game with three worlds: Moon, Mars, Titan. Create a magnificent triumphant cinematic painted panoramic illustration, landscape 16:9. A single astronaut in ivory and navy pressure suit seen from behind stands on a lunar ridge in lower center, looking toward a journey across three distinct environments. Left foreground: silver lunar craters and a believable hexagonal habitat with blue shielded windows and radio dish, Earth distant. Center distance: towering eroded red Martian cliffs, dust-covered terracotta colony and solar arrays under a warm sunrise. Right distance: dark methane-ice shoreline of Titan with amber fog, graphite ice formations and a small industrial refinery; Saturn dominates its sky. Three environments merge into a coherent dramatic illustrated panorama through overlapping atmospheric horizons, not panels or a collage of planets. Signature: two huge incomplete concentric radio arcs, subtle metallic pale light, sweep across the sky linking the three settlements like an echo of one signal. Epic upward composition, earned hopeful expedition mood, rich realistic geology and practical aerospace materials, exquisite painted lighting, deep blacks cool lunar silver rust reds and smoky Titan gold, strong recognizable silhouettes. Leave quiet dark upper-left space for actual game title to be rendered by code. No text, no logos, no watermark, no UI, no framing or loading bar. Avoid generic neon cyberpunk, excessive bloom, checkerboards, random glitter or excessive spaceship clutter. Finished game cover quality, 2048x1152.
