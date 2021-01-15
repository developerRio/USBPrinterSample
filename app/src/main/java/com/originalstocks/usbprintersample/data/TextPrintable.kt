package com.originalstocks.usbprintersample.data

import com.originalstocks.usbprintersample.converter.StringToByteArrayConverter
import com.originalstocks.usbprintersample.converter.Printable

/**
 * Data class for printing Texts
 * */
data class TextPrintable private constructor(
    val text: String,
    val fontSize: Byte,
    val alignment: Byte,
    val newLinesAfter: Int,
    val bold: Byte,
    val underlined: Byte,
    val characterCode: Byte,
    val lineSpacing: Byte,
    val customStringToByteArrayConverter: StringToByteArrayConverter?
) : Printable {
    override fun getPrintableByteArray(printer: Printer): List<ByteArray> {
        val operations = mutableListOf(
            printer.justificationCommand.plus(alignment),
            printer.fontSizeCommand.plus(fontSize),
            printer.emphasizedModeCommand.plus(bold),
            printer.underlineModeCommand.plus(underlined),
            printer.characterCodeCommand.plus(characterCode),
            printer.lineSpacingCommand.plus(lineSpacing)
        )

        customStringToByteArrayConverter?.let {
            operations.add(it.toByteArray(text))
        } ?: run {
            operations.add(printer.stringToByteArrayConverter.toByteArray(text))
        }

        if (newLinesAfter > 0) {
            operations.add(printer.feedLineCommand.plus(newLinesAfter.toByte()))
        }

        return operations
    }

    class Builder {
        private var text = ""
        private var fontSize = DefaultPrinter.FONT_SIZE_NORMAL
        private var alignment: Byte = DefaultPrinter.ALIGNMENT_LEFT
        private var newLinesAfter = 0
        private var bold: Byte = DefaultPrinter.EMPHASIZED_MODE_NORMAL
        private var underlined: Byte = DefaultPrinter.UNDERLINED_MODE_OFF
        private var characterCode: Byte = DefaultPrinter.CHARCODE_PC437
        private var lineSpacing: Byte = DefaultPrinter.LINE_SPACING_30
        private var customStringToByteArrayConverter: StringToByteArrayConverter? = null

        fun setText(text: String): Builder {
            this.text = text
            return this
        }

        fun setFontSize(fontSize: Byte): Builder {
            this.fontSize = fontSize
            return this
        }

        fun setAlignment(alignment: Byte): Builder {
            this.alignment = alignment
            return this
        }

        fun setNewLinesAfter(lines: Int): Builder {
            this.newLinesAfter = lines
            return this
        }

        fun setEmphasizedMode(mode: Byte): Builder {
            this.bold = mode
            return this
        }

        fun setUnderlined(mode: Byte): Builder {
            this.underlined = mode
            return this
        }

        fun setCharacterCode(characterCode: Byte): Builder {
            this.characterCode = characterCode
            return this
        }

        fun setLineSpacing(lineSpacing: Byte): Builder {
            this.lineSpacing = lineSpacing
            return this
        }

        fun setCustomConverter(stringToByteArrayConverter: StringToByteArrayConverter): Builder {
            this.customStringToByteArrayConverter = stringToByteArrayConverter
            return this
        }

        fun build(): Printable {
            return TextPrintable(
                text,
                fontSize,
                alignment,
                newLinesAfter,
                bold,
                underlined,
                characterCode,
                lineSpacing,
                customStringToByteArrayConverter
            )
        }

    }


}












