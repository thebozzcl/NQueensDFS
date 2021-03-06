package cl.bozz.nqueensdfs;

import cl.bozz.nqueensdfs.models.BoardState;
import cl.bozz.nqueensdfs.models.Cell;
import cl.bozz.nqueensdfs.utils.HashStringUtils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

public class NQueensDFS {
    public static void main(final String[] args) {
        // Parse args
        if (args.length != 1) {
            throw new RuntimeException("Invalid input, please provide a board size");
        }
        final int n = Integer.parseInt(args[0]);
        new NQueensDFS().start(n);
    }

    public void start(final int n) {
        final long totalCells = (long) n * n;
        final BigInteger totalPermutations = totalPermutations(totalCells, n);

        // Instantiate auxiliary objects and metrics
        final HashStringUtils hashStringUtils = new HashStringUtils();
        final Stack<BoardState> boardStateStack = new Stack<>();
        final Set<BoardState> terminalBoardStates = new HashSet<>();
        final Set<String> boardStateHashes = new HashSet<>();

        long totalBoardsProcessed = 0;
        long totalTerminalBoards = 0;
        long totalPrunedBoards = 0;

        // Set up initial board
        final Set<Cell> initialAvailableCells = new HashSet<>();
        for (int x = 0; x < n; x ++) {
            for (int y = 0; y < n; y ++) {
                initialAvailableCells.add(new Cell(x, y));
            }
        }
        final BoardState initialBoardState = new BoardState(new TreeSet<>(), initialAvailableCells, n);
        boardStateStack.add(initialBoardState);
        hashStringUtils.generateHashStrings(initialBoardState.getQueenPositions(), n)
                .forEach(boardStateHashes::add);

        final Instant start = Instant.now();

        // Main loop:
        while (!boardStateStack.empty()) {
            // 1. Pop from stack
            final BoardState boardState = boardStateStack.pop();
            totalBoardsProcessed ++;
            if (totalBoardsProcessed % 100 * n == 0) {
                System.out.println("Processed " + totalBoardsProcessed + "/" + totalPermutations.toString() + " boards; found " + totalTerminalBoards + " terminals, pruned " + totalPrunedBoards);
            }

            // 2. Filter out terminal boards and count them towards total
            if (boardState.getQueenPositions().size() == n) {
                terminalBoardStates.add(boardState);
                // TODO: generating the hashes here again is kinda wasteful... maybe store them?
                totalTerminalBoards += hashStringUtils.generateHashStrings(boardState.getQueenPositions(), n).size();
                continue;
            }

            // 3. Generate child boards. For each...
            final Set<BoardState> newBoardStates = boardState.generateChildBoardStates();
            for(final BoardState newBoardState : newBoardStates) {
                // 3.a. Generate all 90-degree rotations for each board, and all their mirrors as well
                //      Using a Set here ensures there's no accidental repetition.
                //      This is a *huge* time saver! It prunes the DFS tree early to a small fraction of its real size.
                final Set<String> newBoardHashes = hashStringUtils.generateHashStrings(newBoardState.getQueenPositions(), n);

                // 3.b. Filter out the board if any of the hashes is already registered
                //      Since they're all topologically identical, there's no need to keep any of them
                boolean shouldPrune = newBoardHashes.stream().anyMatch(boardStateHashes::contains);
                if (shouldPrune) {
                    totalPrunedBoards ++;
                    continue;
                }

                // 3.c. Register all new hashes to avoid repetitions in the future
                newBoardHashes.forEach(boardStateHashes::add);
                boardStateStack.add(newBoardState);
            };
        }

        // Emit metrics
        terminalBoardStates.forEach(boardState -> System.out.println(boardState.toString()));
        System.out.println("Max board permutations: " + totalPermutations.toString());
        System.out.println("Total terminal boards: " + totalTerminalBoards);
        System.out.println("Unique terminal boards identified: " + terminalBoardStates.size());
        System.out.println("Total boards processed: " + totalBoardsProcessed);
        System.out.println("Total boards pruned: " + totalPrunedBoards);

        final Instant end = Instant.now();
        final long ellapsedMillis = end.toEpochMilli() - start.toEpochMilli();
        System.out.println("Time ellapsed (millis): " + ellapsedMillis);
    }

    private static final BigInteger totalPermutations(final long k, final long n) {
        BigInteger result = BigInteger.valueOf(k);
        for (long i = 1; i < n; i ++) {
            final BigInteger mult = BigInteger.valueOf(k - i);
            result = result.multiply(mult);
        }
        return result;
    }
}
