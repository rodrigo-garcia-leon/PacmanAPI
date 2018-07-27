package org.fullstack5.pacman.clients.teaminky.models;

import org.fullstack5.pacman.api.models.Direction;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;

import java.util.List;

public class DQN {
    String exportDir = "/Users/rodrigogarcialeon/Repositories/ing/PacmanDQN/export";
    SavedModelBundle model;

    public DQN() {
        init();
    }

    public void init() {
        try {
            model = SavedModelBundle.load(this.exportDir, "serve");
        } catch (Exception e) {
        }
    }

    public int getGlobalStep() {
        return model.session().runner()
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

        model.session().runner()
                .feed("qnet_x", x)
                .feed("qnet_q_t", q_t)
                .feed("qnet_actions", actions)
                .feed("qnet_rewards", rewards)
                .feed("qnet_terminals", terminals)
                .fetch("qnet_fc4_outputs")
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

        model.session().runner()
                .feed("qnet_x", t_x)
                .feed("qnet_q_t", t_q)
                .feed("qnet_actions", t_actions)
                .feed("qnet_rewards", t_rewards)
                .feed("qnet_terminals", t_terminals)
                .fetch("qnet_fc4_outputs")
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

        model.session().runner()
                .feed("qnet_x", t_x)
                .feed("qnet_q_t", t_q)
                .feed("qnet_actions", t_actions)
                .feed("qnet_rewards", t_rewards)
                .feed("qnet_terminals", t_terminals)
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
