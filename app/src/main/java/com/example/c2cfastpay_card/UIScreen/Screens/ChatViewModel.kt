package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.c2cfastpay_card.UIScreen.components.ChatRepository
import com.example.c2cfastpay_card.UIScreen.components.MatchDetails
import com.example.c2cfastpay_card.data.Message
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 聊天室 ViewModel
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val matchId: String // 透過 Factory 傳入目前的聊天室 ID
) : ViewModel() {
    val myUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // UI 狀態    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // 配對詳情：包含雙方可用來交換的商品列表
    private val _matchDetails = MutableStateFlow<MatchDetails?>(null)
    val matchDetails: StateFlow<MatchDetails?> = _matchDetails.asStateFlow()

    init {
        // 初始化時，立即載入訊息與詳情
        loadMessages()
        loadMatchDetails()
    }

    // 載入並監聽訊息
    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getMessagesFlow(matchId).collect { msgList ->
                _messages.value = msgList
            }
        }
    }

    // 載入配對詳情 (雙方商品)
    private fun loadMatchDetails() {
        viewModelScope.launch {
            val details = chatRepository.getMatchDetails(matchId)
            _matchDetails.value = details
        }
    }

    // 發送文字訊息
    fun sendMessage(text: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(matchId, text)
        }
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val matchId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, matchId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}