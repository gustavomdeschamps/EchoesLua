# Chefes planetários — 21/09/2026

Assets gerados pela ferramenta integrada de imagem e instalados por
`tools/install_planet_bosses.py`. O script recorta cada célula pelo alfa,
normaliza a escala e mantém a base de todos os quadros em `y=354`.

- `assets/textures/lunar_boss_sheet_v1.png`
- `assets/textures/mars_boss_sheet_v1.png`
- `assets/textures/boss_keys_sheet_v1.png`
- fontes preservadas em `tools/source_assets/part3_20260921/`

## Prompt — Guardião da Cratera

```text
Use case: stylized-concept
Asset type: transparent 2D boss spritesheet for the top-down LibGDX game ECHOES, lunar boss.
Primary request: Create exactly one coherent lunar boss called GUARDIÃO DA CRATERA, shown in four animation poses.
Subject: a heavy moon-adapted alien quadruped built from charcoal lunar regolith plates and pale silvery impact-glass armor, with a cold cyan core in the chest, thick forelimbs, compact silhouette, no humanoid astronaut features.
Style/medium: detailed hand-painted realistic 2D game sprite, three-quarter top-down camera, crisp readable silhouette, consistent with a serious sci-fi survival game.
Composition/framing: exactly 4 columns and 1 row in a uniform grid. Pose 1 calm idle; pose 2 both forelimbs lifted preparing a ground slam; pose 3 forelimbs striking the ground; pose 4 low recovery pose. The complete creature must be centered in every equal cell, consistent body scale and identical feet baseline, with at least 15% transparent padding on every side.
Lighting/mood: hard cold upper-left moonlight, subtle cyan core glow.
Constraints: genuine transparent RGBA background; no text, no grid lines, no baked shadow, no dust, no projectiles, no cropped claws or horns, no overlapping cells, same creature identity and camera in every frame.
```

## Prompt — Titã-Ferrugem

```text
Use case: stylized-concept
Asset type: transparent 2D boss spritesheet for the top-down LibGDX game ECHOES, Mars boss.
Primary request: Create exactly one coherent Martian boss called TITÃ-FERRUGEM, shown in four animation poses.
Subject: a massive six-legged biomechanical predator assembled from oxidized iron-red armor, dark basalt joints and worn colony machinery, with a bright amber reactor core, broad crushing forelimbs and a low aggressive silhouette; clearly different from the lunar creature.
Style/medium: detailed hand-painted realistic 2D game sprite, three-quarter top-down camera, crisp readable silhouette, consistent with a serious sci-fi survival game.
Composition/framing: exactly 4 columns and 1 row in a uniform grid. Pose 1 calm idle; pose 2 broad forelimbs lifted preparing a ground slam; pose 3 forelimbs striking the ground; pose 4 low recovery pose. The complete creature must be centered in every equal cell, consistent body scale and identical feet baseline, with at least 15% transparent padding on every side.
Lighting/mood: warm dusty upper-left Martian light, amber reactor glow, restrained rust palette.
Constraints: genuine transparent RGBA background; no text, no grid lines, no baked shadow, no dust cloud, no projectiles, no cropped claws or antennae, no overlapping cells, same creature identity and camera in every frame.
```

## Prompt — chaves físicas

```text
Use case: stylized-concept
Asset type: transparent 2D collectible spritesheet for the top-down LibGDX game ECHOES.
Primary request: Create four distinct expedition boss keys, exactly 4 columns and 1 row: Lunar Key, Mars Key, Titan Key, Light Key.
Subject: compact palm-sized sci-fi key artifacts with a central handle and readable asymmetric silhouette. Column 1 lunar key made from silver impact glass and dark regolith with cyan glow; column 2 Mars key made from oxidized iron and basalt with amber glow; column 3 Titan key made from dark cryogenic metal and methane crystal with violet glow; column 4 Light key made from translucent white-gold energy crystal with pale blue aura.
Style/medium: detailed realistic hand-painted 2D game item sprites, three-quarter top-down view, crisp edges and clear silhouette at 48 pixels.
Composition/framing: uniform four-cell horizontal grid, one complete centered item per cell, identical apparent scale and center pivot, at least 20% transparent padding on every side.
Constraints: genuine transparent RGBA background; no text, no labels, no grid lines, no shadows baked into background, no particles crossing cell boundaries, no cropped pieces, no overlap, no duplicate designs.
```
