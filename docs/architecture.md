# Arquitetura

## Objetivo do produto

O app precisa unir tres camadas:

1. Planejamento de um programa de treino de medio/longo prazo.
2. Execucao guiada da sessao com logger por serie.
3. Analise historica por exercicio e por grupo muscular.

## Regras principais

- A divisao do usuario e livre: cada "dia" eh uma sessao-modelo com nome customizado.
- O programa nao avanca por dia do calendario, e sim por sessoes concluidas.
- Um bloco longo possui uma lista de sessoes e um numero de repeticoes desse bloco.
- Ao terminar o ultimo bloco, o app volta para o primeiro.
- O treino de hoje eh a proxima sessao pendente no ciclo.
- Pular treino ou pular serie deve exigir confirmacao.
- Aquecimento geral e series de aquecimento de exercicio podem ser pulados sem friccao excessiva.

## Modelo de dominio

### Programa

- `TrainingProgram`
- `ProgramBlock`
- `WorkoutDayTemplate`
- `ExerciseTemplate`
- `WarmupSetTarget`
- `WorkSetTarget`

### Execucao

- `WorkoutSessionLog`
- `ExerciseLog`
- `LoggedSet`

### Analitico

- `ExercisePerformanceSummary`
- `MusclePerformanceSummary`

## Persistencia sugerida

Para a proxima etapa, a persistencia local ideal eh Room com tabelas separadas para:

- `programs`
- `program_blocks`
- `workout_days`
- `exercise_templates`
- `warmup_targets`
- `work_targets`
- `session_logs`
- `exercise_logs`
- `set_logs`

## Algoritmo do treino atual

1. Somar sessoes concluidas.
2. Descobrir em qual bloco esse total cai.
3. Descobrir o indice da sessao dentro do bloco atual.
4. Retornar o template dessa sessao como "treino de hoje".

## Estatisticas

### Exercicio

Exibir:

- melhor carga
- melhor estimativa de 1RM
- volume total por sessao
- repeticoes totais por sessao
- tendencia recente versus media anterior

### Grupo muscular

Como um musculo recebe varios exercicios diferentes, a analise deve evitar um "percentual puro" unico. A proposta inicial eh:

- volume total semanal por musculo
- quantidade de series de trabalho por musculo
- carga media normalizada por exercicio
- tendencia por consistencia de exposicao
