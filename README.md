# Build

To build KTTT (main AI):
```
./gradlew jarKTTT
```

To build AutoFighter (used to let two versions fight eachother):
```
./gradlew jarAutoFighter
```

# Run

To run the AI:
```
java -jar kttt*.jar [time] [threads] [repl]
```

To run AutoFighter:
```
java -jar auto_fighter*.jar <name0> "<command0>" <name1> "<command1>"
```
e.g.:
```
java -jar auto_fighter*.jar 5 kt0 "java -jar kttt-1.0.jar 3000 1" kt1 "java -jar kttt-1.0.jar 1500 8"
```
