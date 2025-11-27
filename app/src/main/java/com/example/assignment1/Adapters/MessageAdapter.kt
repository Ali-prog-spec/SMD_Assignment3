package com.example.assignment1.Adapters

import android.app.AlertDialog
import android.graphics.Color // NEW IMPORT
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.ChatMessage
import com.example.assignment1.R
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONObject

class MessageAdapter(
    private val messages: MutableList<ChatMessage>,
    private val currentUserId: String,
    private val baseUrl: String // REQUIRED: Fixes the constructor error
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    var onEdit: ((ChatMessage) -> Unit)? = null
    var onDelete: ((ChatMessage) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val messageText: TextView = itemView.findViewById(R.id.textMessage)
        private val imageView: ImageView = itemView.findViewById(R.id.imageMessage)
        private val container: LinearLayout = itemView.findViewById(R.id.messageContainer)
        private val sharedPostImage: ImageView = itemView.findViewById(R.id.sharedPostImage)
        private val sharedPostText: TextView = itemView.findViewById(R.id.sharedPostText)

        // Indicator for edited status, attached to the main container
        private val editedIndicator: TextView = TextView(itemView.context).apply {
            textSize = 10f
            alpha = 0.6f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                setMargins(0, 0, 8, 0)
            }
            container.addView(this)
        }


        fun bind(msg: ChatMessage) {

            // --- 🛑 HANDLE DELETED MESSAGE ---
            if (msg.isDeleted) {
                messageText.text = if (msg.senderId == currentUserId)
                    "You deleted this message."
                else
                    "This message was deleted."
                messageText.visibility = View.VISIBLE

                // Reset other views
                imageView.visibility = View.GONE
                sharedPostImage.visibility = View.GONE
                sharedPostText.visibility = View.GONE
                editedIndicator.visibility = View.GONE

                itemView.setOnLongClickListener(null) // Disable actions

                // Use simple gray color and text color for deleted message
                messageText.setBackgroundColor(Color.parseColor("#E0E0E0")) // Light Gray background
                messageText.setTextColor(Color.GRAY) // Gray text color

                // Alignment for deleted message
                container.gravity = if (msg.senderId == currentUserId) Gravity.END else Gravity.START
                return
            }

            // Restore text color for normal messages
            messageText.setTextColor(Color.BLACK)

            // Reset views for normal messages
            messageText.visibility = View.GONE
            imageView.visibility = View.GONE
            sharedPostImage.visibility = View.GONE
            sharedPostText.visibility = View.GONE

            // CASE ✅ Shared Post
            if (!msg.postId.isNullOrEmpty()) {
                loadSharedPost(msg)

                // CASE ✅ Image message (Now uses imagePath/URL)
            } else if (!msg.imagePath.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(msg.imagePath)
                    .placeholder(R.drawable.default_user)
                    .error(R.drawable.default_user)
                    .into(imageView)
                imageView.visibility = View.VISIBLE

                // CASE ✅ Text message (This runs ONLY if no Post/Image path exists AND text is not empty)
            } else if (!msg.message.isNullOrEmpty()) { // <-- FIX: Explicitly check for non-empty text
                messageText.text = msg.message
                messageText.visibility = View.VISIBLE
            }
            // Note: If none of the above conditions are met (e.g., empty text, no image, no post),
            // the message content will be hidden, which is the desired outcome for an empty message.


            // Display Edited Status
            if (msg.isEdited) {
                editedIndicator.text = "(edited)"
                editedIndicator.visibility = View.VISIBLE
            } else {
                editedIndicator.visibility = View.GONE
            }

            // ✅ Message Alignment and Background
            if (msg.senderId == currentUserId) {
                container.gravity = Gravity.END
                if (!msg.isDeleted) messageText.setBackgroundResource(R.drawable.message_card_right)
            } else {
                container.gravity = Gravity.START
                if (!msg.isDeleted) messageText.setBackgroundResource(R.drawable.message_card_left)
            }

            // ✅ Long press Edit/Delete
            if (msg.senderId == currentUserId && !msg.isDeleted) {
                val fiveMinutes = 5 * 60 * 1000
                val diff = System.currentTimeMillis() - (msg.timestamp ?: 0)

                itemView.setOnLongClickListener {
                    if (diff <= fiveMinutes) {
                        showOptions(msg)
                    } else {
                        Toast.makeText(itemView.context, "Cannot edit messages older than 5 minutes.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            } else {
                itemView.setOnLongClickListener(null)
            }
        }

        private fun loadSharedPost(msg: ChatMessage) {
            sharedPostText.visibility = View.VISIBLE
            sharedPostText.text = "Loading shared post..."

            val url = baseUrl + "get_post_details.php?post_id=${msg.postId}"

            val stringRequest = StringRequest(Request.Method.GET, url, { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        val post = jsonResponse.getJSONObject("post")
                        val caption = post.getString("caption")
                        val imagePath = post.getString("imagePath")

                        sharedPostText.text = caption

                        if (imagePath.isNotEmpty()) {
                            val imageUrl = baseUrl + imagePath
                            Glide.with(itemView.context)
                                .load(imageUrl)
                                .placeholder(R.drawable.default_user)
                                .error(R.drawable.default_user)
                                .into(sharedPostImage)
                        } else {
                            sharedPostImage.setImageResource(R.drawable.default_user)
                        }
                        sharedPostImage.visibility = View.VISIBLE

                    } else {
                        sharedPostText.text = "⚠ Post removed or not found"
                    }
                } catch (e: Exception) {
                    sharedPostText.text = "⚠ Error loading post details"
                }
            }, { error ->
                sharedPostText.text = "⚠ Network error loading post"
            })

            Volley.newRequestQueue(itemView.context).add(stringRequest)
        }

        private fun showOptions(msg: ChatMessage) {
            val options = arrayOf("Edit", "Delete")
            AlertDialog.Builder(itemView.context)
                .setTitle("Select Action")
                .setItems(options) { _, i ->
                    when (i) {
                        0 -> onEdit?.invoke(msg)
                        1 -> onDelete?.invoke(msg)
                    }
                }.show()
        }
    }
}