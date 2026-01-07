package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.c2cfastpay_card.UIScreen.components.MatchItem
import com.example.c2cfastpay_card.UIScreen.components.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 歷史紀錄/聊天列表的 ViewModel
class HistoryViewModel(
    private val matchRepository: MatchRepository
) : ViewModel() {

    // 儲存配對列表
    private val _matches = MutableStateFlow<List<MatchItem>>(emptyList())
    // 對外公開唯讀的 StateFlow
    val matches: StateFlow<List<MatchItem>> = _matches.asStateFlow()

    // 初始化時載入資料
    init {
        loadMatches()
    }

    // 載入資料的函式
    fun loadMatches() {
        viewModelScope.launch {
            // 呼叫 Repository (這需要您完成步驟一)
            val matchList = matchRepository.getMatches()
            _matches.value = matchList
        }
    }
}

class HistoryViewModelFactory(
    private val matchRepository: MatchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(matchRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}