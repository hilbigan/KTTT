package neural

import main.mergeCSVFiles
import java.io.File

fun main() {
    val threads = 8

    // self-play and generate data
    for(i in 1..threads){
        Thread {
            startSelfplay(File("models/model_predict_game_result.h5"), File("training/data$i.csv"),  150, 8)
        }.apply {
            start()
        }
    }

    // Merge files
    mergeCSVFiles(File("training/merge.csv"), *(1..threads).map { File("training/data$it.csv") }.toTypedArray())

    //TODO train
}