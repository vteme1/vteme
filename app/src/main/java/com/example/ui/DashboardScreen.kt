package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookEntity
import com.example.data.ChapterEntity
import com.example.data.BookmarkEntity
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

// Theme colors mapping helper
fun getCoverGradient(colorName: String): Brush {
    return when (colorName) {
        "Ocean Blue" -> Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))
        "Crimson Red" -> Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFFFF5722)))
        "Sunset Gold" -> Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFFFC107)))
        "Forest Green" -> Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))
        "Vibrant Purple" -> Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFF673AB7)))
        "Nordic Slate" -> Brush.linearGradient(listOf(Color(0xFF607D8B), Color(0xFF455A64)))
        "Coral Rose" -> Brush.linearGradient(listOf(Color(0xFFFF7043), Color(0xFFEC407A)))
        else -> Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63))) // Violet fallback
    }
}

val coverColorsList = listOf(
    "Ocean Blue", "Crimson Red", "Sunset Gold", "Forest Green", "Vibrant Purple", "Nordic Slate", "Coral Rose"
)

@Composable
fun LiquidGlassBackground() {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) {
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F1218), Color(0xFF1E2430))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFFF3F6FA), Color(0xFFE0E5EE))
                    )
                }
            )
    ) {
        // Glowing organic liquid blob shapes
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = (-20).dp)
                .size(280.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isDark) listOf(Color(0x3B2196F3), Color(0x002196F3))
                        else listOf(Color(0x2B2196F3), Color(0x002196F3))
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 100.dp, y = (-150).dp)
                .size(340.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isDark) listOf(Color(0x3BFF4081), Color(0x00FF4081))
                        else listOf(Color(0x2BFF4081), Color(0x00FF4081))
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 100.dp)
                .size(300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isDark) listOf(Color(0x3CE040FB), Color(0x00E040FB))
                        else listOf(Color(0x2CE040FB), Color(0x00E040FB))
                    )
                )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val books by viewModel.filteredBooks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBook by viewModel.selectedBook.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()
    val bookmarks by viewModel.currentBookmarks.collectAsState()
    val currentChapterIndex by viewModel.currentChapterIndex.collectAsState()
    val currentReadSentence by viewModel.currentReadSentence.collectAsState()

    // Dialog state controllers
    var showAddBookDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showCoverEditDialog by remember { mutableStateOf(false) }
    var showChapterEditDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }

    // File Import launcher
    val context = LocalContext.current
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importBookFromFileUri(uri, context)
        }
    }

    // Backup states
    val backupJsonString by viewModel.backupJsonString.collectAsState()
    val backupStatus by viewModel.backupOperationStatus.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidGlassBackground()

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedBook == null) "VTeme" else selectedBook!!.title,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    },
                    navigationIcon = {
                        if (selectedBook != null) {
                            IconButton(onClick = { viewModel.deselectBook() }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад")
                            }
                        }
                    },
                    actions = {
                        if (selectedBook != null) {
                            // Options menu when a book is loaded
                            IconButton(onClick = { showCoverEditDialog = true }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Редактировать обложку и инфо")
                            }
                            IconButton(onClick = { showAddChapterDialog = true }) {
                                Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = "Добавить главу")
                            }
                            IconButton(onClick = {
                                if (selectedBook != null) {
                                    viewModel.deleteCurrentBook()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить книгу",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            // Home screen actions
                            IconButton(onClick = { importFileLauncher.launch("*/*") }) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Импортировать книгу")
                            }
                            IconButton(onClick = { showBackupDialog = true }) {
                                Icon(imageVector = Icons.Default.Backup, contentDescription = "Резервная копия")
                            }
                            IconButton(onClick = { showAddBookDialog = true }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить книгу")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedBook == null) {
                    // HOME DASHBOARD: Shelf View
                    LibraryDashboardView(
                        books = books,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onBookClick = { book -> viewModel.selectBook(book) },
                        onAddClick = { showAddBookDialog = true },
                        onBackupClick = { showBackupDialog = true }
                    )
                } else {
                    // ACTIVE READER VIEW (Text Reader or Audiobook Player)
                ActiveBookView(
                    book = selectedBook!!,
                    chapters = chapters,
                    bookmarks = bookmarks,
                    currentChapterIndex = currentChapterIndex,
                    currentReadSentence = currentReadSentence,
                    isPlaying = viewModel.isPlaying.collectAsState().value,
                    audioPosition = viewModel.audioPosition.collectAsState().value,
                    audioDuration = viewModel.audioDuration.collectAsState().value,
                    playbackSpeed = viewModel.playbackSpeed.collectAsState().value,
                    volume = viewModel.volume.collectAsState().value,
                    onChapterSelect = { idx -> viewModel.selectChapter(idx) },
                    onPrevChapter = { viewModel.prevChapter() },
                    onNextChapter = { viewModel.nextChapter() },
                    onSentenceClick = { idx -> viewModel.updateEbookPosition(idx) },
                    onPlayToggle = { viewModel.toggleAudioPlayback() },
                    onSeek = { pos -> viewModel.seekAudioTo(pos) },
                    onSpeedChange = { sp -> viewModel.changePlaybackSpeed(sp) },
                    onVolumeChange = { vol -> viewModel.changeVolume(vol) },
                    onAddBookmark = { note -> viewModel.addBookmark(note) },
                    onDeleteBookmark = { bookmarkId -> viewModel.deleteBookmark(bookmarkId) },
                    onEditChapterContentClick = { showChapterEditDialog = true },
                    isTtsPlaying = viewModel.isTtsPlaying.collectAsState().value,
                    onTtsPlayToggle = { viewModel.toggleTtsPlayback() },
                    availableVoices = viewModel.availableVoices.collectAsState().value,
                    selectedVoiceName = viewModel.selectedVoiceName.collectAsState().value,
                    onVoiceSelected = { name -> viewModel.selectVoice(name) }
                )
            }

            // DIALOGS
            if (showAddBookDialog) {
                AddBookDialog(
                    onDismiss = { showAddBookDialog = false },
                    onConfirm = { title, author, isAudio, color, desc, initialChapterTitle, initialChapterContent ->
                        viewModel.addNewBook(
                            title = title,
                            author = author,
                            isAudio = isAudio,
                            coverColor = color,
                            description = desc,
                            chapters = listOf(initialChapterTitle to initialChapterContent)
                        )
                        showAddBookDialog = false
                    }
                )
            }

            if (showBackupDialog) {
                BackupAndRestoreDialog(
                    backupText = backupJsonString,
                    operationStatus = backupStatus,
                    onBackUpRequest = { viewModel.generateBackup() },
                    onRestore = { json -> viewModel.restoreBackup(json) },
                    onDismiss = {
                        viewModel.clearBackupStatus()
                        showBackupDialog = false
                    }
                )
            }

            if (showCoverEditDialog && selectedBook != null) {
                EditBookMetadataDialog(
                    book = selectedBook!!,
                    onDismiss = { showCoverEditDialog = false },
                    onConfirm = { t, a, d, color, url ->
                        viewModel.updateBookMetadata(t, a, d, color, url)
                        showCoverEditDialog = false
                    }
                )
            }

            if (showAddChapterDialog && selectedBook != null) {
                AddChapterDialog(
                    isAudio = selectedBook!!.isAudio,
                    onDismiss = { showAddChapterDialog = false },
                    onConfirm = { t, contentOrUrl ->
                        viewModel.updateChapterContent(
                            chapterId = 0, // 0 for append / automatic insert
                            newTitle = t,
                            newContent = contentOrUrl
                        )
                        showAddChapterDialog = false
                    }
                )
            }

            if (showChapterEditDialog && selectedBook != null && chapters.isNotEmpty() && currentChapterIndex in chapters.indices) {
                val activeChapter = chapters[currentChapterIndex]
                EditChapterDialog(
                    chapter = activeChapter,
                    isAudio = selectedBook!!.isAudio,
                    onDismiss = { showChapterEditDialog = false },
                    onConfirm = { title, c ->
                        viewModel.updateChapterContent(activeChapter.id, title, c)
                        showChapterEditDialog = false
                    }
                )
            }
        }
    }
}
}

