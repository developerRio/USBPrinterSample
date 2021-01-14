package com.originalstocks.usbprintersample.converter

/**
 * Default stringToByteArrayConverter
 */
class ArabicStringToByteArrayConverter : StringToByteArrayConverter() {

    override fun convert(input: String): String {
        return UniCode864Mapping().getArabicString(input)
    }

    private fun isProbablyArabic(s: String): Boolean {
        var i = 0
        while (i < s.length) {
            val c = s.codePointAt(i)
            if (c in 0x0600..0x06E0)
                return true
            i += Character.charCount(c)
        }
        return false
    }
}