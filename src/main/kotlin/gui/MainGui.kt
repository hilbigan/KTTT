package gui

import ai.MCTS
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import main.Bitboard
import tornadofx.*


class KTTTApplication : App(GameView::class) {
    var board = Bitboard()

    // GUI AI Skill
    var mcts = getAI()

    private fun getAI() = MCTS(board, 200, 4, debug = true, ponder = false, player = 1)
}

class GameView : View() {

    val board: Bitboard
        get() = (app as KTTTApplication).board

    val mcts: MCTS
        get() = (app as KTTTApplication).mcts

    val buttons = mutableListOf<TTTButton>()

    override val root = hbox {
        gridpane {
            for (field in 0..8) {
                for (square in 0..8) {
                    val button = TTTButton(field, square, this@GameView)
                    buttons.add(button)
                    add(button, (field % 3) * 3 + square % 3, (field / 3) * 3 + square / 3)
                }
            }
        }
    }

    override fun onBeforeShow() {
        updateButtons()
    }

    fun updateButtons(disable: Boolean = false) {
        if (!buttons.isEmpty()) {
            buttons.forEach {
                it.update(board)
                it.isDisable = disable
            }
        }
    }

    fun onMove(){
        updateButtons(disable = true)

        runAsync {
            val move = mcts.nextMove(board)
            board.makeMove(move)
            Platform.runLater {
                updateButtons(disable = false)

                if(board.isGameOver()){
                    val alert = Alert(Alert.AlertType.NONE)
                    alert.headerText = "Game Over!"
                    alert.contentText = "Result: ${board.getGameState()}"
                    val buttonReset = ButtonType("New Game")
                    val buttonClose = ButtonType("Close")
                    alert.buttonTypes.setAll(buttonClose, buttonReset)

                    when (alert.showAndWait().orElseGet { buttonClose }) {
                        buttonReset -> {
                            (app as KTTTApplication).board = Bitboard()
                            updateButtons(disable = false)
                        }
                        else -> {
                            alert.close()
                        }
                    }
                }
            }
        }
    }
}

class TTTButton(val field: Int, val square: Int, view: GameView) : Button() {

    val SIZE = 32.0

    init {
        gridpaneConstraints {
            margin = tornadofx.insets(1.0)
            if (square % 3 == 2) {
                marginRight = 7.0
            }
            if (square > 5) {
                marginBottom = 7.0
            }
        }

        minWidth = SIZE
        minHeight = SIZE
        prefWidth = SIZE
        prefHeight = SIZE
        width = SIZE
        height = SIZE

        setOnAction {
            val move = view.board.buildMove(field, square)
            if (view.board.isLegal(move)) {
                view.board.makeMove(view.board.buildMove(field, square))
                view.onMove()
            }
        }
    }

    fun update(board: Bitboard) {
        val sym = board.toSymbol(board.buildMove(field, square)).trim()
        if (sym.length == 2) {
            if (sym == "[]") {
                style = "-fx-border-color: yellow"
            } else if (sym == "XX") {
                style = "-fx-background-color: red"
            } else {
                style = "-fx-background-color: blue"
            }
        } else {
            style = "-fx-border-color: gray"
        }
        font = Font.font("Arial", FontWeight.EXTRA_BOLD, 12.0)
        text = sym
    }

}

fun main() {
    Application.launch(KTTTApplication::class.java)
}