// 1. HOME SCREEN: LIBRARY VIEW
@Composable
fun LibraryDashboardView(
    books: List<BookEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBookClick: (BookEntity) -> Unit,
    onAddClick: () -> Unit,
    onBackupClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Welcome Header Widget (Glass style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0x2E1B2330) else Color(0x6BFFFFFF)
            ),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = if (isDark) listOf(Color(0x21FFFFFF), Color(0x05FFFFFF))
                    else listOf(Color(0x8CFFFFFF), Color(0x21FFFFFF))
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Привет, Книголюб! 👋",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Поиск в нашей уютной библиотеке"
                               else "В вашей уютной библиотеке собрано ${books.size} книг.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                    )
                }

                Card(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF2196F3).copy(alpha = 0.3f) 
                                         else Color(0xFF2196F3).copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(
                        1.dp, 
                        if (isDark) Color(0xFF2196F3).copy(alpha = 0.5f) 
                        else Color(0xFF2196F3).copy(alpha = 0.3f)
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Search bar (Glassmorphism look)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { 
                    Text(
                        text = "Поиск по названию, автору или описанию...", 
                        color = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF64748B),
                        fontSize = 14.sp
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search icon",
                        tint = if (isDark) Color(0xFF2196F3) else Color(0xFF0284C7)
                    ) 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear, 
                                contentDescription = "Clear",
                                tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isDark) Color(0x3D1F2837) else Color(0x8CFFFFFF),
                    unfocusedContainerColor = if (isDark) Color(0x1F1F2837) else Color(0x52FFFFFF),
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = if (isDark) Color(0x26FFFFFF) else Color(0x3B64748B),
                    focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                    unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Shelf list
        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    if (searchQuery.isNotBlank()) {
                        Text("🔍", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ничего не найдено",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Попробуйте изменить поисковый запрос или сбросить фильтр.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { onSearchQueryChange("") }) {
                            Text("Сбросить поиск")
                        }
                    } else {
                        Text("📚", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ваша полка пуста",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Добавьте свою первую электронную книгу или аудиокнигу, нажав на кнопку плюса сверху!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onAddClick) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Создать книгу")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank()) "Результаты Поиска" else "Ваши Книжные Полки",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(books, key = { it.id }) { book ->
                    BookShelfRowCard(book = book, onClick = { onBookClick(book) })
                }
            }
        }
    }
}

