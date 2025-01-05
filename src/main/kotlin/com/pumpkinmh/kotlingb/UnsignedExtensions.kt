package com.pumpkinmh.kotlingb

infix fun UByte.shr(shiftValue: Int): UByte {
    var intConversion = this.toUInt()
    intConversion = intConversion.shr(shiftValue)
    intConversion = intConversion.and(0xFFu)
    return intConversion.toUByte()
}

infix fun UByte.shl(shiftValue: Int): UByte {
    var intConversion = this.toUInt()
    intConversion = intConversion.shl(shiftValue)
    intConversion = intConversion.and(0xFFu)
    return intConversion.toUByte()
}

infix fun UShort.shr(shiftValue: Int): UShort {
    var intConversion = this.toUInt()
    intConversion = intConversion.shr(shiftValue)
    intConversion = intConversion.and(0xFFFFu)
    return intConversion.toUShort()
}

infix fun UShort.shl(shiftValue: Int): UShort {
    var intConversion = this.toUInt()
    intConversion = intConversion.shl(shiftValue)
    intConversion = intConversion.and(0xFFFFu)
    return intConversion.toUShort()
}

fun UShort.toUBytePair(): Pair<UByte,UByte> {
    val upperByte: UByte = this.and(0xFF00u).shr(8).toUByte()
    val lowerByte: UByte = this.and(0xFFu).toUByte()

    return Pair(upperByte,lowerByte)
}

fun Pair<UByte,UByte>.toUShort(): UShort {
    val upperByte: UShort = this.first.toUShort().shl(8)
    val lowerByte: UShort = this.second.toUShort()

    return upperByte or lowerByte
}

operator fun UByte.get(indexFromLSB: Int): Boolean {
    return this.shr(indexFromLSB).takeLowestOneBit().toUInt() == 1u
}

fun UByte.setBit(index: Int, value: Boolean): UByte {
    val changeMask = if(value) 1u shl index else 0u
    val bitMask = 1u.inv().rotateLeft(index)

    return this.and(bitMask.toUByte()).or(changeMask.toUByte())
}