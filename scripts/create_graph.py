import os.path

import tensorflow as tf

params = {
    'width': 21,
    'height': 19,
    'discount': .95,
    'lr': .0002
}

x = tf.placeholder('float', [None, params['width'], params['height'], 6], name='x')
q_t = tf.placeholder('float', [None], name='q_t')
actions = tf.placeholder("float", [None, 4], name='actions')
rewards = tf.placeholder("float", [None], name='rewards')
terminals = tf.placeholder("float", [None], name='terminals')

# Layer 1 (Convolutional)
layer_name = 'conv1';
size = 3;
channels = 6;
filters = 16;
stride = 1
w1 = tf.Variable(tf.random_normal([size, size, channels, filters], stddev=0.01),
                 name=layer_name + '_weights')
b1 = tf.Variable(tf.constant(0.1, shape=[filters]), name=layer_name + '_biases')
c1 = tf.nn.conv2d(x, w1, strides=[1, stride, stride, 1], padding='SAME',
                  name=layer_name + '_convs')
o1 = tf.nn.relu(tf.add(c1, b1), name=layer_name + '_activations')

# Layer 2 (Convolutional)
layer_name = 'conv2';
size = 3;
channels = 16;
filters = 32;
stride = 1
w2 = tf.Variable(tf.random_normal([size, size, channels, filters], stddev=0.01),
                 name=layer_name + '_weights')
b2 = tf.Variable(tf.constant(0.1, shape=[filters]), name=layer_name + '_biases')
c2 = tf.nn.conv2d(o1, w2, strides=[1, stride, stride, 1], padding='SAME',
                  name=layer_name + '_convs')
o2 = tf.nn.relu(tf.add(c2, b2), name=layer_name + '_activations')
o2_shape = o2.get_shape().as_list()

# Layer 3 (Fully connected)
layer_name = 'fc3';
hiddens = 256;
dim = o2_shape[1] * o2_shape[2] * o2_shape[3]
o2_flat = tf.reshape(o2, [-1, dim], name=layer_name + '_input_flat')
w3 = tf.Variable(tf.random_normal([dim, hiddens], stddev=0.01),
                 name=layer_name + '_weights')
b3 = tf.Variable(tf.constant(0.1, shape=[hiddens]), name=layer_name + '_biases')
ip3 = tf.add(tf.matmul(o2_flat, w3), b3, name=layer_name + '_ips')
o3 = tf.nn.relu(ip3, name=layer_name + '_activations')

# Layer 4
layer_name = 'fc4';
hiddens = 4;
dim = 256
w4 = tf.Variable(tf.random_normal([dim, hiddens], stddev=0.01),
                 name=layer_name + '_weights')
b4 = tf.Variable(tf.constant(0.1, shape=[hiddens]), name=layer_name + '_biases')
y = tf.add(tf.matmul(o3, w4), b4, name=layer_name + '_outputs')

# Q,Cost,Optimizer
discount = tf.constant(params['discount'])
yj = tf.add(rewards, tf.multiply(1.0 - terminals, tf.multiply(discount, q_t)))
Q_pred = tf.reduce_sum(tf.multiply(y, actions), reduction_indices=1)
cost = tf.reduce_sum(tf.pow(tf.subtract(yj, Q_pred), 2), name='cost')
global_step = tf.Variable(0, name='global_step', trainable=False)
optim = tf.train.AdamOptimizer(params['lr']).minimize(cost, global_step=global_step, name='train')

init = tf.global_variables_initializer()

saver_def = tf.train.Saver().as_saver_def()

with open(os.path.join(os.path.abspath(os.path.dirname(__file__)), '../src/main/resources/models/graph.pb'), 'wb') as f:
    f.write(tf.get_default_graph().as_graph_def().SerializeToString())

print('model saved')
