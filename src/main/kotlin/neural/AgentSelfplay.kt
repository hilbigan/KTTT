package neural

import main.mergeCSVFiles
import java.io.File

fun main() {
    if(!File("training").exists()){
        File("training").mkdir()
    }

    val threads = 8

    // self-play and generate data
    val threadList = mutableListOf<Thread>()
    for(i in 1..threads){
        Thread {
            startSelfplay(File("models/model.h5"), File("training/data$i.csv"), gamesToPlay = 100, samplesPerGame = 8, thinkingTime = 500)
        }.apply {
            name = "Selfplay-$i"
            start()
            threadList.add(this)
        }
    }
    threadList.forEach { it.join() }

    // Merge files
    mergeCSVFiles(File("training/merge.csv"), *(1..threads).map { File("training/data$it.csv") }.toTypedArray())

    //TODO train
    //python3 models/keras-test.py
}