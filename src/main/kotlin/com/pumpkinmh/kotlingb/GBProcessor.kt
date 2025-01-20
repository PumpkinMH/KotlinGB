package com.pumpkinmh.kotlingb

import kotlin.math.pow

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

    enum class Flag(val bitIndex: Int) {
        Z(7),
        N(6),
        H(5),
        C(4)
    }

    enum class JumpCondition() {
        Z,
        NZ,
        C,
        NC
    }

    enum class State() {
        NORMAL,
        HALTED,
        STOPPED
    }

    val registers: UByteArray = UByteArray(8)

    var stackPointer: UShort = TODO()
    var programCounter: UShort = TODO()

    val memory: GBMemory = TODO()

    var enableInterrupts: Boolean
    var enableInterruptsNextInstruction: Boolean
    var cpuState: State


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

        registers[ShortRegister.HL.highIndex] = byteAddress.toUBytePair().first
        registers[ShortRegister.HL.lowIndex] = byteAddress.toUBytePair().second

        return Pair(1,2)
    }

    // Copy the value in register A into the address in HL, then decrement HL
    fun LD_HLD_A(): Pair<Int,Int> {
        val sourceByte = registers[ByteRegister.A.index]

        val upperAddressByte = registers[ShortRegister.HL.highIndex]
        val lowerAddressByte = registers[ShortRegister.HL.lowIndex]
        var byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        memory[byteAddress--] = sourceByte

        registers[ShortRegister.HL.highIndex] = byteAddress.toUBytePair().first
        registers[ShortRegister.HL.lowIndex] = byteAddress.toUBytePair().second

        return Pair(1,2)
    }

    // Copy the byte at the address in HL to register A, then decrement HL
    fun LD_A_HLD(): Pair<Int,Int> {
        val upperAddressByte = registers[ShortRegister.HL.highIndex]
        val lowerAddressByte = registers[ShortRegister.HL.lowIndex]
        var byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        val sourceByte = memory[byteAddress--]

        registers[ByteRegister.A.index] = sourceByte

        registers[ShortRegister.HL.highIndex] = byteAddress.toUBytePair().first
        registers[ShortRegister.HL.lowIndex] = byteAddress.toUBytePair().second

        return Pair(1,2)
    }

    // Copy the byte at the address in HL to register A, then increment HL
    fun LD_A_HLI(): Pair<Int,Int> {
        val upperAddressByte = registers[ShortRegister.HL.highIndex]
        val lowerAddressByte = registers[ShortRegister.HL.lowIndex]
        var byteAddress = Pair(upperAddressByte, lowerAddressByte).toUShort()

        val sourceByte = memory[byteAddress++]

        registers[ByteRegister.A.index] = sourceByte

        registers[ShortRegister.HL.highIndex] = byteAddress.toUBytePair().first
        registers[ShortRegister.HL.lowIndex] = byteAddress.toUBytePair().second

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

        memory[addressShort] = stackPointer.toUBytePair().second
        memory[addressShort + 1u] = stackPointer.toUBytePair().first

        return Pair(3,5)
    }

    // Add e8 to the stack pointer register and copy the result to register HL
    fun LD_HL_SPe8(): Pair<Int,Int> {
        val signedByte: Byte = memory[programCounter + 1u].toByte()
        val oldLowerByte = stackPointer.toUBytePair().second
        stackPointer = (stackPointer.toInt() + signedByte.toInt()).toUShort()

        registers[ShortRegister.HL.highIndex] = stackPointer.toUBytePair().first
        registers[ShortRegister.HL.lowIndex] = stackPointer.toUBytePair().second

        setFlag(false, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(carryFrom(oldLowerByte.toUInt(), signedByte.toUInt(), 4), Flag.H)
        setFlag(carryFrom(oldLowerByte.toUInt(), signedByte.toUInt(), 8),Flag.C)

        return Pair(2,3)
    }

    // Copy the value of register HL into the stack pointer register
    fun LD_SP_HL(): Pair<Int,Int> {
        val upperByte = registers[ShortRegister.HL.highIndex]
        val lowerByte = registers[ShortRegister.HL.lowIndex]

        stackPointer = Pair(upperByte, lowerByte).toUShort()

        return Pair(1,2)
    }

    // Add r8 and the carry bit to register A
    fun ADC_A_r8(sourceRegister: ByteRegister): Pair<Int, Int> {
        val carry = getCarryBit()
        val result = (registers[ByteRegister.A.index] + registers[sourceRegister.index] + carry).toUByte()
        registers[ByteRegister.A.index] = result

        setFlag(result.toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(carryFrom(registers[ByteRegister.A.index], registers[sourceRegister.index], carry, 4), Flag.H)
        setFlag(carryFrom(registers[ByteRegister.A.index],registers[sourceRegister.index], carry, 8), Flag.C)

        return Pair(1,1)
    }

    // Add the byte at the address in HL and the carry bit to register A
    fun ADC_A_HL(): Pair<Int,Int> {
        val carry = getCarryBit()
        val upperByte = registers[ShortRegister.HL.highIndex]
        val lowerByte = registers[ShortRegister.HL.lowIndex]
        val byteAddress = Pair(upperByte, lowerByte).toUShort()

        val sum = memory[byteAddress] + carry
        registers[ByteRegister.A.index] = sum.toUByte()

        setFlag(sum.toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(carryFrom(memory[byteAddress], carry, 4), Flag.H)
        setFlag(carryFrom(memory[byteAddress], carry, 8), Flag.C)

        return Pair(1,2)
    }

    // Add the byte n8 and carry bit to register A
    fun ADC_A_n8(): Pair<Int,Int> {
        val carryBit = getCarryBit()
        val immediateValue = memory[programCounter + 1u]
        val registerValue = registers[ByteRegister.A.index]

        val sum = (carryBit + immediateValue + registerValue)
        registers[ByteRegister.A.index] = (carryBit + immediateValue + registerValue).toUByte()

        setFlag(sum == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(carryFrom(carryBit, immediateValue, registerValue,4), Flag.H)
        setFlag(carryFrom(carryBit, immediateValue, registerValue, 8), Flag.C)

        return Pair(2,2)
    }

    // Add the value in r8 to register A
    fun ADD_A_r8(sourceRegister: ByteRegister): Pair<Int,Int> {
        val registerByte = registers[sourceRegister.index]
        val aByte = registers[ByteRegister.A.index]

        val sum = registerByte + aByte
        registers[ByteRegister.A.index] = sum.toUByte()

        setFlag(sum == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(carryFrom(registerByte,aByte,4), Flag.H)
        setFlag(carryFrom(registerByte,aByte,8), Flag.C)

        return Pair(1,1)
    }

    // Add the byte at the address in HL to register A
    fun ADD_A_HL(): Pair<Int,Int> {
        val upperAddressByte = registers[ShortRegister.HL.highIndex]
        val lowerAddressByte = registers[ShortRegister.HL.lowIndex]
        val address = Pair(upperAddressByte, lowerAddressByte).toUShort()

        val byteValue = memory[address]
        val registerValue = registers[ByteRegister.A.index]
        val sum = byteValue + registerValue

        registers[ByteRegister.A.index] = sum.toUByte()

        setFlag(sum == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(carryFrom(byteValue, registerValue, 4),Flag.H)
        setFlag(carryFrom(byteValue, registerValue, 8), Flag.C)

        return Pair(1,2)
    }

    // Add the value n8 into register A
    fun ADD_A_n8(): Pair<Int,Int> {
        val immediateValue = memory[programCounter + 1u]
        val registerValue = registers[ByteRegister.A.index]
        val sum = immediateValue + registerValue

        registers[ByteRegister.A.index] = sum.toUByte()

        setFlag(sum == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(carryFrom(immediateValue, registerValue, 4), Flag.H)
        setFlag(carryFrom(immediateValue,registerValue,8), Flag.C)

        return Pair(2,2)
    }

    // Add the value in r16 to register HL
    fun ADD_HL_r16(sourceRegister: ShortRegister): Pair<Int,Int> {
        val lowerRegister = registers[sourceRegister.lowIndex]
        val upperRegister = registers[sourceRegister.highIndex]
        val registerShort = Pair(upperRegister,lowerRegister).toUShort()

        val lowerHL = registers[ShortRegister.HL.lowIndex]
        val upperHL = registers[ShortRegister.HL.highIndex]
        val hlShort = Pair(upperHL, lowerHL).toUShort()

        val sum = registerShort + hlShort
        registers[ShortRegister.HL.lowIndex] = sum.toUShort().toUBytePair().second
        registers[ShortRegister.HL.highIndex] = sum.toUShort().toUBytePair().first

        // NO Z
        setFlag(false, Flag.N)
        setFlag(carryFrom(registerShort.toUInt(), hlShort.toUInt(), 12), Flag.H)
        setFlag(carryFrom(registerShort.toUInt(), hlShort.toUInt(), 16), Flag.C)

        return Pair(1,2)
    }

    fun ADD_HL_SP(): Pair<Int,Int> {
        val lowerHL = registers[ShortRegister.HL.lowIndex]
        val upperHL = registers[ShortRegister.HL.highIndex]
        val hlShort = Pair(upperHL, lowerHL).toUShort()

        val sum = stackPointer + hlShort
        registers[ShortRegister.HL.lowIndex] = sum.toUShort().toUBytePair().second
        registers[ShortRegister.HL.highIndex] = sum.toUShort().toUBytePair().first

        // NO Z
        setFlag(false, Flag.N)
        setFlag(carryFrom(stackPointer.toUInt(), hlShort.toUInt(), 12), Flag.H)
        setFlag(carryFrom(stackPointer.toUInt(), hlShort.toUInt(), 16), Flag.C)

        return Pair(1,2)
    }

    fun ADD_SP_e8(): Pair<Int,Int> {
        val signedByte: Byte = memory[programCounter + 1u].toByte()

        val sum = stackPointer.toInt() + signedByte

        stackPointer = sum.toUShort()

        setFlag(false, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(carryFrom(signedByte.toUInt(), stackPointer.toUInt(), 4), Flag.H)
        setFlag(carryFrom(signedByte.toUInt(), stackPointer.toUInt(), 8), Flag.C)

        return Pair(2,4)
    }

    fun CP_A_r8(sourceRegister: ByteRegister): Pair<Int,Int> {
        val registerValue = registers[ByteRegister.A.index]
        val sourceValue = registers[sourceRegister.index]
        val result = registerValue - sourceValue

        setFlag(result == 0u, Flag.Z)
        setFlag(true, Flag.N)
        setFlag(borrowFrom(registerValue, sourceValue, 4), Flag.H)
        setFlag(sourceValue > registerValue, Flag.C)

        return Pair(1,1)
    }

    fun CP_A_HL(): Pair<Int,Int> {
        val upperAddress = registers[ShortRegister.HL.highIndex]
        val lowerAddress = registers[ShortRegister.HL.lowIndex]
        val byteAddress = Pair(upperAddress, lowerAddress).toUShort()

        val pointedByte = memory[byteAddress]
        val registerByte = registers[ByteRegister.A.index]
        val result = registerByte - pointedByte

        setFlag(result == 0u, Flag.Z)
        setFlag(true, Flag.N)
        setFlag(borrowFrom(registerByte,pointedByte,4), Flag.H)
        setFlag(pointedByte > registerByte, Flag.C)

        return Pair(1,2)
    }

    fun CP_A_n8(): Pair<Int, Int> {
        val immediateByte = memory[programCounter + 1u]
        val registerByte = registers[ByteRegister.A.index]
        val result = registerByte - immediateByte

        setFlag(result == 0u, Flag.Z)
        setFlag(true, Flag.N)
        setFlag(borrowFrom(registerByte, immediateByte, 4), Flag.H)
        setFlag(immediateByte > registerByte, Flag.C)

        return Pair(2,2)
    }

    fun DEC_r8(sourceRegister: ByteRegister): Pair<Int,Int> {
        val oldValue = registers[sourceRegister.index]
        val result = oldValue- 1u
        registers[sourceRegister.index] = result.toUByte()

        setFlag(result == 0u, Flag.Z)
        setFlag(true, Flag.N)
        setFlag(borrowFrom(oldValue, 1u, 4), Flag.H)

        return Pair(1,1)
    }

    fun DEC_HL(): Pair<Int,Int> {
        val upperAddress = registers[ShortRegister.HL.highIndex]
        val lowerAddress = registers[ShortRegister.HL.lowIndex]
        val byteAddress = Pair(upperAddress,lowerAddress).toUShort()

        val oldValue = memory[byteAddress]
        val result = oldValue - 1u
        memory[byteAddress] = result.toUByte()

        setFlag(result == 0u, Flag.Z)
        setFlag(true, Flag.N)
        setFlag(borrowFrom(oldValue, 1u, 4), Flag.H)

        return Pair(1,3)
    }

    fun INC_r8(sourceRegister: ByteRegister): Pair<Int,Int> {
        val oldValue = registers[sourceRegister.index]
        val result = oldValue + 1u
        registers[sourceRegister.index] = result.toUByte()

        setFlag(result == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(carryFrom(oldValue, 1u, 4), Flag.H)

        return Pair(1,1)
    }

    fun INC_HL(): Pair<Int,Int> {
        val upperAddress = registers[ShortRegister.HL.highIndex]
        val lowerAddress = registers[ShortRegister.HL.lowIndex]
        val byteAddress = Pair(upperAddress,lowerAddress).toUShort()

        val oldValue = memory[byteAddress]
        val result = oldValue + 1u
        memory[byteAddress] = result.toUByte()

        setFlag(result == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(carryFrom(oldValue, 1u, 4), Flag.H)

        return Pair(1,3)
    }

    fun SBC_A_r8(sourceRegister: ByteRegister): Pair<Int,Int> {
        val minuend = registers[ByteRegister.A.index]
        val subtrahend = registers[sourceRegister.index] + getCarryBit()
        val result = minuend - subtrahend
        registers[ByteRegister.A.index] = result.toUByte()

        setFlag(result == 0u, Flag.Z)
        setFlag(true, Flag.N)
        setFlag(borrowFrom(minuend.toUInt(),subtrahend,4), Flag.H)
        setFlag(subtrahend > minuend, Flag.C)

        return Pair(1,1)
    }

    fun SBC_A_HL(): Pair<Int, Int> {
        val upperAddress = registers[ShortRegister.HL.highIndex]
        val lowerAddress = registers[ShortRegister.HL.lowIndex]
        val byteAddress = Pair(upperAddress,lowerAddress).toUShort()

        val minuend = registers[ByteRegister.A.index]
        val subtrahend = memory[byteAddress] + getCarryBit()
        val result = minuend - subtrahend
        registers[ByteRegister.A.index] = result.toUByte()

        setFlag(result == 0u, Flag.Z)
        setFlag(true, Flag.N)
        setFlag(borrowFrom(minuend.toUInt(),subtrahend,4), Flag.H)
        setFlag(subtrahend > minuend, Flag.C)

        return Pair(1,2)
    }

    fun SBC_A_n8(): Pair<Int,Int> {
        val minuend = registers[ByteRegister.A.index]
        val subtrahend = memory[programCounter + 1u] + getCarryBit()
        val result = minuend - subtrahend
        registers[ByteRegister.A.index] = result.toUByte()

        setFlag(result == 0u, Flag.Z)
        setFlag(true, Flag.N)
        setFlag(borrowFrom(minuend.toUInt(),subtrahend,4), Flag.H)
        setFlag(subtrahend > minuend, Flag.C)

        return Pair(2,2)
    }

    fun SUB_A_r8(sourceRegister: ByteRegister): Pair<Int,Int> {
        val minuend = registers[ByteRegister.A.index]
        val subtrahend = registers[sourceRegister.index]
        val result = minuend - subtrahend
        registers[ByteRegister.A.index] = result.toUByte()

        setFlag(result == 0u, Flag.Z)
        setFlag(true, Flag.N)
        setFlag(borrowFrom(minuend, subtrahend, 4), Flag.H)
        setFlag(subtrahend > minuend, Flag.C)

        return Pair(1,1)
    }

    fun SUB_A_HL(): Pair<Int,Int> {
        val upperAddress = registers[ShortRegister.HL.highIndex]
        val lowerAddress = registers[ShortRegister.HL.lowIndex]
        val byteAddress = Pair(upperAddress,lowerAddress).toUShort()

        val minuend = registers[ByteRegister.A.index]
        val subtrahend = memory[byteAddress]
        val result = minuend - subtrahend
        registers[ByteRegister.A.index] = result.toUByte()

        setFlag(result == 0u, Flag.Z)
        setFlag(true, Flag.N)
        setFlag(borrowFrom(minuend, subtrahend, 4), Flag.H)
        setFlag(subtrahend > minuend, Flag.C)

        return Pair(1,2)
    }

    fun SUB_A_n8(): Pair<Int,Int> {
        val minuend = registers[ByteRegister.A.index]
        val subtrahend = memory[programCounter + 1u]
        val result = minuend - subtrahend
        registers[ByteRegister.A.index] = result.toUByte()

        setFlag(result == 0u, Flag.Z)
        setFlag(true, Flag.N)
        setFlag(borrowFrom(minuend, subtrahend, 4), Flag.H)
        setFlag(subtrahend > minuend, Flag.C)

        return Pair(2,2)
    }

    fun DEC_r16(sourceRegister: ShortRegister): Pair<Int,Int> {
        val upperByte = registers[sourceRegister.highIndex]
        val lowerByte = registers[sourceRegister.lowIndex]

        var registerValue = Pair(upperByte,lowerByte).toUShort()
        registerValue = (registerValue - 1u).toUShort()

        registers[sourceRegister.highIndex] = registerValue.toUBytePair().first
        registers[sourceRegister.lowIndex] = registerValue.toUBytePair().second

        return Pair(1,2)
    }

    fun INC_r16(sourceRegister: ShortRegister): Pair<Int,Int> {
        val upperByte = registers[sourceRegister.highIndex]
        val lowerByte = registers[sourceRegister.lowIndex]

        var registerValue = Pair(upperByte,lowerByte).toUShort()
        registerValue = (registerValue + 1u).toUShort()

        registers[sourceRegister.highIndex] = registerValue.toUBytePair().first
        registers[sourceRegister.lowIndex] = registerValue.toUBytePair().second

        return Pair(1,2)
    }

    fun AND_A_source(value: UByte) {
        registers[ByteRegister.A.index] = registers[ByteRegister.A.index] and value

        setFlag(registers[ByteRegister.A.index].toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(true, Flag.H)
        setFlag(false, Flag.C)
    }

    fun AND_A_r8(source: ByteRegister): Pair<Int,Int> {
        AND_A_source(getFromRegister(source))

        return Pair(1,1)
    }

    fun AND_A_HL(): Pair<Int,Int> {
        AND_A_source(getFromAddressInHL())

        return Pair(1,2)
    }

    fun AND_A_n8(): Pair<Int,Int> {
        AND_A_source(getFromImmediate())

        return Pair(2,2)
    }

    fun CPL(): Pair<Int,Int> {
        registers[ByteRegister.A.index] = registers[ByteRegister.A.index].inv()

        setFlag(true, Flag.N)
        setFlag(true, Flag.H)

        return Pair(1,1)
    }

    fun OR_A_source(value: UByte) {
        registers[ByteRegister.A.index] = registers[ByteRegister.A.index].or(value)

        setFlag(registers[ByteRegister.A.index].toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(false,Flag.H)
        setFlag(false, Flag.C)
    }

    fun OR_A_r8(source: ByteRegister): Pair<Int,Int> {
        OR_A_source(registers[source.index])

        return Pair(1,1)
    }

    fun OR_A_HL(): Pair<Int,Int> {
        OR_A_source(getFromAddressInHL())

        return Pair(1,2)
    }

    fun OR_A_n8(): Pair<Int,Int> {
        OR_A_source(getFromImmediate())

        return Pair(2,2)
    }

    fun XOR_A_source(value: UByte) {
        registers[ByteRegister.A.index] = registers[ByteRegister.A.index].xor(value)

        setFlag(registers[ByteRegister.A.index].toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(false,Flag.H)
        setFlag(false, Flag.C)
    }

    fun XOR_A_r8(source: ByteRegister): Pair<Int, Int> {
        XOR_A_source(registers[source.index])

        return Pair(1,1)
    }

    fun XOR_A_HL(): Pair<Int, Int> {
        XOR_A_source(getFromAddressInHL())

        return Pair(1,2)
    }

    fun XOR_A_n8(): Pair<Int,Int> {
        XOR_A_source(getFromImmediate())

        return Pair(2,2)
    }

    fun BIT_u3_source(value: UByte) {
        val bitIndex = getFromImmediate()
        val setZero = value[bitIndex.toInt()]

        setFlag(setZero, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(true, Flag.H)
    }

    fun BIT_u3_r8(source: ByteRegister): Pair<Int,Int> {
        BIT_u3_source(getFromRegister(source))

        return Pair(2,2)
    }

    fun BIT_u3_HL(): Pair<Int,Int> {
        BIT_u3_source(getFromAddressInHL())

        return Pair(2,3)
    }

    fun RES_u3_source(value: UByte): UByte {
        val bitIndex = getFromImmediate()
        return value.setBit(bitIndex.toInt(), false)
    }

    fun RES_u3_r8(register: ByteRegister): Pair<Int,Int> {
        registers[register.index] = RES_u3_source(registers[register.index])

        return Pair(2,2)
    }

    fun RES_u3_HL(): Pair<Int,Int> {
        setToAddressInHL(RES_u3_source(getFromAddressInHL()))

        return Pair(2,4)
    }

    fun SET_u3_source(value: UByte): UByte {
        val bitIndex = getFromImmediate()
        return value.setBit(bitIndex.toInt(), true)
    }

    fun SET_u3_r8(register: ByteRegister): Pair<Int, Int> {
        setToRegister(SET_u3_source(getFromRegister(register)), register)

        return Pair(2,2)
    }

    fun SET_u3_HL(): Pair<Int, Int> {
        setToAddressInHL(SET_u3_source(getFromAddressInHL()))

        return Pair(2,4)
    }

    // Rotate left by 1 through the carry flag
    fun RL_source(value: UByte): UByte {
        var result = value.shl(1)
        if(getFlag(Flag.C)) {
            ++result
        }
        if(value >= 128u) {
            setFlag(true, Flag.C)
        } else {
            setFlag(false, Flag.C)
        }
        setFlag(result.toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(false, Flag.H)

        return result
    }

    fun RL_r8(register: ByteRegister): Pair<Int,Int> {
        setToRegister(RL_source(getFromRegister(register)), register)

        return Pair(2,2)
    }

    fun RL_HL(): Pair<Int,Int> {
        setToAddressInHL(RL_source(getFromAddressInHL()))

        return Pair(2,4)
    }

    fun RLA(): Pair<Int,Int> {
        setToRegister(RL_source(getFromRegister(ByteRegister.A)), ByteRegister.A)

        setFlag(false, Flag.Z)

        return Pair(1,1)
    }

    // Rotate left by 1, set carry flag based on previous most significant bit
    fun RLC_source(value: UByte): UByte {
        var result = value.shl(1)
        if(value >= 128u) {
            setFlag(true, Flag.C)
            ++result
        } else {
            setFlag(false, Flag.C)
        }

        setFlag(result.toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(false, Flag.H)

        return result
    }

    fun RLC_r8(register: ByteRegister): Pair<Int,Int> {
        setToRegister(RLC_source(getFromRegister(register)), register)

        return Pair(2,2)
    }

    fun RLC_HL(): Pair<Int,Int> {
        setToAddressInHL(RLC_source(getFromAddressInHL()))

        return Pair(2,4)
    }

    fun RLCA(): Pair<Int,Int> {
        RLC_r8(ByteRegister.A)

        setFlag(false, Flag.Z)

        return Pair(1,1)
    }

    fun RR_source(value: UByte): UByte {
        var result = value.shr(1)
        if(getFlag(Flag.C)) {
            result = (result + 128u).toUByte()
        }
        if((value % 2u) == 1u) {
            setFlag(true, Flag.C)
        } else {
            setFlag(false, Flag.C)
        }

        setFlag(result.toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(false, Flag.H)

        return result
    }

    fun RR_r8(register: ByteRegister): Pair<Int, Int> {
        setToRegister(RR_source(getFromRegister(register)), register)

        return Pair(2,2)
    }

    fun RR_HL(): Pair<Int,Int> {
        setToAddressInHL(RR_source(getFromAddressInHL()))

        return Pair(2,4)
    }

    fun RRA(): Pair<Int,Int> {
        RR_r8(ByteRegister.A)

        setFlag(false, Flag.Z)

        return Pair(1,1)
    }

    fun RRC_source(value: UByte): UByte {
        var result = value.shr(1)
        if((value % 2u) == 1u) {
            result = (result + 128u).toUByte()
            setFlag(true, Flag.C)
        } else {
            setFlag(false, Flag.C)
        }

        setFlag(result.toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(false, Flag.H)

        return result
    }

    fun RRC_r8(register: ByteRegister): Pair<Int, Int> {
        setToRegister(RRC_source(getFromRegister(register)), register)

        return Pair(2,2)
    }

    fun RRC_HL(): Pair<Int,Int> {
        setToAddressInHL(RRC_source(getFromAddressInHL()))

        return Pair(2,4)
    }

    fun RRCA(): Pair<Int,Int> {
        RRC_r8(ByteRegister.A)

        setFlag(false, Flag.Z)

        return Pair(1,1)
    }

    fun SLA_source(value: UByte): UByte {
        val result = value.toByte().shla().toUByte()
        setFlag(result.toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(false, Flag.H)
        setFlag(value >= 128u, Flag.C)

        return result
    }

    fun SLA_r8(register: ByteRegister): Pair<Int, Int> {
        setToRegister(SLA_source(getFromRegister(register)), register)

        return Pair(2,2)
    }

    fun SLA_HL(): Pair<Int,Int> {
        setToAddressInHL(SLA_source(getFromAddressInHL()))

        return Pair(2,4)
    }

    fun SRA_source(value: UByte): UByte {
        var result = value.shr(1)
        if(value >= 128u) {
            result = (result + 128u).toUByte()
        }

        setFlag(result.toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(false, Flag.H)
        setFlag(value % 2u == 1u, Flag.C)

        return result
    }

    fun SRA_r8(register: ByteRegister): Pair<Int, Int> {
        setToRegister(SRA_source(getFromRegister(register)), register)

        return Pair(2,2)
    }

    fun SRA_HL(): Pair<Int,Int> {
        setToAddressInHL(SRA_source(getFromAddressInHL()))

        return Pair(2,4)
    }

    fun SRL_source(value: UByte): UByte {
        val result = value.shr(1)

        setFlag(result.toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(false, Flag.H)
        setFlag(value % 2u == 1u, Flag.C)

        return result
    }

    fun SRL_r8(register: ByteRegister): Pair<Int, Int> {
        setToRegister(SRL_source(getFromRegister(register)), register)

        return Pair(2,2)
    }

    fun SRL_HL(): Pair<Int,Int> {
        setToAddressInHL(SRL_source(getFromAddressInHL()))

        return Pair(2,4)
    }

    fun SWAP_source(value: UByte): UByte {
        val upperBits = value.and(0xF0u)
        val lowerBits = value.and(0x0Fu)
        val newValue = (upperBits.shr(4)) or (lowerBits.shl(4))

        setFlag(newValue.toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.N)
        setFlag(false, Flag.H)
        setFlag(false, Flag.C)

        return newValue
    }

    fun SWAP_r8(register: ByteRegister): Pair<Int, Int> {
        setToRegister(SWAP_source(getFromRegister(register)), register)

        return Pair(2,2)
    }

    fun SWAP_HL(): Pair<Int,Int> {
        setToAddressInHL(SWAP_source(getFromAddressInHL()))

        return Pair(2,4)
    }

    fun JUMP_source(value: UShort) {
        programCounter = value
    }

    fun JP_HL(): Pair<Int, Int> {
        JUMP_source(getRegisterShort(ShortRegister.HL))

        return Pair(1,1)
    }

    fun JP_n16(): Pair<Int, Int> {
        JUMP_source(getFromImmediateShort())

        return Pair(3,4)
    }

    fun JUMP_cc_source(value: UShort, condition: JumpCondition): Boolean {
        val executeJump = when(condition) {
            JumpCondition.Z -> getFlag(Flag.Z)
            JumpCondition.NZ -> !getFlag(Flag.Z)
            JumpCondition.C -> getFlag(Flag.C)
            JumpCondition.NC -> !getFlag(Flag.C)
        }

        if(executeJump) {
            programCounter = value
        }

        return executeJump
    }

    fun CALL_n16(): Pair<Int,Int> {
        JUMP_source(getFromImmediateShort())
        stackPush(memory[programCounter + 3u])

        return Pair(3,6)
    }

    fun CALL_cc_n16(condition: JumpCondition): Pair<Int,Int> {
        val jumped = JUMP_cc_source(getFromImmediateShort(), condition)
        if(jumped) {
            stackPush(memory[programCounter + 3u])
            return Pair(3,6)
        } else {
            return Pair(3,3)
        }
    }

    fun JP_cc_n16(condition: JumpCondition): Pair<Int,Int> {
        val address = getFromImmediateShort()
        val jumped = JUMP_cc_source(address, condition)
        if(jumped) {
            return Pair(3,4)
        } else {
            return Pair(3,3)
        }
    }

    fun JUMP_relative_source(value: UShort, offset: Byte) {
        JUMP_source((value.toInt() + offset.toInt()).toUShort())
    }

    fun JUMP_CC_relative_source(value: UShort, offset: Byte, condition: JumpCondition): Boolean {
        val jumped = when(condition) {
            JumpCondition.Z -> getFlag(Flag.Z)
            JumpCondition.NZ -> !getFlag(Flag.Z)
            JumpCondition.C -> getFlag(Flag.C)
            JumpCondition.NC -> !getFlag(Flag.C)
        }

        if(jumped) {
            JUMP_relative_source(value, offset)
            return true
        } else {
            return false
        }
    }

    fun JR_n16(): Pair<Int,Int> {
        val offset = getFromImmediate().toByte()
        JUMP_relative_source((programCounter + 2u).toUShort(), offset)

        return Pair(2,3)
    }

    fun JR_cc_n16(condition: JumpCondition): Pair<Int,Int> {
        val jumped = JUMP_CC_relative_source((programCounter + 2u).toUShort(), getFromImmediate().toByte(), condition)
        if(jumped) {
            return Pair(2,3)
        } else {
            return Pair(2,2)
        }
    }

    fun RET_cc(condition: JumpCondition): Pair<Int, Int> {
        val doReturn = when(condition) {
            JumpCondition.Z -> getFlag(Flag.Z)
            JumpCondition.NZ -> !getFlag(Flag.Z)
            JumpCondition.C -> getFlag(Flag.C)
            JumpCondition.NC -> !getFlag(Flag.C)
        }

        if(doReturn) {
            programCounter = stackPopShort()
            return Pair(1,5)
        } else {
            return Pair(1,2)
        }
    }

    fun RET(): Pair<Int,Int> {
        programCounter = stackPopShort()

        return Pair(1,4)
    }

    fun RST(vector: UByte): Pair<Int,Int> {
        stackPush(memory[programCounter + 1u])
        JUMP_source(vector.toUShort())

        return Pair(1,4)
    }

    fun RETI(): Pair<Int,Int> {
        RET()

        enableInterrupts = true

        return Pair(1,4)
    }

    fun CCF(): Pair<Int,Int> {
        setFlag(false, Flag.N)
        setFlag(false, Flag.H)
        setFlag(!getFlag(Flag.C), Flag.C)

        return Pair(1,1)
    }

    fun SCF(): Pair<Int,Int> {
        setFlag(false, Flag.N)
        setFlag(false, Flag.H)
        setFlag(true, Flag.C)

        return Pair(1,1)
    }

    fun DEC_SP(): Pair<Int,Int> {
        stackPointer--

        return Pair(1,2)
    }

    fun INC_SP(): Pair<Int,Int> {
        stackPointer++

        return Pair(1,2)
    }

    fun POP_r16(register: ShortRegister): Pair<Int,Int> {
        registers[register.lowIndex] = stackPopByte()
        registers[register.highIndex] = stackPopByte()

        return Pair(1,3)
    }

    fun POP_AF(): Pair<Int,Int> {
        POP_r16(ShortRegister.AF)

        setFlag(getFromRegister(ByteRegister.FLAG)[7], Flag.Z)
        setFlag(getFromRegister(ByteRegister.FLAG)[6], Flag.N)
        setFlag(getFromRegister(ByteRegister.FLAG)[5], Flag.H)
        setFlag(getFromRegister(ByteRegister.FLAG)[4], Flag.C)

        return Pair(1,3)
    }

    fun PUSH_r16(register: ShortRegister): Pair<Int,Int> {
        stackPush(registers[register.highIndex])
        stackPush(registers[register.lowIndex])

        return Pair(1,4)
    }

    fun PUSH_AF(): Pair<Int,Int> {
        PUSH_r16(ShortRegister.AF)

        return Pair(1,4)
    }

    fun DI(): Pair<Int,Int> {
        enableInterrupts = false

        return Pair(1,1)
    }

    fun EI(): Pair<Int,Int> {
        enableInterruptsNextInstruction = true

        return Pair(1,1)
    }

    fun HALT(): Pair<Int,Int> {
        cpuState = State.HALTED

        return Pair(1,1)
    }

    fun DAA(): Pair<Int,Int> {
        if(getFlag(Flag.N)) {
            var adjustment = 0u
            if(getFlag(Flag.H)) {
                adjustment += 0x6u
            }
            if(getFlag(Flag.C)) {
                adjustment += 0x60u
            }
            val oldValue = registers[ByteRegister.A.index]
            registers[ByteRegister.A.index] = (registers[ByteRegister.A.index] - adjustment).toUByte()
            setFlag(adjustment  > oldValue, Flag.C)
        } else {
            var adjustment = 0u
            if(getFlag(Flag.H) || (getFromRegister(ByteRegister.A).and(0xFu) > 0x9u)) {
                adjustment += 0x6u
            }
            if(getFlag(Flag.C) || (getFromRegister(ByteRegister.A) > 0x9Fu)) {
                adjustment += 0x60u
            }
            val oldValue = registers[ByteRegister.A.index]
            registers[ByteRegister.A.index] = (registers[ByteRegister.A.index] - adjustment).toUByte()
            setFlag(borrowFrom(oldValue, adjustment.toUByte(), 8), Flag.C)
        }

        setFlag(getFromRegister(ByteRegister.A).toUInt() == 0u, Flag.Z)
        setFlag(false, Flag.H)

        return Pair(1,1)
    }

    fun STOP(): Pair<Int,Int> {
        cpuState = State.STOPPED

        return Pair(2,1)
    }

    private fun stackPush(byte: UByte) {
        memory[--stackPointer] = byte
    }

    private fun stackPush(short: UShort) {
        memory[--stackPointer] = short.toUBytePair().first
        memory[--stackPointer] = short.toUBytePair().second
    }

    private fun stackPopByte(): UByte {
        return memory[stackPointer++]
    }

    private fun stackPopShort(): UShort {
        val lowerByte = memory[stackPointer++]
        val upperByte = memory[stackPointer++]
        return Pair(upperByte,lowerByte).toUShort()
    }



    private fun executeOpcode() {
        val opcodeByte = memory[programCounter]
        val returnPair = when(opcodeByte.toUInt()) {
            0xCBu -> TODO()
            0x00u -> nop()
            0x01u -> LD_r16_n16(ShortRegister.BC)
            0x02u -> LD_r16_A(ShortRegister.BC)
            0x03u -> INC_r16(ShortRegister.BC)
            0x04u -> INC_r8(ByteRegister.B)
            0x05u -> DEC_r8(ByteRegister.B)
            0x06u -> LD_r8_n8(ByteRegister.B)
            0x07u -> RLCA()
            0x08u -> LD_n16_SP()
            0x09u -> ADD_HL_r16(ShortRegister.BC)
            0x0Au -> LD_A_r16(ShortRegister.BC)
            0x0Bu -> DEC_r16(ShortRegister.BC)
            0x0Cu -> INC_r8(ByteRegister.C)
            0x0Du -> DEC_r8(ByteRegister.C)
            0x0Eu -> LD_r8_n8(ByteRegister.C)
            0x0Fu -> RRCA()
            0x10u -> STOP()
            0x11u -> LD_r16_n16(ShortRegister.DE)
            0x12u -> LD_r16_A(ShortRegister.DE)
            0x13u -> INC_r16(ShortRegister.DE)
            0x14u -> INC_r8(ByteRegister.D)
            0x15u -> DEC_r8(ByteRegister.D)
            0x16u -> LD_r8_n8(ByteRegister.D)
            0x17u -> RLA()
            0x18u -> JR_n16()
            0x19u -> ADD_HL_r16(ShortRegister.DE)
            0x1Au -> LD_A_r16(ShortRegister.DE)
            0x1Bu -> DEC_r16(ShortRegister.DE)
            0x1Cu -> INC_r8(ByteRegister.E)
            0x1Du -> DEC_r8(ByteRegister.E)
            0x1Eu -> LD_r8_n8(ByteRegister.E)
            0x1Fu -> RRA()
            0x20u -> JR_cc_n16(JumpCondition.NZ)
            0x21u -> LD_r16_n16(ShortRegister.HL)
            0x22u -> LD_HLI_A()
            0x23u -> INC_r16(ShortRegister.HL)
            0x24u -> INC_r8(ByteRegister.H)
            0x25u -> DEC_r8(ByteRegister.H)
            0x26u -> LD_r8_n8(ByteRegister.H)
            0x27u -> DAA()
            0x28u -> JR_cc_n16(JumpCondition.Z)
            0x29u -> ADD_HL_r16(ShortRegister.HL)
            0x2Au -> LD_A_HLI()
            0x2Bu -> DEC_HL()
            0x2Cu -> INC_r8(ByteRegister.L)
            0x2Du -> DEC_r8(ByteRegister.L)
            0x2Eu -> LD_r8_n8(ByteRegister.L)
            0x2Fu -> CPL()
            0x30u -> JR_cc_n16(JumpCondition.NC)
            0x31u -> LD_SP_n16()
            0x32u -> LD_HLD_A()
            0x33u -> INC_SP()
            0x34u -> INC_HL()
            0x35u -> DEC_HL()
            0x36u -> LD_HL_n8()
            0x37u -> SCF()
            0x38u -> JR_cc_n16(JumpCondition.C)
            0x39u -> ADD_HL_SP()
            0x3Au -> LD_A_HLD()
            0x3Bu -> DEC_SP()
            0x3Cu -> INC_r8(ByteRegister.A)
            0x3Du -> DEC_r8(ByteRegister.A)
            0x3Eu -> LD_r8_n8(ByteRegister.A)
            0x3Fu -> CCF()
            0x40u -> nop()
            0x41u -> LD_r8_r8(ByteRegister.B, ByteRegister.C)
            0x42u -> LD_r8_r8(ByteRegister.B, ByteRegister.D)
            0x43u -> LD_r8_r8(ByteRegister.B, ByteRegister.E)
            0x44u -> LD_r8_r8(ByteRegister.B, ByteRegister.H)
            0x45u -> LD_r8_r8(ByteRegister.B, ByteRegister.L)
            0x46u -> LD_r8_HL(ByteRegister.B)
            0x47u -> LD_r8_r8(ByteRegister.B, ByteRegister.A)
            0x48u -> LD_r8_r8(ByteRegister.C, ByteRegister.B)
            0x49u -> nop()
            0x4Au -> LD_r8_r8(ByteRegister.C, ByteRegister.D)
            0x4Bu -> LD_r8_r8(ByteRegister.C, ByteRegister.E)
            0x4Cu -> LD_r8_r8(ByteRegister.C, ByteRegister.H)
            0x4Du -> LD_r8_r8(ByteRegister.C, ByteRegister.L)
            0x4Eu -> LD_r8_HL(ByteRegister.C)
            0x4Fu -> LD_r8_r8(ByteRegister.C, ByteRegister.A)
            0x50u -> LD_r8_r8(ByteRegister.D, ByteRegister.B)
            0x51u -> LD_r8_r8(ByteRegister.D, ByteRegister.C)
            0x52u -> nop()
            0x53u -> LD_r8_r8(ByteRegister.D, ByteRegister.E)
            0x54u -> LD_r8_r8(ByteRegister.D, ByteRegister.H)
            0x55u -> LD_r8_r8(ByteRegister.D, ByteRegister.L)
            0x56u -> LD_r8_HL(ByteRegister.D)
            0x57u -> LD_r8_r8(ByteRegister.D, ByteRegister.A)
            0x58u -> LD_r8_r8(ByteRegister.E, ByteRegister.B)
            0x59u -> LD_r8_r8(ByteRegister.E, ByteRegister.C)
            0x5Au -> LD_r8_r8(ByteRegister.E, ByteRegister.D)
            0x5Bu -> nop()
            0x5Cu -> LD_r8_r8(ByteRegister.E, ByteRegister.H)
            0x5Du -> LD_r8_r8(ByteRegister.E, ByteRegister.L)
            0x5Eu -> LD_r8_HL(ByteRegister.E)
            0x5Fu -> LD_r8_r8(ByteRegister.E, ByteRegister.A)
            0x60u -> LD_r8_r8(ByteRegister.H, ByteRegister.B)
            0x61u -> LD_r8_r8(ByteRegister.H, ByteRegister.C)
            0x62u -> LD_r8_r8(ByteRegister.H, ByteRegister.D)
            0x63u -> LD_r8_r8(ByteRegister.H, ByteRegister.E)
            0x64u -> nop()
            0x65u -> LD_r8_r8(ByteRegister.H, ByteRegister.L)
            0x66u -> LD_r8_HL(ByteRegister.H)
            0x67u -> LD_r8_r8(ByteRegister.H, ByteRegister.A)
            0x68u -> LD_r8_r8(ByteRegister.L, ByteRegister.B)
            0x69u -> LD_r8_r8(ByteRegister.L, ByteRegister.C)
            0x6Au -> LD_r8_r8(ByteRegister.L, ByteRegister.D)
            0x6Bu -> LD_r8_r8(ByteRegister.L, ByteRegister.E)
            0x6Cu -> LD_r8_r8(ByteRegister.L, ByteRegister.H)
            0x6Du -> nop()
            0x6Eu -> LD_r8_HL(ByteRegister.L)
            0x6Fu -> LD_r8_r8(ByteRegister.L, ByteRegister.A)
            0x70u -> LD_HL_r8(ByteRegister.B)
            0x71u -> LD_HL_r8(ByteRegister.C)
            0x72u -> LD_HL_r8(ByteRegister.D)
            0x73u -> LD_HL_r8(ByteRegister.E)
            0x74u -> LD_HL_r8(ByteRegister.H)
            0x75u -> LD_HL_r8(ByteRegister.L)
            0x76u -> HALT()
            0x77u -> LD_HL_r8(ByteRegister.A)
            0x78u -> LD_r8_r8(ByteRegister.A, ByteRegister.B)
            0x79u -> LD_r8_r8(ByteRegister.A, ByteRegister.C)
            0x7Au -> LD_r8_r8(ByteRegister.A, ByteRegister.D)
            0x7Bu -> LD_r8_r8(ByteRegister.A, ByteRegister.E)
            0x7Cu -> LD_r8_r8(ByteRegister.A, ByteRegister.H)
            0x7Du -> LD_r8_r8(ByteRegister.A, ByteRegister.L)
            0x7Eu -> LD_r8_HL(ByteRegister.A)
            0x7Fu -> nop()
            0x80u -> ADD_A_r8(ByteRegister.B)
            0x81u -> ADD_A_r8(ByteRegister.C)
            0x82u -> ADD_A_r8(ByteRegister.D)
            0x83u -> ADD_A_r8(ByteRegister.E)
            0x84u -> ADD_A_r8(ByteRegister.H)
            0x85u -> ADD_A_r8(ByteRegister.L)
            0x86u -> ADD_A_HL()
            0x87u -> ADD_A_r8(ByteRegister.A)
            0x88u -> ADC_A_r8(ByteRegister.B)
            0x89u -> ADC_A_r8(ByteRegister.C)
            0x8Au -> ADC_A_r8(ByteRegister.D)
            0x8Bu -> ADC_A_r8(ByteRegister.E)
            0x8Cu -> ADC_A_r8(ByteRegister.H)
            0x8Du -> ADC_A_r8(ByteRegister.L)
            0x8Eu -> ADC_A_HL()
            0x8Fu -> ADC_A_r8(ByteRegister.A)
            0x90U -> SUB_A_r8(ByteRegister.B)
            0x91u -> SUB_A_r8(ByteRegister.C)
            0x92u -> SUB_A_r8(ByteRegister.D)
            0x93u -> SUB_A_r8(ByteRegister.E)
            0x94u -> SUB_A_r8(ByteRegister.H)
            0x95u -> SUB_A_r8(ByteRegister.L)
            0x96u -> SUB_A_HL()
            0x97u -> SUB_A_r8(ByteRegister.A)
            0x98u -> SBC_A_r8(ByteRegister.B)
            0x99u -> SBC_A_r8(ByteRegister.C)
            0x9Au -> SBC_A_r8(ByteRegister.D)
            0x9Bu -> SBC_A_r8(ByteRegister.E)
            0x9Cu -> SBC_A_r8(ByteRegister.H)
            0x9Du -> SBC_A_r8(ByteRegister.L)
            0x9Eu -> SBC_A_HL()
            0x9Fu -> SBC_A_r8(ByteRegister.A)
            0xA0u -> AND_A_r8(ByteRegister.B)
            0xA1u -> AND_A_r8(ByteRegister.C)
            0xA2u -> AND_A_r8(ByteRegister.D)
            0xA3u -> AND_A_r8(ByteRegister.E)
            0xA4u -> AND_A_r8(ByteRegister.H)
            0xA5u -> AND_A_r8(ByteRegister.L)
            0xA6u -> AND_A_HL()
            0xA7u -> AND_A_r8(ByteRegister.A)
            0xA8u -> XOR_A_r8(ByteRegister.B)
            0xA9u -> XOR_A_r8(ByteRegister.C)
            0xAAu -> XOR_A_r8(ByteRegister.D)
            0xABu -> XOR_A_r8(ByteRegister.E)
            0xACu -> XOR_A_r8(ByteRegister.H)
            0xADu -> XOR_A_r8(ByteRegister.L)
            0xAEu -> XOR_A_HL()
            0xAFu -> XOR_A_r8(ByteRegister.A)
            0xB0u -> OR_A_r8(ByteRegister.B)
            0xB1u -> OR_A_r8(ByteRegister.C)
            0xB2u -> OR_A_r8(ByteRegister.D)
            0xB3u -> OR_A_r8(ByteRegister.E)
            0xB4u -> OR_A_r8(ByteRegister.H)
            0xB5u -> OR_A_r8(ByteRegister.L)
            0xB6u -> OR_A_HL()
            0xB7u -> OR_A_r8(ByteRegister.A)
            0xB8u -> CP_A_r8(ByteRegister.B)
            0xB9u -> CP_A_r8(ByteRegister.C)
            0xBAu -> CP_A_r8(ByteRegister.D)
            0xBBu -> CP_A_r8(ByteRegister.E)
            0xBCu -> CP_A_r8(ByteRegister.H)
            0xBDu -> CP_A_r8(ByteRegister.L)
            0xBEu -> CP_A_HL()
            0xBFu -> CP_A_r8(ByteRegister.A)
            0xC0u -> RET_cc(JumpCondition.NZ)
            0xC1u -> POP_r16(ShortRegister.BC)
            0xC2u -> JP_cc_n16(JumpCondition.NZ)
            0xC3u -> JP_n16()
            0xC4u -> CALL_cc_n16(JumpCondition.NZ)
            0xC5u -> PUSH_r16(ShortRegister.BC)
            0xC6u -> ADD_A_n8()
            0xC7u -> RST(0x00u)
            0xC8u -> RET_cc(JumpCondition.Z)
            0xC9u -> RET()
            0xCAu -> JP_cc_n16(JumpCondition.Z)
            // 0xCB is a special prefix, so it is at the tob
            0xCCu -> CALL_cc_n16(JumpCondition.Z)
            0xCDu -> CALL_n16()
            0xCEu -> ADC_A_n8()
            0xCFu -> RST(0x08u)
            0xD0u -> RET_cc(JumpCondition.NC)
            0xD1u -> POP_r16(ShortRegister.DE)
            0xD2u -> JP_cc_n16(JumpCondition.NC)
            // 0xD3 is not an opcode
            0xD4u -> CALL_cc_n16(JumpCondition.NC)
            0xD5u -> PUSH_r16(ShortRegister.DE)
            0xD6u -> SUB_A_n8()
            0xD7u -> RST(0x10u)
            0xD8u -> RET_cc(JumpCondition.C)
            0xD9u -> RETI()
            0xDAu -> JP_cc_n16(JumpCondition.C)
            // 0xDB is not an opcode
            0xDCu -> CALL_cc_n16(JumpCondition.C)
            // 0xDD is not an opcode
            0xDEu -> SBC_A_n8()
            0xDFu -> RST(0x18u)
            0xE0u -> LDH_n16_A()
            0xE1u -> POP_r16(ShortRegister.HL)
            0xE2u -> LDH_C_A()
            // 0xE3 is not an opcode
            // 0xE4 is not an opcode
            0xE5u -> PUSH_r16(ShortRegister.HL)
            0xE6u -> AND_A_n8()
            0xE7u -> RST(0x20u)
            0xE8u -> ADD_SP_e8()
            0xE9u -> JP_HL()
            0xEAu -> LD_n16_A()
            // 0xEB is not an opcode
            // 0xEC is not an opcode
            // 0xED is not an opcode
            0xEEu -> XOR_A_n8()
            0xEFu -> RST(0x28u)
            0xF0u -> LDH_A_n16()
            0xF1u -> POP_AF()
            0xF2u -> LDH_A_C()
            0xF3u -> DI()
            // 0xF4 is not an opcode
            0xF5u -> PUSH_AF()
            0xF6u -> OR_A_n8()
            0xF7u -> RST(0x30u)
            0xF8u -> LD_HL_SPe8()
            0xF9u -> LD_SP_HL()
            0xFAu -> LD_A_n16()
            0xFBu -> EI()
            // 0xFC is not an opcode
            // 0xFD is not an opcode
            0xFEu -> CP_A_n8()
            0xFFu -> RST(0x38u)

        }
    }

    private fun carryFrom(primary: UInt, secondary: UInt, binaryPlace: Int): Boolean {
        val mask = (2.0).pow(binaryPlace).toUInt() - 1u
        return (primary and mask) + (secondary and mask) > mask
    }

    private fun carryFrom(primary: UByte, secondary: UByte, binaryPlace: Int): Boolean {
        return carryFrom(primary.toUInt(), secondary.toUInt(), binaryPlace)
    }

    private fun carryFrom(primary: UInt, secondary: UInt, tertiary: UInt, binaryPlace: Int): Boolean {
        val mask = (2.0).pow(binaryPlace).toUInt() - 1u
        return (primary and mask) + (secondary and mask) + (tertiary and mask) > mask
    }

    private fun carryFrom(primary: UByte, secondary: UByte, tertiary: UByte, binaryPlace: Int): Boolean {
        return carryFrom(primary.toUInt(), secondary.toUInt(), tertiary.toUInt(), binaryPlace)
    }

    private fun borrowFrom(minuend: UByte, subtrahend: UByte, binaryPlace: Int): Boolean {
        return borrowFrom(minuend.toUInt(), subtrahend.toUInt(), binaryPlace)
    }

    private fun borrowFrom(minuend: UInt, subtrahend: UInt, binaryPlace: Int): Boolean {
        val mask = (2.0).pow(binaryPlace).toUInt() - 1u
        return (minuend and mask) < (subtrahend and mask)
    }

    private fun setFlag(value: Boolean, flag: Flag) {
        val changeMask = if(value) 1u shl flag.bitIndex else 0u
        val bitMask = 1u.inv().rotateLeft(flag.bitIndex)

        var flagValue = registers[ByteRegister.FLAG.index].toUInt()
        flagValue = flagValue.and(bitMask).or(changeMask)

        registers[ByteRegister.FLAG.index] = flagValue.toUByte()
    }

    private fun getFlag(flag: Flag): Boolean {
        return ((registers[ByteRegister.FLAG.index] shr flag.bitIndex).takeLowestOneBit()) == 1u.toUByte()
    }

    private fun getCarryBit(): UByte {
        return (if (getFlag(Flag.C)) 1u else 0u).toUByte()
    }

    private fun getRegisterShort(register: ShortRegister): UShort {
        val upperByte = registers[register.highIndex]
        val lowerByte = registers[register.lowIndex]
        return Pair(upperByte, lowerByte).toUShort()
    }

    private fun setRegisterShort(value: UShort, register: ShortRegister) {
        registers[register.highIndex] = value.toUBytePair().first
        registers[register.lowIndex] = value.toUBytePair().second
    }

    private fun getFromAddressInHL(): UByte {
        val address = getRegisterShort(ShortRegister.HL)
        return memory[address]
    }

    private fun getFromRegister(register: ByteRegister): UByte {
        return registers[register.index]
    }

    private fun getFromImmediate(): UByte {
        return memory[programCounter + 1u]
    }

    private fun getFromImmediateShort(): UShort {
        val upperByte = memory[programCounter + 2u]
        val lowerByte = memory[programCounter + 1u]
        return Pair(upperByte, lowerByte).toUShort()
    }

    private fun setToAddressInHL(byte: UByte) {
        val address = getRegisterShort(ShortRegister.HL)
        memory[address] = byte
    }

    private fun setToRegister(byte: UByte, register: ByteRegister) {
        registers[register.index] = byte
    }

    fun Byte.shla(): Byte {
        var result = (this * 2).toByte()
        if(this < 0 && result >= 0) {
            result = (-128 + result).toByte()
        } else if(this > 0 && result <= 0) {
            result = (128 + result).toByte()
        }

        return result
    }
}