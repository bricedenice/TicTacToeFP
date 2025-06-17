package com.example.tictactoe.fp;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;



public class FunctionalGame {
    /* In an enterprise environment,
     the constants will be contained in its own class to separate
     the Main (FunctionalGame) class and storing the constants.*/
    private static final int BOARD_SIZE = 3;
    private static final char EMPTY_CELL = ' ';
    private static final String BOARD_BORDER = "---------";
    private static final Random RANDOM = new Random();
    /*
    Reuse Random instance, Supplier<Random> would be a flexible enterprise solution.
    Allowing one to, for instance, lazily evaluate and set particular logic
    to select different levels of Random generation, prioritized by speed or security.

    public class MonitoredGameConfiguration {
    private final Supplier<Random> randomSupplier;
    private final MetricsCollector metrics;

    public MonitoredGameConfiguration(MetricsCollector metrics) {
        this.metrics = metrics;
        this.randomSupplier = () -> {
            long startTime = System.nanoTime();

            Random random = createOptimalRandom();

            long creationTime = System.nanoTime() - startTime;
            metrics.recordRandomCreationTime(creationTime);

            return random;
        };
    }

    private Random createOptimalRandom() {
        // Government systems need performance metrics
        if (metrics.getAverageCreationTime() > MAX_ALLOWED_TIME) {
            return ThreadLocalRandom.current(); // Faster option
        } else {
            return new SecureRandom(); // More secure option
        }
    }
}
     */

    // enums would be in its own class as well,
    public enum PlayerType {
        USER,
        EASY,
        MEDIUM,
        HARD;

        // Helper method to check if player is "AI"
        public boolean isAI() {
            return this != USER;
        }
    }

/*  Records are essential for removing boiler, maintaining immutability
    by capturing the complete game state at any moment.
    Additionally, key information for the instance like Player and GameConfig are encapsulated.*/
public record GameState(List<List<Character>> board, char currentPlayer, int moveCount) {}
    public record Player(String name, char mark, PlayerType type) {}
    public record GameConfig(Player player1, Player player2) {}


    // Each method maintains immutability where it can, Scanner I/O is mutable by function as it must change with input
    public List<List<Character>> createEmptyBoard(int size, char emptyCell) { // Using Monadic composition
        return IntStream.range(0, size)
                .mapToObj(i -> Collections.nCopies(size, emptyCell))
                .toList();
    }

    public String formatBoard(List<List<Character>> board) {
        return BOARD_BORDER + "\n" +
                board.stream()
                        .map(row -> "| " + row.stream().map(String::valueOf).collect(Collectors.joining(" ")) + " |")
                        .collect(Collectors.joining("\n")) +
                "\n" + BOARD_BORDER;
    }


