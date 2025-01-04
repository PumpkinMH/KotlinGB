package com.pumpkinmh.kotlingb

@OptIn(ExperimentalUnsignedTypes::class)
class GBMemory(internal val cartridge: GBCartridge) {
    companion object {
        const val ROM_BANK_FIXED_START = 0x0000
        const val ROM_BANK_FIXED_END = 0x3FFF

        const val ROM_BANK_DYNAMIC_START = 0x4000
        const val ROM_BANK_DYNAMIC_END = 0x7FFF

        const val VRAM_START = 0x8000
        const val VRAM_END = 0x9FFF

        const val EXTERNAL_RAM_START = 0xA000
        const val EXTERNAL_RAM_END = 0xBFFF

        const val WORK_RAM_FIXED_START = 0xC000
        const val WORK_RAM_FIXED_END = 0xCFFF

        const val WORK_RAM_DYNAMIC_START = 0xD000
        const val WORK_RAM_DYNAMIC_END = 0xDFFF

        const val ECHO_RAM_START = 0xE000
        const val ECHO_RAM_END = 0xFDFF

        const val OBJECT_ATTRIBUTE_START = 0xFE00
        const val OBJECT_ATTRIBUTE_END = 0xFE9F

        const val UNUSABLE_START = 0xFEA0
        const val UNUSABLE_END = 0xFEFF

        const val IO_REGISTERS_START = 0xFF00
        const val IO_REGISTERS_END = 0xFF7F

        const val HIGH_RAM_START = 0xFF80
        const val HIGH_RAM_END = 0xFFFE

        const val IE_REGISTER_START_END = 0xFFFF
    }

    internal val workRAM: UByteArray = UByteArray(WORK_RAM_DYNAMIC_END - WORK_RAM_FIXED_START)
    internal val highRAM: UByteArray = UByteArray(HIGH_RAM_END - HIGH_RAM_START)

    init {
        for(i in workRAM.indices) {
            workRAM[i] = (0x00..0xFF).random().toUByte()
        }

        for(i in highRAM.indices) {
            highRAM[i] = (0x00..0xFF).random().toUByte()
        }
    }

    internal operator fun get(address: Int): UByte {
        if(address > 0xFFFF) {
            throw IllegalArgumentException("Hex address " + Integer.toHexString(address) + " is too large for 16 bit address space")
        }

        when (address) {
            in ROM_BANK_FIXED_START..ROM_BANK_FIXED_END -> return cartridge[address]
            in ROM_BANK_DYNAMIC_START..ROM_BANK_DYNAMIC_END -> return cartridge[address]
            in VRAM_START..VRAM_END -> TODO()
            in EXTERNAL_RAM_START..EXTERNAL_RAM_END -> TODO()
            in WORK_RAM_FIXED_START..WORK_RAM_FIXED_END -> return workRAM[address - WORK_RAM_FIXED_START]
            in WORK_RAM_DYNAMIC_START..WORK_RAM_DYNAMIC_END -> return workRAM[address - WORK_RAM_FIXED_START]
            in ECHO_RAM_START..ECHO_RAM_END -> return workRAM[address - ECHO_RAM_START]
            in OBJECT_ATTRIBUTE_START..OBJECT_ATTRIBUTE_END -> TODO()
            in UNUSABLE_START..UNUSABLE_END -> TODO()
            in IO_REGISTERS_START..IO_REGISTERS_END -> TODO()
            in HIGH_RAM_START..HIGH_RAM_END -> return highRAM[address - HIGH_RAM_START]
            IE_REGISTER_START_END -> TODO()
        }
        return 0x00u // This should never be returned
    }

    internal operator fun get(address: UShort): UByte {
        return get(address.toInt())
    }

    internal operator fun get(address: UInt): UByte {
        return get(address.toInt())
    }

    internal operator fun get(lowerAddress: Int, upperAddress: Int): UShort {
        val leastSignificantByte = get(lowerAddress)
        val mostSignificantByte = get(upperAddress)

        val actualShort: UShort = Pair(mostSignificantByte, leastSignificantByte).toUShort()

        return actualShort
    }

    internal operator fun get(lowerAddress: UShort, upperAddress: UShort): UShort {
        return get(lowerAddress.toInt(), upperAddress.toInt())
    }

    internal operator fun get(lowerAddress: UInt, upperAddress: UInt): UShort {
        return get(lowerAddress.toInt(),upperAddress.toInt())
    }

    internal operator fun set(address: Int, data: UByte) {
        if(address > 0xFFFF) {
            throw IllegalArgumentException("Hex address " + Integer.toHexString(address) + " is too large for 16 bit address space")
        }

        TODO()
    }

    internal operator fun set(address: UShort, data: UByte) {
        set(address.toInt(), data)
    }

    internal operator fun set(address: UInt, data: UByte) {
        set(address.toInt(), data)
    }

    internal operator fun set(lowerAddress: Int, upperAddress: Int, data: UShort) {
        val bytePair = data.toUBytePair()
        val leastSignifcantByte: UByte = bytePair.second
        val mostSignificantByte:UByte = bytePair.first

        set(lowerAddress,leastSignifcantByte)
        set(upperAddress,mostSignificantByte)

        TODO()
    }

    internal operator fun set(lowerAddress: UShort, upperAddress: UShort, data: UShort) {
        set(lowerAddress.toInt(), upperAddress.toInt(), data)
    }

    internal operator fun set(lowerAddress: UInt, upperAddress: UInt, data: UShort) {
        set(lowerAddress.toInt(), upperAddress.toInt(), data)
    }

}