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