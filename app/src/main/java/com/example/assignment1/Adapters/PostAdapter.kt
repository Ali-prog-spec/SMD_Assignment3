package com.example.assignment1.UI.adapters

import Comment
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.DataClass.Post
import com.example.assignment1.R
import com.example.assignment1.Utils.BaseUrlUtil
import com.bumptech.glide.Glide
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.IOException
import kotlin.random.Random
import java.util.HashMap

class PostAdapter(
    // FIX: Changed List<Post> to MutableList<Post> to allow state updates
    private val context: Context,
    private val postsList: MutableList<Post>
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var CURRENT_DB_USER_ID: Int = 0
    private val PREFS_NAME = "SocialAppPrefs"
    private val KEY_USER_ID = "current_user_id"
    private lateinit var baseUrl: String

    init {
        loadUserId()
        try {
            baseUrl = BaseUrlUtil.getBaseUrl(context)
        } catch (e: IOException) {
            Toast.makeText(context, "Base URL Initialization Error.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Loads the stored MySQL user ID from Shared Preferences.
     */
    private fun loadUserId() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        CURRENT_DB_USER_ID = prefs.getInt(KEY_USER_ID, 0)
        if (CURRENT_DB_USER_ID == 0) {
            Log.e("PostAdapter", "MySQL User ID not found in SharedPreferences!")
        }
    }

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.postUsername)
        val caption: TextView = view.findViewById(R.id.postCaption)
        val postImage: ImageView = view.findViewById(R.id.postImage)
        val likeBtn: ImageView = view.findViewById(R.id.btnLike)
        val likeCount: TextView = view.findViewById(R.id.postLikes)
        val commentRecycler: RecyclerView = view.findViewById(R.id.commentRecyclerView)
        val postComments: TextView = view.findViewById(R.id.postComments)
        // Ensure btnComment is accessible for click listener
        val btnComment: ImageView = view.findViewById(R.id.btnComment)
        val commentBox: View = view.findViewById(R.id.commentBox)
        val editBox: EditText = view.findViewById(R.id.editBox)
        val btnSend: ImageView = view.findViewById(R.id.btnSend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int = postsList.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postsList[position]
        // Pass position to bindPost
        bindPost(holder, post, position)
    }

    private fun bindPost(holder: PostViewHolder, post: Post, position: Int) {
        holder.username.text = post.username
        holder.caption.text = post.caption
        holder.likeCount.text = "${post.likeCount} likes"
        holder.postComments.text = "View all ${post.commentCount} comments"

        // Image Loading using baseUrl
        val fullImageUrl = baseUrl + post.postImageBase64

        Glide.with(context)
            .load(fullImageUrl)
            .placeholder(R.drawable.default_user)
            .error(R.drawable.default_icon)
            .into(holder.postImage)

        val isLiked = post.isLiked
        holder.likeBtn.setImageResource(if (isLiked) R.drawable.like_dark else R.drawable.like)

        // Like button click
        holder.likeBtn.setOnClickListener {
            toggleLike(post, holder, isLiked, position)
        }

        // --- COMMENT RECYCLER VIEW SETUP ---
        val actualComments = post.comments.toMutableList()
        val adapter = CommentAdapter(context, actualComments)
        holder.commentRecycler.layoutManager = LinearLayoutManager(context)
        holder.commentRecycler.adapter = adapter
        adapter.notifyDataSetChanged()

        // --- VISIBILITY TOGGLE FIX ---

        // CRUCIAL: Reset views to GONE when binding/recycling to prevent state bleed
        holder.commentBox.visibility = View.GONE
        holder.commentRecycler.visibility = View.GONE

        // Define the robust toggle logic
        val toggleCommentViews = {
            val isVisible = holder.commentRecycler.visibility == View.VISIBLE
            if (isVisible) {
                // Hide
                holder.commentRecycler.visibility = View.GONE
                holder.commentBox.visibility = View.GONE
            } else {
                // Show
                holder.commentRecycler.visibility = View.VISIBLE
                holder.commentBox.visibility = View.VISIBLE
            }
        }

        // 1. Click listener for "View all comments" text
        holder.postComments.setOnClickListener {
            toggleCommentViews()
        }

        // 2. Click listener for the comment button icon
        holder.btnComment.setOnClickListener {
            toggleCommentViews()
        }

        // --- POST COMMENT LOGIC ---
        holder.btnSend.setOnClickListener {
            val text = holder.editBox.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            if (CURRENT_DB_USER_ID == 0) {
                Toast.makeText(context, "Error: Cannot comment, user not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Send the comment to the API
            sendCommentToApi(post, holder, text)

            // 2. Clear input and hide box immediately for better UX
            holder.editBox.text.clear()
            holder.commentBox.visibility = View.GONE
        }
    }

    private fun sendCommentToApi(post: Post, holder: PostViewHolder, commentText: String) {
        val url = baseUrl + "add_comment.php"

        // Optimistic Comment Object (Placeholder data)
        val newComment = Comment(
            commentId = "-1",
            userId = CURRENT_DB_USER_ID.toString(),
            // Use holder.username.text as a stand-in for the current user's name
            username = holder.username.text.toString(),
            text = commentText,
            timestamp = System.currentTimeMillis(),
            userDpBase64 = "http://localhost/dummy/dp/my_dp.jpg"
        )

        // Use the comment list attached to this post (through its adapter)
        val actualComments = post.comments.toMutableList()
        val adapter = holder.commentRecycler.adapter as CommentAdapter

        val stringRequest = object : StringRequest(Method.POST, url, { response ->
            try {
                val jsonResponse = JSONObject(response)
                if (jsonResponse.getBoolean("success")) {
                    val newCommentId = jsonResponse.getString("new_comment_id")
                    val newCommentCount = jsonResponse.getInt("new_comment_count")

                    // A. Update the temporary comment object with the real ID from the server
                    val finalComment = newComment.copy(commentId = newCommentId)

                    // B. Add the final comment to the list and notify adapter
                    actualComments.add(finalComment)
                    adapter.notifyItemInserted(actualComments.size - 1)
                    holder.commentRecycler.scrollToPosition(actualComments.size - 1)

                    // C. Update the comment count text on the post
                    holder.postComments.text = "View all ${newCommentCount} comments"

                    // D. Update the Post object in postsList for state consistency
                    val position = postsList.indexOfFirst { it.postId == post.postId }
                    if (position != -1) {
                        // Create a new, immutable list of comments for the updated Post data class
                        val updatedCommentsList = post.comments.toMutableList().apply { add(finalComment) }
                        val updatedPost = post.copy(
                            commentCount = newCommentCount,
                            comments = updatedCommentsList.toList() // Convert back to immutable List
                        )
                        postsList[position] = updatedPost
                    }

                    Toast.makeText(context, "Comment posted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Comment failed: ${jsonResponse.getString("message")}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Comment response error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, { error ->
            Toast.makeText(context, "Comment network error: ${error.message}", Toast.LENGTH_LONG).show()
        }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["post_id"] = post.postId
                params["user_id"] = CURRENT_DB_USER_ID.toString()
                params["text"] = commentText
                return params
            }
        }
        Volley.newRequestQueue(context).add(stringRequest)
    }

    private fun toggleLike(post: Post, holder: PostViewHolder, currentlyLiked: Boolean, position: Int) {
        if (CURRENT_DB_USER_ID == 0) {
            Toast.makeText(context, "Login required to like posts.", Toast.LENGTH_SHORT).show()
            return
        }

        val action = if (currentlyLiked) "unlike" else "like"
        val url = baseUrl + "like_post.php"

        val stringRequest = object : StringRequest(Method.POST, url, { response ->
            try {
                val jsonResponse = JSONObject(response)
                if (jsonResponse.getBoolean("success")) {
                    val newCount = jsonResponse.getInt("new_like_count")
                    val isNowLiked = jsonResponse.getBoolean("is_liked")

                    // FIX: Create a NEW Post object with the updated state
                    val updatedPost = post.copy(
                        likeCount = newCount,
                        isLiked = isNowLiked
                    )

                    // FIX: Update the backing list and notify adapter
                    postsList[position] = updatedPost
                    notifyItemChanged(position)

                    Toast.makeText(context, "Success: $action", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Like failed: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Like response error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            Toast.makeText(context, "Like network error: ${error.message}", Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["post_id"] = post.postId
                params["user_id"] = CURRENT_DB_USER_ID.toString()
                params["action"] = action
                return params
            }
        }
        Volley.newRequestQueue(context).add(stringRequest)
    }
}