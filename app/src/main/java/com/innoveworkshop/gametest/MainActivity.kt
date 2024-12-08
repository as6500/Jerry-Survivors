package com.innoveworkshop.gametest

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.innoveworkshop.gametest.engine.Circle
import com.innoveworkshop.gametest.engine.GameObject
import com.innoveworkshop.gametest.engine.GameSurface
import com.innoveworkshop.gametest.engine.Rectangle
import com.innoveworkshop.gametest.R
import com.innoveworkshop.gametest.engine.Vector
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.media.MediaPlayer
import com.innoveworkshop.gametest.assets.DroppingRectangle
import kotlin.math.pow

//todo: clean up code
//todo: screen manager
//todo: fix double circles on app change DONE
//todo: jail jerry DONE
//todo: jerry meow easteregg DONE

class MainActivity : AppCompatActivity() {
    protected var gameSurface: GameSurface? = null
    //    protected var upButton: Button? = null
    //     protected var downButton: Button? = null
    protected var leftButton: Button? = null
    protected var rightButton: Button? = null
    protected var startButton: Button? = null
    private var isGameOver = false // Track game state
    private var isPaused = true
    var survivalCount: Int = 0
    protected var game: Game? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val highScoreKey = "high_score"  // Key for saving high score
    private var meowSound: MediaPlayer? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        meowSound = MediaPlayer.create(this, R.raw.meow)

        gameSurface = findViewById<View>(R.id.gameSurface) as GameSurface
        game = Game(this)
        gameSurface!!.setRootGameObject(game)

        gameSurface!!.setOnTouchListener { v, event ->
            if (!isPaused && !isGameOver) {
                // Check if the player sprite (circle) is tapped
                val x = event.x
                val y = event.y

                // Check if the circle is tapped
                game?.circle?.let {
                    if (it.isTapped(x, y)) {
                        // Play the sound when the sprite is tapped
                        meowSound?.start()
                    }
                }
            }
            true
        }

        setupControls()