@Composable
fun BookShelfRowCard(
    book: BookEntity,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0x1F1E293B) else Color(0x66FFFFFF)
        ),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = if (isDark) listOf(Color(0x1AFFFFFF), Color(0x05FFFFFF))
                else listOf(Color(0x9CFFFFFF), Color(0x21FFFFFF))
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant Cover representation
            Box(
                modifier = Modifier
                    .size(width = 65.dp, height = 95.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getCoverGradient(book.coverColor)),
                contentAlignment = Alignment.Center
            ) {
                if (!book.coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = "Обложка книги",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize().padding(6.dp)
                    ) {
                        Text(
                            text = if (book.isAudio) "🎧" else "📖",
                            fontSize = 18.sp
                        )
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 8.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Format badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = if (book.isAudio) "АУДИО" else "ТЕКСТ",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (book.isAudio) Color(0xFFE8F5E9).copy(alpha = 0.8f) else Color(0xFFE3F2FD).copy(alpha = 0.8f),
                            labelColor = if (book.isAudio) Color(0xFF2E7D32) else Color(0xFF1565C0)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (book.isAudio) Color(0x332E7D32) else Color(0x331565C0)
                        )
                    )

                    // Reading progress
                    val progressText = if (book.isAudio) {
                        "Глава ${book.currentChapterIndex + 1}"
                    } else {
                        "Гл. ${book.currentChapterIndex + 1}, абз. ${book.currentReadSentence + 1}"
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF94A3B8)
            )
        }
    }
}

