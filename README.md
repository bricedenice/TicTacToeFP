# TicTacToeFP
## Author
Brice Hagood - Aspiring Java developer focusing on FP and database integration.

A functional programming (FP) implementation of Tic-Tac-Toe in Java, showcasing immutable data, pure functions, declarative Streams, and higher-order functions. Built with Maven to demonstrate modern Java skills for enterprise roles (e.g., Java, Oracle) in Puerto Rico.

## Features
- **Functional Programming**: Immutable `List<List<Character>>` board, `record` for state (`GameState`, `Player`), and pure functions.
- **Declarative Logic**: Java Streams for board initialization, win checks, and move processing.
- **Immutability**: New `GameState` for each move, ensuring no side effects.
- **Higher-Order Functions**: Uses `map`, `anyMatch`, `collect`, and `Optional<GameState>` for functional move handling.
- **Recursive Game Loop**: `playGame` uses recursion for immutable state transitions.
- Supports user vs. user, user vs. bot, and bot vs. bot modes.

## Technical Details
- **Java Version**: 17+ (records, `toList()`).
- **Build Tool**: Maven for dependency management (JUnit, Oracle JDBC).
- **Structure**: `com.example.tictactoe.fp.FunctionalGame`.
- **Key Methods**:
  - `createEmptyBoard`: Initializes immutable board with Streams.
  - `tryMove`: Pure function returning `Optional<GameState>` for move validation.
  - `hasWon`: Declarative win check using Streams.
  - `playGame`: Recursive loop for functional control flow.
- **Testing**: JUnit tests for purity and correctness.
- **Database**: Oracle SQL integration via JDBC to store game states (planned).

## How to Run
1. Clone the repository.
2. Open in IntelliJ IDEA with Java 17+ and Maven.
3. Run `mvn compile` and `FunctionalGame.main()`.
4. Enter commands (e.g., `start user easy`).

## Future Enhancements
- Complete Oracle SQL integration to store game states.
- Add JavaFX or web UI.
- Implement advanced AI (e.g., minimax).

## Relevance to Employers
Demonstrates modern Java (Streams, records, Optional), FP principles, Maven, and Oracle JDBC skills.

