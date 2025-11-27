package com.example.assignment1.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.R
import com.example.assignment1.DataClass.User

class SearchAdapter(
    private var userList: List<User>,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    // ViewHolder class holds the item views
    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarText: TextView = itemView.findViewById(R.id.avatarText)
        val usernameText: TextView = itemView.findViewById(R.id.postUsername)
        val areaText: TextView = itemView.findViewById(R.id.post_area)
        val mainLayout: LinearLayout = itemView.findViewById(R.id.main)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search, parent, false) // your xml file
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val user = userList[position]

        // ✅ Set the username
        holder.usernameText.text = user.username

        // ✅ Set the area (optional — you can customize what goes here)
        holder.areaText.text = "${user.firstName} ${user.lastName}"

        // ✅ Set avatar initials (first letters of name)
        val initials = user.username.take(2).uppercase()
        holder.avatarText.text = initials

        // ✅ Handle click on the full item
        holder.mainLayout.setOnClickListener {
            onItemClick(user)
        }
    }

    override fun getItemCount(): Int = userList.size

    // ✅ To update the data dynamically (after search filtering)
    fun updateList(newList: List<User>) {
        userList = newList
        notifyDataSetChanged()
    }
}
