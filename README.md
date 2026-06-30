# Korolev Engine

Protótipo de **Motor de Controle de Variabilidade em Tempo de Execução** (Runtime Variability Control) para Feature
Flags estruturadas como Linha de Produto de Software (SPL). Projeto final para a disciplina de Tópicos Avançados em
Engenharia de Software 2 (TAES2) na Universidade Federal de Pernambuco (UFPE).

---

## 🛠️ Funcionalidades

1. **Modelagem Hierárquica**: Representação de Feature Flags como grafos acíclicos estruturados.
2. **Motor de Validação (Pure Domain Service)**: Valida restrições lógicas estruturais baseadas em padrões *Strategy*:
    - **Hierarquia**: Filho ativo requer pai ativo.
    - **Mandatório**: Pai ativo requer filhos mandatórios ativos.
    - **Requires**: Restrição cruzada (*cross-tree requires*).
    - **Excludes**: Exclusão mútua (*excludes* / XOR).
3. **Resolução de Deadlock (Bulk Update)**: Endpoint para atualizar múltiplos estados em lote, prevenindo bloqueios de
   validação individuais intermediários.
4. **Suporte a UVL (Universal Variability Language)**: Importação e exportação de arquivos `.uvl`.
5. **Visualização em Grafo ASCII**: Endpoint `/api/flags/graph` para representação estrutural textual.

---

## 💻 Como Executar

### 1. Iniciar o Servidor

```bash
./mvnw spring-boot:run
```

### 2. Rodar a Suíte de Testes (34 testes)

```bash
./mvnw clean test
```

### 3. Acessar Swagger UI (Interativo)

Abra no navegador:
👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**
