![kttt](kttt.png)

# Download

### GUI
User-friendly GUI: [KTTT-GUI](https://circleci-latest-artifact.herokuapp.com/hilbigan/KTTT/release/kttt-gui-1.0.jar)  
The AI difficulty is hard-coded to 2000ms/4x for now. To adjust, build yourself. Future versions might use the main program (KTTT artifact) as ai backend.

### KTTT
The main program, with configurable settings (See usage below): [KTTT](https://circleci-latest-artifact.herokuapp.com/hilbigan/KTTT/release/kttt-1.0.jar)

### Others
Let two different versions fight eachother: [AutoFighter](https://circleci-latest-artifact.herokuapp.com/hilbigan/KTTT/release/auto_fighter-1.0.jar)

[CircleCI-Link](https://circleci.com/gh/hilbigan/KTTT)

# Build

[![CircleCI](https://circleci.com/gh/hilbigan/KTTT/tree/master.svg?style=shield)](https://circleci.com/gh/hilbigan/KTTT/tree/master)

To build the GUI:
```
./gradlew jarGui
```
Edit ``src/main/kotlin/gui/MainGui`` to adjust AI strength.

To build KTTT (main AI):
```
./gradlew jarKTTT
```

To build AutoFighter (used to let two versions fight eachother):
```
./gradlew jarAutoFighter
```

To build Selfplay (lets agent fight itself and collects samples from the games for learning):
```
./gradlew jarSelfplay
```
# Run

To run the AI:
```
java -jar kttt*.jar [-h] [TIME] [THREADS] [--mcts] [--from-position FROM_POSITION]
                    [--movegen] [--movegen-depth MOVEGEN_DEPTH] [-r] [--persistent] [-p]
                    [-f MODEL_FILE] [-d]

// For more usage info
java -jar kttt*.jar -h
```
Available commands: 
- ``reset``: Reset board
- ``start``: Skip turn / let AI make first move
- ``draw``: Draw board (human-readable)
- ``exit``: Exit
- ``x y``, x, y ∈ [0, 8]: Makes a move in field x, square y. A field is a subboard of size 3x3, of which the whole
board contains 9 of. The square specifies the exact position within the 3x3 subboard. 0 is always on the top left,
8 on the bottom right. Examples: ``4 4`` is the center position. ``0 0`` is the position in the very top left corner.

To run AutoFighter:
```
java -jar auto_fighter*.jar <name0> "<command0>" <name1> "<command1>"
```
e.g.:
```
java -jar auto_fighter-1.0.jar 10 mcts "java -jar kttt-1.0.jar 100 4 --mcts --persistent -d" nn "java -jar kttt-1.0.jar 100 4 --nn --persistent -f ../../models/model.h5 -d"
java -jar auto_fighter-1.0.jar 10 mcts0 "java -jar kttt-1.0.jar 1000 4 --mcts --persistent -d" mcts1 "java -jar kttt-1.0.jar 1000 4 --mcts --persistent -d"
```

# Terminology
**Field**: A subboard of size 3x3, of which the board contains 9. In the image at the top, the center
field is taken by "O". Labelled 0 through 9 from top left to bottom right. Internally, fields are encoded in one-hot
format and can easily be represented as a three digit octal number (0o777 = All fields set, etc).  
**Square**: Nine squares make up one subboard. Labelled 0 through 9 from top left to bottom right.  
**Moves**: A move is a pair of (Field, Square). Examples: ``4 4`` is the center position. ``0 0`` is the position in the very top left corner.  
**Chance Move**: A move which, after being played, will allow the opponent to freely choose the field in which he wants to play his move.
(More exactly: A move (F, S), where S is a blocked field).  
**Moves - Internal representation**: A move is internally represented by an integer M:  
``M & (1 << 25)``: Set, if the move is a chance move. (Important if the move is to be undone)  
``M & (0xFFFF)``: The lower 16 bits are reserved for encoding the square. (One-Hot)  
``M & (0x1FF << 16)``: The next 9 bits are reserved for encoding the field. (One-Hot)  
**Players**: "**X**" = 0, "**O**" = 1