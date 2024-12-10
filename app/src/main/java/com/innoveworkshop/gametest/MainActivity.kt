package com.innoveworkshop.gametest

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.innoveworkshop.gametest.engine.GameObject
import com.innoveworkshop.gametest.engine.GameSurface
import com.innoveworkshop.gametest.engine.Vector
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaPlayer
import com.innoveworkshop.gametest.assets.DroppingRectangle
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    protected var gameSurface: GameSurface? = null
    protected var leftButton: Button? = null
    protected var rightButton: Button? = null
    protected var startButton: Button? = null

    private var isGameOver = false
    private var isPaused = true
    private val highScoreKey = "high_score"  // key for saving high score

    var survivalCount: Int = 0
    protected var game: Game? = null
    private var meowSound: MediaPlayer? = null
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        meowSound = MediaPlayer.create(this, R.raw.meow)

        gameSurface = findViewById<View>(R.id.gameSurface) as GameSurface
        game = Game(this)
        gameSurface!!.setRootGameObject(game)

        //EASTEREGG: if the player is tapped then jerry does a meow sound
        gameSurface!!.setOnTouchListener { v, event ->
            if (!isPaused && !isGameOver) {
                val x = event.x
                val y = event.y

                game?.circle?.let {
                    if (it.isTapped(x, y)) {
                        meowSound?.start()
                    }
                }
            }
            true
        }

        setupControls()

        //  listener to detect window focus changes, if doesnt have the focus then pause
        gameSurface?.viewTreeObserver?.addOnWindowFocusChangeListener { hasFocus ->
            if (!hasFocus) {
                if (!isPaused) { // only pause if not already paused
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

        //start button resets the game if its over and toggles the pause state
        startButton = findViewById<View>(R.id.start_button) as Button
        startButton!!.setOnClickListener {
            if (isGameOver) {
                game?.resetGame()
                isGameOver = false
                survivalCount = 0
                game!!.startGame()
            } else {
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

        //draw the sprite on the center of the screen
        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            canvas?.drawBitmap(
                Bitmap.createScaledBitmap(bitmap, width.toInt(), height.toInt(), true),
                position.x - width / 2,
                position.y - height / 2,
                Paint()
            )
        }

        // check if the tap coordinates are within the sprite's bounds
        fun isTapped(x: Float, y: Float): Boolean {
            val centerX = position.x
            val centerY = position.y

            val radius = width / 2

            val distance = Math.sqrt(
                ((x - centerX).toDouble().pow(2) + (y - centerY).toDouble().pow(2))
            )

            return distance <= radius
        }

    }

    inner class Game(private val context: android.content.Context) : GameObject() {
        private fun loadBitmap(resourceId: Int, width: Int, height: Int): Bitmap {
            val originalBitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            return Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        }

        private val fallingItems = mutableListOf<DroppingRectangle>()
        private val velocities = mutableMapOf<DroppingRectangle, Float>()
        var circle: Sprite? = null

        private var surface: GameSurface? = null
        private val gravity = 10f // gravity value for falling items similar to earths (9.80665 m/s)

        private val spawnInterval: Long = 1000 // time interval between spawns
        private var lastSpawnTime: Long = 0
        private var highScore: Int = 0 // best score to be perserved on the app

        //doesnt let the player leave the screen all the way
        fun constrainPlayerPosition() {
            circle?.let { player ->
                val halfJerry = player.width / 2
                val screenWidth = surface!!.width
                player.position.x = player.position.x.coerceIn(halfJerry, screenWidth - halfJerry)
            }
        }

        // bitmaps for the falling cats
        private lateinit var playerBitmap: Bitmap
        private lateinit var orangeCatBitmap: Bitmap
        private lateinit var orangeRobotBitmap: Bitmap
        private lateinit var whiteCatBitmap: Bitmap
        private lateinit var whiteRobotBitmap: Bitmap

        override fun onStart(surface: GameSurface?) {
            super.onStart(surface)
            this.surface = surface

            // get high score from sharedpreferences
            highScore = sharedPreferences.getInt(highScoreKey, 0)

            if (circle == null) {
                // load bitmaps
                playerBitmap = loadBitmap(R.drawable.jerry, 95, 200) ?: throw RuntimeException("Failed to load player bitmap")
                orangeCatBitmap = loadBitmap(R.drawable.orangecat, 100, 100) ?: throw RuntimeException("Failed to load orange bitmap")
                orangeRobotBitmap = loadBitmap(R.drawable.orangerobot, 100, 100) ?: throw RuntimeException("Failed to load orange bitmap")
                whiteCatBitmap = loadBitmap(R.drawable.whitecat, 100, 100) ?: throw RuntimeException("Failed to load white bitmap")
                whiteRobotBitmap = loadBitmap(R.drawable.whiterobot, 100, 100) ?: throw RuntimeException("Failed to load white bitmap")

                // initialiseing jerry sprite at the center of the screen and near the bottom
                val initialPosition = Vector(
                    (surface?.width ?: 0) / 2f,
                    (surface?.height ?: 0) - 150f
                )

                circle = Sprite( //place sprite on initial position and give it 150 width and height
                    bitmap = playerBitmap,
                    position = initialPosition,
                    width = 150f,
                    height = 150f
                )
                //add game object
                surface?.addGameObject(circle!!)
            }
        }

        fun startGame() {
            // game starts only when the start button is pressed
            spawnNewFallingItem()
        }

        private fun spawnNewFallingItem() {
            if (surface == null) return

            //assign a type of cat to the dropping rectangle colour
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
                    (Math.random() * (screenWidth - 200) + 100).toFloat(),
                    0f
                ),
                width = 100f,
                height = 100f,
            )
            //add square to list and to surface
            fallingItems.add(item)
            surface!!.addGameObject(item)

            //item initial velocity is 900, otherwise its super slow
            velocities[item] = 900f

            //velocities[item] = 2000f
        }


        override fun onFixedUpdate() {
            if (isGameOver || isPaused) return // skip updates if the game is over or paused
            super.onFixedUpdate()

            //only spawn new item if enough time has passed, reset time since last spawn and then add to the score
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastSpawnTime >= spawnInterval) {
                spawnNewFallingItem()
                lastSpawnTime = currentTime
                survivalCount++
            }

            //iterating through the fallingItems list,
            val iterator = fallingItems.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()

                val deltaTime = 0.016f // time per frame (60 FPS)
                val currentVelocity = velocities[item] ?: continue // get current velocity
                val newVelocity =
                    currentVelocity + gravity * deltaTime //finalvelocity = initialvelocity + aceleration * time
                velocities[item] = newVelocity //assign the new velocity

                // update position based on velocity
                item.position.y += newVelocity * 0.016f // displacement = velocity * time
                //remove from fallingitems, from game surface and from velocities if it hits the floor
                if (item.position.y + item.height > (surface?.height ?: 0)) {
                    iterator.remove()
                    surface?.removeGameObject(item)
                    velocities.remove(item)
                }
            }
            checkCollisions()
        }

        override fun onDraw(canvas: Canvas?) {
            //draw texts
            super.onDraw(canvas)

            val paint = Paint().apply {
                textSize = 50f
                isAntiAlias = true
            }

            // resolve colorPrimary dynamically
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

            if (isGameOver && canvas != null) {
                canvas.drawText(gameOverText, x, y, paint)
                canvas.drawText(highScoreText, ((surface?.width ?: 0) - highScoreWidth) / 2, y + 100, paint)
            }

            if (isPaused && canvas != null){
                canvas.drawText(highScoreText, ((surface?.width ?: 0) - highScoreWidth) / 2, y + 100, paint)
            }
        }

        fun resetGame() {
            // reset score and gameover bool
            survivalCount = 0
            isGameOver = false

            // clear falling items from game surface
            fallingItems.forEach { item ->
                surface?.removeGameObject(item)
            }
            fallingItems.clear()  // clear the fallingItems list

            // clear velocities for falling items
            velocities.clear()

            // put the player in the center of the screen again
            circle?.position = Vector(
                (surface?.width ?: 0) / 2f,
                (surface?.height ?: 0) - 150f
            )
        }


        private fun checkCollisions() {
            val iterator = fallingItems.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (checkDistance(circle!!, item)) {
                    iterator.remove() // remove from fallingItems list
                    surface?.removeGameObject(item) // remove from GameSurface
                    velocities.remove(item) // remove velocityies
                    isGameOver = true //declare game over
                    checkHighScore()
                    break // End loop if game is over
                }
            }
        }

        private fun checkDistance(sprite: Sprite, rect: DroppingRectangle): Boolean {
            val spriteCenterX = sprite.position.x
            val spriteCenterY = sprite.position.y
            val rectCenterX = rect.position.x
            val rectCenterY = rect.position.y

            // distance between the centers
            val dx = spriteCenterX - rectCenterX
            val dy = spriteCenterY - rectCenterY
            val distance = Math.sqrt((dx * dx + dy * dy).toDouble())

            // radii if using hypotenuse
            //val spriteRadius = Math.sqrt((sprite.width * sprite.width + sprite.height * sprite.height).toDouble()) / 2
            //val rectRadius = Math.sqrt((rect.width * rect.width + rect.height * rect.height).toDouble()) / 2

            //radii if using half width
            val spriteRadius = sprite.width / 2
            val rectRadius = rect.width / 2

            // collision occurs if the distance is less than the sum of the radii
            return distance < (spriteRadius + rectRadius)
        }

        //if the score from the round is best than the previous high score then set it as the new highscore
        private fun checkHighScore() {
            if (survivalCount > highScore) {
                highScore = survivalCount
                saveHighScore()
            }
        }

        //push the highscore to shared preferences
        private fun saveHighScore() {
            val editor = sharedPreferences.edit()
            editor.putInt(highScoreKey, highScore)
            editor.apply()
        }
    }

}