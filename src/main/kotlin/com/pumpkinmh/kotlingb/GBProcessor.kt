package com.pumpkinmh.kotlingb

@OptIn(ExperimentalUnsignedTypes::class)
class GBProcessor {
    enum class ByteRegister(val registerIndex: Int) {
        REGISTER_A(0),
        REGISTER_B(2),
        REGISTER_C(3),
        REGISTER_D(4),
        REGISTER_E(5),
        REGISTER_H(6),
        REGISTER_L(7),
        REGISTER_FLAG(1)
    }

    enum class ShortRegister(val highRegisterIndex: Int, val lowRegisterIndex: Int) {
        REGISTER_AF(0,1),
        REGISTER_BC(2,3),
        REGISTER_DE(4,5),
        REGISTER_HL(6,7)
    }

    val registers: UByteArray = UByteArray(8)

    var stackPointer: UShort = TODO()
    var programCounter: UShort = TODO()

    val memory: GBMemory = TODO()


    // Opcodes
    // First value in pair represents the amount to increment PC
    // Second value in pair represents the number of clock cycles used

    // Load the value from the register on the right into the register on the left
    fun LD_r8_r8(targetRegister: ByteRegister, sourceRegister: ByteRegister): Pair<Int,Int> {
        registers[targetRegister.registerIndex] = registers[sourceRegister.registerIndex]

        return Pair(1,1)
    }

    // Load the value n8 (from byte 2) into the register r8
    fun LD_r8_n8(targetRegister: ByteRegister): Pair<Int,Int> {
        val sourceValue = memory[programCounter + 1u]
        registers[targetRegister.registerIndex] = sourceValue

        return Pair(2,2)
    }

    // Load the value n16 (from bytes 2 and 3) into register r16
    fun LD_r16_n16(targetRegister: ShortRegister): Pair<Int,Int> {
        val lowerByte = memory[programCounter + 1u]
        val upperByte = memory[programCounter + 2u]

        registers[targetRegister.highRegisterIndex] = upperByte
        registers[targetRegister.lowRegisterIndex] = lowerByte

        return Pair(3,3)
    }

    fun LD_HL_r8(sourceRegister: ShortRegister): Pair<Int,Int> {
        TODO()
    }



}