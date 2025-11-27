// com.example.assignment1.UI.adapters.NotificationAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.assignment1.DataClass.Notification
import com.example.assignment1.R
import com.example.assignment1.Utils.BaseUrlUtil
import org.json.JSONObject
import java.util.HashMap

class NotificationAdapter(
    private val context: Context,
    private val notifList: ArrayList<Notification>,
    private val currentUserId: String // Pass current user ID from activity
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val baseUrl: String = BaseUrlUtil.getBaseUrl(context)

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.notification_icon)
        val title: TextView = itemView.findViewById(R.id.notification_title)
        val message: TextView = itemView.findViewById(R.id.notification_message)
        val container: LinearLayout = itemView.findViewById(R.id.action_buttons_container)
        val btnAccept: ImageButton = itemView.findViewById(R.id.btn_accept)
        val btnReject: ImageButton = itemView.findViewById(R.id.btn_reject)
        val btnDeleteGeneral: ImageButton = itemView.findViewById(R.id.btn_delete_general)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notif = notifList[position]

        // Load profile image
        val finalImageUrl = baseUrl + notif.sender_dp_path.substringAfter("uploads/") // Re-use logic from profile
        Glide.with(context).load(finalImageUrl).placeholder(R.drawable.default_icon).into(holder.icon)

        // Set Title and Message based on type
        holder.title.text = notif.sender_username

        when (notif.type) {
            "follow_request" -> {
                holder.message.text = "wants to follow you."
                holder.container.visibility = View.VISIBLE
                holder.btnDeleteGeneral.visibility = View.GONE
            }
            "like" -> {
                holder.message.text = "liked your post."
                holder.container.visibility = View.GONE
                holder.btnDeleteGeneral.visibility = View.VISIBLE
            }
            "comment" -> {
                holder.message.text = "commented on your post."
                holder.container.visibility = View.GONE
                holder.btnDeleteGeneral.visibility = View.VISIBLE
            }
            "message" -> {
                holder.message.text = "sent you a message."
                holder.container.visibility = View.GONE
                holder.btnDeleteGeneral.visibility = View.VISIBLE
            }
            else -> {
                holder.message.text = "New activity."
                holder.container.visibility = View.GONE
                holder.btnDeleteGeneral.visibility = View.VISIBLE
            }
        }

        // Follow Request Handlers
        holder.btnAccept.setOnClickListener { handleFollowAction("accept", notif) }
        holder.btnReject.setOnClickListener { handleFollowAction("reject", notif) }

        // General Delete Handler (for like, comment, message)
        holder.btnDeleteGeneral.setOnClickListener { deleteGeneralNotification(notif) }
    }

    override fun getItemCount(): Int = notifList.size

    // --- API Handlers ---

    private fun handleFollowAction(action: String, notif: Notification) {
        val url = baseUrl + "handle_follow_request.php"

        val stringRequest = object : StringRequest(Request.Method.POST, url, { response ->
            try {
                val jsonResponse = JSONObject(response)
                if (jsonResponse.getBoolean("success")) {
                    Toast.makeText(context, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                    removeItem(notif) // Remove item from list on success
                } else {
                    Toast.makeText(context, "Action failed: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "API response error.", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            Toast.makeText(context, "Network error during follow action.", Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["action"] = action
                params["receiver_id"] = currentUserId
                params["sender_id"] = notif.sender_id.toString()
                params["notif_id"] = notif.id.toString()
                return params
            }
        }
        Volley.newRequestQueue(context).add(stringRequest)
    }

    private fun deleteGeneralNotification(notif: Notification) {
        val url = baseUrl + "delete_notification.php"

        val stringRequest = object : StringRequest(Request.Method.POST, url, { response ->
            try {
                val jsonResponse = JSONObject(response)
                if (jsonResponse.getBoolean("success")) {
                    Toast.makeText(context, "Notification deleted.", Toast.LENGTH_SHORT).show()
                    removeItem(notif) // Remove item from list on success
                } else {
                    Toast.makeText(context, "Deletion failed: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "API response error.", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            Toast.makeText(context, "Network error during deletion.", Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["notif_id"] = notif.id.toString()
                params["receiver_id"] = currentUserId
                return params
            }
        }
        Volley.newRequestQueue(context).add(stringRequest)
    }

    private fun removeItem(notif: Notification) {
        val index = notifList.indexOf(notif)
        if (index != -1) {
            notifList.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}