        // Add a listener to detect window focus changes
        gameSurface?.viewTreeObserver?.addOnWindowFocusChangeListener { hasFocus ->
            if (!hasFocus) {
                // App has lost focus (e.g., multitasking mode)
                if (!isPaused) { // Only pause if not already paused
                    isPaused = true
                }
            }
        }
    }


    private fun setupControls() {
        leftButton = findViewById<View>(R.id.left_button) as Button
        leftButton!!.setOnClickListener { if (isPaused == false && isGameOver == false){
            game!!.circle!!.position.x -= 30f
            game?.constrainPlayerPosition()}
        }


        rightButton = findViewById<View>(R.id.right_button) as Button
        rightButton!!.setOnClickListener {if (isPaused == false && isGameOver == false){
            game!!.circle!!.position.x += 30f
            game?.constrainPlayerPosition()}
        }


        startButton = findViewById<View>(R.id.start_button) as Button
        startButton!!.setOnClickListener {
            if (isGameOver) {
                // Reset the game if it's over
                game?.resetGame()
                isGameOver = false
                survivalCount = 0 // Reset survival count
                game!!.startGame()  // Start the game after reset
            } else {
                // Toggle between Pause and Resume
                isPaused = !isPaused
            }
        }

    }

    class Sprite(
        private val bitmap: Bitmap,
        position: Vector,
        var width: Float,
        var height: Float
    ) : GameObject() {

        init {
            this.position = position
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            canvas?.drawBitmap(
                Bitmap.createScaledBitmap(bitmap, width.toInt(), height.toInt(), true),
                position.x - width / 2, // Center the sprite
                position.y - height / 2, // Center the sprite
                Paint()
            )
        }

        fun isTapped(x: Float, y: Float): Boolean {
            // Check if the tap coordinates are within the sprite's bounds
            val left = position.x - width / 2
            val right = position.x + width / 2
            val top = position.y - height / 2
            val bottom = position.y + height / 2

            return x >= left && x <= right && y >= top && y <= bottom
        }
    }

    inner class Game(private val context: android.content.Context) : GameObject() {
        private fun loadBitmap(resourceId: Int, width: Int, height: Int): Bitmap {
            val originalBitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            return Bitmap.createScaledBitmap(originalBitmap, width, height, true)
            Log.d("images", "load success?")
        }

        private val velocities = mutableMapOf<DroppingRectangle, Float>()
        var circle: Sprite? = null
        private val fallingItems = mutableListOf<DroppingRectangle>()
        private var surface: GameSurface? = null
        private val gravity = 10f // Gravity value for falling items

        private val spawnInterval: Long = 1000 // Time interval between spawns (in milliseconds)
        private var lastSpawnTime: Long = 0

        fun constrainPlayerPosition() {
            circle?.let { player ->
                val halfJerry = player.width / 2
                val screenWidth = surface!!.width
                player.position.x = player.position.x.coerceIn(halfJerry, screenWidth - halfJerry)
            }
        }


        // Survival and high score tracking
        private var highScore: Int = 0

        // Bitmaps for the falling items
        private lateinit var playerBitmap: Bitmap
        private lateinit var orangeCatBitmap: Bitmap
        private lateinit var orangeRobotBitmap: Bitmap
        private lateinit var whiteCatBitmap: Bitmap
        private lateinit var whiteRobotBitmap: Bitmap

        override fun onStart(surface: GameSurface?) {
            super.onStart(surface)
            this.surface = surface

            // Load high score from SharedPreferences
            highScore = sharedPreferences.getInt(highScoreKey, 0)

            if (circle == null) {
                // Load resources
                playerBitmap = loadBitmap(R.drawable.jerry, 95, 200) ?: throw RuntimeException("Failed to load player bitmap")
                orangeCatBitmap = loadBitmap(R.drawable.orangecat, 100, 100) ?: throw RuntimeException("Failed to load orange bitmap")
                orangeRobotBitmap = loadBitmap(R.drawable.orangerobot, 100, 100) ?: throw RuntimeException("Failed to load orange bitmap")
                whiteCatBitmap = loadBitmap(R.drawable.whitecat, 100, 100) ?: throw RuntimeException("Failed to load white bitmap")
                whiteRobotBitmap = loadBitmap(R.drawable.whiterobot, 100, 100) ?: throw RuntimeException("Failed to load white bitmap")

                // Initialize the player-controlled sprite
                val initialPosition = Vector(
                    (surface?.width ?: 0) / 2f, // Center horizontally
                    (surface?.height ?: 0) - 150f // Near the bottom
                )
                circle = Sprite(
                    bitmap = playerBitmap,
                    position = initialPosition,
                    width = 150f,
                    height = 150f
                )
                surface?.addGameObject(circle!!)
            }
        }

        fun startGame() {
            // Game logic starts only when the "Start" button is pressed
            spawnNewFallingItem()
        }

        private fun spawnNewFallingItem() {
            if (surface == null) return // Ensure surface is available before proceeding

            val bitmapColorMap = mapOf(
                orangeCatBitmap to Color.RED,
                orangeRobotBitmap to Color.BLUE,
                whiteCatBitmap to Color.GREEN,
                whiteRobotBitmap to Color.YELLOW
            )

            val (selectedBitmap, selectedColor) = bitmapColorMap.entries.random()

            val screenWidth = surface!!.width

            val item = DroppingRectangle(
                bitmap = selectedBitmap,
                position = Vector(
                    (Math.random() * screenWidth).toFloat(),
                    0f // Start at the top of the screen
                ),
                width = 100f,
                height = 100f,
                //dropRate = 0f // Drop rate will be calculated using physics
            )

            fallingItems.add(item)
            surface!!.addGameObject(item)

            // Initialize velocity
            velocities[item] = 900f
        }


        override fun onFixedUpdate() {
            if (isGameOver || isPaused) return // Skip updates if the game is over or paused
            super.onFixedUpdate()

            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastSpawnTime >= spawnInterval) {
                spawnNewFallingItem()
                lastSpawnTime = currentTime
                survivalCount++
            }

            val iterator = fallingItems.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()

                // Update velocity and position
                val deltaTime = 0.016f // Time per frame (60 FPS)
                val currentVelocity = velocities[item] ?: continue // Get current velocity
                val newVelocity = currentVelocity + gravity * deltaTime
                velocities[item] = newVelocity

                // Update position based on velocity
                item.position.y += newVelocity * 0.016f // Displacement = velocity * time

                // Remove if it hits the bottom
                if (item.position.y + item.height > (surface?.height ?: 0)) {
                    iterator.remove() // Remove from fallingItems
                    surface?.removeGameObject(item) // Remove from GameSurface
                    velocities.remove(item) // Remove velocity tracking
                }
            }

            checkFloorCollisions()
        }



        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)

            val paint = Paint().apply {
                textSize = 50f
                isAntiAlias = true
            }

            // Resolve colorPrimary dynamically
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorPrimary,
                typedValue,
                true
            )
            paint.color = typedValue.data
            val gameOverText = "Game Over! Score: $survivalCount"
            val textWidth = paint.measureText(gameOverText)
            val x = ((surface?.width ?: 0) - textWidth) / 2f
            val y = (surface?.height ?: 0) / 2f
            val highScoreText = "High Score: $highScore"
            val highScoreWidth = paint.measureText(highScoreText)
            // Draw Game Over screen
            if (isGameOver && canvas != null) {
                canvas.drawText(gameOverText, x, y, paint)
                canvas.drawText(highScoreText, ((surface?.width ?: 0) - highScoreWidth) / 2, y + 100, paint)
            }

            if (isPaused && canvas != null){
                canvas.drawText(highScoreText, ((surface?.width ?: 0) - highScoreWidth) / 2, y + 100, paint)
            }
        }

        fun resetGame() {
            // Reset game elements for a fresh start
            survivalCount = 0
            isGameOver = false

            // Clear falling items and remove them from the surface
            fallingItems.forEach { item ->
                surface?.removeGameObject(item) // Remove from GameSurface
            }
            fallingItems.clear()  // Clear the fallingItems list

            // Clear velocities for falling items
            velocities.clear()

            // Reset the player's position
            circle?.position = Vector(
                (surface?.width ?: 0) / 2f, // Center the player horizontally
                (surface?.height ?: 0) - 150f // Near the bottom of the screen
            )
        }


        private fun checkFloorCollisions() {
            val iterator = fallingItems.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (checkCollision(circle!!, item)) {
                    iterator.remove() // Remove from fallingItems list
                    surface?.removeGameObject(item) // Remove from GameSurface
                    velocities.remove(item) // Remove velocity tracking
                    isGameOver = true
                    checkHighScore()
                    break // End loop if game is over
                }
            }
        }

        private fun checkCollision(sprite: Sprite, rect: DroppingRectangle): Boolean {
            // Center of the sprite
            val spriteCenterX = sprite.position.x
            val spriteCenterY = sprite.position.y

            // Center of the rectangle
            val rectCenterX = rect.position.x
            val rectCenterY = rect.position.y

            // Distance between the centers
            val dx = spriteCenterX - rectCenterX
            val dy = spriteCenterY - rectCenterY
            val distance = Math.sqrt((dx * dx + dy * dy).toDouble())

            // radii if using hypotenuse
            //val spriteRadius = Math.sqrt((sprite.width * sprite.width + sprite.height * sprite.height).toDouble()) / 2
            //val rectRadius = Math.sqrt((rect.width * rect.width + rect.height * rect.height).toDouble()) / 2

            //radii if using half width
            val spriteRadius = sprite.width / 2
            val rectRadius = rect.width / 2

            // Collision occurs if the distance is less than the sum of the radii
            return distance < (spriteRadius + rectRadius)
        }

        private fun checkHighScore() {
            if (survivalCount > highScore) {
                highScore = survivalCount
                saveHighScore()
            }
        }

        private fun saveHighScore() {
            val editor = sharedPreferences.edit()
            editor.putInt(highScoreKey, highScore)
            editor.apply()
        }
    }

}