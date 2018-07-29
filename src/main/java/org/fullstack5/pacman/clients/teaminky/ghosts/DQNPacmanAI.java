package org.fullstack5.pacman.clients.teaminky.ghosts;

import org.fullstack5.pacman.api.models.Direction;
import org.fullstack5.pacman.api.models.Maze;
import org.fullstack5.pacman.api.models.Result;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.clients.teaminky.models.DQN;
import org.fullstack5.pacman.clients.teaminky.models.DQNGameState;
import org.fullstack5.pacman.clients.teaminky.models.Experience;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DQNPacmanAI {
    private static final float epsFinal = 0.1f;
    private static final float epsStep = 100000.0f;
    private static final int replayMemorySize = 10000;
    private static final int trainingStart = 10000;
    private static final int batchSize = 32;
    private static final float REWARD_WON = 100.0f;
    private static final float REWARD_LOST = -100.0f;
    private static final float REWARD_GHOST_EATEN = 50.0f;
    private static final float REWARD_DOT_OR_CAPSULE_EATEN = 10.0f;
    private static final float REWARD_PENALTY = -1.0f;
    private final Maze maze;
    private final DQN dqn;
    private final LinkedList<Experience> experiences = new LinkedList<>();
    private DQNGameState previousState;
    private int globalStep;
    private float eps = 1.0f;
    private Direction lastDirection;
    private int localCount;

    public DQNPacmanAI(Maze maze) {
        this.maze = maze;
        dqn = new DQN(maze.getWidth(), maze.getHeight());
    }

    private void updateGlobalStep() {
        globalStep = dqn.getGlobalStep();
    }

    public Direction runAI(GameState gameState) {
        final DQNGameState currentState = DQNGameState.createState(maze, gameState);

        if (previousState != null) {
            observationStep(currentState);

            if (localCount > trainingStart) {
                train();
            } else {
                localCount++;
            }

            updateGlobalStep();
            updateEps();
        }

        previousState = currentState;
        lastDirection = getMove(currentState);

        return lastDirection;
    }

    private void updateEps() {
        eps = Math.max(epsFinal, 1.0f - globalStep / epsStep);
    }

    private void observationStep(DQNGameState state) {
        final float reward = calculateReward(state);
        final Experience experience = new Experience(previousState, reward, lastDirection, state);

        experiences.addFirst(experience);
        if (experiences.size() > replayMemorySize) {
            experiences.removeLast();
        }
    }

    private float calculateReward(DQNGameState state) {
        if (state.getResult().isPresent()) {
            final Result result = state.getResult().get();
            return result == Result.PACMAN_LOST ? REWARD_LOST : REWARD_WON;
        } else {
            if (state.getNumScaredGhosts() < previousState.getNumScaredGhosts()) {
                return REWARD_GHOST_EATEN;
            } else if ((state.getNumCapsules() < previousState.getNumCapsules()) || (state.getNumDots() < previousState.getNumDots())) {
                return REWARD_DOT_OR_CAPSULE_EATEN;
            } else {
                return REWARD_PENALTY;
            }
        }
    }

    private void train() {
        final List<Integer> indexes = IntStream.rangeClosed(0, experiences.size() - 1)
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(indexes);

        final List<Experience> trainingExperiences = new LinkedList<>();
        for (int i = 0; i < batchSize; i++) {
            trainingExperiences.add(experiences.get(indexes.get(i)));
        }

        dqn.train(trainingExperiences);
    }

    private Direction getMove(DQNGameState state) {
        return Math.random() > eps ? dqn.getMove(state) : Direction.random();
    }

    public void resetState() {
        previousState = null;
        lastDirection = null;
    }
}
