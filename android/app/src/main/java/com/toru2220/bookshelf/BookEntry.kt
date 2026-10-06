package com.toru2220.bookshelf

data class BookEntry(
    val type: String,
    val path: String,
    val cover: String,
    val title: String,
    val currentPosition: String,
    val progress: Double,
) {
    val isFolder: Boolean get() = type == "Folder"

    val openable: Boolean
        get() {
            val ext = path.substringAfterLast('.', "").lowercase()
            return ext == "zip" || ext == "cbz"
        }
}

data class LibraryPage(
    val books: List<BookEntry>,
    val hasMore: Boolean,
)
