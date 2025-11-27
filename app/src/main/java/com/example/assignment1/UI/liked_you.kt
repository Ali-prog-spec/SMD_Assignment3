package com.example.assignment1.UI

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.assignment1.R


class liked_you : AppCompatActivity() {

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //  open camera page
//            val intent = Intent(this, Camera_page::class.java)
//            startActivity(intent)
            // open Actual Phone's Camera
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, 200)


        }
        else {
            // Camera page opening
//            val intent = Intent(this, Camera_page::class.java)
//            startActivity(intent)
            //for phone's camera opening
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, 200)

            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }
    private fun setActiveNavIcon(active: String) {
        val home = findViewById<ImageView>(R.id.nav_home)
        val search = findViewById<ImageView>(R.id.nav_search)
        val create = findViewById<ImageView>(R.id.nav_create)
        val like = findViewById<ImageView>(R.id.nav_like)
        val profile = findViewById<ImageView>(R.id.nav_profile)

        // Reset all icons to default state (no tint, base icon)
        val activeColor = ContextCompat.getColor(this, R.color.dark_brown)
        home.setImageResource(R.drawable.home)


        search.setImageResource(R.drawable.search)
        search.setColorFilter(activeColor)


        like.setImageResource(R.drawable.like)
        like.setColorFilter(activeColor)



        // Apply active state
        when (active) {
            "home" -> {
                home.setImageResource(R.drawable.home_dark)
                home.setColorFilter(activeColor)
            }
            "search" -> {
                search.setImageResource(R.drawable.search_dark)
                search.setColorFilter(activeColor)
            }

            "like" -> {
                like.setImageResource(R.drawable.like_dark)
                like.setColorFilter(activeColor)
            }
            "profile" -> {
                profile.setColorFilter(activeColor)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_liked_you)
        setActiveNavIcon("like")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomBar = findViewById<View>(R.id.customNavBar)
        val home = bottomBar.findViewById<ImageView>(R.id.nav_home)
        val search = bottomBar.findViewById<ImageView>(R.id.nav_search)
        val create = bottomBar.findViewById<ImageView>(R.id.nav_create)
        val like = bottomBar.findViewById<ImageView>(R.id.nav_like)
        val profile = bottomBar.findViewById<ImageView>(R.id.nav_profile)

        home.setOnClickListener {
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

            val intent = Intent(this, home_page::class.java)
            startActivity(intent)
        }

        search.setOnClickListener {
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

            val intent = Intent(this, search_page::class.java)
            startActivity(intent)
        }

        create.setOnClickListener {
            // ask for camera permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    100 // request code
                )
            } else {
                // open camera page

//                val intent = Intent(this, Camera_page::class.java)
//                startActivity(intent)

                // open phone's camera
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 200)

            }
        }

        like.setOnClickListener {
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

            val intent = Intent(this, Liked_following::class.java)
            startActivity(intent)
        }

        profile.setOnClickListener {
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val intent = Intent(this, View_profile::class.java)
            startActivity(intent)
        }


    }
}
