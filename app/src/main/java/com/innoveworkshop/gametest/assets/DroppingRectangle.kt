package com.innoveworkshop.gametest.assets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.innoveworkshop.gametest.engine.Rectangle
import com.innoveworkshop.gametest.engine.Vector

class DroppingRectangle(
    position: Vector?,
    width: Float,
    height: Float,
    //dropRate: Float,
    private val bitmap: Bitmap,
    //color: Int
) : Rectangle(position, width, height) {
    var dropRate: Float = 0f

    init {
        this.dropRate = dropRate
    }

    override fun onFixedUpdate() {
        super.onFixedUpdate()

        if (!isFloored) position.y += dropRate
    }

    override fun onDraw(canvas: Canvas?) {
        // Draw the bitmap
        canvas?.drawBitmap(
            Bitmap.createScaledBitmap(bitmap, width.toInt(), height.toInt(), true),
            position.x - width / 2, // Center the rectangle horizontally
            position.y - height / 2, // Center the rectangle vertically
            Paint()
        )
    }
}
