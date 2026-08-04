package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val database: BrowserDatabase) {
    private val bookmarkDao = database.bookmarkDao()
    private val historyDao = database.historyDao()
    private val settingDao = database.settingDao()

    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()
    val allHistory: Flow<List<HistoryEntry>> = historyDao.getAllHistory()

    fun isUrlBookmarked(url: String): Flow<Boolean> = bookmarkDao.isUrlBookmarked(url)

    suspend fun addBookmark(title: String, url: String) {
        bookmarkDao.insertBookmark(Bookmark(title = title, url = url))
    }

    suspend fun removeBookmark(id: Int) {
        bookmarkDao.deleteBookmark(id)
    }

    suspend fun removeBookmarkByUrl(url: String) {
        bookmarkDao.deleteBookmarkByUrl(url)
    }

    suspend fun addHistoryEntry(title: String, url: String) {
        historyDao.insertHistory(HistoryEntry(title = title, url = url))
    }

    suspend fun removeHistoryEntry(id: Int) {
        historyDao.deleteHistoryEntry(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    fun getSettingFlow(key: String): Flow<String?> = settingDao.getSettingFlow(key)
    
    suspend fun getSettingValue(key: String): String? = settingDao.getSettingValue(key)

    suspend fun saveSetting(key: String, value: String) {
        settingDao.saveSetting(BrowserSetting(key, value))
    }
}
