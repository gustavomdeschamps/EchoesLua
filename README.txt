ECHOES — Campanha Parte 3

COMO EXECUTAR
Código compatível com Java 17; verificação local realizada com JDK 21. Na pasta do projeto: gradlew.bat :lwjgl3:run
O primeiro uso do Gradle pode precisar baixar dependências.

CONTROLES
WASD / SETAS: movimento nas fases originais; WASD nas novas arenas e Aharin.
SHIFT: correr.
ESPAÇO / CLIQUE ESQUERDO: disparar. MOUSE: mira independente do corpo.
Q: dash nas fases originais.
E: estação, NPC ou portal.
I: abrir/fechar inventário. C dentro do inventário: consumir uma comida.
M: abrir/fechar mapa. ESC: pausa ou fechar inventário/mapa.
F5: salvar. F9: carregar nas fases originais; Continuar no menu restaura a fase da campanha.
ENTER: pular a abertura somente depois de seis segundos; avançar diálogo em Aharin.
ESPAÇO: avançar diálogo quando houver uma conversa aberta.

ONDE TESTAR OS CHEFES E PORTAIS
Lua: reative três sistemas, fabrique o rifle e neutralize os hostis.
Interaja com o portal autorizado para entrar na cratera do Guardião.
Ao vencer, a CHAVE_LUA entra na mochila. Portal leste da arena leva a Marte.
Marte: reative as três estações, neutralize os hostis e receba a autorização de Ayyub.
O portal de Titã leva primeiro à arena do Titã-Ferrugem; sem missões, a luta fica bloqueada.
Ao vencer, CHAVE_MARTE entra na mochila. Portal leste da arena leva a Titã.
Titã: converse com a pesquisadora, elimine os caçadores e enfrente o Soberano a nordeste.
A vitória concede CHAVE_TITA. O portal de Calisto fica a nordeste, perto da cratera do chefe.
Calisto: o boss fica no centro-leste da cratera. Um único objeto passa pelas formas 1, 2 e 3.
Só a terceira derrota concede CHAVE_LUZ. A estação oeste repõe munição e oxigênio com E.
Portal leste: bloqueado sem CHAVE_LUZ, leva a Aharin com a chave.
Aharin: siga a passarela oeste até o terraço e aproxime-se das três entidades de Luz.
O painel superior direito acende E · FALAR quando você está ao alcance; fora dele o E não conversa.
Três falas disparam o encerramento.
Os chefes concedem melhorias de equipamento, limitadas ao nível 3 e visíveis no HUD/inventário.
Dano do rifle: 20 na base, +5 por nível de arma. Armadura reduz 15% do dano por nível.
A base 20 é adaptação do guia (que sugere 10) ao HP deste projeto: com 10 uma única
forma de Calisto custaria 22 acertos contra um pente de 30.

CUTSCENES — ALTERNATIVA DOCUMENTADA DO PDF
Intro: Animation de seis enquadramentos, câmera, arcos de sinal e música; duração de 7,2 s.
Ending: quatro frases com fades durante dez segundos, depois tela de vitória e opção de menu.
Não depende de WEBM/OGG ou plugin de reprodução de vídeo.

SAVE
Versão 6 preserva comida, chaves, níveis de equipamento, fase e forma/HP do boss de Calisto.
Saves anteriores não recebem chaves sem lutar. Chefes de Lua/Marte podem reiniciar a luta ao continuar.

ENTREGA ACADÊMICA
Preencher nome completo/turma antes de enviar. A gravação de cinco minutos e o ZIP de entrega
não são gerados automaticamente pelo jogo. Para empacotar, excluir build/ e .idea/.
