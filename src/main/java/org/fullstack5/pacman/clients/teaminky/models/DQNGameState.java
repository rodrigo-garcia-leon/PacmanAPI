package org.fullstack5.pacman.clients.teaminky.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.fullstack5.pacman.api.models.Maze;
import org.fullstack5.pacman.api.models.Position;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.api.models.response.MovingPiece;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
public final class DQNGameState {
    float[][] walls;
    float[][] pacman;
    float[][] dot;
    float[][] capsules;
    float[][] ghosts;
    float[][] scaredGhosts;

    public static DQNGameState createState(Maze maze, GameState gameState) {
        return new DQNGameState(
                DQNGameState.createWalls(maze),
                DQNGameState.createPacman(maze, gameState),
                DQNGameState.createDot(maze, gameState),
                DQNGameState.createCapsules(maze, gameState),
                DQNGameState.createGhosts(maze, gameState),
                DQNGameState.createScaredGhosts(maze, gameState)
        );
    }

    private static float[][] createWalls(Maze maze) {
        float[][] walls = new float[maze.getWidth()][maze.getHeight()];

        for (int i = 0; i < maze.getWidth(); i++) {
            for (int j = 0; j < maze.getHeight(); j++) {
                if (maze.getWalls()[i][j]) {
                    walls[i][j] = 1.0f;
                }
            }
        }

        return walls;
    }

    private static float[][] createPacman(Maze maze, GameState gameState) {
        float[][] pacman = new float[maze.getWidth()][maze.getHeight()];
        Position position = gameState.getPacman().getCurrentPosition();
        pacman[position.getX() - 1][position.getY() - 1] = 1.0f;
        return pacman;
    }

    private static float[][] createDot(Maze maze, GameState gameState) {
        float[][] dot = new float[maze.getWidth()][maze.getHeight()];

        for (Position position : gameState.getRemainingDots()) {
            dot[position.getX() - 1][position.getY() - 1] = 1.0f;
        }

        return dot;
    }

    private static float[][] createCapsules(Maze maze, GameState gameState) {
        float[][] capsules = new float[maze.getWidth()][maze.getHeight()];

        for (Position position : gameState.getRemainingPellets()) {
            capsules[position.getX() - 1][position.getY() - 1] = 1.0f;
        }

        return capsules;
    }

    private static float[][] createGhosts(Maze maze, GameState gameState) {
        float[][] ghosts = new float[maze.getWidth()][maze.getHeight()];
        Position position;
        for (MovingPiece ghost : DQNGameState.getGhosts(gameState)) {
            if (!ghost.isVulnerable()) {
                position = ghost.getCurrentPosition();
                ghosts[position.getX() - 1][position.getY() - 1] = 1.0f;
            }
        }
        return ghosts;
    }

    private static float[][] createScaredGhosts(Maze maze, GameState gameState) {
        float[][] scaredGhosts = new float[maze.getWidth()][maze.getHeight()];
        Position position;
        for (MovingPiece ghost : DQNGameState.getGhosts(gameState)) {
            if (ghost.isVulnerable()) {
                position = ghost.getCurrentPosition();
                scaredGhosts[position.getX() - 1][position.getY() - 1] = 1.0f;
            }
        }
        return scaredGhosts;
    }

    private static List<MovingPiece> getGhosts(GameState gameState) {
        List<MovingPiece> ghosts = new ArrayList<>();
        ghosts.add(gameState.getBlinky());
        ghosts.add(gameState.getPinky());
        ghosts.add(gameState.getInky());
        ghosts.add(gameState.getClyde());
        return ghosts;
    }

    public float[][][][] getX() {
        int cols = 19;
        int rows = 21;

        float[][][][] matrix_x = new float[1][cols][rows][6];
        float[][] subMatrix;

        for (int k = 0; k < 6; k++) {
            subMatrix = getSubMatrix(k);

            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    matrix_x[0][i][j][k] = subMatrix[i][j];
                }
            }
        }

        return matrix_x;
    }

    private float[][] getSubMatrix(int index) {
        switch (index) {
            case 0:
                return walls;
            case 1:
                return pacman;
            case 2:
                return dot;
            case 3:
                return capsules;
            case 4:
                return ghosts;
            case 5:
                return scaredGhosts;
            default:
                return walls;
        }
    }
}
