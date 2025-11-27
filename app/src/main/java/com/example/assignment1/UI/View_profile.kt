package com.example.assignment1.UI

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assignment1.R
import com.example.assignment1.Adapters.ProfilePostAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class View_profile : AppCompatActivity() {

    private lateinit var postRecycler: RecyclerView
    private lateinit var username: TextView
    private lateinit var username2: TextView
    private lateinit var postsCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView
    private lateinit var profileImage: ImageView

    private val postBitmaps = ArrayList<Bitmap>()
    private lateinit var adapter: ProfilePostAdapter

    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_profile)

        username = findViewById(R.id.username)
        username2 = findViewById(R.id.username2)
        postsCount = findViewById(R.id.posts)
        followersCount = findViewById(R.id.followers)
        followingCount = findViewById(R.id.following)
        profileImage = findViewById(R.id.profile_image)
        fetchUserData()
        fetchUserPosts()
        postRecycler = findViewById(R.id.profileRecycler)
        postRecycler.layoutManager = GridLayoutManager(this, 3)
        adapter = ProfilePostAdapter(this, postBitmaps)
        postRecycler.adapter = adapter


    }

    private fun fetchUserData() {
        val currentUserId = auth.currentUser?.uid ?: return

        dbRef.child("Users").child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    username.text = snapshot.child("username").value.toString()
                    username2.text = snapshot.child("username").value.toString()
                    followersCount.text = snapshot.child("followers").childrenCount.toString()
                    followingCount.text = snapshot.child("following").childrenCount.toString()

                    val dp64 = snapshot.child("dp64").value?.toString()
                    if (!dp64.isNullOrEmpty()) {
                        val bytes = Base64.decode(dp64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        profileImage.setImageBitmap(bitmap)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchUserPosts() {
        val currentUserId = auth.currentUser?.uid ?: return

        dbRef.child("Posts")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    postBitmaps.clear()
                    var count = 0

                    for (postSnap in snapshot.children) {
                        val userId = postSnap.child("userId").value.toString()
                        val base64Img = postSnap.child("imageUrl").value.toString()

                        if (userId == currentUserId && base64Img.isNotEmpty()) {
                            val bytes = Base64.decode(base64Img, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            postBitmaps.add(bitmap)
                            count++
                        }
                    }

                    postsCount.text = count.toString()
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
