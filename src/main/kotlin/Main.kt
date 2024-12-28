package org.example

import com.pumpkinmh.kotlingb.GBCartridge
import com.pumpkinmh.kotlingb.shr
import java.io.File

fun main() {
//    val myMemory = GBCartridge(File("tetris.gb"))

    val testShort: UShort = 0xACBDu
    val testByte: UByte = testShort.toInt().and(0xFF00).shr(8).toUByte()

    val secondTestByte: UByte = testShort.and(0xFF00u).shr(8).toUByte()
    println("Hello World!")
}