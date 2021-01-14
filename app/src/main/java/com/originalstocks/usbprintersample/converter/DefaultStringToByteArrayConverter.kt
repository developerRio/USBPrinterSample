package com.originalstocks.usbprintersample.converter


/**
 * Default stringToByteArrayConverter
 */
class DefaultStringToByteArrayConverter : StringToByteArrayConverter() {
    override fun convert(input: Char): Byte {
        if (input == '€') {
            return (0x80).toByte()
        }
        return input.toByte()
    }
}