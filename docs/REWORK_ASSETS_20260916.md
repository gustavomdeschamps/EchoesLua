# ECHOES — troca visual de produção

## Escopo

Troca de 36 imagens de produção: terrenos e aberturas dos três mundos,
rochas, estações, base, portais, itens, arma, personagens, inimigos e efeitos.
O HUD aprovado, suas fontes e seus componentes não são redesenhados neste lote.
Áudio não foi substituído neste lote.

Os nomes de arquivo permanecem compatíveis com o carregador, mas o conteúdo é
substituído. Não há uma segunda camada de imagens antigas por baixo das novas.
Versões anteriores ficam fora da pasta de execução, em
`tools/source_assets/before_rework_20260914/`, incluindo os atlas anteriores.

## Contratos de integração

- Quadros normalizados de 313 × 313 pixels, com margens transparentes.
- Pés dos atores na linha 278; mesma escala entre quadros de uma sequência.
- Caminhada e corrida possuem fontes dedicadas, instaladas de forma reproduzível.
- Recorte do atlas mantém posição, dimensões originais e espelhamento.
- Cenários e arma preservam proporção; colisões de rochas usam o tamanho desenhado.
- Morte dos inimigos não reutiliza os quadros de dano como um ciclo completo.
- Fontes geradas são separadas dos arquivos finais e registradas no manifesto.

## Reconstrução

1. `tools/install_rework_20260914.py`: normaliza e substitui as imagens finais.
2. `tools/pack_visual_atlases.py`: valida, separa quadros e reconstrói atlas em uma
   pasta nova; remove páginas antigas somente após guardar cópia recuperável.
3. `tools/verify_local.py --capture`: compila, executa testes e abre as três fases
   reais para captura, com perfil isolado para preservar os saves do jogador.

Não executar o gerador completo de candidatos antigos em
`prepare_visual_assets.py`: seu modo `--validate-only` é o apropriado para esta revisão.

## Limites da verificação

Compilação local de `core` e do lançador concluída; 124 testes aprovados.
A normalização de quadros terminou com zero avisos de alinhamento.
Capturas incluem 19 composições/quadros e as telas reais de Lua, Marte e Titã.
O verificador seleciona a versão LWJGL indicada em `gradle.properties`, sem misturar
bibliotecas de versões antigas encontradas no cache local.

Capturas e testes conferem carregamento, alinhamento, transparência e integração.
A abertura de cada fase não equivale a concluir uma campanha inteira. A avaliação
de fluidez e sensação das animações continua exigindo uma rodada jogável.

Manifestos: `tools/source_assets/rework_20260914/manifest.json` e `installed.json`.
Capturas: `build/asset-qa/runtime/`.
