# Build

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

To run AutoFighter:
```
java -jar auto_fighter*.jar <name0> "<command0>" <name1> "<command1>"
```
e.g.:
```
java -jar auto_fighter-1.0.jar 10 mcts "java -jar kttt-1.0.jar 100 4 --mcts --persistent -d" nn "java -jar kttt-1.0.jar 100 4 --nn --persistent -f ../../models/model.h5 -d"
```
