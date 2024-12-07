package com.innoveworkshop.gametest.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.Timer
import java.util.TimerTask

class GameSurface @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {
    private val holder: SurfaceHolder
    private var timer: Timer? = null
    private var root: GameObject? = null

    // Create the GameObject list.
    private val gameObjects = ArrayList<GameObject>()

    init {
        // Ensure we are on top of everything.
        setZOrderOnTop(true)

        // Set up the SurfaceHolder event handler.
        holder = getHolder()
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // Ensure we get the onDraw events.
                setWillNotDraw(false)

                // Start up the root object.
                root!!.onStart(this@GameSurface)

                // Set up the fixed update timer.
                timer = Timer()
                timer!!.scheduleAtFixedRate(FixedUpdateTimer(), 0, (1000 / 30).toLong())
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // TODO: Stop everything.
            }
        })
    }

    fun setRootGameObject(root: GameObject?) {
        this.root = root
    }

    fun addGameObject(gameObject: GameObject) {
        gameObjects.add(gameObject)
        gameObject.onStart(this)
    }

    fun removeGameObject(gameObject: GameObject): Boolean {
        return gameObjects.remove(gameObject)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //canvas.drawColor(Color.CYAN)
        root!!.onDraw(canvas)
        for (gameObject in gameObjects) {
            gameObject.onDraw(canvas)
        }
    }

    internal inner class FixedUpdateTimer : TimerTask() {
        override fun run() {
            for (gameObject in gameObjects) {
                gameObject.onFixedUpdate()
            }

            root!!.onFixedUpdate()
            invalidate()
        }
    }
}
