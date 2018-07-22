package org.fullstack5.pacman.clients.teaminky.ghosts;

import org.fullstack5.pacman.api.models.Direction;
import org.fullstack5.pacman.api.models.Maze;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.clients.teaminky.models.DQNGameState;

public class DQNPacmanAI {
    private Maze maze;
    private DQNGameState previousState;

    public DQNPacmanAI(Maze maze) {
        this.maze = maze;
    }

    public Direction runAI(GameState gameState) {
        DQNGameState currentState = DQNGameState.createState(maze, gameState);
        if (previousState != null) {
            observationStep(currentState);
            train();
        }
        previousState = currentState;
        return getMove(currentState);
    }

    private void observationStep(DQNGameState state) {
    }

    private void train() {

    }

    private Direction getMove(DQNGameState state) {
        return Direction.random();
    }
}
