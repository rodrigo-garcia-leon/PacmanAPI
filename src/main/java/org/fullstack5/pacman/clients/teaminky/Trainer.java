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
import org.fullstack5.pacman.clients.teampacman.ClientUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Trainer implements Runnable {
    private Maze maze;
    private long step;
    private GameRunner gameRunner;
    private GameState gameState;
    private PacmanGui gui;
    private AStarGhostAI ghostAI;
    private long gameDelay = 0;

    public static void main(final String... args) {
        System.out.println("Team Inky trainer");

        new Thread(new Trainer()).start();
        ;
    }

    @Override
    public final void run() {
        System.out.println("Start...");
        List<Direction> ghostsDirections;

        while (step < 1000) {
            if (shouldResetGameRunner()) {
                resetGameRunner();
            }

            gameState = gameRunner.createState();

            if (gui != null) {
                gui.updateState(gameState);
            }

            gameRunner.setDirection(getRandomDirection(), Piece.Type.PACMAN);
            ghostsDirections = ghostAI.runAI(gameState);
            for (Direction direction : ghostsDirections) {
                gameRunner.setDirection(direction, ClientUtils.getGhostType(ghostsDirections.indexOf(direction)));
            }

            gameRunner.performStep();

            System.out.println(String.format("step %d...", step));

            if (gameDelay > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(gameDelay);
                } catch (Exception e) {
                }
            }

            step++;
        }
    }

    private Direction getRandomDirection() {
        return Direction.WEST;
    }

    private Boolean shouldResetGameRunner() {
        return gameRunner == null || gameRunner.isFinished();
    }

    private void resetGameRunner() {
        System.out.println("Resetting game runner...");

        if (maze == null) {
            try {
                maze = MazeLoader.loadMaze(1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Game game = new Game(maze);
        gameRunner = new GameRunner(game);
        ghostAI = new AStarGhostAI(maze);

        if (gameDelay > 0) {
            gui = new PacmanGui(maze, Duration.ofMillis((long) (((float) gameDelay) * 0.9f)));
            gui.initialize();
        }
    }

}
