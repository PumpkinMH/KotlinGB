package com.pumpkinmh.kotlingb

import kotlin.math.sign

@OptIn(ExperimentalUnsignedTypes::class)
class GBProcessor {
    enum class ByteRegister(val index: Int) {
        A(0),
        B(2),
        C(3),
        D(4),
        E(5),
        H(6),
        L(7),
        FLAG(1)
    }

    enum class ShortRegister(val highIndex: Int, val lowIndex: Int) {
        AF(0,1),
        BC(2,3),
        DE(4,5),
        HL(6,7);

        fun getIndexPair(): Pair<Int,Int> {
            return Pair(this.highIndex, this.lowIndex)
        }
    }

    val registers: UByteArray = UByteArray(8)

    var stackPointer: UShort = TODO()
    var programCounter: UShort = TODO()

    val memory: GBMemory = TODO()


    // Opcodes
    // First value in pair represents the amount to increment PC
    // Second value in pair represents the number of clock cycles used

    // No OPeration
    fun nop(): Pair<Int,Int> {
        return Pair(1,1)
    }

    // Load the value from the register on the right into the register on the left
    fun LD_r8_r8(targetRegister: ByteRegister, sourceRegister: ByteRegister): Pair<Int,Int> {
        registers[targetRegister.index] = registers[sourceRegister.index]

        return Pair(1,1)
    }

    // Load the value n8 (from byte 2) into the register r8
    fun LD_r8_n8(targetRegister: ByteRegister): Pair<Int,Int> {
        val sourceValue = memory[programCounter + 1u]
        registers[targetRegister.index] = sourceValue

        return Pair(2,2)
    }

    // Load the value n16 (from bytes 2 and 3) into register r16
    fun LD_r16_n16(targetRegister: ShortRegister): Pair<Int,Int> {
        val lowerByte = memory[programCounter + 1u]
        val upperByte = memory[programCounter + 2u]

        registers[targetRegister.highIndex] = upperByte
        registers[targetRegister.lowIndex] = lowerByte

        return Pair(3,3)
    }

    // Copy the value in register r8 to the address in register HL
    fun LD_HL_r8(sourceRegister: ByteRegister): Pair<Int,Int> {
        val sourceByte = registers[sourceRegister.index]

        val upperAddressByte = registers[ShortRegister.HL.highIndex]
        val lowerAddressByte = registers[ShortRegister.HL.lowIndex]

        val byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        memory[byteAddress] = sourceByte

        return Pair(1,2)
    }

    // Copy the byte n8 to the address in register HL
    fun LD_HL_n8(): Pair<Int,Int> {
        val sourceByte = memory[programCounter + 1u]

        val upperAddressbyte = registers[ShortRegister.HL.highIndex]
        val lowerAddressByte = registers[ShortRegister.HL.lowIndex]

        val byteAddress = Pair(upperAddressbyte, lowerAddressByte).toUShort()

        memory[byteAddress] = sourceByte

        return Pair(2,3)
    }

    // Copy the value at the address in register HL to register r8
    fun LD_r8_HL(targetRegister: ByteRegister): Pair<Int,Int> {
        val upperAddressByte = registers[ShortRegister.HL.highIndex]
        val lowerAddressByte = registers[ShortRegister.HL.lowIndex]
        val byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        val sourceByte = memory[byteAddress]
        registers[targetRegister.index] = sourceByte

        return Pair(1, 2)
    }

    // Copy the value in register A into the address in register r16
    fun LD_r16_A(targetRegister: ShortRegister): Pair<Int,Int> {
        val sourceByte = registers[ByteRegister.A.index]

        val upperAddressByte = registers[targetRegister.highIndex]
        val lowerAddressByte = registers[targetRegister.lowIndex]
        val byteAddress = Pair(upperAddressByte,lowerAddressByte).toUShort()

        memory[byteAddress] = sourceByte

        return Pair(1,2)
    }

    // Copy the value in register A into the address in n16
    fun LD_n16_A(): Pair<Int,Int> {
        val sourceByte = registers[ByteRegister.A.index]

        val upperAddressByte = memory[programCounter + 2u]
        val lowerAddressByte = memory[programCounter + 1u]
        val byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        memory[byteAddress] = sourceByte

        return Pair(3, 4)
    }

    // Copy the value in register A into the address in n16 such that the address is between $FF00 and $FFFF
    fun LDH_n16_A(): Pair<Int,Int> {
        val sourceByte = registers[ByteRegister.A.index]

        val offsetByte = memory[programCounter + 1u]
        val byteAddress = Pair(0xFFu.toUByte(), offsetByte).toUShort()

        memory[byteAddress] = sourceByte

        return Pair(2,3)
    }

    // Copy the value in register A into the byte at address $FF00 + register C
    fun LDH_C_A(): Pair<Int,Int> {
        val sourceByte = registers[ByteRegister.A.index]

        val offsetByte = registers[ByteRegister.C.index]
        val byteAddress = Pair(0xFFu.toUByte(), offsetByte).toUShort()

        memory[byteAddress] = sourceByte

        return Pair(1,2)
    }

    // Copy the byte from the address in r16 into register A
    fun LD_A_r16(sourceRegister: ShortRegister): Pair<Int,Int> {
        val upperAddressByte = registers[sourceRegister.highIndex]
        val lowerAddressByte = registers[sourceRegister.lowIndex]
        val byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        val sourceByte = memory[byteAddress]
        registers[ByteRegister.A.index] = sourceByte

        return Pair(1,2)
    }

