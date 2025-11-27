package com.example.assignment1.Adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.DataClass.User
import com.example.assignment1.R

class SendPostAdapter(
    private val context: Context,
    private val userList: List<User>,
    private val onShareClick: (User) -> Unit // Callback when share button is clicked
) : RecyclerView.Adapter<SendPostAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.userProfileImage)
        val username: TextView = view.findViewById(R.id.username)
        val btnShare: ImageView = view.findViewById(R.id.btnSendUser) // matches your XML
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_send_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        holder.username.text = user.username

        // Load profile image safely
        try {
            if (!user.dp64.isNullOrEmpty()) {
                val imageBytes = Base64.decode(user.dp64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } else {
                holder.profileImage.setImageResource(R.drawable.dp)
            }
        } catch (e: Exception) {
            holder.profileImage.setImageResource(R.drawable.dp)
        }

        // Handle share button click
        holder.btnShare.setOnClickListener {
            onShareClick(user)
        }
    }
}
