package com.example.skyfight

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair

class MainActivity : AppCompatActivity() {

    private lateinit var mainImage: View
    private lateinit var mainName: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainImage = findViewById(R.id.mainImage)
        mainName = findViewById(R.id.appName)

        // Delay for 1 second before navigating to MenuActivity
        Handler().postDelayed({
            startMenuActivityWithTransition()
        }, 1000)
    }

    private fun startMenuActivityWithTransition() {
        // Define shared element pairs
        val pairs = arrayOf<Pair<View, String>>(
            Pair(mainImage, mainImage.transitionName),
            Pair(mainName, mainName.transitionName)
        )

        // Create ActivityOptionsCompat
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, *pairs)

        // Start MenuActivity with shared element transition
        val intent = Intent(this, MenuActivity::class.java)
        startActivity(intent, options.toBundle())

        // Finish the current activity to prevent it from being displayed after the transition
        finish()
    }
}
