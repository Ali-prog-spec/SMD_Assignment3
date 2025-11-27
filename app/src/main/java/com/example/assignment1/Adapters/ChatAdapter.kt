package com.example.assignment1.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.DataClass.ChatIttem
import com.example.assignment1.R
import com.bumptech.glide.Glide // 1. IMPORT GLIDE

class ChatAdapter(
    private val chatList: MutableList<ChatIttem>,
    private val onItemClick: (ChatIttem) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatItem = chatList[position]
        holder.bind(chatItem)
        holder.itemView.setOnClickListener { onItemClick(chatItem) }
    }

    override fun getItemCount(): Int = chatList.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userImage: ImageView = itemView.findViewById(R.id.user_image)
        private val usernameText: TextView = itemView.findViewById(R.id.username_text)
        private val lastMessageText: TextView = itemView.findViewById(R.id.message_text)

        fun bind(chatItem: ChatIttem) {
            usernameText.text = chatItem.username
            lastMessageText.text = chatItem.message

            // 2. FIX: Use Glide to load the image from the HTTP URL
            if (chatItem.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(chatItem.imageUrl) // chatItem.imageUrl is now the full HTTP URL
                    .placeholder(R.drawable.default_user) // Placeholder while loading
                    .error(R.drawable.default_user)      // Fallback on error
                    .into(userImage)
            } else {
                userImage.setImageResource(R.drawable.default_user)
            }
        }
    }
}