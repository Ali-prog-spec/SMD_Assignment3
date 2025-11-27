package com.example.assignment1.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assignment1.DataClass.Reel
import com.example.assignment1.R

class ReelsAdapter(
    private val reelsList: List<Reel>,
    private val onReelClick: (Reel) -> Unit
) : RecyclerView.Adapter<ReelsAdapter.ReelViewHolder>() {

    inner class ReelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reelImage: ImageView = itemView.findViewById(R.id.dp)
        val usernameText: TextView = itemView.findViewById(R.id.username)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reel, parent, false)
        return ReelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReelViewHolder, position: Int) {
        val reel = reelsList[position]

        // Load image (Glide handles both Base64 URLs and Firebase URLs)
        Glide.with(holder.itemView.context)
            .load(reel.imageURL)
            .placeholder(R.drawable.dp8) // optional
            .into(holder.reelImage)

        holder.usernameText.text = reel.username

        // Optional: make seen/unseen effect (like faded)
        holder.reelImage.alpha = if (reel.isSeen) 0.5f else 1.0f

        // Handle click
        holder.itemView.setOnClickListener {
            onReelClick(reel)
        }
    }

    override fun getItemCount(): Int = reelsList.size
}


