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

}