package com.example.skyfight


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val highScore = sharedPreferences.getInt("HighScore", 0)


        val highScoreTextView: TextView = findViewById(R.id.highscore)
        val newGameButton: View = findViewById(R.id.newGame)

        val resumeButton: Button = findViewById(R.id.resume)

        resumeButton.setOnClickListener {
            // Resume the game
            val intent = Intent(this@MenuActivity, GameActivity::class.java)
            startActivity(intent)
        }

        highScoreTextView.text = highScore.toString()

        newGameButton.setOnClickListener {
            // Reset the score to 0
            val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putInt("Score", 0)
            editor.apply()

            // Start a new game
            val intent = Intent(this@MenuActivity, GameActivity::class.java)
            startActivity(intent)
        }
    }
}

