package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@JsonClass(generateAdapter = true)
data class BookBackupModel(
    val id: Int,
    val title: String,
    val author: String,
    val isAudio: Boolean,
    val currentChapterIndex: Int,
    val currentPositionMs: Long,
    val currentReadSentence: Int,
    val coverColor: String,
    val coverUrl: String?,
    val description: String,
    val addedAt: Long
)

@JsonClass(generateAdapter = true)
data class ChapterBackupModel(
    val id: Int,
    val bookId: Int,
    val title: String,
    val content: String,
    val durationMs: Long,
    val sortOrder: Int
)

@JsonClass(generateAdapter = true)
data class BookmarkBackupModel(
    val id: Int,
    val bookId: Int,
    val chapterIndex: Int,
    val position: Long,
    val note: String,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class LibraryBackup(
    val books: List<BookBackupModel>,
    val chapters: List<ChapterBackupModel>,
    val bookmarks: List<BookmarkBackupModel>,
    val backupDate: Long = System.currentTimeMillis()
)

class BookRepository(private val bookDao: BookDao) {

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBookWithChapters(bookId: Int): Flow<BookWithChapters?> {
        return bookDao.getBookWithChapters(bookId)
    }

    fun getBookmarksForBook(bookId: Int): Flow<List<BookmarkEntity>> {
        return bookDao.getBookmarksForBook(bookId)
    }

    suspend fun insertBook(book: BookEntity): Long {
        return bookDao.insertBook(book)
    }

    suspend fun insertChapters(chapters: List<ChapterEntity>) {
        bookDao.insertChapters(chapters)
    }

    suspend fun deleteChaptersForBook(bookId: Int) {
        bookDao.deleteChaptersForBook(bookId)
    }

    suspend fun deleteBook(id: Int) {
        bookDao.deleteBook(id)
        bookDao.deleteChaptersForBook(id)
    }

    suspend fun insertBookmark(bookmark: BookmarkEntity) {
        bookDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(id: Int) {
        bookDao.deleteBookmark(id)
    }

    suspend fun updateBookProgress(bookId: Int, chapterIndex: Int, positionMs: Long, sentenceIndex: Int) {
        bookDao.updateBookProgress(bookId, chapterIndex, positionMs, sentenceIndex)
    }

    // JSON Backup generation
    suspend fun exportLibraryToJson(): String {
        val books = bookDao.getAllBooksList().map {
            BookBackupModel(
                id = it.id,
                title = it.title,
                author = it.author,
                isAudio = it.isAudio,
                currentChapterIndex = it.currentChapterIndex,
                currentPositionMs = it.currentPositionMs,
                currentReadSentence = it.currentReadSentence,
                coverColor = it.coverColor,
                coverUrl = it.coverUrl,
                description = it.description,
                addedAt = it.addedAt
            )
        }

        val chapters = bookDao.getAllChaptersList().map {
            ChapterBackupModel(
                id = it.id,
                bookId = it.bookId,
                title = it.title,
                content = it.content,
                durationMs = it.durationMs,
                sortOrder = it.sortOrder
            )
        }

        val bookmarks = bookDao.getAllBookmarksList().map {
            BookmarkBackupModel(
                id = it.id,
                bookId = it.bookId,
                chapterIndex = it.chapterIndex,
                position = it.position,
                note = it.note,
                createdAt = it.createdAt
            )
        }

        val backup = LibraryBackup(books, chapters, bookmarks)
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(LibraryBackup::class.java)
        return adapter.toJson(backup)
    }

    // JSON Backup import / restore
    suspend fun importLibraryFromJson(json: String): Boolean {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(LibraryBackup::class.java)
            val backup = adapter.fromJson(json) ?: return false

            // Clean database before restore
            bookDao.clearAllBooks()
            bookDao.clearAllChapters()
            bookDao.clearAllBookmarks()

            // Restore elements
            backup.books.forEach {
                bookDao.insertBook(
                    BookEntity(
                        id = it.id,
                        title = it.title,
                        author = it.author,
                        isAudio = it.isAudio,
                        currentChapterIndex = it.currentChapterIndex,
                        currentPositionMs = it.currentPositionMs,
                        currentReadSentence = it.currentReadSentence,
                        coverColor = it.coverColor,
                        coverUrl = it.coverUrl,
                        description = it.description,
                        addedAt = it.addedAt
                    )
                )
            }

            val chaptersList = backup.chapters.map {
                ChapterEntity(
                    id = it.id,
                    bookId = it.bookId,
                    title = it.title,
                    content = it.content,
                    durationMs = it.durationMs,
                    sortOrder = it.sortOrder
                )
            }
            bookDao.insertChapters(chaptersList)

            backup.bookmarks.forEach {
                bookDao.insertBookmark(
                    BookmarkEntity(
                        id = it.id,
                        bookId = it.bookId,
                        chapterIndex = it.chapterIndex,
                        position = it.position,
                        note = it.note,
                        createdAt = it.createdAt
                    )
                )
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
