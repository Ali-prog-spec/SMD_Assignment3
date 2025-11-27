package com.example.assignment1.Utils

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.widget.Toast

/**
 * Extension function for Activity to display a Toast notification positioned at the top.
 * This function provides a central way to display standardized notifications across the app.
 *
 * @param message The text to display in the Toast.
 * @param duration The length of the Toast display (Toast.LENGTH_SHORT or Toast.LENGTH_LONG).
 */
fun Activity.showTopToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    val toast = Toast.makeText(this, message, duration)
    // Set the gravity to position the toast at the top center.
    // The offset moves it down 100 density-independent pixels (dp) from the top edge.
    val yOffset = (100 * resources.displayMetrics.density).toInt()
    toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, yOffset)
    toast.show()
}

// Optional: A Context version if you need to use it in non-Activity classes (like Services or Adapters)
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}