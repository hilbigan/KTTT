package main

open class GameState
class Running : GameState(){
    override fun toString(): String {
        return "main.Running()"
    }
}
class Tied : GameState(){
    override fun toString(): String {
        return "main.Tied()"
    }
}
class Won(val who: Int) : GameState(){
    override fun toString(): String {
        return "main.Won($who)"
    }
}