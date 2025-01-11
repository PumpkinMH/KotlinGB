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

    fun JP_cc_n16(address: UShort, condition: JumpCondition): Pair<Int,Int> {
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
        JUMP_source(vector.toUShort())

        return Pair(1,4)
    }

    fun RETI(): Pair<Int,Int> {
        RET()

        TODO()

        return Pair(1,4)
    }

    fun EI(): Pair<Int,Int> {
        TODO()

        return Pair(1,1)
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