package com.example.assignment1.UI

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.assignment1.R
import com.google.firebase.database.FirebaseDatabase

class PostDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        val postId = intent.getStringExtra("postId")
        val captionView = findViewById<TextView>(R.id.postCaption)
        val imageView = findViewById<ImageView>(R.id.postImage)

        if (postId != null) {
            val postRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId)
            postRef.get().addOnSuccessListener { snapshot ->
                val caption = snapshot.child("caption").getValue(String::class.java) ?: ""
                val base64 = snapshot.child("postImageBase64").getValue(String::class.java) ?: ""
                captionView.text = caption

                if (base64.isNotEmpty()) {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }
}