# Ajuste de interface — 17/09/2026

- Painéis de gameplay, diálogo e pausa: fundo escuro e moldura azul,
  sem cantos brancos ou marcadores amarelos decorativos.
- `GameplayStatusHud` desenha a mesma composição de suporte vital e carga
  em Lua, Marte e Titã. As informações específicas das missões permanecem no topo.
- Ícones da carga usam os sprites reais de oxigênio, comida e gelo,
  com proporção e offsets do atlas preservados.
- Retratos dos três NPCs preservam o canvas e a proporção, sem esticar o recorte.
  O painel de conversa restaura a cor do batch após escurecer o fundo.
- Pause com quatro botões no estilo do menu: retomar, configurações, menu e sair.
  Mouse e atalhos são atendidos; opções abertas não repassam input à partida.
- Carregamento assíncrono continua funcionando, sem barra ou percentual visível
  sobre a imagem de abertura.

Reconstrução das superfícies: `tools/build_clean_ui.py`, seguido de
`tools/pack_visual_atlases.py`. As imagens anteriores ficam em
`tools/source_assets/before_ui_cleanup_20260917/`, fora da execução.

Validação: compilação local, 126 testes aprovados e capturas dos três mundos,
das três conversas, do menu/hover, da abertura e da pausa.
Não equivale a testar uma campanha inteira nem todas as combinações de input.
