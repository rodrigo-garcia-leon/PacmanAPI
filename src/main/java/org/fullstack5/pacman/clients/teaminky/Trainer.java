package org.fullstack5.pacman.clients.teaminky;

import org.fullstack5.pacman.api.MazeLoader;
import org.fullstack5.pacman.api.models.Direction;
import org.fullstack5.pacman.api.models.Game;
import org.fullstack5.pacman.api.models.Maze;
import org.fullstack5.pacman.api.models.Piece;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.clients.teaminky.game.GameRunner;
import org.fullstack5.pacman.clients.teaminky.game.PacmanGui;
import org.fullstack5.pacman.clients.teaminky.ghosts.AStarGhostAI;
import org.fullstack5.pacman.clients.teaminky.ghosts.DQNPacmanAI;
import org.fullstack5.pacman.clients.teampacman.ClientUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Trainer implements Runnable {
    private final long gameDelay = 0;
    private Maze maze;
    private long step;
    private GameRunner gameRunner;
    private PacmanGui gui;
    private AStarGhostAI ghostAI;
    private DQNPacmanAI pacmanAI;

    private Trainer() {
        try {
            maze = MazeLoader.loadMaze(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ghostAI = new AStarGhostAI(maze);
        pacmanAI = new DQNPacmanAI();
    }

    public static void main(final String... args) {
        new Thread(new Trainer()).start();
    }

    @Override
    public final void run() {
        GameState gameState;

        long MAX_STEP = 1000;
        while (step < MAX_STEP) {
            checkGameRunner();
            gameState = gameRunner.createState();
            updateGui(gameState);
            updatePacman(gameState);
            updateGhosts(gameState);
            gameRunner.performStep();
            waitGameDelay();
            step++;
        }
    }


    private void checkGameRunner() {
        if (shouldResetGameRunner()) {
            resetGameRunner();
        }
    }

    private Boolean shouldResetGameRunner() {
        return gameRunner == null || gameRunner.isFinished();
    }

    private void resetGameRunner() {
        Game game = new Game(maze);
        gameRunner = new GameRunner(game);

        if (gameDelay == 0 || gui != null) {
            return;
        }

        initGui();
    }

    private void initGui() {
        gui = new PacmanGui(maze, Duration.ofMillis((long) (((float) gameDelay) * 0.9f)));
        gui.initialize();
    }

    private void updateGui(GameState gameState) {
        if (gui == null) {
            return;
        }

        gui.updateState(gameState);
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

    private void waitGameDelay() {
        if (gameDelay == 0) {
            return;
        }

        try {
            TimeUnit.MILLISECONDS.sleep(gameDelay);
        } catch (Exception ignored) {
        }
    }
}
