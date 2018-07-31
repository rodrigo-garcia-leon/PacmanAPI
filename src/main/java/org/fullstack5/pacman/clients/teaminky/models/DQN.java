package org.fullstack5.pacman.clients.teaminky.models;

import org.apache.commons.io.IOUtils;
import org.fullstack5.pacman.api.models.Direction;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SuppressWarnings("SpellCheckingInspection")
public class DQN {
    private static final String COST = "cost";
    private static final String GLOBAL_STEP = "global_step";
    private static final int CHECKPOINT_SAVE_COUNT = 1000;
    private static final String X = "x";
    private static final String Q_T = "q_t";
    private static final String ACTIONS = "actions";
    private static final String REWARDS = "rewards";
    private static final String TERMINALS = "terminals";
    private static final String FC_4_OUTPUTS = "fc4_outputs";
    private static final String TRAIN = "train";
    private static final String SAVE_CONST = "save/Const";
    private static final String SAVE_RESTORE_ALL = "save/restore_all";
    private static final String INIT = "init";
    private static final String CKPT = "ckpt";
    private static final String SAVE_CONTROL_DEPENDENCY = "save/control_dependency";
    private static final int N_ACTIONS = 4;
    private static final int N_OUTPUTS = 4;
    private static final String checkpointPath = "checkpoint";
    private static final InputStream modelInputStream;

    static {
        modelInputStream = DQN.class.getClassLoader().getResourceAsStream("models/graph.pb");
    }

    private final int cols;
    private final int rows;
    private Session sess;
    private Tensor<String> checkpointPrefix;
    private long checkpointCount = 0;

    public DQN(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        init();
    }

    private void init() {
        try {
            final byte[] graphDef = IOUtils.toByteArray(modelInputStream);
            final Graph graph = new Graph();
            sess = new Session(graph);
            graph.importGraphDef(graphDef);
            checkpointPrefix = Tensors.create(Paths.get(checkpointPath, CKPT).toString());
            if (Files.exists(Paths.get(checkpointPath))) {
                System.out.println("loading checkpoint...");
                sess.runner().feed(SAVE_CONST, checkpointPrefix).addTarget(SAVE_RESTORE_ALL).run();
            } else {
                System.out.println("starting from scratch...");
                sess.runner().addTarget(INIT).run();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getGlobalStep() {
        return sess.runner()
                .fetch(GLOBAL_STEP)
                .run()
                .get(0)
                .intValue();
    }

    public Direction getMove(DQNGameState state) {
        final Tensor<Float> x = Tensor.create(state.getX(), Float.class);
        final Tensor<Float> q_t = Tensor.create(0.0f, Float.class);
        final Tensor<Float> actions = Tensor.create(new float[1][N_ACTIONS], Float.class);
        final Tensor<Float> rewards = Tensor.create(0.0f, Float.class);
        final Tensor<Float> terminals = Tensor.create(0.0f, Float.class);
        final float[][] y = new float[1][N_OUTPUTS];

        sess.runner()
                .feed(X, x)
                .feed(Q_T, q_t)
                .feed(ACTIONS, actions)
                .feed(REWARDS, rewards)
                .feed(TERMINALS, terminals)
                .fetch(FC_4_OUTPUTS)
                .run()
                .get(0)
                .copyTo(y);

        float maxValue = 0.0f;
        int maxIndex = 0;

        for (int i = 0; i < N_OUTPUTS; i++) {
            if (y[0][i] > maxValue) {
                maxValue = y[0][i];
                maxIndex = i;
            }
        }

        return getDirectionFromActionIndex(maxIndex);
    }

    public float train(List<Experience> experiences) {
        final float[][][][] currentStates = new float[experiences.size()][cols][rows][DQNGameState.N_STATE_MATRICES];
        final float[][][][] previousStates = new float[experiences.size()][cols][rows][DQNGameState.N_STATE_MATRICES];
        final float[] q = new float[experiences.size()];
        final float[] rewards = new float[experiences.size()];
        final float[] terminals = new float[experiences.size()];
        final float[][] actions = new float[experiences.size()][N_ACTIONS];

        for (int i = 0; i < experiences.size(); i++) {
            currentStates[i] = experiences.get(i).getCurrentState().getX()[0];
            previousStates[i] = experiences.get(i).getPreviousState().getX()[0];
            rewards[i] = experiences.get(i).getReward();
            terminals[i] = experiences.get(i).getCurrentState().getResult().isPresent() ? 1.0f : 0.0f;
            actions[i] = getActionFromDirection(experiences.get(i).getLastDirection());
        }

        Tensor<Float> t_x = Tensor.create(currentStates, Float.class);
        Tensor<Float> t_q = Tensor.create(q, Float.class);
        final Tensor<Float> t_actions = Tensor.create(actions, Float.class);
        final Tensor<Float> t_rewards = Tensor.create(rewards, Float.class);
        final Tensor<Float> t_terminals = Tensor.create(terminals, Float.class);

        final float[][] y = new float[experiences.size()][N_OUTPUTS];

        sess.runner()
                .feed(X, t_x)
                .feed(Q_T, t_q)
                .feed(ACTIONS, t_actions)
                .feed(REWARDS, t_rewards)
                .feed(TERMINALS, t_terminals)
                .fetch(FC_4_OUTPUTS)
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

        float cost = sess.runner()
                .feed(X, t_x)
                .feed(Q_T, t_q)
                .feed(ACTIONS, t_actions)
                .feed(REWARDS, t_rewards)
                .feed(TERMINALS, t_terminals)
                .addTarget(TRAIN)
                .fetch(COST)
                .run()
                .get(0)
                .floatValue();

        checkpointCount++;
        if (checkpointCount % CHECKPOINT_SAVE_COUNT == 0) {
            saveCheckpoint();
        }

        return cost;
    }

    private void saveCheckpoint() {
        System.out.println("saving checkpoint...");
        sess.runner().feed(SAVE_CONST, checkpointPrefix).addTarget(SAVE_CONTROL_DEPENDENCY).run();
    }

    private float[] getActionFromDirection(Direction direction) {
        final float[] action = new float[N_ACTIONS];

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