    public Optional<GameState> tryMove(GameState state, int row, int col, char mark) {
        if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE &&
                state.board().get(row).get(col) == EMPTY_CELL) {
            List<List<Character>> newBoard = state.board().stream()
                    .map(r -> r.stream().collect(Collectors.toList()))
                    .collect(Collectors.toList());
            newBoard.get(row).set(col, mark);
            return Optional.of(new GameState(newBoard, state.currentPlayer() == 'X' ? 'O' : 'X', state.moveCount() + 1));
        }
        return Optional.empty();
    }

    public boolean hasWon(List<List<Character>> board, char mark) {
        boolean rowWin = board.stream().anyMatch(row -> row.stream().allMatch(c -> c == mark));
        boolean colWin = IntStream.range(0, BOARD_SIZE)
                .anyMatch(col -> board.stream().allMatch(row -> row.get(col) == mark));
        boolean diag1Win = IntStream.range(0, BOARD_SIZE).allMatch(i -> board.get(i).get(i) == mark);
        boolean diag2Win = IntStream.range(0, BOARD_SIZE).allMatch(i -> board.get(i).get(BOARD_SIZE - 1 - i) == mark);
        return rowWin || colWin || diag1Win || diag2Win;
    }

    public boolean isDraw(GameState state) {
        return state.moveCount() == BOARD_SIZE * BOARD_SIZE;
    }

    public Optional<GameState> tryUserMove(GameState state, String input) {
        if (input.matches("[1-3] [1-3]")) {
            String[] parts = input.split(" ");
            int row = Integer.parseInt(parts[0]) - 1;
            int col = Integer.parseInt(parts[1]) - 1;
            return tryMove(state, row, col, state.currentPlayer());
        }
        return Optional.empty();
    }

    public Optional<GameState> tryRandomMove(GameState state, Random random, char mark) {
        List<int[]> emptyCells = IntStream.range(0, BOARD_SIZE)
                .boxed()
                .flatMap(i -> IntStream.range(0, BOARD_SIZE)
                        .filter(j -> state.board().get(i).get(j) == EMPTY_CELL)
                        .mapToObj(j -> new int[]{i, j}))
                .collect(Collectors.toList());
        if (emptyCells.isEmpty()) {
            return Optional.empty();
        }
        int[] move = emptyCells.get(random.nextInt(emptyCells.size()));
        return tryMove(state, move[0], move[1], mark);
    }

    // Medium AI strategy (try to win, block, or random)
    public Optional<GameState> tryMediumMove(GameState state, char mark) {
        char opponentMark = (mark == 'X') ? 'O' : 'X';

        // First, try to win
        Optional<GameState> winMove = tryWinningMove(state, mark);
        if (winMove.isPresent()) {
            return winMove;
        }

        // Second, try to block opponent
        Optional<GameState> blockMove = tryWinningMove(state, opponentMark);
        if (blockMove.isPresent()) {
            // Convert opponent winning move to our blocking move
            return blockMove.map(blockedState -> {
                // Find where opponent would win and place our mark there instead
                return findBlockingMove(state, opponentMark, mark);
            }).orElse(Optional.empty());
        }

        // Finally, make random move
        return tryRandomMove(state, RANDOM, mark);
    }

    // Find winning move for a player
    private Optional<GameState> tryWinningMove(GameState state, char mark) {
        return IntStream.range(0, BOARD_SIZE)
                .boxed()
                .flatMap(row -> IntStream.range(0, BOARD_SIZE)
                        .filter(col -> state.board().get(row).get(col) == EMPTY_CELL)
                        .mapToObj(col -> new int[]{row, col}))
                .map(move -> tryMove(state, move[0], move[1], mark))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(newState -> hasWon(newState.board(), mark))
                .findFirst();
    }

    // Find blocking move
    private Optional<GameState> findBlockingMove(GameState state, char opponentMark, char ourMark) {
        return IntStream.range(0, BOARD_SIZE)
                .boxed()
                .flatMap(row -> IntStream.range(0, BOARD_SIZE)
                        .filter(col -> state.board().get(row).get(col) == EMPTY_CELL)
                        .mapToObj(col -> new int[]{row, col}))
                .map(move -> {
                    // Check if opponent would win with this move
                    Optional<GameState> opponentMove = tryMove(state, move[0], move[1], opponentMark);
                    if (opponentMove.isPresent() && hasWon(opponentMove.get().board(), opponentMark)) {
                        // Block by placing our mark there
                        return tryMove(state, move[0], move[1], ourMark);
                    }
                    return Optional.<GameState>empty();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    /**
     * Hard AI strategy using the Minimax algorithm.
     *
     * TODO: Implement full Minimax algorithm for unbeatable AI
     * Current implementation uses medium strategy as a placeholder.
     *
     * @implNote This is a temporary implementation that will be replaced with
     *           the Minimax algorithm. The current implementation uses the
     *           medium strategy for testing purposes.
     *
     * @see #tryMediumMove(GameState, char) Current implementation
     * @see <a href="https://en.wikipedia.org/wiki/Minimax">Minimax Algorithm</a>
     *
     * This is also an example of how I prefer to address TODOs, with enough context for any future developer to implement a solution.
     */
    public Optional<GameState> tryHardMove(GameState state, char mark) {
        // For now, use medium strategy (can implement minimax later)
        return tryMediumMove(state, mark);
    }

    // Check if game has ended
    private Optional<String> checkGameEnd(GameState state, Player player1, Player player2) {
        if (hasWon(state.board(), player1.mark())) {
            return Optional.of(player1.mark() + " wins");
        }
        if (hasWon(state.board(), player2.mark())) {
            return Optional.of(player2.mark() + " wins");
        }
        if (isDraw(state)) {
            return Optional.of("Draw");
        }
        return Optional.empty();
    }

    // Get current player
    private Player getCurrentPlayer(GameState state, Player player1, Player player2) {
        return state.currentPlayer() == player1.mark() ? player1 : player2;
    }

    // Handle user move
    private GameState handleUserMove(GameState state, Scanner scanner) {
        System.out.print("Enter the coordinates: ");
        String input = scanner.nextLine();
        return tryUserMove(state, input)
                .orElseGet(() -> {
                    System.out.println("You should enter numbers from 1 to 3!");
                    return state;
                });
    }

    // Handle AI move with strategy pattern
    private GameState handleAIMove(GameState state, Player current) {
        System.out.println("Making move level \"" + current.type().name().toLowerCase() + "\"");

        return switch (current.type()) {
            case EASY -> tryRandomMove(state, RANDOM, current.mark()).orElse(state);
            case MEDIUM -> tryMediumMove(state, current.mark()).orElse(state);
            case HARD -> tryHardMove(state, current.mark()).orElse(state);
            case USER -> state; // Should never happen in this context
        };
    }

    // Main move dispatcher using pattern matching
    private GameState makeMove(GameState state, Player current, Scanner scanner) {
        return switch (current.type()) {
            case USER -> handleUserMove(state, scanner);
            case EASY, MEDIUM, HARD -> handleAIMove(state, current);
        };
    }

    // Clean functional game loop
    public String playGame(GameState state, Player player1, Player player2, Scanner scanner) {
        System.out.println(formatBoard(state.board()));

        // Check for game end conditions first
        Optional<String> gameEnd = checkGameEnd(state, player1, player2);
        if (gameEnd.isPresent()) {
            return gameEnd.get();
        }

        // Get current player and make move
        Player current = getCurrentPlayer(state, player1, player2);
        GameState nextState = makeMove(state, current, scanner);

        // Continue game recursively
        return playGame(nextState, player1, player2, scanner);
    }

    // Parse string to PlayerType enum
    private PlayerType parsePlayerType(String typeString) {
        return switch (typeString.toLowerCase()) {
            case "user" -> PlayerType.USER;
            case "easy" -> PlayerType.EASY;
            case "medium" -> PlayerType.MEDIUM;
            case "hard" -> PlayerType.HARD;
            default -> throw new IllegalArgumentException("Unknown player type: " + typeString);
        };
    }

    // Main parcing method that associates user input with an enum
    public Optional<GameConfig> parseCommand(String input) {
        if (input.matches("start (user|easy|medium|hard) (user|easy|medium|hard)")) {
            String[] parts = input.split(" ");
            try {
                return Optional.of(new GameConfig(
                        new Player("Player 1", 'X', parsePlayerType(parts[1])),
                        new Player("Player 2", 'O', parsePlayerType(parts[2]))
                ));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Main game loop that handles user input and game initialization.
     * This method:
     * 1. Creates a Scanner for user input
     * 2. Enters an infinite loop until "exit" is entered
     * 3. Parses user commands for game configuration
     * 4. Initializes and starts the game when valid configuration is provided
     */
    public void displayMenu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Input command: ");
            String input = scanner.nextLine();
            if (input.equals("exit")) {
                System.out.println("Exiting...");
                return;
            }
            Optional<GameConfig> config = parseCommand(input);
            if (config.isPresent()) {
                GameState initialState = new GameState(
                        createEmptyBoard(BOARD_SIZE, EMPTY_CELL), 'X', 0);
                System.out.println(playGame(initialState, config.get().player1(), config.get().player2(), scanner));
            } else {
                System.out.println("Bad parameters! Expected: start <player1> <player2>");
                System.out.println("Available types: user, easy, medium, hard");
            }
        }
    }

    /**
     * JDBC connection URL for Oracle database.
     *
     * TODO: Update with dynamic JDBC URL configuration
     * Current implementation uses hardcoded local development URL:
     * - Host: localhost
     * - Port: 1521
     * - Service: XE
     *
     * @implNote This is a temporary hardcoded value that should be replaced with
     *           a configuration-based approach for different environments
     *
     * @see java.sql.Connection
     * @see java.sql.DriverManager
     */
    public void saveGameState(GameState state) {
        String jdbcUrl = "jdbc:oracle:thin:@localhost:1521/XE"; // TODO: Update with jdbcUrl which is created by
        String username = "tictactoe";
        String password = "password";
        String sql = "INSERT INTO game_states (board, current_player, move_count) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String boardString = state.board().stream()
                    .flatMap(List::stream)
                    .map(String::valueOf)
                    .collect(Collectors.joining());
            stmt.setString(1, boardString);
            stmt.setString(2, String.valueOf(state.currentPlayer()));
            stmt.setInt(3, state.moveCount());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Entry point for the game.
     * Creates a new instance of FunctionalGame and starts the game loop.
     */
    public static void main(String[] args) {
        new FunctionalGame().displayMenu();
    }
}