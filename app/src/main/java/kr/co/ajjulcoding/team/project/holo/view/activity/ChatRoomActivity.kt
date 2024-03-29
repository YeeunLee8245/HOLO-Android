package kr.co.ajjulcoding.team.project.holo.view.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import kotlinx.coroutines.*
import kr.co.ajjulcoding.team.project.holo.*
import kr.co.ajjulcoding.team.project.holo.base.BaseActivity
import kr.co.ajjulcoding.team.project.holo.data.ChatBubble
import kr.co.ajjulcoding.team.project.holo.data.ChatNotificationBody
import kr.co.ajjulcoding.team.project.holo.data.HoloUser
import kr.co.ajjulcoding.team.project.holo.data.SimpleChatRoom
import kr.co.ajjulcoding.team.project.holo.databinding.ActivityChatRoomBinding
import kr.co.ajjulcoding.team.project.holo.view.fragment.PostScoreDialogFragment
import kr.co.ajjulcoding.team.project.holo.view.viewmodel.ChatRoomViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ChatRoomActivity() : BaseActivity<ActivityChatRoomBinding>({
    ActivityChatRoomBinding.inflate(it)
}) {
    private lateinit var userEmail:String
    private lateinit var userInfo: HoloUser
    private lateinit var chatRoomData: SimpleChatRoom
    private lateinit var chatRoomViewModel: ChatRoomViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatRoomViewModel = ViewModelProvider(this).get(ChatRoomViewModel::class.java)
        userInfo = intent.getParcelableExtra<HoloUser>(AppTag.USER_INFO)!!
        userEmail = userInfo.uid
        chatRoomData = intent.getParcelableExtra<SimpleChatRoom>(AppTag.CHATROOM_TAG)!!
        randomNum = chatRoomData.randomDouble
        setViewData()
        chatRoomViewModel.getChatBubbleLi(chatRoomData.title, chatRoomData.randomDouble!!)
        binding.recyclerBubble.adapter = ChatRoomAdapter()
        val bubbleObserver = object : Observer<ArrayList<ChatBubble>>{
            override fun onChanged(bubbleLi: ArrayList<ChatBubble>?) {
                Log.d("채팅방 옵저버 확인", bubbleLi.toString())
                (binding.recyclerBubble.adapter as ChatRoomAdapter).replaceBubbles(bubbleLi!!)
            }
        }
        binding.recyclerBubble.adapter!!.registerAdapterDataObserver(ChatDataObserver())

        chatRoomViewModel.chatBubbleLi.observe(this, bubbleObserver)
        chatRoomViewModel.validScore.observe(this){

        }
        binding.btnSendText.setOnClickListener {
            Log.d("네트워크 확인", checkNetwork().toString())
            if (!checkNetwork()){   // 네트워크 X
                showAlertDialog("네트워크 연결을 확인할 수 없습니다!", *arrayOf("확인"))
                return@setOnClickListener
            }

            sendMSG()   // 채팅 전송
        }
        binding.btnSubmit.setOnClickListener {
            if (!checkNetwork()){   // 네트워크 X
                showAlertDialog("네트워크 연결을 확인할 수 없습니다!", *arrayOf("확인"))
                return@setOnClickListener
            }
            val numStar:Float = binding.ratingBar.rating // 0.1f
            if (numStar == 0f){
                showAlertDialog("별점을 드래그하여 점수를 설정해주세요!", *arrayOf("확인"))
                return@setOnClickListener
            }
            // TODO: 별점 등록 확인
            CoroutineScope(Dispatchers.Main).launch {
                val result: Deferred<Pair<Boolean, String>> = chatRoomViewModel.checkValidStar(userInfo.uid ,chatRoomData.title,
                    chatRoomData.randomDouble!!
                )
                val resultDefer: Pair<Boolean, String> = result.await()
                if (resultDefer.first == true)
                    showPostScoreDialog(numStar, resultDefer.second)
                else
                    showAlertDialog("이미 별점을 등록하셨습니다.", *arrayOf("확인"))
            }

        }
        binding.ratingBar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            if (rating < 0.5f)
                binding.ratingBar.rating = 0.5f
        }
    }

    override fun onPause() {
        randomNum = null
        super.onPause()
    }

    private fun setViewData(){
        if (chatRoomData.semail != userEmail){  // 사용자는 remail
            binding.txtNickName.setText(chatRoomData.snickName)
        }else
            binding.txtNickName.setText(chatRoomData.rnickName)

        binding.txtTitle.setText(chatRoomData.title)
    }

    private fun checkNetwork(): Boolean{
        val conManager = getSystemService(ConnectivityManager::class.java)
        val currentNet = conManager.activeNetwork ?: return false
        val actNet = conManager.getNetworkCapabilities(currentNet) ?: return false

        return when {
            actNet.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNet.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            else -> false
        }
    }

    private fun showAlertDialog(msg:String, vararg option:String){
        AlertDialog.Builder(this)
            .setTitle(msg)
            .setCancelable(false)
            .setItems(option, object : DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface, idx: Int) {
                    dialog.dismiss()
                }
            })
            .create().show()
    }

    private fun sendMSG(){
        val content = binding.editChat.text.toString()
        CoroutineScope(Dispatchers.Main).launch {
            val defVaild: Deferred<Boolean> = chatRoomViewModel.setChatBubble(userInfo, chatRoomData, content)
            val resultVaild = defVaild.await()
            if (resultVaild == false)
                return@launch
            sendAlarm(content)
        }

        binding.editChat.setText("")
    }

    private fun sendAlarm(content: String){
        var toEmail: String = chatRoomData.remail
        if (toEmail == userInfo.uid)
            toEmail = chatRoomData.semail
        CoroutineScope(Dispatchers.Main).launch {
            val defNameToken: Deferred<Pair<String,String>> = chatRoomViewModel.getUserNicknameAndToken(toEmail)
            val rstNameToken = defNameToken.await()
            val data = ChatNotificationBody.ChatNotificationData(
                "${userInfo.nickName} 님으로부터 채팅이 도착했습니다", content, chatRoomData
            )
            val body = ChatNotificationBody(rstNameToken.second, data)
            Log.d("바디바디", body.toString())
            chatRoomViewModel.sendChatPushAlarm(body)
        }
    }

    private fun showPostScoreDialog(numStar: Float, direction: String){
        val dialog: PostScoreDialogFragment = PostScoreDialogFragment()
        supportFragmentManager.let { fragmentManager ->
            if (null == fragmentManager.findFragmentByTag(AppTag.POSTSCORE_TAG))
                dialog!!.show(fragmentManager, AppTag.POSTSCORE_TAG)
        }
        dialog!!.setPostOnBtnClicked(object : PostScoreDialogFragment.PostOnBtnClickListener {
            override fun PostOnBtnClicked(vaild: Boolean) {
                var toEmail:String = chatRoomData.remail
                if (chatRoomData.remail == userInfo.uid)
                    toEmail = chatRoomData.semail
                val deferred:Deferred<Boolean> = chatRoomViewModel.postScore(toEmail, direction,
                    numStar, chatRoomData.title, chatRoomData.randomDouble!!)   // 별점 전송
                CoroutineScope(Dispatchers.Main).launch {
                    val resultDeferred:Boolean = deferred.await()
                    if (resultDeferred){
                        deleteChatRoom()    // 채팅방 삭제 체커
                    }
                }
            }
        })
    }

    private fun deleteChatRoom(){
        Log.d("채팅방 삭제 확인 시작","ㅇㅇ")
        val deferred:Deferred<Boolean> = chatRoomViewModel.deleteChatRoom(chatRoomData.title, chatRoomData.randomDouble!!)
        CoroutineScope(Dispatchers.Main).launch {
            val resultDeferred:Boolean = deferred.await()
            if (resultDeferred)
                finish()
            else
                showAlertDialog("서버 오류입니다. 나중에 다시 시도해주세요.", *arrayOf("확인"))
        }
        // TODO: 채팅방 삭제하기
    }

    companion object{
        var randomNum: Double? = null
        const val LEFT_BUBBLE = 1
        const val RIGHT_BUBBLE = 2
        val chatBubbleDiffUtil = object : DiffUtil.ItemCallback<ChatBubble>(){
            override fun areItemsTheSame(oldItem: ChatBubble, newItem: ChatBubble): Boolean {
                Log.d("옵저버버 데이터 확인4", oldItem.currentTime.toString()+" "+newItem.currentTime.toString())
                return oldItem.currentTime == newItem.currentTime
            }

            override fun areContentsTheSame(oldItem: ChatBubble, newItem: ChatBubble): Boolean {
                Log.d("옵저버 데이터 확인5", oldItem.toString()+"다름 "+newItem.toString())
                return oldItem == newItem && oldItem.currentTime == newItem.currentTime
            }

        }
    }
    inner class ChatDataObserver(): RecyclerView.AdapterDataObserver(){
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            binding.recyclerBubble.scrollToPosition(0)
            super.onItemRangeInserted(positionStart, itemCount)
        }
    }
    // TODO: 채팅 실시간 보내서 UI 출력
    inner class ChatRoomAdapter(): ListAdapter<ChatBubble, RecyclerView.ViewHolder>(
        chatBubbleDiffUtil
    ){

        inner class LeftViewHolder(view: View): RecyclerView.ViewHolder(view){
            private val textChat: TextView = view.findViewById(R.id.txtLeftChat)
            private val textDate: TextView = view.findViewById(R.id.txtLeftDate)

            fun bind(item: ChatBubble){
                val time:Timestamp = item.currentTime!!
                val millisec:Long = time.seconds * 1000 + time.nanoseconds / 1000000
                val sdf = SimpleDateFormat("E요일, kk:mm", Locale.KOREA)
                val tmpDate = Date(millisec)
                val date = sdf.format(tmpDate)
                textDate.setText(date)
                textChat.setText(item.content)
            }
        }
        inner class RightViewHolder(view: View): RecyclerView.ViewHolder(view){
            private val textChat: TextView = view.findViewById(R.id.txtRightChat)
            private val textDate: TextView = view.findViewById(R.id.txtRightDate)

            fun bind(item: ChatBubble){
                val time:Timestamp = item.currentTime!!
                val millisec:Long = time.seconds * 1000 + time.nanoseconds / 1000000
                val sdf = SimpleDateFormat("E요일, kk:mm", Locale.KOREA)
                val tmpDate = Date(millisec)
                val date = sdf.format(tmpDate)
                textDate.setText(date)
                textChat.setText(item.content)
            }
        }
        override fun getItemViewType(position: Int): Int {
            return currentList!!.get(position).let { bubble ->
                if (bubble.nickname == userInfo.nickName)
                    RIGHT_BUBBLE
                else
                    LEFT_BUBBLE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view: View?
            return when (viewType) {
                LEFT_BUBBLE -> {
                    view = LayoutInflater.from(parent.context).inflate(
                        R.layout.item_chat_left_recycler,
                        parent,
                        false)
                    LeftViewHolder(view)
                }

                else -> {
                    view = LayoutInflater.from(parent.context).inflate(
                        R.layout.item_chat_right_recycler,
                        parent,
                        false)
                    RightViewHolder(view)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            currentList!!.get(position).let { bubble ->
                var bubbleType:Int = LEFT_BUBBLE
                if (bubble.nickname == userInfo.nickName)
                    bubbleType = RIGHT_BUBBLE
                when (bubbleType){
                    LEFT_BUBBLE -> {
                        (holder as LeftViewHolder).bind(bubble)
                    }
                    else -> {
                        (holder as RightViewHolder).bind(bubble)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return currentList.size
        }

        fun replaceBubbles(newBubbleLi: ArrayList<ChatBubble>){
            submitList(newBubbleLi)
        }

    }
}