package com.example.assignment1.Utils // Place this in your preferred utility package

import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.HttpHeaderParser
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException

/**
 * Custom Volley request for multipart uploads, specifically for files and text data.
 * This is essential for sending images/files to a PHP server.
 */
abstract class VolleyMultipartRequest(
    method: Int,
    url: String,
    listener: Response.Listener<NetworkResponse>,
    errorListener: Response.ErrorListener
) : Request<NetworkResponse>(method, url, errorListener) {

    private val twoHyphens = "--"
    private val lineEnd = "\r\n"
    private val boundary: String = "apic" + System.currentTimeMillis() // Unique boundary string
    private val mListener: Response.Listener<NetworkResponse> = listener

    /**
     * DataPart is a class that holds the name of the file, the byte data, and the MIME type.
     */
    class DataPart(val fileName: String, val content: ByteArray, val type: String = "image/jpeg")

    // Abstract methods to be implemented by the caller
    protected abstract fun getByteData(): Map<String, DataPart>
    override fun getParams(): Map<String, String>? = null // Params handled separately in getByteData

    override fun getHeaders(): Map<String, String> {
        return HashMap()
    }

    override fun getBodyContentType(): String {
        return "multipart/form-data;boundary=$boundary"
    }

    @Throws(AuthFailureError::class)
    override fun getBody(): ByteArray? {
        val bos = ByteArrayOutputStream()
        try {
            // Write text parameters (if any)
            val params = getParams()
            if (params != null && params.isNotEmpty()) {
                params.forEach { (key, value) ->
                    buildTextPart(bos, key, value)
                }
            }

            // Write file data
            val data = getByteData()
            if (data.isNotEmpty()) {
                data.forEach { (key, dataPart) ->
                    buildDataPart(bos, dataPart, key)
                }
            }

            // End boundary
            bos.write((twoHyphens + boundary + twoHyphens + lineEnd).toByteArray())
            return bos.toByteArray()
        } catch (e: IOException) {
            VolleyLog.e("VolleyMultipartRequest", "IOException writing to ByteArrayOutputStream")
        }
        return null
    }

    @Throws(UnsupportedEncodingException::class)
    private fun buildTextPart(outputStream: ByteArrayOutputStream, parameterName: String, parameterValue: String) {
        outputStream.write((twoHyphens + boundary + lineEnd).toByteArray())
        outputStream.write(("Content-Disposition: form-data; name=\"$parameterName\"$lineEnd").toByteArray())
        outputStream.write(("Content-Type: text/plain; charset=UTF-8$lineEnd").toByteArray())
        outputStream.write(lineEnd.toByteArray())
        outputStream.write(parameterValue.toByteArray(charset("UTF-8")))
        outputStream.write(lineEnd.toByteArray())
    }

    private fun buildDataPart(outputStream: ByteArrayOutputStream, dataPart: DataPart, inputName: String) {
        outputStream.write((twoHyphens + boundary + lineEnd).toByteArray())
        outputStream.write("Content-Disposition: form-data; name=\"$inputName\"; filename=\"${dataPart.fileName}\"$lineEnd".toByteArray())
        outputStream.write("Content-Type: ${dataPart.type}$lineEnd".toByteArray())
        outputStream.write(lineEnd.toByteArray())

        outputStream.write(dataPart.content)

        outputStream.write(lineEnd.toByteArray())
    }

    // Volley boilerplate
    override fun parseNetworkResponse(response: NetworkResponse): Response<NetworkResponse> {
        return try {
            Response.success(
                response,
                HttpHeaderParser.parseCacheHeaders(response)
            )
        } catch (e: Exception) {
            Response.error(com.android.volley.ParseError(e))
        }
    }

    override fun deliverResponse(response: NetworkResponse) {
        mListener.onResponse(response)
    }
}