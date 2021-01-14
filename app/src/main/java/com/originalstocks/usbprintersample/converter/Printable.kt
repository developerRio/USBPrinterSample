package com.originalstocks.usbprintersample.converter

import com.originalstocks.printstocks.data.printer.Printer

/**
 * Interface for conversion any string into ByteArray
 * */
interface Printable {
    fun getPrintableByteArray(printer: Printer): List<ByteArray>
}