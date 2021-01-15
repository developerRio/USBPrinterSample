package com.originalstocks.usbprintersample.converter

import com.originalstocks.usbprintersample.data.Printer

/**
 * Interface for conversion any string into ByteArray
 * */
interface Printable {
    fun getPrintableByteArray(printer: Printer): List<ByteArray>
}