    // Copy the byte at address n16 into register A
    fun LD_A_n16(): Pair<Int,Int> {
        val upperAddressByte = memory[programCounter + 2u]
        val lowerAddressByte = memory[programCounter + 1u]
        val byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        val sourceByte = memory[byteAddress]
        registers[ByteRegister.A.index] = sourceByte

        return Pair(3, 4)
    }

    // Copy the byte at address n16 into register A such that the address is between $FF00 and $FFFF
    fun LDH_A_n16(): Pair<Int, Int> {
        val offsetByte = memory[programCounter + 1u]
        val byteAddress = Pair(0xFFu.toUByte(), offsetByte).toUShort()

        val sourceByte = memory[byteAddress]
        registers[ByteRegister.A.index]

        return Pair(2,3)
    }

    // Copy the byte at address $FF00 + register C into register A
    fun LDH_A_C(): Pair<Int,Int> {
        val offsetByte = registers[ByteRegister.C.index]
        val byteAddress = Pair(0xFFu.toUByte(), offsetByte).toUShort()

        val sourceByte = memory[byteAddress]
        registers[ByteRegister.A.index] = sourceByte

        return Pair(1,2)
    }

    // Copy the value in register A into the address in HL, then increment HL
    fun LD_HLI_A(): Pair<Int,Int> {
        val sourceByte = registers[ByteRegister.A.index]

        val upperAddressByte = registers[ShortRegister.HL.highIndex]
        val lowerAddressByte = registers[ShortRegister.HL.lowIndex]
        var byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        memory[byteAddress++] = sourceByte

        registers[ShortRegister.HL.highIndex] = byteAddress.toBytePair().first
        registers[ShortRegister.HL.lowIndex] = byteAddress.toBytePair().second

        return Pair(1,2)
    }

    // Copy the value in register A into the address in HL, then decrement HL
    fun LD_HLD_A(): Pair<Int,Int> {
        val sourceByte = registers[ByteRegister.A.index]

        val upperAddressByte = registers[ShortRegister.HL.highIndex]
        val lowerAddressByte = registers[ShortRegister.HL.lowIndex]
        var byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        memory[byteAddress--] = sourceByte

        registers[ShortRegister.HL.highIndex] = byteAddress.toBytePair().first
        registers[ShortRegister.HL.lowIndex] = byteAddress.toBytePair().second

        return Pair(1,2)
    }

    // Copy the byte at the address in HL to register A, then decrement HL
    fun LD_A_HLD(): Pair<Int,Int> {
        val upperAddressByte = registers[ShortRegister.HL.highIndex]
        val lowerAddressByte = registers[ShortRegister.HL.lowIndex]
        var byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        val sourceByte = memory[byteAddress--]

        registers[ByteRegister.A.index] = sourceByte

        registers[ShortRegister.HL.highIndex] = byteAddress.toBytePair().first
        registers[ShortRegister.HL.lowIndex] = byteAddress.toBytePair().second

        return Pair(1,2)
    }

    // Copy the byte at the address in HL to register A, then increment HL
    fun LD_A_HLI(): Pair<Int,Int> {
        val upperAddressByte = registers[ShortRegister.HL.highIndex]
        val lowerAddressByte = registers[ShortRegister.HL.lowIndex]
        var byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        val sourceByte = memory[byteAddress++]

        registers[ByteRegister.A.index] = sourceByte

        registers[ShortRegister.HL.highIndex] = byteAddress.toBytePair().first
        registers[ShortRegister.HL.lowIndex] = byteAddress.toBytePair().second

        return Pair(1,2)
    }

    // Copy the value in n16 into the stack pointer register
    fun LD_SP_n16(): Pair<Int,Int> {
        val lowerByte = memory[programCounter + 1u]
        val upperByte = memory[programCounter + 2u]
        val addressShort = Pair(upperByte, lowerByte).toUShort()

        stackPointer = addressShort

        return Pair(3,3)
    }

    // Copy the lower stack pointer bytes into address n16 and upper bytes into n16 + 1
    fun LD_n16_SP(): Pair<Int,Int> {
        val lowerByte = memory[programCounter + 1u]
        val upperByte = memory[programCounter + 2u]
        val addressShort = Pair(upperByte,lowerByte).toUShort()

        memory[addressShort] = stackPointer.toBytePair().second
        memory[addressShort + 1u] = stackPointer.toBytePair().first

        return Pair(3,5)
    }

    // Add e8 to the stack pointer register and copy the result to register HL
    fun LD_HL_SPe8(): Pair<Int,Int> {
        val signedByte: Byte = memory[programCounter + 1u].toByte()
        stackPointer = (stackPointer.toInt() + signedByte.toInt()).toUShort()

        registers[ShortRegister.HL.highIndex] = stackPointer.toBytePair().first
        registers[ShortRegister.HL.lowIndex] = stackPointer.toBytePair().second

        TODO("Flag operations are not yet implemented")
        return Pair(2,3)
    }

    // Copy the value of register HL into the stack pointer register
    fun LD_SP_HL(): Pair<Int,Int> {
        val upperByte = registers[ShortRegister.HL.highIndex]
        val lowerByte = registers[ShortRegister.HL.lowIndex]

        stackPointer = Pair(upperByte, lowerByte).toUShort()

        return Pair(1,2)
    }



}