package com.example.assignment1.Adapters

import android.util.Log // 💡 Import Log for debugging
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.DataClass.Story
import com.example.assignment1.R
import com.bumptech.glide.Glide

class StoryAdapter(
    private val stories: List<Story>,
    private val onClick: (Story) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.storyProfileImage)
        val username: TextView = view.findViewById(R.id.storyUsername)
        val statusDot: View? = view.findViewById(R.id.statusDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        holder.username.text = story.username

        // CRITICAL FIX: Handle null/empty/bad URL strings
        val imageUrl = story.profileImage

        // Check if the URL is valid (not empty, not the string "null")
        if (imageUrl.isNullOrEmpty() || imageUrl.equals("null", ignoreCase = true)) {
            // Use default image if the URL is bad or missing
            holder.imageView.setImageResource(R.drawable.default_user)
        } else {
            // Load the image using Glide
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.default_user)
                .error(R.drawable.default_user)
                .into(holder.imageView)
        }

        // Show status based on API data
        if (story.status.equals("online", ignoreCase = true)) {
            holder.statusDot?.visibility = View.VISIBLE
        } else {
            // Ensure status dot is hidden if offline or status is missing
            holder.statusDot?.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onClick(story)
        }
    }

    override fun getItemCount(): Int {
        // 💡 DEBUG LOGGING: Print the entire list contents
        // Note: We use the Story Data Class's toString() method for a clean output.
        // This log will fire every time the adapter is checked for item count.
        Log.d("STORY_ADAPTER_DATA", "Final Story List Count: ${stories.size}. Data: $stories")

        return stories.size
    }
}