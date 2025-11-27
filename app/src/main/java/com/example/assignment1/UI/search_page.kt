package com.example.assignment1.UI

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.assignment1.R

class search_page : AppCompatActivity() {
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val search_profile= findViewById<ImageView>(R.id.ivSearchIcon)
        search_profile.setOnClickListener {
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

            val intent = Intent(this, Search_screen::class.java)
            startActivity(intent)

        }
        val bottomBar = findViewById<View>(R.id.customNavBar)
        val home = bottomBar.findViewById<ImageView>(R.id.nav_home)
        val search = bottomBar.findViewById<ImageView>(R.id.nav_search)
        val create = bottomBar.findViewById<ImageView>(R.id.nav_create)
        val like = bottomBar.findViewById<ImageView>(R.id.nav_like)
        val profile = bottomBar.findViewById<ImageView>(R.id.nav_profile)
        val searchBar = findViewById<TextView>(R.id.searchEditText)
        searchBar.setOnClickListener {
            val intent=Intent(this, Search_screen::class.java)
            startActivity(intent)
        }
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
