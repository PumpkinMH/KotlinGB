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
        val oldLowerByte = stackPointer.toBytePair().second
        stackPointer = (stackPointer.toInt() + signedByte.toInt()).toUShort()

        registers[ShortRegister.HL.highIndex] = stackPointer.toBytePair().first
        registers[ShortRegister.HL.lowIndex] = stackPointer.toBytePair().second

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
        registers[ShortRegister.HL.lowIndex] = sum.toUShort().toBytePair().second
        registers[ShortRegister.HL.highIndex] = sum.toUShort().toBytePair().first

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
        registers[ShortRegister.HL.lowIndex] = sum.toUShort().toBytePair().second
        registers[ShortRegister.HL.highIndex] = sum.toUShort().toBytePair().first

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
        return (registers[ByteRegister.FLAG.index] shr flag.bitIndex) == 1u.toUByte()
    }

    private fun getCarryBit(): UByte {
        return (if (getFlag(Flag.C)) 1u else 0u).toUByte()
    }



}