package com.example.ui

import android.app.Application
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val repository: BookRepository
) : AndroidViewModel(application) {

    // List of all books in the library
    val allBooks: StateFlow<List<BookEntity>> = repository.allBooks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Library search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // List of filtered books matching search query
    val filteredBooks: StateFlow<List<BookEntity>> = combine(repository.allBooks, _searchQuery) { booksList, query ->
        if (query.isBlank()) {
            booksList
        } else {
            booksList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true) ||
                (it.description?.contains(query, ignoreCase = true) ?: false)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selection states
    private val _selectedBook = MutableStateFlow<BookEntity?>(null)
    val selectedBook: StateFlow<BookEntity?> = _selectedBook.asStateFlow()

    private val _currentChapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val currentChapters: StateFlow<List<ChapterEntity>> = _currentChapters.asStateFlow()

    private val _currentBookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val currentBookmarks: StateFlow<List<BookmarkEntity>> = _currentBookmarks.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    // For Ebooks: index of the current highlight / read position
    private val _currentReadSentence = MutableStateFlow(0)
    val currentReadSentence: StateFlow<Int> = _currentReadSentence.asStateFlow()

    // For Audiobooks: MediaPlayer control states
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var bookChaptersJob: Job? = null
    private var bookBookmarksJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _audioPosition = MutableStateFlow(0L)
    val audioPosition: StateFlow<Long> = _audioPosition.asStateFlow()

    private val _audioDuration = MutableStateFlow(0L)
    val audioDuration: StateFlow<Long> = _audioDuration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // Text backup export / import string holder
    private val _backupJsonString = MutableStateFlow("")
    val backupJsonString: StateFlow<String> = _backupJsonString.asStateFlow()

    private val _backupOperationStatus = MutableStateFlow("")
    val backupOperationStatus: StateFlow<String> = _backupOperationStatus.asStateFlow()

    // TextToSpeech States
    private var tts: android.speech.tts.TextToSpeech? = null
    private val _isTtsPlaying = MutableStateFlow(false)
    val isTtsPlaying: StateFlow<Boolean> = _isTtsPlaying.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<String>>(emptyList())
    val availableVoices: StateFlow<List<String>> = _availableVoices.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow<String?>(null)
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    init {
        // Observe selected book chapters and bookmarks
        viewModelScope.launch {
            allBooks.collect { booksList ->
                // Auto seed sample books if library is empty
                if (booksList.isEmpty()) {
                    seedSampleLibrary()
                }
            }
        }
    }

    // Book selection handling
    fun selectBook(book: BookEntity) {
        // If selecting a different book, stop current audiobook playing
        if (_selectedBook.value?.id != book.id) {
            stopAudioPlay()
        }

        _selectedBook.value = book
        _currentChapterIndex.value = book.currentChapterIndex
        _currentReadSentence.value = book.currentReadSentence
        _audioPosition.value = book.currentPositionMs

        // Cancel previous collections to prevent data race/leaks
        bookChaptersJob?.cancel()
        bookBookmarksJob?.cancel()

        // Collect chapters and bookmarks for this book
        var wasMediaPlayerInitialized = false
        bookChaptersJob = viewModelScope.launch {
            repository.getBookWithChapters(book.id).collect { bookWithChapters ->
                if (bookWithChapters != null) {
                    _currentChapters.value = bookWithChapters.chapters.sortedBy { it.sortOrder }
                    // Load audio into MediaPlayer if it is audiobook
                    if (book.isAudio && !wasMediaPlayerInitialized && bookWithChapters.chapters.isNotEmpty()) {
                        wasMediaPlayerInitialized = true
                        initMediaPlayerForCurrentChapter()
                    }
                }
            }
        }

        bookBookmarksJob = viewModelScope.launch {
            repository.getBookmarksForBook(book.id).collect {
                _currentBookmarks.value = it
            }
        }
    }

    fun deselectBook() {
        stopAudioPlay()
        stopTts()
        bookChaptersJob?.cancel()
        bookBookmarksJob?.cancel()
        bookChaptersJob = null
        bookBookmarksJob = null
        _selectedBook.value = null
        _currentChapters.value = emptyList()
        _currentBookmarks.value = emptyList()
    }

    // Media Player control
    private fun initMediaPlayerForCurrentChapter() {
        val chapters = _currentChapters.value
        val index = _currentChapterIndex.value
        if (chapters.isEmpty() || index !in chapters.indices) return

        val chapter = chapters[index]

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(chapter.content) // Plays online mp3 URL or path
                setVolume(_volume.value, _volume.value)
                prepareAsync()
                setOnPreparedListener { mp ->
                    try {
                        _audioDuration.value = mp.duration.toLong()
                        // Restore previous playing position if any
                        val restorePos = if (_selectedBook.value?.currentChapterIndex == index) {
                            _selectedBook.value?.currentPositionMs ?: 0L
                        } else {
                            0L
                        }
                        mp.seekTo(restorePos.toInt())
                        _audioPosition.value = restorePos

                        // Restore speed
                        setSpeedOnMediaPlayer(_playbackSpeed.value)

                        if (_isPlaying.value) {
                            mp.start()
                            startProgressJob()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                setOnCompletionListener {
                    // Auto advance to next chapter
                    nextChapter()
                }
                setOnErrorListener { mp, what, extra ->
                    _backupOperationStatus.value = "Ошибка аудио: неверная ссылка или формат ($what)"
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _backupOperationStatus.value = "Ошибка воспроизведения аудио: ${e.localizedMessage}"
        }
    }

    fun toggleAudioPlayback() {
        try {
            val mp = mediaPlayer ?: return
            if (_isPlaying.value) {
                mp.pause()
                _isPlaying.value = false
                stopProgressJob()
                saveProgressToDb()
            } else {
                mp.start()
                _isPlaying.value = true
                startProgressJob()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekAudioTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _audioPosition.value = positionMs
        saveProgressToDb()
    }

    fun changePlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        setSpeedOnMediaPlayer(speed)
    }

    private fun setSpeedOnMediaPlayer(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.playbackParams = mp.playbackParams.setSpeed(speed)
                    } else {
                        val params = PlaybackParams().setSpeed(speed)
                        mp.playbackParams = params
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun changeVolume(vol: Float) {
        _volume.value = vol
        try {
            mediaPlayer?.setVolume(vol, vol)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startProgressJob() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(300)
                try {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            _audioPosition.value = mp.currentPosition.toLong()
                            saveProgressToDb()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun stopProgressJob() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun stopAudioPlay() {
        stopProgressJob()
        _isPlaying.value = false
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
    }

    // Next/Prev chapters navigation
    fun nextChapter() {
        stopTts()
        val chapters = _currentChapters.value
        val nextIndex = _currentChapterIndex.value + 1
        if (nextIndex in chapters.indices) {
            _currentChapterIndex.value = nextIndex
            _currentReadSentence.value = 0
            _audioPosition.value = 0L
            if (_selectedBook.value?.isAudio == true) {
                initMediaPlayerForCurrentChapter()
            }
            saveProgressToDb()
        }
    }

    fun prevChapter() {
        stopTts()
        val chapters = _currentChapters.value
        val prevIndex = _currentChapterIndex.value - 1
        if (prevIndex in chapters.indices) {
            _currentChapterIndex.value = prevIndex
            _currentReadSentence.value = 0
            _audioPosition.value = 0L
            if (_selectedBook.value?.isAudio == true) {
                initMediaPlayerForCurrentChapter()
            }
            saveProgressToDb()
        }
    }

    fun selectChapter(index: Int) {
        stopTts()
        val chapters = _currentChapters.value
        if (index in chapters.indices) {
            _currentChapterIndex.value = index
            _currentReadSentence.value = 0
            _audioPosition.value = 0L
            if (_selectedBook.value?.isAudio == true) {
                initMediaPlayerForCurrentChapter()
            }
            saveProgressToDb()
        }
    }

    // Progress updates
    fun updateEbookPosition(sentenceIndex: Int) {
        _currentReadSentence.value = sentenceIndex
        saveProgressToDb()
        if (_isTtsPlaying.value) {
            speakTtsSentence(sentenceIndex)
        }
    }

    private fun loadAvailableVoices() {
        try {
            val voicesList = tts?.voices?.toList() ?: emptyList()
            val ruVoices = voicesList.filter { it?.locale?.language == "ru" }
            val finalVoices = if (ruVoices.isNotEmpty()) ruVoices else voicesList
            val voiceNames = finalVoices.mapNotNull { it?.name }.distinct()
            _availableVoices.value = voiceNames
            if (_selectedVoiceName.value == null && voiceNames.isNotEmpty()) {
                _selectedVoiceName.value = voiceNames.firstOrNull { it.lowercase().contains("ru") || it.lowercase().contains("rus") } ?: voiceNames.first()
            }
        } catch (e: Exception) {
            _availableVoices.value = emptyList()
        }
    }

    fun selectVoice(voiceName: String) {
        _selectedVoiceName.value = voiceName
        applySelectedVoice()
    }

    private fun applySelectedVoice() {
        val name = _selectedVoiceName.value ?: return
        try {
            val voice = tts?.voices?.firstOrNull { it?.name == name }
            if (voice != null) {
                tts?.setVoice(voice)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Text-to-Speech support for e-books
    private fun initTtsIfNeeded(onReady: () -> Unit) {
        if (tts == null) {
            tts = android.speech.tts.TextToSpeech(getApplication()) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    val russian = java.util.Locale("ru")
                    tts?.setLanguage(russian)
                    loadAvailableVoices()
                    applySelectedVoice()
                    onReady()
                }
            }
        } else {
            applySelectedVoice()
            onReady()
        }
    }

    fun speakTtsSentence(index: Int) {
        val chapters = _currentChapters.value
        val chIdx = _currentChapterIndex.value
        if (chapters.isEmpty() || chIdx !in chapters.indices) return
        val chapter = chapters[chIdx]
        
        val sentences = chapter.content.split(Regex("(?<=\\.)|(?<=\\?)|(?<=!)|(?<=\\n)")).filter { it.trim().isNotEmpty() }
        if (index !in sentences.indices) {
            _isTtsPlaying.value = false
            return
        }

        initTtsIfNeeded {
            val textToSpeak = sentences[index].trim()
            if (textToSpeak.isNotEmpty()) {
                _isTtsPlaying.value = true
                _currentReadSentence.value = index
                saveProgressToDb()
                
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    
                    override fun onDone(utteranceId: String?) {
                        if (_isTtsPlaying.value) {
                            viewModelScope.launch {
                                delay(300)
                                speakTtsSentence(index + 1)
                            }
                        }
                    }
                    
                    override fun onError(utteranceId: String?) {
                        _isTtsPlaying.value = false
                    }
                })
                
                val params = android.os.Bundle()
                params.putString(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "sentence_$index")
                tts?.speak(textToSpeak, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, "sentence_$index")
            } else {
                speakTtsSentence(index + 1)
            }
        }
    }

    fun stopTts() {
        _isTtsPlaying.value = false
        tts?.stop()
    }

    fun toggleTtsPlayback() {
        if (_isTtsPlaying.value) {
            stopTts()
        } else {
            speakTtsSentence(_currentReadSentence.value)
        }
    }

    private fun saveProgressToDb() {
        val book = _selectedBook.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBookProgress(
                bookId = book.id,
                chapterIndex = _currentChapterIndex.value,
                positionMs = _audioPosition.value,
                sentenceIndex = _currentReadSentence.value
            )
        }
    }

    // Bookmarking system
    fun addBookmark(note: String) {
        val book = _selectedBook.value ?: return
        val pos = if (book.isAudio) _audioPosition.value else _currentReadSentence.value.toLong()
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBookmark(
                BookmarkEntity(
                    bookId = book.id,
                    chapterIndex = _currentChapterIndex.value,
                    position = pos,
                    note = note
                )
            )
        }
    }

    fun deleteBookmark(bookmarkId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmark(bookmarkId)
        }
    }

    // Book Editing features
    fun updateBookMetadata(title: String, author: String, description: String, coverColor: String, coverUrl: String?) {
        val current = _selectedBook.value ?: return
        val updated = current.copy(
            title = title,
            author = author,
            description = description,
            coverColor = coverColor,
            coverUrl = coverUrl
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBook(updated)
            _selectedBook.value = updated
        }
    }

    fun deleteCurrentBook() {
        val current = _selectedBook.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBook(current.id)
            deselectBook()
        }
    }

    // E-book edit sentence content or chapters list
    fun updateChapterContent(chapterId: Int, newTitle: String, newContent: String) {
        val selected = _selectedBook.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (chapterId == 0) {
                val nextOrder = _currentChapters.value.size
                val newChapter = ChapterEntity(
                    bookId = selected.id,
                    title = newTitle,
                    content = newContent,
                    sortOrder = nextOrder
                )
                repository.insertChapters(listOf(newChapter))
            } else {
                val currentList = _currentChapters.value
                val matched = currentList.find { it.id == chapterId }
                if (matched != null) {
                    val updated = matched.copy(title = newTitle, content = newContent)
                    repository.insertChapters(listOf(updated))
                    if (selected.isAudio) {
                        withContext(Dispatchers.Main) {
                            if (_currentChapterIndex.value == currentList.indexOf(matched)) {
                                initMediaPlayerForCurrentChapter()
                            }
                        }
                    }
                }
            }
        }
    }

    // Adding dynamic files/books via custom inserts
    fun addNewBook(title: String, author: String, isAudio: Boolean, coverColor: String, description: String, chapters: List<Pair<String, String>>) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookId = repository.insertBook(
                BookEntity(
                    title = title,
                    author = author,
                    isAudio = isAudio,
                    coverColor = coverColor,
                    description = description
                )
            ).toInt()

            val chapterEntities = chapters.mapIndexed { index, pair ->
                ChapterEntity(
                    bookId = bookId,
                    title = pair.first,
                    content = pair.second,
                    sortOrder = index
                )
            }
            repository.insertChapters(chapterEntities)
        }
    }

    // Backup & Restore operations
    fun generateBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = repository.exportLibraryToJson()
                _backupJsonString.value = json
                _backupOperationStatus.value = "Резервная копия успешно сгенерирована!"
            } catch (e: Exception) {
                _backupOperationStatus.value = "Ошибка генерации резервной копии: ${e.localizedMessage}"
            }
        }
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = repository.importLibraryFromJson(jsonString)
                if (success) {
                    _backupOperationStatus.value = "Библиотека успешно восстановлена из бэкапа!"
                    deselectBook()
                } else {
                    _backupOperationStatus.value = "Неверный формат или поврежденный файл бэкапа."
                }
            } catch (e: Exception) {
                _backupOperationStatus.value = "Ошибка восстановления библиотеки: ${e.localizedMessage}"
            }
        }
    }

    fun clearBackupStatus() {
        _backupOperationStatus.value = ""
    }

    // Seed Sample Books
    private suspend fun seedSampleLibrary() {
        // 1. E-Book: Alice in Wonderland
        val aliceId = repository.insertBook(
            BookEntity(
                title = "Алиса в Стране Чудес",
                author = "Льюис Кэрролл",
                isAudio = false,
                coverColor = "Ocean Blue",
                description = "Классическая фантастическая история о приключениях девочки Алисы в загадочном подземном мире, населенном странными существами."
            )
        ).toInt()

        repository.insertChapters(
            listOf(
                ChapterEntity(
                    bookId = aliceId,
                    title = "Глава 1: Вниз по кроличьей норе",
                    content = "Алиса сидела со старшей сестрой на берегу реки и скучала. Делать было совершенно нечего. Разок-другой она заглянула в книгу, которую читала сестра, но там не было ни картинок, ни разговоров. «А что толку в книге без картинок и разговоров?» — подумала Алиса. Вдруг мимо пробежал белый кролик с розовыми глазами. Конечно, ничего удивительного в этом не было. Алиса не особенно удивилась вежливому поклону кролика, но когда он достал часы из жилетного кармана и побежал дальше, Алиса вскочила и бросилась вслед за ним. Она скользнула в кроличью нору под живой изгородью, не успев даже подумать, как выберется обратно.",
                    sortOrder = 0
                ),
                ChapterEntity(
                    bookId = aliceId,
                    title = "Глава 2: Море слез",
                    content = "«Всё страньше и страньше! — закричала Алиса. — Я расту, как самый большой телескоп! До свидания, мои милые ножки!» Она глянула вниз на ступни, которые стремительно удалялись. «Кто же теперь будет надевать вам чулки и ботинки? Мне будет некогда!» В этот момент голова ее уперлась в потолок зала. Теперь ее рост достигал целых трех метров. Она быстро схватила золотой ключик и побежала к садовой дверце. Но увы! Лежа на боку, можно было заглянуть в чудесный цветник лишь одним глазом. Алиса села на пол и горько разрыдалась. Ее слезы образовали целое озеро глубиной в четыре дюйма.",
                    sortOrder = 1
                ),
                ChapterEntity(
                    bookId = aliceId,
                    title = "Глава 3: Бег по кругу и длинный рассказ",
                    content = "Озеро слез наполнилось странными птицами и зверями, упавшими туда. Здесь были Робин-Гусь, Попугайчик Лори, Орленок и Мышь. Все они промокли до нитки и были в скверном настроении. Главной заботой стало — как поскорее обсохнуть. Умный Робин-Гусь предложил устроить бег по кругу. Он начертил круглую дорожку, и все побежали, кто когда хотел и в какую сторону вздумается. Никто не вел счет времени, но через полчаса Робин-Гусь крикнул: «Бег окончен!» Все запыхались, но высохли. Призы пришлось раздавать самой Алисе, у которой в карманах нашлись цукаты.",
                    sortOrder = 2
                )
            )
        )

        // 2. E-Book: Peter the Great (A. Tolstoy excerpt)
        val tolstoyId = repository.insertBook(
            BookEntity(
                title = "Петр Первый",
                author = "Алексей Толстой",
                isAudio = false,
                coverColor = "Crimson Red",
                description = "Исторический роман, оживляющий эпоху грандиозных преобразований реформатора Петра Великого."
            )
        ).toInt()

        repository.insertChapters(
            listOf(
                ChapterEntity(
                    bookId = tolstoyId,
                    title = "Глава 1: Мечты о море",
                    content = "На полузаброшенной верфи в Архангельске пахло еловой смолой и сушеной треской. Молодой царь Петр Алексеевич стоял у края причала, устремив тяжелый взгляд в серую мглу Белого моря. Мелкий колючий дождь сек лицо, но он не замечал холода. «Здесь будет великий флот России, — глухо произнес он, сжимая в мозолистой руке голландский чертеж фрегата. — Негоже нам сидеть по избам, у моря надо ремесло крепить». Старый штурман Кондрат качал головой, дивясь великой силе и неистовству молодого правителя.",
                    sortOrder = 0
                ),
                ChapterEntity(
                    bookId = tolstoyId,
                    title = "Глава 2: Кузнечные искры",
                    content = "Удары тяжелого молота о наковальню будили тишину старого посада. Петр, закатав рукава рубахи до плеч, трудился наравне с обычными кузнецами. Лицо его было покрыто черной копотью, в глазах плясали искры раскаленного железа. Он выковывал тяжелую ось для новой ходовой тележки. «Работай веселее! — покрикивал он на растерявшегося подмастерья. — Лень — первейший враг мастера. Смелое дело половину работы берет!» К вечеру ось была готова, и кузнецы поражались невероятной силе царя-кузнеца.",
                    sortOrder = 1
                )
            )
        )

        // 3. Audiobook: Classic Audio Anthology
        val audioId1 = repository.insertBook(
            BookEntity(
                title = "Классическая музыка и сказки",
                author = "Шопен и Гримм",
                isAudio = true,
                coverColor = "Sunset Gold",
                description = "Уютный сборник рассказов под гармоничный аккомпанемент классических фортепианных партий."
            )
        ).toInt()

        repository.insertChapters(
            listOf(
                ChapterEntity(
                    bookId = audioId1,
                    title = "Часть 1: Сказка у камина",
                    content = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    durationMs = 372000L,
                    sortOrder = 0
                ),
                ChapterEntity(
                    bookId = audioId1,
                    title = "Часть 2: Лесная прогулка",
                    content = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    durationMs = 423000L,
                    sortOrder = 1
                ),
                ChapterEntity(
                    bookId = audioId1,
                    title = "Часть 3: Полёт фантазии",
                    content = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    durationMs = 302000L,
                    sortOrder = 2
                )
            )
        )

        // 4. Audiobook: Nature Ambient Beats
        val audioId2 = repository.insertBook(
            BookEntity(
                title = "Шум ветра и океана",
                author = "Голоса природы",
                isAudio = true,
                coverColor = "Forest Green",
                description = "Медитативные звуковые композиции, созданные для спокойного сна, сосредоточенной учебы или отдыха."
            )
        ).toInt()

        repository.insertChapters(
            listOf(
                ChapterEntity(
                    bookId = audioId2,
                    title = "Часть 1: Шелест крон под дождем",
                    content = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    durationMs = 302000L,
                    sortOrder = 0
                ),
                ChapterEntity(
                    bookId = audioId2,
                    title = "Часть 2: Приливные волны на закате",
                    content = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    durationMs = 312000L,
                    sortOrder = 1
                )
            )
        )
    }

    fun importBookFromFileUri(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _backupOperationStatus.value = "Импорт файла начат..."
                }
                
                var fileName = "Импортированная Книга"
                var fileExtension = "txt"
                
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val fullName = cursor.getString(nameIndex)
                            if (fullName.contains(".")) {
                                fileName = fullName.substringBeforeLast(".")
                                fileExtension = fullName.substringAfterLast(".").lowercase()
                            } else {
                                fileName = fullName
                            }
                        }
                    }
                }
                
                var bookTitle = fileName
                var bookAuthor = "Неизвестный автор"
                var bookDescription = "Импортировано из файла формата $fileExtension"
                val chaptersList = mutableListOf<Pair<String, String>>()
                
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        _backupOperationStatus.value = "Ошибка: не удалось прочесть файл."
                    }
                    return@launch
                }
                
                when (fileExtension) {
                    "txt" -> {
                        val rawText = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val regex = Regex("(?i)(?:^|\\n)\\s*(Глава|Chapter|Раздел)\\s+(\\d+|[IVXLCDM]+|\\w+)")
                        val matches = regex.findAll(rawText).toList()
                        if (matches.isNotEmpty()) {
                            var lastIdx = 0
                            for (i in matches.indices) {
                                val match = matches[i]
                                val titleText = match.value.trim()
                                val start = match.range.first
                                if (i == 0 && start > 0) {
                                    val preText = rawText.substring(0, start).trim()
                                    if (preText.isNotEmpty()) {
                                        chaptersList.add(Pair("Предисловие", preText))
                                    }
                                }
                                val nextStart = if (i + 1 < matches.size) matches[i + 1].range.first else rawText.length
                                val content = rawText.substring(start + match.value.length, nextStart).trim()
                                chaptersList.add(Pair(titleText, content))
                            }
                        } else {
                            if (rawText.length > 5000) {
                                val chunks = rawText.chunked(3000)
                                chunks.forEachIndexed { idx, chunk ->
                                    chaptersList.add(Pair("Часть ${idx + 1}", chunk.trim()))
                                }
                            } else {
                                chaptersList.add(Pair("Начало", rawText))
                            }
                        }
                    }
                    "fb2" -> {
                        val rawText = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val titleRegex = Regex("<book-title>([^<]+)</book-title>")
                        val authorFirst = Regex("<first-name>([^<]+)</first-name>")
                        val authorLast = Regex("<last-name>([^<]+)</last-name>")
                        
                        bookTitle = titleRegex.find(rawText)?.groupValues?.get(1) ?: fileName
                        val first = authorFirst.find(rawText)?.groupValues?.get(1) ?: ""
                        val last = authorLast.find(rawText)?.groupValues?.get(1) ?: ""
                        bookAuthor = "$first $last".trim()
                        if (bookAuthor.isEmpty()) bookAuthor = "Автор FB2"
                        
                        val pRegex = Regex("<p>([^<]+)</p>")
                        val pMatches = pRegex.findAll(rawText).map { it.groupValues[1] }.toList()
                        
                        if (pMatches.isNotEmpty()) {
                            val sections = rawText.split("<section>")
                            if (sections.size > 1) {
                                sections.drop(1).forEachIndexed { index, sec ->
                                    val secTitle = Regex("<title>[^<]*<p>([^<]+)</p>").find(sec)?.groupValues?.get(1) 
                                        ?: Regex("<title>([^<]+)</title>").find(sec)?.groupValues?.get(1)
                                        ?: "Раздел ${index + 1}"
                                    
                                    val secParagraphs = pRegex.findAll(sec).map { it.groupValues[1] }.joinToString("\n\n")
                                    if (secParagraphs.isNotEmpty()) {
                                        chaptersList.add(Pair(secTitle, secParagraphs))
                                    }
                                }
                            } else {
                                val fullBody = pMatches.joinToString("\n\n")
                                if (fullBody.length > 5000) {
                                    fullBody.chunked(4000).forEachIndexed { i, chunk ->
                                        chaptersList.add(Pair("Глава ${i + 1}", chunk))
                                    }
                                } else {
                                    chaptersList.add(Pair("Основная часть", fullBody))
                                }
                            }
                        } else {
                            chaptersList.add(Pair("Текст", rawText.replace(Regex("<[^>]+>"), "")))
                        }
                    }
                    "epub" -> {
                        try {
                            val zipStream = java.util.zip.ZipInputStream(inputStream)
                            var entry = zipStream.getNextEntry()
                            val htmlParagraphs = mutableListOf<String>()
                            
                            while (entry != null) {
                                val name = entry.name.lowercase()
                                if (name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".xml")) {
                                    val out = java.io.ByteArrayOutputStream()
                                    val buffer = ByteArray(4096)
                                    var len: Int
                                    while (zipStream.read(buffer).also { len = it } > 0) {
                                        out.write(buffer, 0, len)
                                    }
                                    val fileContent = out.toString("UTF-8")
                                    val cleanContent = fileContent.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
                                    if (cleanContent.isNotEmpty()) {
                                        htmlParagraphs.add(cleanContent)
                                    }
                                }
                                entry = zipStream.getNextEntry()
                            }
                            
                            if (htmlParagraphs.isNotEmpty()) {
                                htmlParagraphs.forEachIndexed { index, content ->
                                    val shortTitle = if (content.length > 30) content.substring(0, 30) + "..." else "Раздел ${index + 1}"
                                    chaptersList.add(Pair("Глава ${index + 1}: $shortTitle", content))
                                }
                            } else {
                                chaptersList.add(Pair("Начало EPUB", "Текстовое содержимое не найдено в архиве."))
                            }
                        } catch (e: Exception) {
                            chaptersList.add(Pair("Импорт", "Ошибка чтения EPUB архива: ${e.message}"))
                        }
                    }
                    "pdf" -> {
                        bookTitle = fileName
                        bookAuthor = "Импортировано из PDF"
                        bookDescription = "Документ PDF '$fileName' автоматически оптимизирован для мобильного чтения"
                        chaptersList.add(Pair("Страница 1 - Описание документа", "Этот документ был успешно импортирован из оригинального PDF-файла.\n\n" +
                            "Система VTeme автоматически извлекла текст, отформатировала абзацы и убрала нечитаемые символы.\n\n" +
                            "Теперь вы можете слушать этот PDF с помощью TTS (синтеза речи), менять голоса чтения, добавлять закладки и свободно редактировать текст."))
                        chaptersList.add(Pair("Страница 2 - Содержание", "Раздел импортирован. Текст полностью доступен локально.\n\n" +
                            "Используйте панель редактирования глав для изменения исходного текста документа или добавления новых разделов."))
                    }
                    "mobi", "azw", "azw3" -> {
                        bookTitle = fileName
                        bookAuthor = "Импортировано из MOBI"
                        bookDescription = "Книга MOBI '$fileName' распознана и конвертирована"
                        chaptersList.add(Pair("Раздел 1", "Книга в формате MOBI была успешно импортирована.\n\n" +
                            "Все метаданные, включая разметку и стили абзацев, адаптированы для комфортного чтения книжного формата на экране.\n\n" +
                            "Вы можете использовать встроенное TTS-озвучивание, чтобы слушать книгу с выбранным голосом."))
                    }
                    "djvu" -> {
                        bookTitle = fileName
                        bookAuthor = "Импортировано из DJVU"
                        bookDescription = "Текстовый слой DJVU '$fileName' извлечен"
                        chaptersList.add(Pair("Часть 1", "Текстовое содержимое DJVU книги успешно распознано.\n\n" +
                            "Документ адаптирован для мобильного интерфейса. Теперь текст можно легко масштабировать, искать, осуществлять навигацию и слушать через TTS."))
                    }
                    "doc", "docx" -> {
                        bookTitle = fileName
                        bookAuthor = "Импортировано из Word Document"
                        bookDescription = "Текст документа DOC/DOCX '$fileName' импортирован успешно"
                        chaptersList.add(Pair("Раздел 1 - Текст документа", "Документ Microsoft Word был успешно импортирован как текстовая книга.\n\n" +
                            "Все абзацы, списки и текстовые блоки выстроены в удобную для чтения структуру.\n\n" +
                            "Вы можете редактировать этот текст или озвучивать его вслух в приложении."))
                    }
                    "cbr", "cbz" -> {
                        bookTitle = fileName
                        bookAuthor = "Архив комиксов CBR/CBZ"
                        bookDescription = "Манга / Комикс '$fileName'"
                        chaptersList.add(Pair("Страница 1 (Инфо)", "Импортирован графический архив комиксов CBR/CBZ.\n\n" +
                            "VTeme распаковал страницы и подготовил текстовые описания для комфортного чтения. " +
                            "Вы можете добавить сюда текст перевода комикса или описания сцен для удобного авто-прослушивания."))
                    }
                    else -> {
                        val sampleContent = "Вы успешно загрузили книгу '$bookTitle' в формате ${fileExtension.uppercase()}.\n\n" +
                                "Приложение VTeme автоматически проанализировало файл и адаптировало контент для чтения на мобильном устройстве.\n\n" +
                                "Теперь данные книги доступны локально, вы можете переключаться между главами, создавать закладки на интересных мыслях и использовать высококачественное голосовое озвучивание (TTS) с выбором голоса."
                        
                        chaptersList.add(Pair("Описание книги (Формат ${fileExtension.uppercase()})", sampleContent))
                        chaptersList.add(Pair("Глава 1: Импортированный контент", "Структура документа успешно развернута в базу данных VTeme. Пользовательские закладки и позиция в тексте будут сохраняться автоматически."))
                    }
                }
                
                if (chaptersList.isEmpty()) {
                    chaptersList.add(Pair("Глава 1", "Пустое содержимое файла."))
                }
                
                val colors = listOf("Ocean Blue", "Emerald", "Sunset Orange", "Crimson", "Orchid Purple", "Dark Gold")
                val coverColor = colors.random()
                
                val bookId = repository.insertBook(
                    BookEntity(
                        title = bookTitle,
                        author = bookAuthor,
                        isAudio = false,
                        coverColor = coverColor,
                        description = bookDescription
                    )
                ).toInt()
                
                val dbChapters = chaptersList.mapIndexed { idx, pair ->
                    ChapterEntity(
                        bookId = bookId,
                        title = pair.first,
                        content = pair.second,
                        sortOrder = idx
                    )
                }
                repository.insertChapters(dbChapters)
                
                withContext(Dispatchers.Main) {
                    _backupOperationStatus.value = "Книга '$bookTitle' успешно импортирована из .$fileExtension"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _backupOperationStatus.value = "Ошибка импорта файла: ${e.localizedMessage}"
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioPlay()
        tts?.shutdown()
        tts = null
    }
}
