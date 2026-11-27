# Sistema de Gestão de Academia

Projeto Final de Programação Orientada a Objetos (UFAL).

Sistema desktop em Java para gerenciar uma academia: cadastro de alunos e instrutores, matrículas em planos, treinos e registro de entrada na catraca. A interface é feita em Swing e os dados ficam salvos em um banco SQLite local.

## Funcionalidades

- **Alunos:** cadastro com validação de CPF, telefone, data de nascimento, objetivo de treino e restrições médicas.
- **Instrutores:** cadastro com CREF e especialidade.
- **Matrículas:** quatro planos (Mensal, Trimestral, Semestral e Anual). O sistema permite cancelar, renovar, simular a mensalidade, listar as matrículas a vencer e calcular o faturamento mensal. Alunos com 60 anos ou mais têm 30% de desconto e estudantes têm 20%.
- **Treinos:** criados a partir do objetivo do aluno, já com exercícios padrão, que podem ser adicionados ou removidos.
- **Frequência (catraca):** a entrada só é liberada para matrícula ativa e dentro do prazo. O sistema também conta as presenças por mês.
- **Notificações:** quando uma matrícula é cancelada, renovada ou vence, aparece um alerta no rodapé da tela e um registro de log.

## Arquitetura (MVC)

```
br.ufal.academia
├── model        entidades, enums e padrões de projeto (regras de negócio)
├── view         telas Swing (JPanel/JFrame)
├── controller   interfaces I*Controller e suas implementações
├── dao          interfaces DAO, implementações SQLite (*DAOImpl) e ConnectionFactory
└── exception    exceções checked do domínio
```

- **Model:** Pessoa (abstrata), Aluno, Instrutor, Plano, Matricula, Treino e Frequencia guardam os dados e validam as próprias regras. O model não conhece view nem controller.
- **View:** cada tela recebe pelo construtor apenas a _interface_ do seu controller. A view não tem regra de negócio nem SQL: lê os campos, chama o controller e mostra o resultado ou o erro (JOptionPane).
- **Controller:** concentra as regras que envolvem mais de uma classe. Busca dados nos DAOs, valida, escolhe a Strategy, usa as Factories e registra os Observers.
- **DAO:** cada entidade tem uma interface DAO e uma implementação para SQLite. Operações que gravam em mais de uma tabela (aluno + restrições médicas, treino + exercícios) usam transação com commit/rollback.

O projeto não tem camada Service nem Facade separadas. A interface de cada controller já funciona como a porta de entrada da view, então uma camada a mais só repassaria as chamadas.

Os diagramas de classe estão em `docs/uml/`:

- `diagrama-dominio.drawio.png`: domínio, padrões de projeto e exceções
- `diagrama-mvc.drawio.png`: arquitetura MVC e persistência
- `diagrama-classes.drawio`: arquivo editável (abre no draw.io)

## Padrões de projeto

| Padrão        | Onde                                                                                                            | Por que usamos                                                                                                                                                                                             |
| ------------- | --------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Strategy**  | `CalculoMensalidadeStrategy` com `CalculoPadraoStrategy`, `DescontoEstudanteStrategy` e `DescontoIdosoStrategy` | O cálculo da mensalidade muda conforme o aluno. A `Matricula` só chama `calcularMensalidade()` e não precisa de `if` para cada tipo de desconto. Para criar um novo desconto, basta criar uma nova classe. |
| **Observer**  | `MatriculaObserver` com `LogAuditoriaObserver`, `AlertaInterfaceObserver` e `MatriculaView`                     | Quando a matrícula muda de situação (cancelada, renovada ou vencida), várias partes do sistema precisam saber. A `Matricula` avisa os observers sem conhecer as classes concretas.                         |
| **Factory**   | `PlanoFactory` e `TreinoFactory`                                                                                | Centralizam a criação dos planos padrão (duração e valor) e dos treinos com exercícios padrão por objetivo, sem espalhar esses valores pelo código.                                                        |
| **Singleton** | `ConnectionFactory`                                                                                             | Garante uma única configuração de acesso ao banco `academia.db`, que cria as tabelas na primeira execução.                                                                                                 |

## Pré-requisitos

- **Java 25 (JDK 25)**. O projeto não compila em versões anteriores.
- Não precisa instalar o Maven: o projeto usa o Maven Wrapper (`mvnw`), que baixa tudo na primeira execução.
- Precisa de internet na primeira execução, para baixar as dependências (SQLite JDBC, FlatLaf e JUnit 5).

## Como executar

Na pasta do projeto:

```bash
# Linux / macOS / Git Bash
./mvnw exec:java

# Windows (PowerShell ou CMD)
mvnw.cmd exec:java
```

Pelo IntelliJ, também dá para rodar a classe `br.ufal.academia.Main`.

Na primeira execução, o sistema cria o arquivo `academia.db` na pasta do projeto e carrega dados de demonstração (instrutores, alunos, matrículas, treinos e entradas). Esses dados só são carregados quando o banco não tem nenhum aluno. Para voltar ao estado inicial, feche o programa e apague o arquivo `academia.db`.

## Como rodar os testes

```bash
# Linux / macOS / Git Bash
./mvnw test

# Windows
mvnw.cmd test
```

Os testes usam JUnit 5 e cobrem entidades, padrões, controllers, DAOs (com banco SQLite temporário), integração e telas. Eles não usam nem alteram o `academia.db`.

## Integrantes

- Maria Vitória Bezerra Alves de Palmas
- Kacio Gabriel Tenorio Ferreira da Silva
- João Pedro Apolinário da Costa
- Amélia Vitória Menezes Gomes

## Uso de Inteligência Artificial

Conforme as regras do projeto, declaramos o uso de ferramentas de IA generativa como apoio ao desenvolvimento. O projeto foi planejado, escrito e testado pelos integrantes. A IA foi usada de forma pontual, como uma ferramenta de consulta.

**Ferramenta utilizada:** assistente de IA generativa.

**Como a IA foi usada:**

- **Partes mais complexas:** explicação de conceitos e de como aplicá-los no projeto, como a validação de CPF, a persistência de objetos no banco e a organização das telas Swing.
- **Correção de bugs:** ajuda para entender mensagens de erro e encontrar a causa de alguns problemas durante o desenvolvimento.
- **Revisão:** revisão de algumas partes do código, apontando melhorias e possíveis erros.
- **Simplificação:** sugestões de caminhos mais simples para resolver algumas partes que estavam mais complicadas do que o necessário.

**Validação humana:**

- Toda sugestão foi lida, entendida e adaptada pelos integrantes antes de entrar no projeto. Nada foi copiado sem entendimento.
- Cada alteração passou por issue, branch própria, execução de todos os testes e pull request revisado pelos integrantes antes do merge.
- O sistema foi testado manualmente pela interface (cadastros, matrícula, cancelamento, renovação, catraca e treinos).
