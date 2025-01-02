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

fun UShort.toBytePair(): Pair<UByte,UByte> {
    val upperByte: UByte = this.and(0xFF00u).shr(8).toUByte()
    val lowerByte: UByte = this.and(0xFFu).toUByte()

    return Pair(upperByte,lowerByte)
}

fun Pair<UByte,UByte>.toUShort(): UShort {
    val upperByte: UShort = this.first.toUShort().shl(8)
    val lowerByte: UShort = this.second.toUShort()

    return upperByte or lowerByte
}