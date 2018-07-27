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
    private final float epsFinal = 0.1f;
    private final float epsStep = 10000.0f;
    private Maze maze;
    private DQNGameState previousState;
    private DQN dqn;
    private int globalStep;
    private float eps = 1.0f;
    private Direction lastDirection;
    private int replayMemorySize = 100000;
    private LinkedList<Experience> experiences;
    private int localCount = 0;
    private int trainingStart = 50;
    private int batchSize = 32;

    public DQNPacmanAI(Maze maze) {
        this.maze = maze;
        dqn = new DQN();
        experiences = new LinkedList<>();
        updateGlobalStep();
    }

    private void updateGlobalStep() {
        globalStep = dqn.getGlobalStep();
    }

    public Direction runAI(GameState gameState) {
        DQNGameState currentState = DQNGameState.createState(maze, gameState);
        if (previousState != null) {
            observationStep(currentState);
            if (localCount > trainingStart) {
                train();
            }
            localCount++;
            updateEps();
        }
        previousState = currentState;
        return getMove(currentState);
    }

    private void updateEps() {
        eps = Math.max(epsFinal, 1.0f - localCount / epsStep);
    }

    private void observationStep(DQNGameState state) {
        float reward = calculateReward(state);
        Experience experience = new Experience(previousState, reward, lastDirection, state);
        experiences.addFirst(experience);
        if (experiences.size() > replayMemorySize) {
            experiences.removeLast();
        }
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
        List<Integer> indexes = IntStream.rangeClosed(0, experiences.size())
                .boxed()
                .collect(Collectors.toList());

        Collections.shuffle(indexes);
        List<Experience> trainingExperiences = new LinkedList<>();
        for (int i = 0; i < batchSize; i++) {
            trainingExperiences.add(experiences.get(indexes.get(i)));
        }

        dqn.train(trainingExperiences);
    }

    private Direction getMove(DQNGameState state) {
        lastDirection = Math.random() > eps ? dqn.getMove(state) : Direction.random();
        return lastDirection;
    }

    public void resetState() {
        previousState = null;
        lastDirection = null;
    }
}
