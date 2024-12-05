package com.innoveworkshop.gametest

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.innoveworkshop.gametest.assets.DroppingRectangle
import com.innoveworkshop.gametest.engine.Circle
import com.innoveworkshop.gametest.engine.GameObject
import com.innoveworkshop.gametest.engine.GameSurface
import com.innoveworkshop.gametest.engine.Rectangle
import com.innoveworkshop.gametest.R
import com.innoveworkshop.gametest.engine.Vector


//todo: make collisions like in rigidbody
//todo: change the distance formula to be the proper formula


class MainActivity : AppCompatActivity() {
    protected var gameSurface: GameSurface? = null
//    protected var upButton: Button? = null
//    protected var downButton: Button? = null
    protected var leftButton: Button? = null
    protected var rightButton: Button? = null

    protected var game: Game? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val highScoreKey = "high_score"  // Key for saving high score

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)

        gameSurface = findViewById<View>(R.id.gameSurface) as GameSurface

        game = Game(this)
        gameSurface!!.setRootGameObject(game)

        setupControls()
    }

    private fun setupControls() {
//        upButton = findViewById<View>(R.id.up_button) as Button
//        upButton!!.setOnClickListener { game!!.circle!!.position.y -= 30f }
//
//        downButton = findViewById<View>(R.id.down_button) as Button
//        downButton!!.setOnClickListener { game!!.circle!!.position.y += 30f }

        leftButton = findViewById<View>(R.id.left_button) as Button
        leftButton!!.setOnClickListener { game!!.circle!!.position.x -= 30f }

        rightButton = findViewById<View>(R.id.right_button) as Button
        rightButton!!.setOnClickListener { game!!.circle!!.position.x += 30f }
    }

    inner class Game(private val context: android.content.Context) : GameObject() {
        var circle: Circle? = null
        private val fallingItems = mutableListOf<DroppingRectangle>()
        private var surface: GameSurface? = null
        private val gravity = 0.5f // Gravity value for falling items
        private var isGameOver = false // Track game state

        private val paint = Paint().apply {
            color = Color.WHITE
            textSize = 100f
            isAntiAlias = true
        }

        private var lastSpawnTime: Long = 0
        private val spawnInterval: Long = 1000 // Time interval between spawns (in milliseconds)

        // Survival and high score tracking
        private var survivalCount: Int = 0 // Count how many pills survived
        private var highScore: Int = 0 // Store the high score

        override fun onStart(surface: GameSurface?) {
            super.onStart(surface)
            this.surface = surface

            // Load high score from SharedPreferences
            highScore = sharedPreferences.getInt(highScoreKey, 0)

            // Create the player-controlled circle
            circle = Circle(
                (surface!!.width / 2).toFloat(),
                (surface.height - 200).toFloat(), // Position the circle near the bottom
                100f,
                Color.RED
            )
            surface.addGameObject(circle!!)

            // Initialize the first falling item
            spawnNewFallingItem()
        }

        override fun onFixedUpdate() {
            if (isGameOver) return // Stop updates if the game is over

            super.onFixedUpdate()

            // Check the time since the last spawn and create a new falling item if enough time has passed
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastSpawnTime >= spawnInterval) {
                spawnNewFallingItem()
                lastSpawnTime = currentTime // Update last spawn time
                survivalCount++ // Increase the survival count when a new pill spawns
            }

            // Update falling items and remove any that fall off the screen
            val iterator = fallingItems.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item.position.y > surface!!.height) {
                    // Remove items that fall off the screen
                    iterator.remove()
                    surface!!.removeGameObject(item)
                }
            }

            // Add collision detection
            checkCollisions()
        }

        override fun onDraw(canvas: android.graphics.Canvas?) {
            super.onDraw(canvas)

            val paint = android.graphics.Paint().apply {
                style = android.graphics.Paint.Style.FILL
                textSize = 50f // Adjust the text size as needed
                isAntiAlias = true
            }

            // Dynamically resolve the primary color depending on the theme
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorPrimary,
                typedValue,
                true
            )
            paint.color = typedValue.data

            // Draw "Game Over" text if the game is over
            if (isGameOver && canvas != null) {
                val text = "Game Over! Score: $survivalCount"
                val textWidth = paint.measureText(text)
                val x = (surface!!.width - textWidth) / 2.toFloat() // Center text horizontally
                val y = (surface!!.height / 2).toFloat() // Center text vertically
                canvas.drawText(text, x, y, paint)

                // Draw High Score
                val highScoreText = "High Score: $highScore"
                val highScoreWidth = paint.measureText(highScoreText)
                canvas.drawText(highScoreText, (surface!!.width - highScoreWidth) / 2, y + 100, paint)
            }
        }




        private fun spawnNewFallingItem() {
            // Create a new falling rectangle at a random horizontal position
            val item = DroppingRectangle(
                Vector(
                    (Math.random() * surface!!.width).toFloat(), // Random horizontal position
                    0f // Start at the top of the screen
                ),
                50f, 50f, gravity, Color.BLUE
            )
            fallingItems.add(item)
            surface!!.addGameObject(item)
        }

        private fun checkCollisions() {
            // Check if the circle collides with any falling items
            for (item in fallingItems) {
                if (checkCollision(circle!!, item)) {
                    isGameOver = true // End the game
                    checkHighScore() // Check if this is a new high score
                    break
                }
            }
        }

        private fun checkCollision(circle: Circle, rect: DroppingRectangle): Boolean {
            // Calculate if the circle's bounding box intersects the rectangle's bounding box
            val circleLeft = circle.position.x - circle.radius
            val circleRight = circle.position.x + circle.radius
            val circleTop = circle.position.y - circle.radius
            val circleBottom = circle.position.y + circle.radius

            val rectLeft = rect.position.x
            val rectRight = rect.position.x + rect.width
            val rectTop = rect.position.y
            val rectBottom = rect.position.y + rect.height

            // Check if the bounding boxes overlap
            return !(circleRight < rectLeft || circleLeft > rectRight || circleBottom < rectTop || circleTop > rectBottom)
        }

        private fun checkHighScore() {
            // Update high score if the current score is higher
            if (survivalCount > highScore) {
                highScore = survivalCount
                saveHighScore() // Save the new high score to SharedPreferences
            }
        }

        private fun saveHighScore() {
            // Save the new high score to SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putInt(highScoreKey, highScore)
            editor.apply()
        }
    }

    class DroppingRectangle(
        position: Vector,
        val recwidth: Float,
        val recheight: Float,
        val recgravity: Float, // Gravity applied to the item
        color: Int
    ) : Rectangle(position, recwidth, recheight, color) {

        var velocity = Vector(0f, 0f) // Initial velocity is 0

        override fun onFixedUpdate() {
            super.onFixedUpdate()

            // Apply gravity to vertical velocity (y-axis)
            velocity.y += recgravity

            // Update the position based on velocity
            position.x += velocity.x
            position.y += velocity.y
        }

        fun resetPosition(screenWidth: Float) {
            // Reset the rectangle to the top of the screen with a random horizontal position
            position.y = 0f
            position.x = (Math.random() * screenWidth).toFloat()
            velocity.y = 0f // Reset vertical velocity to 0
        }
    }
}
