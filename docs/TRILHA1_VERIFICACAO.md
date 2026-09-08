# Trilha 1 — registro de verificação, 8 de setembro de 2026

Este registro não substitui o checklist de aceite do documento do usuário.
Não houve commit: o documento exige a travessia e os ensaios antes dele.

## Verificado nesta rodada

- Compilação Java de core e launcher com dependências locais.
- 57 testes JUnit passando, incluindo CampaignTransferTest: recursos parciais,
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
- `git diff --check` sem erros.

## Implementado, mas ainda exige verificação visual completa

- Conversa da pesquisadora em Titã, HUD de missão e áudio dessa fase.
- Transferências reais por portal com recursos parciais nos três mundos.
- Fix de fonte própria nas interfaces e escala estável em todas as sequências.
- Ataques do chefe preservados quando recebe tiros, sem interrupção contínua.
- Itens renováveis e refinamento sem consumir gelo quando a munição está cheia.
- Animações sem bleeding: pipeline atualizado, mas não conferidas todas as poses.
- Retrato de Ayla passou a usar a arte do próprio NPC; compilado após a sessão.

## Pendências do documento original

- Remoção da cópia aninhada: tentativa anterior bloqueada pela revisão de
  segurança; nenhuma exclusão dessa pasta foi feita nesta rodada.
- Inventário e limpeza final dos assets órfãos, distinguindo fontes de produção.
- Diferenciação do segundo portal com a arte vertical solicitada.
- Todas as hitboxes e animações nos três mundos, em janela com debug ligado.
- Validação de continuidade de carregamento/abertura em dez inicializações.
- Vitória, opções e pausa em fullscreen e na sequência completa de retornos.
- Três sessões reais de cinco minutos cruzando Lua, Marte e Titã.
- Suíte final e commit somente depois do aceite acima.

## Ambiente

O Gradle apresentou `Unable to establish loopback connection` nesta máquina.
Compilação e JUnit foram executados diretamente com as dependências já presentes
no cache local. Saves de teste usam `build/test-home`, separados do save do jogador.
