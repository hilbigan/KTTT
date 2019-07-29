import tensorflow
import numpy as np
from keras.models import Sequential
from keras.layers import Dense
from keras.layers import Activation
from keras.optimizers import Adam
import time
import pandas

model = Sequential()
model.add(Dense(units=81*4, activation='tanh', input_dim=81))
model.add(Dense(units=81*16, activation='tanh'))
model.add(Dense(units=81*4, activation='tanh'))
model.add(Dense(units=9, activation='tanh'))
model.add(Dense(units=1, activation='tanh'))

# uncomment to continue training saved model
model.load_weights("model.h5")

"""
start = time.time_ns()
model.predict(np.array([[0,0,0,0,0,0,0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0]]))
print(time.time_ns() - start)
"""

"""
print(model.predict(np.array([[
    1,1,1 ,0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    1,1,1, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    0,0,0, 0,0,0, 0,0,0,
    1,1,1, 0,0,0, 0,0,0]])))

model.save("model_predict_game_result.h5")
"""

model.compile(loss='mean_squared_error',
              optimizer='sgd',
              metrics=['acc'])

td = pandas.read_csv("../training/merge.csv", header=None)
x = td.iloc[:,0:81]
y = td.iloc[:,81]
model.fit(x, y, epochs=10, batch_size=16)
loss_and_metrics = model.evaluate(x, y, batch_size=128)
print(loss_and_metrics)

model.save("model.h5")
print("Saved model to disk")