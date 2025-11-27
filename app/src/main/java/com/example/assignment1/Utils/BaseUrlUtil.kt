package com.example.assignment1.Utils

import android.content.Context
import com.example.assignment1.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object BaseUrlUtil {
    @Throws(IOException::class)
    fun getBaseUrl(context: Context): String {
        val input: InputStream = context.resources.openRawResource(R.raw.base_url)
        val reader = BufferedReader(InputStreamReader(input))
        val builder = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            builder.append(line)
        }

        return builder.toString().trim()
    }
}