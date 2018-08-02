package org.fullstack5.pacman.clients.teaminky;

import org.fullstack5.pacman.api.MazeLoader;
import org.fullstack5.pacman.api.models.Direction;
import org.fullstack5.pacman.api.models.Game;
import org.fullstack5.pacman.api.models.Maze;
import org.fullstack5.pacman.api.models.Piece;
import org.fullstack5.pacman.api.models.Result;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.clients.teaminky.game.GameRunner;
import org.fullstack5.pacman.clients.teaminky.game.PacmanGui;
import org.fullstack5.pacman.clients.teaminky.ghosts.AStarGhostAI;
import org.fullstack5.pacman.clients.teaminky.pacman.DQNPacmanAI;
import org.fullstack5.pacman.clients.teampacman.ClientUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Trainer implements Runnable {
    private static final int MAZE_ID = 1;
    private long gameDelay;
    private long paintDelay;
    private Maze maze;
    private GameRunner gameRunner;
    private PacmanGui gui;
    private AStarGhostAI ghostAI;
    private DQNPacmanAI pacmanAI;

    private Trainer(long gameDelay) {
        try {
            maze = MazeLoader.loadMaze(MAZE_ID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.gameDelay = gameDelay;
        paintDelay = (long) (((float) gameDelay) * 0.9f);
        ghostAI = new AStarGhostAI(maze);
        pacmanAI = new DQNPacmanAI(maze);
    }

    public static void main(final String... args) {
        long gameDelay = Integer.parseInt(args[0]);
        new Thread(new Trainer(gameDelay)).start();
    }

    @Override
    public final void run() {
        initGameRunner();
        GameState gameState;

        //noinspection InfiniteLoopStatement
        while (true) {
            gameState = gameRunner.createState();
            if (gui != null) {
                gui.updateState(gameState);
            }

            updatePacman(gameState);
            updateGhosts(gameState);
            gameRunner.performStep();

            if (gameRunner.isFinished()) {
                finishGame(gameRunner.createState());
                resetGameRunner();
            }

            if (gameDelay > 0) {
                waitGameDelay();
            }
        }
    }

    private void initGameRunner() {
        resetGameRunner();
        if (gameDelay > 0) {
            initGui();
        }
    }

    private void resetGameRunner() {
        Game game = new Game(maze);
        gameRunner = new GameRunner(game);
    }

    private void initGui() {
        gui = new PacmanGui(maze, Duration.ofMillis(paintDelay));
        gui.initialize();
    }

    private void updatePacman(GameState gameState) {
        Direction direction = pacmanAI.runAI(gameState);
        gameRunner.setDirection(direction, Piece.Type.PACMAN);
    }

    private void updateGhosts(GameState gameState) {
        List<Direction> ghostsDirections = ghostAI.runAI(gameState);
        for (Direction direction : ghostsDirections) {
            gameRunner.setDirection(direction, ClientUtils.getGhostType(ghostsDirections.indexOf(direction)));
        }
    }

    private void finishGame(GameState state) {
        finishPacman(state);
        printStats(state);
    }

    private void finishPacman(GameState state) {
        pacmanAI.runAI(state);
        pacmanAI.resetState();
    }

    private void printStats(GameState state) {
        state.getResult().ifPresent(result -> {
            if (result == Result.PACMAN_WON) {
                System.out.println(String.format("result: %s; score: %d", "WON", state.getPacmanScore()));
            } else {
                System.out.println(String.format("result: %s; score: %d", "LOST", state.getPacmanScore()));
            }
        });
    }

    private void waitGameDelay() {
        try {
            TimeUnit.MILLISECONDS.sleep(gameDelay);
        } catch (Exception ignored) {
        }
    }
}
