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
