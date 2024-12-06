package com.innoveworkshop.gametest.engine

import android.graphics.Canvas
import android.graphics.Paint

open class Rectangle(position: Vector?, var width: Float, var height: Float) :
    GameObject(
        position!!
    ), Caged {
    // Set up the paint.
    var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        //paint.color = color
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // Calculate the positions of the points that form the rectangle.
        val left = position.x - (width / 2)
        val top = position.y - (height / 2)
        val right = position.x + (width / 2)
        val bottom = position.y + (height / 2)

        canvas!!.drawRect(left, top, right, bottom, paint)
    }

    override fun hitLeftWall(): Boolean {
        return (position.x - width / 2) <= gameSurface!!.width
    }

    override fun hitRightWall(): Boolean {
        return (position.x + width / 2) >= gameSurface!!.width
    }

    override val isFloored: Boolean
        get() = (position.y + height / 2) >= gameSurface!!.height
}