// 2. ACTIVE VIEW: READER/PLAYER CONTAINER
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveBookView(
    book: BookEntity,
    chapters: List<ChapterEntity>,
    bookmarks: List<BookmarkEntity>,
    currentChapterIndex: Int,
    currentReadSentence: Int,
    // audiobook player state flows
    isPlaying: Boolean,
    audioPosition: Long,
    audioDuration: Long,
    playbackSpeed: Float,
    volume: Float,
    onChapterSelect: (Int) -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSentenceClick: (Int) -> Unit,
    onPlayToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onAddBookmark: (String) -> Unit,
    onDeleteBookmark: (Int) -> Unit,
    onEditChapterContentClick: () -> Unit,
    isTtsPlaying: Boolean = false,
    onTtsPlayToggle: () -> Unit = {},
    availableVoices: List<String> = emptyList(),
    selectedVoiceName: String? = null,
    onVoiceSelected: (String) -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Содержание, 1: Закладки, 2: Описание
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Tab indicator view
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF2196F3)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                selectedContentColor = Color(0xFF2196F3),
                unselectedContentColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    Text(if (book.isAudio) "🎧 Плеер" else "📖 Читалка", fontWeight = FontWeight.Bold)
                }
            }
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                selectedContentColor = Color(0xFF2196F3),
                unselectedContentColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    Text("🔖 Закладки (${bookmarks.size})", fontWeight = FontWeight.Bold)
                }
            }
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                selectedContentColor = Color(0xFF2196F3),
                unselectedContentColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    Text("📋 Главы (${chapters.size})", fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeTab) {
                0 -> {
                    // MAIN READING MODE OR AUDIO PLAYING CONTAINER
                    if (book.isAudio) {
                        AudioBookPlayerPage(
                            book = book,
                            chapters = chapters,
                            currentChapterIdx = currentChapterIndex,
                            isPlaying = isPlaying,
                            position = audioPosition,
                            duration = audioDuration,
                            speed = playbackSpeed,
                            volume = volume,
                            onPlayToggle = onPlayToggle,
                            onSeek = onSeek,
                            onSpeedChange = onSpeedChange,
                            onVolumeChange = onVolumeChange,
                            onNextChapter = onNextChapter,
                            onPrevChapter = onPrevChapter,
                            onAddBookmark = onAddBookmark,
                            onEditChapterClick = onEditChapterContentClick
                        )
                    } else {
                        EbookTextReaderPage(
                            book = book,
                            chapters = chapters,
                            currentChapterIdx = currentChapterIndex,
                            currentReadIndex = currentReadSentence,
                            onPrevChapter = onPrevChapter,
                            onNextChapter = onNextChapter,
                            onSentenceClick = onSentenceClick,
                            onAddBookmark = onAddBookmark,
                            onEditChapterClick = onEditChapterContentClick,
                            isTtsPlaying = isTtsPlaying,
                            onTtsPlayToggle = onTtsPlayToggle,
                            availableVoices = availableVoices,
                            selectedVoiceName = selectedVoiceName,
                            onVoiceSelected = onVoiceSelected
                        )
                    }
                }
                1 -> {
                    // BOOKMARKS MANAGEMENTTAB
                    BookmarksTab(
                        bookmarks = bookmarks,
                        onDelete = onDeleteBookmark,
                        onAdd = onAddBookmark
                    )
                }
                2 -> {
                    // CHAPTER LIST TAB
                    ChaptersTab(
                        chapters = chapters,
                        activeChapterIdx = currentChapterIndex,
                        onChapterClick = onChapterSelect,
                        isAudio = book.isAudio
                    )
                }
            }
        }
    }
}

