package main

abstract class GameState

class Running : GameState(){
    override fun toString(): String {
        return "Running()"
    }
}
class Tied : GameState(){
    override fun toString(): String {
        return "Tied()"
    }
}
class Won(val who: Int) : GameState(){
    override fun toString(): String {
        return "Won($who)"
    }
}