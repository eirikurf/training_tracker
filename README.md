# TreinoCiclico

Aplicativo Android para planejamento e registro de treinos de musculacao com:

- divisao livre por sessoes, sem restringir PPL, upper/lower ou nomes fixos
- blocos longos com repeticao por quantidade de sessoes concluidas
- metas por exercicio e progressao ao longo de semanas
- logger detalhado por serie, incluindo aquecimento, carga, repeticoes, descanso, RPE e tecnica avancada
- historico em calendario e estatisticas por exercicio ou grupo muscular

## Estrutura inicial

- `app/`: app Android em Kotlin + Jetpack Compose
- `docs/architecture.md`: regras de negocio, modelo do dominio e persistencia sugerida
- `docs/protocol-seed-template.md`: formato sugerido para transformar o PDF em seed de programa

## Estado atual

Esta primeira entrega prioriza:

- scaffold do app
- modelo de dominio
- fluxo principal de telas
- seed demonstrativo com 2 blocos longos e split de 5 sessoes

A persistencia definitiva com Room e a importacao fiel do PDF ficaram documentadas para a proxima iteracao.
