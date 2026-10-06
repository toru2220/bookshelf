package com.toru2220.bookshelf

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ShelfException(val code: Int, message: String) : Exception(message)

class ShelfApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    fun list(baseUrl: String, folder: String, page: Int): LibraryPage {
        val body = get(libraryUrl(baseUrl, folder, page))
        val json = JSONObject(body)
        val arr = json.optJSONArray("books")
        val books = buildList {
            if (arr == null) return@buildList
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                add(
                    BookEntry(
                        type = item.optString("type"),
                        path = item.optString("path"),
                        cover = item.optString("cover"),
                        title = item.optString("title"),
                        currentPosition = item.optString("currentPosition"),
                        progress = item.optDouble("progress", 0.0),
                    ),
                )
            }
        }
        return LibraryPage(books = books, hasMore = json.optBoolean("hasMore", false))
    }

    fun pageCount(baseUrl: String, bookPath: String): Int {
        val url = baseUrl.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegments("book/cbz/pages")
            .addQueryParameter("path", bookPath)
            .build()
        val json = JSONObject(get(url.toString()))
        return json.optInt("pages", 0)
    }

    fun pageUrl(baseUrl: String, bookPath: String, page: Int): String {
        return baseUrl.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegments("book/cbz")
            .addQueryParameter("path", bookPath)
            .addQueryParameter("page", page.toString())
            .build()
            .toString()
    }

    fun coverUrl(baseUrl: String, cover: String): String? {
        if (cover.isBlank()) return null
        val builder = baseUrl.trimEnd('/').toHttpUrl().newBuilder().addPathSegment("cover")
        cover.trim('/').split('/').filter { it.isNotEmpty() }.forEach { segment ->
            builder.addPathSegment(segment)
        }
        return builder.build().toString()
    }

    fun reportAccess(baseUrl: String, bookPath: String) {
        runCatching {
            val url = baseUrl.trimEnd('/').toHttpUrl().newBuilder()
                .addPathSegments("api/access")
                .addQueryParameter("path", bookPath)
                .build()
            get(url.toString())
        }
    }

    fun reportProgress(baseUrl: String, bookPath: String, position: String, progress: Double) {
        runCatching {
            val url = baseUrl.trimEnd('/').toHttpUrl().newBuilder()
                .addPathSegments("api/progress")
                .addQueryParameter("path", bookPath)
                .addQueryParameter("position", position)
                .addQueryParameter("progress", progress.toString())
                .build()
            get(url.toString())
        }
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ShelfException(response.code, "HTTP ${response.code}")
            }
            return text
        }
    }

    companion object {
        fun libraryUrl(baseUrl: String, folder: String, page: Int): String {
            val root = baseUrl.trimEnd('/')
            val encodedFolder = folder.trim('/').split('/')
                .filter { it.isNotEmpty() }
                .joinToString("/") { segment ->
                    URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
                }
            val path = if (encodedFolder.isEmpty()) "/api/root/" else "/api/root/$encodedFolder"
            return "$root$path?sort=title&order=asc&page=$page"
        }
    }
}