// A. EBOOK TEXT READER PAGE
@Composable
fun EbookTextReaderPage(
    book: BookEntity,
    chapters: List<ChapterEntity>,
    currentChapterIdx: Int,
    currentReadIndex: Int,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSentenceClick: (Int) -> Unit,
    onAddBookmark: (String) -> Unit,
    onEditChapterClick: () -> Unit,
    isTtsPlaying: Boolean = false,
    onTtsPlayToggle: () -> Unit = {},
    availableVoices: List<String> = emptyList(),
    selectedVoiceName: String? = null,
    onVoiceSelected: (String) -> Unit = {}
) {
    var voiceMenuExpanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    if (chapters.isEmpty() || currentChapterIdx !in chapters.indices) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF2196F3))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Загрузка содержимого глав...",
                    color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF1E293B)
                )
            }
        }
        return
    }

    val chapter = chapters[currentChapterIdx]
    // Split textbook chapters by punctuation/paragraph breaks into list of sentences
    val sentences = remember(chapter.content) {
        chapter.content.split(Regex("(?<=\\.)|(?<=\\?)|(?<=!)|(?<=\\n)")).filter { it.trim().isNotEmpty() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        // Quick control utility
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF4FC3F7) else Color(0xFF0284C7),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = { onEditChapterClick() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0x54FFFFFF) else Color(0x3B64748B)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDark) Color.White else Color(0xFF1E293B)
                ),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Править текст", fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(onClick = {
                onAddBookmark("Заметка на абзаце ${currentReadIndex + 1}")
            }) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd, 
                    contentDescription = "Добавить в закладки",
                    tint = if (isDark) Color(0xFF4FC3F7) else Color(0xFF0284C7)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // TTS (Text-to-speech option Card-style layout - Glassmorphic)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0x281B2330) else Color(0x6BFFFFFF)
            ),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = if (isDark) listOf(Color(0x1AFFFFFF), Color(0x05FFFFFF))
                    else listOf(Color(0x9CFFFFFF), Color(0x21FFFFFF))
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (isTtsPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTtsPlaying) "Озвучивание: предложение ${currentReadIndex + 1}" else "Озвучить текст вслух",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDark) Color.White else Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FilledTonalButton(
                        onClick = onTtsPlayToggle,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isDark) Color(0xFF2196F3).copy(alpha = 0.25f) else Color(0xFF2196F3).copy(alpha = 0.15f),
                            contentColor = if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isTtsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isTtsPlaying) "Остановить" else "Озвучить", fontSize = 12.sp)
                    }
                }

                if (availableVoices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = if (isDark) Color(0x1AFFFFFF) else Color(0x1F000000))
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFB0BEC5) else Color(0xFF546E7A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Голос озвучивания:",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
                            )
                        }

                        Box {
                            Surface(
                                onClick = { voiceMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x33000000))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = selectedVoiceName?.substringAfterLast(".")?.take(18) ?: "Системный",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = voiceMenuExpanded,
                                onDismissRequest = { voiceMenuExpanded = false }
                            ) {
                                availableVoices.forEach { voice ->
                                    val displayVoice = voice.substringAfterLast(".")
                                    DropdownMenuItem(
                                        text = { Text(displayVoice, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            onVoiceSelected(voice)
                                            voiceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content Scrollable (Glassmorphic Window reader box)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0x141E293B) else Color(0x42FFFFFF)
            ),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = if (isDark) listOf(Color(0x14FFFFFF), Color(0x05FFFFFF))
                    else listOf(Color(0x8CFFFFFF), Color(0x21FFFFFF))
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            val scrollState = rememberScrollState()

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    sentences.forEachIndexed { index, sentence ->
                        val isHighlighted = index == currentReadIndex
                        Text(
                            text = sentence.trim() + " ",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isHighlighted) {
                                if (isDark) Color(0xFF4FC3F7) else Color(0xFF0284C7)
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1E293B)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isHighlighted) {
                                        if (isDark) Color(0xFF2196F3).copy(alpha = 0.3f)
                                        else Color(0xFF2196F3).copy(alpha = 0.15f)
                                    } else Color.Transparent
                                )
                                .clickable { onSentenceClick(index) }
                                .padding(vertical = 5.dp, horizontal = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Navigation Row Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPrevChapter,
                enabled = currentChapterIdx > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF1F2937) else Color(0xFFE2E8F0),
                    contentColor = if (isDark) Color.White else Color(0xFF1E293B)
                )
            ) {
                Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Предыдущая")
            }

            Text(
                text = "${currentChapterIdx + 1} / ${chapters.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF1E293B)
            )

            Button(
                onClick = onNextChapter,
                enabled = currentChapterIdx < chapters.size - 1,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF1F2937) else Color(0xFFE2E8F0),
                    contentColor = if (isDark) Color.White else Color(0xFF1E293B)
                )
            ) {
                Text("Следующая")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// B. AUDIOBOOK PLAYER PAGE
@Composable
fun AudioBookPlayerPage(
    book: BookEntity,
    chapters: List<ChapterEntity>,
    currentChapterIdx: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    speed: Float,
    volume: Float,
    onPlayToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
    onAddBookmark: (String) -> Unit,
    onEditChapterClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    if (chapters.isEmpty() || currentChapterIdx !in chapters.indices) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF2196F3))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Загрузка аудио дорожек...",
                    color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF1E293B)
                )
            }
        }
        return
    }

    val activeChapter = chapters[currentChapterIdx]

    // Rotate animation state for Retro Vinyl Disk Cover effect during playing
    val rotationAnimation = rememberInfiniteTransition(label = "Artwork rotation")
    val angle by rotationAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation angle"
    )

    val currentRotation = if (isPlaying) angle else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Glassmorphic Artwork Card Container
        Card(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .size(220.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0x1F1E293B) else Color(0x66FFFFFF)
            ),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = if (isDark) listOf(Color(0x1AFFFFFF), Color(0x05FFFFFF))
                    else listOf(Color(0x9CFFFFFF), Color(0x21FFFFFF))
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl Record Design Artwork representing audiobook chapters
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .rotate(currentRotation)
                        .clip(CircleShape)
                        .background(getCoverGradient(book.coverColor)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!book.coverUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = "Обложка книги",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Groove lines representation
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.85f)
                            .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .border(5.dp, Color.Black.copy(alpha = 0.45f), CircleShape)
                    )

                    // Central core
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        // Glassmorphic Player Console Control Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0x281B2330) else Color(0x6BFFFFFF)
            ),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = if (isDark) listOf(Color(0x1AFFFFFF), Color(0x05FFFFFF))
                    else listOf(Color(0x9CFFFFFF), Color(0x21FFFFFF))
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and description
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = activeChapter.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else Color(0xFF1E293B),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Аудиокнига: ${book.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Time slider progress bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                        onValueChange = { percent -> onSeek((percent * duration).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2196F3),
                            activeTrackColor = Color(0xFF2196F3),
                            inactiveTrackColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimer(position), 
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
                        )
                        Text(
                            text = formatTimer(duration), 
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
                        )
                    }
                }

                // Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevChapter,
                        enabled = currentChapterIdx > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious, 
                            contentDescription = "Предыдущая глава", 
                            tint = if (currentChapterIdx > 0) (if (isDark) Color.White else Color(0xFF1E293B)) else (if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f)),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(onClick = { onSeek(java.lang.Math.max(0L, position - 10000L)) }) {
                        Icon(
                            imageVector = Icons.Default.Replay10, 
                            contentDescription = "Назад на 10с", 
                            tint = if (isDark) Color(0xFF90CAF9) else Color(0xFF0284C7),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onPlayToggle,
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Воспроизведение",
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(onClick = { onSeek(java.lang.Math.min(duration, position + 10000L)) }) {
                        Icon(
                            imageVector = Icons.Default.Forward10, 
                            contentDescription = "Вперед на 10с", 
                            tint = if (isDark) Color(0xFF90CAF9) else Color(0xFF0284C7),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextChapter,
                        enabled = currentChapterIdx < chapters.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext, 
                            contentDescription = "Следующая глава", 
                            tint = if (currentChapterIdx < chapters.size - 1) (if (isDark) Color.White else Color(0xFF1E293B)) else (if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f)),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }

        // Glassmorphic Speed control + Action buttons Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0x1F1E293B) else Color(0x42FFFFFF)
            ),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = if (isDark) listOf(Color(0x14FFFFFF), Color(0x05FFFFFF))
                    else listOf(Color(0x8CFFFFFF), Color(0x21FFFFFF))
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Playback speed selections bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF90CAF9) else Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Скорость:  ", 
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF1E293B)
                    )

                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { velocity ->
                        val isSelected = speed == velocity
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable { onSpeedChange(velocity) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    Color(0xFF2196F3).copy(alpha = if (isDark) 0.3f else 0.15f)
                                } else {
                                    if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000)
                                }
                            ),
                            border = BorderStroke(
                                1.dp, 
                                if (isSelected) Color(0xFF2196F3) 
                                else (if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000))
                            )
                        ) {
                            Text(
                                text = "${velocity}x",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = if (isSelected) Color(0xFF2196F3) else (if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }

                HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x12000000))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onAddBookmark("Аудио-закладка на ${formatTimer(position)}") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.15f),
                            contentColor = Color(0xFF2196F3)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Поставить метку", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = onEditChapterClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569)
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x26000000)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Править MP3 ссылку", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Media duration format helper
fun formatTimer(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

// Custom drop shadow modifier
fun Modifier.shadowEffect(colorName: String): Modifier = this

@Composable
fun MaterialTheme.outlineVariant(): Color {
    return MaterialTheme.colorScheme.outlineVariant
}

// C. BOOKMARKS TAB CONTENT
@Composable
fun BookmarksTab(
    bookmarks: List<BookmarkEntity>,
    onDelete: (Int) -> Unit,
    onAdd: (String) -> Unit
) {
    var showAddQuickNote by remember { mutableStateOf(false) }
    var quickNoteText by remember { mutableStateOf("") }
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ваши закладки", 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color(0xFF1E293B)
            )
            Button(
                onClick = { showAddQuickNote = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = if (isDark) 0.6f else 0.1f),
                    contentColor = if (isDark) Color.White else Color(0xFF1E293B)
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x33000000))
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Добавить", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (showAddQuickNote) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0x281B2330) else Color(0x6BFFFFFF)
                ),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = if (isDark) listOf(Color(0x19FFFFFF), Color(0x05FFFFFF))
                        else listOf(Color(0x8CFFFFFF), Color(0x21FFFFFF))
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Создать новую закладку-заметку", 
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quickNoteText,
                        onValueChange = { quickNoteText = it },
                        placeholder = { Text("Напишите комментарий...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            showAddQuickNote = false
                            quickNoteText = ""
                        }) {
                            Text("Отмена", color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (quickNoteText.isNotEmpty()) {
                                    onAdd(quickNoteText)
                                    showAddQuickNote = false
                                    quickNoteText = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("Создать", color = Color.White)
                        }
                    }
                }
            }
        }

        if (bookmarks.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "У вас нет закладок к этой книге.", 
                    color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(bookmarks) { bookmark ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0x1F1E293B) else Color(0x4DFFFFFF)
                        ),
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                colors = if (isDark) listOf(Color(0x14FFFFFF), Color(0x05FFFFFF))
                                else listOf(Color(0x8CFFFFFF), Color(0x21FFFFFF))
                            )
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Глава ${bookmark.chapterIndex + 1} " +
                                            "(Позиция: ${bookmark.position})",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color(0xFF90CAF9) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = bookmark.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF334155)
                                )
                            }
                            IconButton(onClick = { onDelete(bookmark.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete, 
                                    contentDescription = "Удалить", 
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// D. CHAPTERS LIST TAB
@Composable
fun ChaptersTab(
    chapters: List<ChapterEntity>,
    activeChapterIdx: Int,
    onChapterClick: (Int) -> Unit,
    isAudio: Boolean
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        Text(
            text = "Список глав книги", 
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isDark) Color.White else Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (chapters.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Главы книги еще не созданы.",
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chapters.size) { index ->
                    val isSelected = index == activeChapterIdx
                    val chapter = chapters[index]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapterClick(index) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                Color(0xFF2196F3).copy(alpha = if (isDark) 0.25f else 0.15f)
                            } else {
                                if (isDark) Color(0x141E293B) else Color(0x42FFFFFF)
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(1.dp, Color(0xFF2196F3))
                        } else {
                            BorderStroke(
                                1.dp,
                                Brush.linearGradient(
                                    colors = if (isDark) listOf(Color(0x14FFFFFF), Color(0x05FFFFFF))
                                    else listOf(Color(0x8CFFFFFF), Color(0x21FFFFFF))
                                )
                            )
                        },
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}.",
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.width(28.dp),
                                    color = if (isSelected) Color(0xFF2196F3) else (if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = chapter.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isDark) Color.White else Color(0xFF1E293B),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isAudio) {
                                Text(
                                    text = formatTimer(chapter.durationMs),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. DIALOGS DECLARATORY
@Composable
fun AddBookDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var isAudio by remember { mutableStateOf(false) }
    var colorSelected by remember { mutableStateOf("Ocean Blue") }
    var description by remember { mutableStateOf("") }

    var initialChapterTitle by remember { mutableStateOf("Глава 1") }
    var initialChapterContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isSystemInDarkTheme()) Color(0xEB111827) else Color(0xEBFFFFFF),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Добавить Новую Книгу",
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF1E293B)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название Книги") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Автор") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание / Аннотация") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Select Color cover
                Text("Дизайн Обложки (Цветная палитра):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    coverColorsList.forEach { col ->
                        val isSelected = colorSelected == col
                        Card(
                            modifier = Modifier
                                .clickable { colorSelected = col },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Text(text = col, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                        }
                    }
                }

                // E-book or Audiobook type toggle switch row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Это Аудиокнига (MP3)?", fontWeight = FontWeight.Bold)
                    Switch(checked = isAudio, onCheckedChange = { isAudio = it })
                }

                // Initial Chapter Content
                Text("Начальная Глава:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = initialChapterTitle,
                    onValueChange = { initialChapterTitle = it },
                    label = { Text("Название главы") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = initialChapterContent,
                    onValueChange = { initialChapterContent = it },
                    label = { Text(if (isAudio) "URL mp3 файла (пример: https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3)" else "Текст главы") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && author.isNotEmpty() && initialChapterContent.isNotEmpty()) {
                        onConfirm(title, author, isAudio, colorSelected, description, initialChapterTitle, initialChapterContent)
                    }
                },
                enabled = title.isNotEmpty() && author.isNotEmpty() && initialChapterContent.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                )
            ) {
                Text("Создать книгу", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                )
            ) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun EditBookMetadataDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }
    var description by remember { mutableStateOf(book.description) }
    var colorSelected by remember { mutableStateOf(book.coverColor) }
    var coverUrl by remember { mutableStateOf(book.coverUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isSystemInDarkTheme()) Color(0xEB111827) else Color(0xEBFFFFFF),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Редактировать Обложку",
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF1E293B)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Автор") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = coverUrl,
                    onValueChange = { coverUrl = it },
                    label = { Text("URL изображение обложки (Опционально)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Палитра темы обложки:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.8f) else Color(0xFF1E293B)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    coverColorsList.forEach { col ->
                        val isSelected = colorSelected == col
                        Card(
                            modifier = Modifier
                                .clickable { colorSelected = col },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    Color(0xFF2196F3).copy(alpha = if (isSystemInDarkTheme()) 0.25f else 0.15f)
                                } else {
                                    if (isSystemInDarkTheme()) Color(0x1F293754) else Color(0x0D000000)
                                }
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF2196F3) else Color.Transparent)
                        ) {
                            Text(
                                text = col,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF2196F3) else (if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.7f) else Color(0xFF475569))
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(title, author, description, colorSelected, if (coverUrl.isEmpty()) null else coverUrl)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                )
            ) {
                Text("Сохранить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                )
            ) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun AddChapterDialog(
    isAudio: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var contentOrUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isSystemInDarkTheme()) Color(0xEB111827) else Color(0xEBFFFFFF),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Добавить Главу Книги",
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF1E293B)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название Главы") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contentOrUrl,
                    onValueChange = { contentOrUrl = it },
                    label = { Text(if (isAudio) "URL mp3 файла или путь к аудиофайлу" else "Текстовое содержимое главы") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && contentOrUrl.isNotEmpty()) {
                        onConfirm(title, contentOrUrl)
                    }
                },
                enabled = title.isNotEmpty() && contentOrUrl.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                )
            ) {
                Text("Добавить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                )
            ) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun EditChapterDialog(
    chapter: ChapterEntity,
    isAudio: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(chapter.title) }
    var contentOrUrl by remember { mutableStateOf(chapter.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isSystemInDarkTheme()) Color(0xEB111827) else Color(0xEBFFFFFF),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Редактировать Содержимое Главы",
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF1E293B)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contentOrUrl,
                    onValueChange = { contentOrUrl = it },
                    label = { Text(if (isAudio) "URL к MP3 аудиофайлу" else "Текстовое содержимое") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, contentOrUrl) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                )
            ) {
                Text("Обновить главу", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                )
            ) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun BackupAndRestoreDialog(
    backupText: String,
    operationStatus: String,
    onBackUpRequest: () -> Unit,
    onRestore: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var importText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isSystemInDarkTheme()) Color(0xEB111827) else Color(0xEBFFFFFF),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Резервное копирование и Импорт",
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF1E293B)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Вы можете сгенерировать JSON бэкап вашей библиотеки, скопировать его и сохранить в безопасное место. Для восстановления вставьте ранее сохраненный бэкап в текстовое поле ниже.",
                    fontSize = 13.sp,
                    color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                )

                // Back up generator section
                Button(
                    onClick = onBackUpRequest,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сгенерировать Копию Библиотеки", fontWeight = FontWeight.Bold)
                }

                if (backupText.isNotEmpty()) {
                    OutlinedTextField(
                        value = backupText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("JSON Резервный Код библиотеки") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )

                    Button(
                        onClick = { clipboardManager.setText(AnnotatedString(backupText)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = if (isSystemInDarkTheme()) 0.6f else 0.1f),
                            contentColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF1E293B)
                        ),
                        border = BorderStroke(1.dp, if (isSystemInDarkTheme()) Color(0x33FFFFFF) else Color(0x33000000)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Скопировать код в буфер", fontWeight = FontWeight.Bold)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp), color = if (isSystemInDarkTheme()) Color(0x14FFFFFF) else Color(0x12000000))

                // Restore importing section
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("Вставьте JSON код бэкапа для восстановления") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                )

                Button(
                    onClick = {
                        if (importText.isNotEmpty()) {
                            onRestore(importText)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = importText.isNotEmpty()
                ) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Восстановить библиотеку", fontWeight = FontWeight.Bold)
                }

                if (operationStatus.isNotEmpty()) {
                    Text(
                        text = operationStatus,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                )
            ) {
                Text("Закрыть", fontWeight = FontWeight.Bold)
            }
        }
    )
}
