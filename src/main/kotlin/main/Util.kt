package main

import kotlin.random.Random

fun oct(i: Int): Int {
    return Integer.valueOf(i.toString(), 8)!!
}

fun popcnt(i: Int): Int = Integer.bitCount(i)

fun Boolean.toInt(): Int {
    return if(this) 1 else 0
}

/**
 * Prints timing information on the given task
 *
 * @param name Name of the task
 * @param runnable Task to be executed
 */
fun benchmark(name: String, runnable: () -> Unit){
    println("Running $name ...")
    val start = System.currentTimeMillis()
    runnable.invoke()
    println("Time: ${System.currentTimeMillis() - start}ms")
}

var totalGlobalTime: Long = 0
fun globalTime(runnable: () -> Unit){
    val start = System.currentTimeMillis()
    runnable.invoke()
    totalGlobalTime += System.currentTimeMillis() - start
}

private val random = Random(System.currentTimeMillis())
fun random(max: Int) = random.nextInt(max)
fun random() = random.nextDouble()

fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)