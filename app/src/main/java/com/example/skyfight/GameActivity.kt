package com.example.skyfight

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var bullet: ImageView
    private lateinit var jet: ImageView
    private lateinit var leftButton: ImageView
    private lateinit var rightButton: ImageView
    private lateinit var ufo: ImageView
    private lateinit var scoreTextView: TextView

    private var movementSpeed = 50f
    private var movementHandler: Handler? = null
    private var maxX = 0f
    private var score = 0
    private var collisionDetected = false
    private var isGamePaused = false
    private var bulletAnim: ObjectAnimator? = null
    private var ufoHandler: Handler? = null
    private var ufoRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Load the game state and score
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        score = sharedPreferences.getInt("Score", 0)
        isGamePaused = sharedPreferences.getBoolean("IsGamePaused", false)
        val highScore = sharedPreferences.getInt("HighScore", 0)

        // Update the score TextView
        scoreTextView = findViewById(R.id.scoreno)
        scoreTextView.text = score.toString()
        val highScoreTextView: TextView = findViewById(R.id.hsno)
        highScoreTextView.text = highScore.toString()

        bullet = findViewById(R.id.bullet)
        jet = findViewById(R.id.jet)
        leftButton = findViewById(R.id.left_button)
        rightButton = findViewById(R.id.right_button)
        ufo = findViewById(R.id.ufo)
        maxX = resources.displayMetrics.widthPixels.toFloat()

        val cloud1: ImageView = findViewById(R.id.cloud1)
        val cloud2: ImageView = findViewById(R.id.cloud2)
        val cloud3: ImageView = findViewById(R.id.cloud3)

        // Create the animations
        val animation1 = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 1f,
            Animation.RELATIVE_TO_PARENT, -1f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0f
        )
        animation1.duration = 10000 // Set duration to 10 seconds
        animation1.repeatCount = Animation.INFINITE // Set to repeat indefinitely
        animation1.repeatMode = Animation.RESTART // Restart animation when it reaches the end

        val animation2 = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, -1f,
            Animation.RELATIVE_TO_PARENT, 1f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0f
        )
        animation2.duration = 3000 // Set duration to 10 seconds
        animation2.repeatCount = Animation.INFINITE // Set to repeat indefinitely
        animation2.repeatMode = Animation.RESTART // Restart animation when it reaches the end


        leftButton.setOnTouchListener { _, event ->
            handleButtonTouch(event, true)
        }
        rightButton.setOnTouchListener { _, event ->
            handleButtonTouch(event, false)
        }

        updateScore()
        startCollisionCheckLoop()
        startBulletAnimation()
        startUfoAnimation()
        cloud1.startAnimation(animation1)
        cloud3.startAnimation(animation1)
        cloud2.startAnimation(animation2)
    }

    private fun startBulletAnimation() {
        // Only start a new animation if the previous one has finished or not started yet
        if (bulletAnim?.isRunning != true) {
            bullet.visibility = View.VISIBLE

            val initialY = jet.y - bullet.height
            val finalY = -resources.displayMetrics.heightPixels.toFloat()

            bullet.x = jet.x

            bulletAnim = ObjectAnimator.ofFloat(bullet, "translationY", initialY, finalY)
            bulletAnim?.duration = 1000

            bulletAnim?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    bullet.x = jet.x
                    startBulletAnimation()
                }
            })

            bulletAnim?.addUpdateListener { animator ->
                Log.d("BulletPosition", "X: ${bullet.x}, Y: ${bullet.y}")

                if (!collisionDetected && isCollisionDetected()) {
                    Log.d("Collision", "Collision Detected!")
                    updateScore()
                    destroyBulletAndUfo()
                    collisionDetected = true
                }
            }
            bulletAnim?.start()
        }
    }

    private fun startUfoAnimation() {
        ufo.visibility = View.VISIBLE

        val initialY = -ufo.height.toFloat()
        val finalY = resources.displayMetrics.heightPixels.toFloat()

        // Set the UFO's initial X position to a random value only when the animation starts
        ufo.x = Random.nextInt(0, resources.displayMetrics.widthPixels - ufo.width).toFloat()
        ufo.y = initialY

        ufoRunnable = object : Runnable {
            override fun run() {
                // Update the UFO's Y position
                ufo.y += 2 // Adjust the speed as needed

                // If the UFO has reached the bottom of the screen, reset its Y position and change its X position
                if (ufo.y >= finalY) {
                    gameOver()
                    return
                }

                // If a collision is detected, update the score and destroy the bullet and UFO
                if (!collisionDetected && isCollisionDetected()) {
                    ufo.y = initialY
                    ufo.x = Random.nextInt(0, resources.displayMetrics.widthPixels - (ufo.width*2)).toFloat()
                    updateScore()
                    destroyBulletAndUfo()
                    collisionDetected = true
                }

                // Repeat this runnable
                ufoHandler?.postDelayed(this, 0) // Adjust the delay as needed
            }
        }

        // Start the runnable
        ufoHandler = Handler()
        ufoHandler?.post(ufoRunnable!!)
    }

    private fun startCollisionCheckLoop() {
        val collisionCheckRunnable = object : Runnable {
            override fun run() {
                // Check for collision
                if (isCollisionDetected()) {
                    // Log for debugging
                    Log.d("Collision", "Collision Detected!")
                    updateScore()
                    destroyBulletAndUfo()
                }
                // Repeat the check after a delay (adjust delay as needed)
                Handler().postDelayed(this, 10) // Adjusted delay
            }
        }
        // Start the collision check loop
        Handler().post(collisionCheckRunnable)
    }

    private fun isBoundingBoxCollision(): Boolean {
        val bulletBounds = Rect(
            bullet.x.toInt(),
            bullet.y.toInt(),
            (bullet.x + bullet.width).toInt(),
            (bullet.y + bullet.height).toInt()
        )
        val ufoBounds = Rect(
            ufo.x.toInt(),
            ufo.y.toInt(),
            (ufo.x + ufo.width).toInt(),
            (ufo.y + ufo.height).toInt()
        )
        return Rect.intersects(bulletBounds, ufoBounds)
    }

    private fun isCollisionDetected(): Boolean {
        return isBoundingBoxCollision()
    }

    private fun destroyBulletAndUfo() {
        // Clear the animations
        bullet.clearAnimation()
        ufo.clearAnimation()

        // Make the bullet and the UFO disappear
        bullet.visibility = View.GONE
        ufo.visibility = View.GONE

        // Reset the positions of the bullet and the UFO
        bullet.y = jet.y // Set the bullet's initial Y position to the jet's Y position
        ufo.y = -ufo.height.toFloat()

        // Reset the collisionDetected flag when the animations are restarted
        collisionDetected = false

        startBulletAnimation()
        startUfoAnimation()
    }

    private fun updateScore() {
        score++
        scoreTextView.text = score.toString()

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        var highScore = sharedPreferences.getInt("HighScore", 0)

        if (score > highScore) {
            highScore = score
            saveHighScore(highScore)
            val highScoreTextView: TextView = findViewById(R.id.hsno)
            highScoreTextView.text = highScore.toString() // Update the high score TextView in real-time
        }
    }

    private fun handleButtonTouch(event: MotionEvent, isLeftButton: Boolean): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                movementHandler = Handler()
                startMoving(isLeftButton)
            }

            MotionEvent.ACTION_UP -> {
                movementHandler?.removeCallbacksAndMessages(null)
            }
        }
        return true
    }

    private fun startMoving(isLeftButton: Boolean) {
        movementHandler?.post(object : Runnable {
            override fun run() {
                move(isLeftButton)
                movementHandler?.postDelayed(this, 50) // Adjust the delay as needed
            }
        })
    }

    private fun move(isLeftButton: Boolean) {

        val currentX = jet.x
        val dx = movementSpeed * if (isLeftButton) -1 else 1
        val newX = currentX + dx
        val jetWidth = jet.width.toFloat()
        val minX = 0f // No offset for left edge
        val maxX = resources.displayMetrics.widthPixels.toFloat() - jetWidth

        val clampedX = newX.coerceIn(minX, maxX)
        jet.x = clampedX
    }

    private fun saveHighScore(score: Int) {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("HighScore", score)
        editor.apply()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Pause the game
        isGamePaused = true
        pauseGame()

        // Save the game state and score
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("Score", score)
        editor.putBoolean("IsGamePaused", isGamePaused)
        editor.apply()

        Log.d("GameActivity", "Game paused. Score: $score, IsGamePaused: $isGamePaused")

        // Go back to the main menu
        val intent = Intent(this@GameActivity, MenuActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()

        // Load the game state and score
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        score = sharedPreferences.getInt("Score", 0)
        isGamePaused = sharedPreferences.getBoolean("IsGamePaused", false)

        Log.d("GameActivity", "Game resumed. Score: $score, IsGamePaused: $isGamePaused")

        if (isGamePaused) {
            // Resume the game
            isGamePaused = false
            resumeGame()
        }
    }

    private fun pauseGame() {
        // Pause all animations and other game activities
        bullet.animation?.let { it.cancel() }
        ufo.animation?.let { it.cancel() }
        // Add code to pause other game activities if needed

        Log.d("GameActivity", "Game activities paused")
    }

    private fun resumeGame() {
        // Resume all animations and other game activities
        startBulletAnimation()
        startUfoAnimation()
        // Add code to resume other game activities if needed

        Log.d("GameActivity", "Game activities resumed")
    }

    private fun gameOver() {
        // Pause the game
        isGamePaused = true
        pauseGame()

        // Display the game over dialog with the final score
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Game Over")
        builder.setMessage("Your score: $score")
        builder.setPositiveButton("OK") { _, _ ->
            // Go back to the main menu
            val intent = Intent(this@GameActivity, MenuActivity::class.java)
            startActivity(intent)
            finish()
        }
        builder.show()
    }
}


