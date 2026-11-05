package com.asad.nexus.networkmonitor.notification

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/** Draws the speed number and unit into a white bitmap for the status bar small icon. */
object StatusIcon {
    private const val SIZE = 96

    fun render(value: String, unit: String): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        paint.textSize = 58f
        while (paint.measureText(value) > SIZE - 4 && paint.textSize > 20f) paint.textSize -= 2f
        canvas.drawText(value, SIZE / 2f, 54f, paint)
        paint.textSize = 30f
        while (paint.measureText(unit) > SIZE - 4 && paint.textSize > 14f) paint.textSize -= 2f
        canvas.drawText(unit, SIZE / 2f, 90f, paint)
        return bmp
    }
}
