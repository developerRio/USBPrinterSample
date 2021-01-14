package com.originalstocks.usbprintersample.converter

import android.graphics.Bitmap

/**
 * Base class for Default Image Helper which converts [bitmap] into [ByteArray]
 * */

class DefaultPrintingImagesHelper : PrintingImagesHelper {
    override fun getBitmapAsByteArray(bitmap: Bitmap): ByteArray {
        return ImageUtils.decodeBitmap(bitmap)!!
    }

}