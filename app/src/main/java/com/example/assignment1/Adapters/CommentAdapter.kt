package com.example.assignment1.UI.adapters

import Comment
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.R
import com.bumptech.glide.Glide // 💡 REQUIRED for URL loading

class CommentAdapter(
    private val context: Context,
    private val comments: MutableList<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    // 🛑 Firebase references removed

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val commentUserDp: ImageView = itemView.findViewById(R.id.commentUserDp)
        val commentUsername: TextView = itemView.findViewById(R.id.commentUsername)
        val commentText: TextView = itemView.findViewById(R.id.commentText)
        val commentTime: TextView = itemView.findViewById(R.id.commentTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        holder.commentUsername.text = comment.username
        holder.commentText.text = comment.text

        // Format time as "x minutes ago"
        val timeAgo = android.text.format.DateUtils.getRelativeTimeSpanString(
            comment.timestamp,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS
        )
        holder.commentTime.text = timeAgo.toString()

        // 💡 Load user DP from URL (userDpBase64 now holds the URL string)
        setUserDpFromUrl(holder.commentUserDp, comment.userDpBase64)
    }

    // 💡 New function to load image using Glide from a URL
    private fun setUserDpFromUrl(imageView: ImageView, dpUrl: String?) {
        if (!dpUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(dpUrl)
                .placeholder(R.drawable.dp1) // Placeholder
                .error(R.drawable.dp1) // Error image
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.dp1) // Default local drawable
        }
    }

    override fun getItemCount(): Int = comments.size
}