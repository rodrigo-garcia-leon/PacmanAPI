package org.fullstack5.pacman.clients.teaminky.models;

import org.fullstack5.pacman.api.models.Direction;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class DQN {
    Graph graph;
    Session sess;
    String modelPath = "/Users/rodrigogarcialeon/Repositories/ing/PacmanAPI/src/main/java/org/fullstack5/pacman/clients/teaminky/models/graph.pb";

    public DQN() {
        init();
    }

    public void init() {
        try {
            final byte[] graphDef = Files.readAllBytes(Paths.get(modelPath));
            graph = new Graph();
            sess = new Session(graph);
            graph.importGraphDef(graphDef);
            sess.runner().addTarget("init").run();
        } catch (Exception e) {
        }
    }

    public int getGlobalStep() {
        return sess.runner()
                .fetch("global_step")
                .run()
                .get(0)
                .intValue();
    }

    public Direction getMove(DQNGameState state) {
        Tensor<Float> x = Tensor.create(state.getX(), Float.class);
        Tensor<Float> q_t = Tensor.create(0.0f, Float.class);
        Tensor<Float> actions = Tensor.create(new float[1][4], Float.class);
        Tensor<Float> rewards = Tensor.create(0.0f, Float.class);
        Tensor<Float> terminals = Tensor.create(0.0f, Float.class);
        float[][] y = new float[1][4];

        sess.runner()
                .feed("x", x)
                .feed("q_t", q_t)
                .feed("actions", actions)
                .feed("rewards", rewards)
                .feed("terminals", terminals)
                .fetch("fc4_outputs")
                .run()
                .get(0)
                .copyTo(y);

        float maxValue = 0.0f;
        int maxIndex = 0;

        for (int i = 0; i < 4; i++) {
            if (y[0][i] > maxValue) {
                maxValue = y[0][i];
                maxIndex = i;
            }
        }

        return getDirectionFromActionIndex(maxIndex);
    }

    public void train(List<Experience> experiences) {
        int cols = 19;
        int rows = 21;

        float[][][][] currentStates = new float[experiences.size()][cols][rows][6];
        float[][][][] previousStates = new float[experiences.size()][cols][rows][6];
        float[] q = new float[experiences.size()];
        float[] rewards = new float[experiences.size()];
        float[] terminals = new float[experiences.size()];
        float[][] actions = new float[experiences.size()][4];

        for (int i = 0; i < experiences.size(); i++) {
            currentStates[i] = experiences.get(i).getCurrentState().getX()[0];
            previousStates[i] = experiences.get(i).getPreviousState().getX()[0];
            rewards[i] = experiences.get(i).getReward();
            terminals[i] = experiences.get(i).getCurrentState().getResult().isPresent() ? 1.0f : 0.0f;
            actions[i] = getActionFromDirection(experiences.get(i).getLastDirection());
        }

        Tensor<Float> t_x = Tensor.create(currentStates, Float.class);
        Tensor<Float> t_q = Tensor.create(q, Float.class);
        Tensor<Float> t_actions = Tensor.create(actions, Float.class);
        Tensor<Float> t_rewards = Tensor.create(rewards, Float.class);
        Tensor<Float> t_terminals = Tensor.create(terminals, Float.class);

        float[][] y = new float[experiences.size()][4];

        sess.runner()
                .feed("x", t_x)
                .feed("q_t", t_q)
                .feed("actions", t_actions)
                .feed("rewards", t_rewards)
                .feed("terminals", t_terminals)
                .fetch("fc4_outputs")
                .run()
                .get(0)
                .copyTo(y);

        float maxValue;
        for (int i = 0; i < y.length; i++) {
            maxValue = 0.0f;
            for (int j = 0; j < y[i].length; j++) {
                if (y[i][j] > maxValue) {
                    maxValue = y[i][j];
                }
            }
            q[i] = maxValue;
        }

        t_x = Tensor.create(previousStates, Float.class);
        t_q = Tensor.create(q, Float.class);

        sess.runner()
                .feed("x", t_x)
                .feed("q_t", t_q)
                .feed("actions", t_actions)
                .feed("rewards", t_rewards)
                .feed("terminals", t_terminals)
                .addTarget("train")
                .run();
    }

    private float[] getActionFromDirection(Direction direction) {
        float[] action = new float[4];

        switch (direction) {
            case NORTH:
                action[0] = 1.0f;
            case EAST:
                action[1] = 1.0f;
            case SOUTH:
                action[2] = 1.0f;
            case WEST:
                action[3] = 1.0f;
            default:
                action[0] = 1.0f;
        }
        return action;
    }

    private Direction getDirectionFromActionIndex(int index) {
        switch (index) {
            case 0:
                return Direction.NORTH;
            case 1:
                return Direction.EAST;
            case 2:
                return Direction.SOUTH;
            case 3:
                return Direction.WEST;
            default:
                return Direction.NORTH;
        }

    }
}
