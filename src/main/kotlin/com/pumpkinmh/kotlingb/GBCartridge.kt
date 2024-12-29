package com.pumpkinmh.kotlingb

import java.io.File
import java.io.FileInputStream

@OptIn(ExperimentalUnsignedTypes::class)
class GBCartridge internal constructor(romFile: File) {

    private val cartridgeByteData: UByteArray

    init {
        // Store file bytes in a temporary expandable list, then transfer to a proper array
        val fileInputStream = FileInputStream(romFile)
        val fileBufferData = mutableListOf<Int>()

        var data: Int
        while(fileInputStream.read().also { data = it} != -1) {
            fileBufferData.add(data)
        }

        cartridgeByteData = UByteArray(fileBufferData.size)
        for(i in cartridgeByteData.indices) {
            cartridgeByteData[i] = fileBufferData[i].toUByte()
        }

        fileInputStream.close()
    }

    private val cartrideTypeByte: UByte
    private val romSize: UByte
    private val ramSize: UByte

    init {
        cartrideTypeByte = cartridgeByteData[0x147]
        romSize = cartridgeByteData[0x148]
        ramSize = cartridgeByteData[0x149]

        if(cartrideTypeByte.toUInt() != 0x00u) {
            throw NotImplementedError("This cartridge has a mapper. Mappers are not supported yet")
        }
    }
}