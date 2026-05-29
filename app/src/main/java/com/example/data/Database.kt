package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val isAudio: Boolean,
    val currentChapterIndex: Int = 0,
    val currentPositionMs: Long = 0L,
    val currentReadSentence: Int = 0,
    val coverColor: String = "Violet", // Name of theme color
    val coverUrl: String? = null,
    val description: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val title: String,
    val content: String, // Text content for ebook, or file path/URL/resource for audiobook
    val durationMs: Long = 180000L, // For mp3 audiobook segment
    val sortOrder: Int = 0
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val chapterIndex: Int,
    val position: Long, // Char index (sentence index) for ebook, or ms playing for audio
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class BookWithChapters(
    @Embedded val book: BookEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "bookId"
    )
    val chapters: List<ChapterEntity>
)

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY addedAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Transaction
    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookWithChapters(bookId: Int): Flow<BookWithChapters?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersForBook(bookId: Int)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: Int)

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarksForBook(bookId: Int): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Int)

    @Query("UPDATE books SET currentChapterIndex = :chapterIndex, currentPositionMs = :positionMs, currentReadSentence = :sentence WHERE id = :bookId")
    suspend fun updateBookProgress(bookId: Int, chapterIndex: Int, positionMs: Long, sentence: Int)

    // Backup & Restore utilities
    @Query("DELETE FROM books")
    suspend fun clearAllBooks()

    @Query("DELETE FROM chapters")
    suspend fun clearAllChapters()

    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks()

    @Query("SELECT * FROM books")
    suspend fun getAllBooksList(): List<BookEntity>

    @Query("SELECT * FROM chapters")
    suspend fun getAllChaptersList(): List<ChapterEntity>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksList(): List<BookmarkEntity>
}

@Database(entities = [BookEntity::class, ChapterEntity::class, BookmarkEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
