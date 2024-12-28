package org.example

import com.pumpkinmh.kotlingb.GBCartridge
import java.io.File

fun main() {
    val myMemory = GBCartridge(File("tetris.gb"))

    val testShort: UShort = 0xACBDu
    val testByte: UByte = testShort.toInt().and(0xFF00).shr(8).toUByte()
    println("Hello World!")
}