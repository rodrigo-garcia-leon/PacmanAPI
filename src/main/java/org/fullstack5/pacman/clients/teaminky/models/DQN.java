package org.fullstack5.pacman.clients.teaminky.models;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;

public class DQN {
    String exportDir = "/Users/rodrigogarcialeon/Repositories/ing/PacmanDQN/export";

    public void init() throws Exception {
        try (SavedModelBundle model = SavedModelBundle.load(this.exportDir, "serve")) {
            System.out.println("loaded model");

            float[][][][] matrix_x = new float[1][19][21][6];
            Tensor<Float> x = Tensor.create(matrix_x, Float.class);
            Tensor<Float> q_t = Tensor.create(0.0f, Float.class);
            float[][] matrix_actions = new float[1][4];
            Tensor<Float> actions = Tensor.create(matrix_actions, Float.class);
            Tensor<Float> rewards = Tensor.create(0.0f, Float.class);
            Tensor<Float> terminals = Tensor.create(0.0f, Float.class);

            Tensor y = model.session().runner()
                    .feed("qnet_x", x)
                    .feed("qnet_q_t", q_t)
                    .feed("qnet_actions", actions)
                    .feed("qnet_rewards", rewards)
                    .feed("qnet_terminals", terminals)
                    .fetch("qnet_fc4_outputs")
                    .run()
                    .get(0);

            System.out.println(y.toString());
        }
    }

    public static void main(String[] args) {
        DQN dqn = new DQN();
        try {
            dqn.init();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
