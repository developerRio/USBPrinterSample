package com.originalstocks.usbprintersample.converter

import android.graphics.Bitmap

interface PrintingImagesHelper {
    fun getBitmapAsByteArray(bitmap: Bitmap): ByteArray
}