package kr.co.ajjulcoding.team.project.holo.view.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kr.co.ajjulcoding.team.project.holo.data.ChatBubble
import kr.co.ajjulcoding.team.project.holo.data.ChatNotificationBody
import kr.co.ajjulcoding.team.project.holo.data.HoloUser
import kr.co.ajjulcoding.team.project.holo.data.SimpleChatRoom
import kr.co.ajjulcoding.team.project.holo.repository.Repository
import kotlin.Exception

class ChatRoomViewModel: ViewModel() {
    private var listenerRgst:ListenerRegistration? = null
    private var _chatBubbleLi = MutableLiveData<ArrayList<ChatBubble>>()
    val chatBubbleLi:LiveData<ArrayList<ChatBubble>> get() = _chatBubbleLi
    private var _sendError = MutableLiveData<Exception>()
    private var _validScore = MutableLiveData<Boolean>()
    val validScore: LiveData<Boolean> get() = _validScore
    val sendError: LiveData<Exception> = _sendError
    private val repository = Repository()

    init {
        _chatBubbleLi.value = ArrayList<ChatBubble>()
    }

    fun setChatBubble(userData: HoloUser, chatData: SimpleChatRoom, content: String) = viewModelScope.async {
        val vaild = repository.setChatBubble(userData, chatData, content, _sendError)
        return@async vaild
    }

    fun getUserNicknameAndToken(email: String) = viewModelScope.async{
        val nameToken:Pair<String,String> = repository.getUserNicknameAndToken(email)
        return@async nameToken
    }

    fun sendChatPushAlarm(notifiBody: ChatNotificationBody) = viewModelScope.launch{
        repository.sendChatPushAlarm(notifiBody)
    }

    fun getChatBubbleLi(title:String, randomDouble:Double) = viewModelScope.launch{
        if (listenerRgst == null)
            listenerRgst = repository.getChatBubbleLi(title, randomDouble, _chatBubbleLi)
        Log.d("채팅방 리스너 등록", listenerRgst.toString())
    }

    fun checkValidStar(userEmail: String, chatTitle:String, chatRandom:Double) = viewModelScope.async {
        val result: Pair<Boolean, String> = repository.checkValidStar(userEmail, chatTitle, chatRandom)
        return@async result
    }

    fun postScore(email:String, direction: String, star:Float, chatTitle:String, chatRandom:Double) = viewModelScope.async {
        val result: Boolean = repository.postScore(email, direction, star, chatTitle, chatRandom)
        return@async result == true
    }

    fun deleteChatRoom(chatTitle:String, chatRandom:Double) = viewModelScope.async {
            val result: Boolean = repository.deleteChatRoom(chatTitle, chatRandom)
            Log.d("채팅방 삭제 결과", result.toString())
            return@async result == true
    }

    override fun onCleared() {
        super.onCleared()
        listenerRgst?.remove()
        listenerRgst = null
    }
}