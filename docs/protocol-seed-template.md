# Seed do protocolo do PDF

Como a extracao automatica do PDF nao ficou confiavel neste ambiente, o scaffold foi preparado para receber um seed estruturado.

## Campos minimos por exercicio

- nome
- grupos musculares alvo
- aquecimentos sugeridos
- series de trabalho alvo
- repeticoes alvo
- descanso alvo
- RPE alvo
- observacoes

## Exemplo de estrutura

```json
{
  "blocks": [
    {
      "name": "Bloco 1",
      "repeatCount": 8,
      "days": [
        {
          "name": "Sessao A",
          "exercises": [
            {
              "name": "Preencher com exercicio do PDF",
              "muscles": ["CHEST"],
              "warmups": [
                { "reps": 12, "loadKg": 20.0 }
              ],
              "workSets": [
                { "targetReps": 8, "targetRpe": 8.0, "restSeconds": 120 }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```
