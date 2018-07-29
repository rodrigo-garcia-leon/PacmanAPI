package org.fullstack5.pacman.clients.teaminky.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.fullstack5.pacman.api.models.Maze;
import org.fullstack5.pacman.api.models.Position;
import org.fullstack5.pacman.api.models.Result;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.api.models.response.MovingPiece;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("SpellCheckingInspection")
@AllArgsConstructor
@Getter
public final class DQNGameState {
    public static final int N_STATE_MATRICES = 6;
    int cols;
    int rows;
    float[][] walls;
    float[][] pacman;
    float[][] dot;
    float[][] capsules;
    float[][] ghosts;
    float[][] scaredGhosts;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Optional<Result> result;

    public static DQNGameState createState(Maze maze, GameState gameState) {
        return new DQNGameState(
                maze.getWidth(),
                maze.getHeight(),
                DQNGameState.createWalls(maze),
                DQNGameState.createPacman(maze, gameState),
                DQNGameState.createDot(maze, gameState),
                DQNGameState.createCapsules(maze, gameState),
                DQNGameState.createGhosts(maze, gameState),
                DQNGameState.createScaredGhosts(maze, gameState),
                gameState.getResult()
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

    public int getNumScaredGhosts() {
        return countMatrix(scaredGhosts);
    }

    public int getNumDots() {
        return countMatrix(dot);
    }

    public int getNumCapsules() {
        return countMatrix(capsules);
    }

    private int countMatrix(float[][] matrix) {
        int count = 0;

        for (float[] cols : matrix) {
            for (float cell : cols) {
                if (cell > 0.0f) {
                    count++;
                }
            }
        }

        return count;
    }

    public float[][][][] getX() {
        float[][][][] x = new float[1][cols][rows][N_STATE_MATRICES];
        float[][] submatrix;

        for (int k = 0; k < N_STATE_MATRICES; k++) {
            submatrix = getSubmatrix(k);

            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    x[0][i][j][k] = submatrix[i][j];
                }
            }
        }

        return x;
    }

    private float[][] getSubmatrix(int index) {
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
