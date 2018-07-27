package org.fullstack5.pacman.clients.teaminky.ghosts;

import org.fullstack5.pacman.api.models.Direction;
import org.fullstack5.pacman.api.models.Maze;
import org.fullstack5.pacman.api.models.Result;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.clients.teaminky.models.DQN;
import org.fullstack5.pacman.clients.teaminky.models.DQNGameState;

public class DQNPacmanAI {
    private Maze maze;
    private DQNGameState previousState;
    private DQN dqn;
    private int globalStep;
    private float eps = 1.0f;

    public DQNPacmanAI(Maze maze) {
        this.maze = maze;
        dqn = new DQN();
        globalStep = dqn.getGlobalStep();
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
        float reward = calculateReward(state);
    }

    private float calculateReward(DQNGameState state) {
        if (state.getResult().isPresent()) {
            Result result = state.getResult().get();
            return result == Result.PACMAN_LOST ? -500.0f : 100.0f;
        } else {
            if (state.getNumScaredGhosts() < previousState.getNumScaredGhosts()) {
                return 50.0f;
            } else if ((state.getNumCapsules() < previousState.getNumCapsules()) || (state.getNumDots() < previousState.getNumDots())) {
                return 10.0f;
            } else {
                return -1.0f;
            }
        }
    }

    private void train() {

    }

    private Direction getMove(DQNGameState state) {
        return Math.random() > eps ? dqn.getMove(state) : Direction.random();
    }

    public void resetState() {
        previousState = null;
    }
}
