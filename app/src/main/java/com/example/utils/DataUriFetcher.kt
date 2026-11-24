package com.example.utils

import android.net.Uri
import android.util.Base64
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer

class DataUriFetcher(
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val raw = data.toString()
        val commaIndex = raw.indexOf(',')
        require(commaIndex > 5) { "Invalid data URI" }

        val header = raw.substring(5, commaIndex)
        val payload = raw.substring(commaIndex + 1)
        val mimeType = header.substringBefore(';').ifBlank { "image/jpeg" }
        val isBase64 = header.split(';').any { it.equals("base64", ignoreCase = true) }

        val bytes = if (isBase64) {
            Base64.decode(payload, Base64.DEFAULT)
        } else {
            Uri.decode(payload).toByteArray(Charsets.UTF_8)
        }

        val buffer = Buffer().write(bytes)
        return SourceResult(
            source = ImageSource(source = buffer, context = options.context),
            mimeType = mimeType,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (data.scheme.equals("data", ignoreCase = true)) {
                DataUriFetcher(data, options)
            } else {
                null
            }
        }
    }
}
