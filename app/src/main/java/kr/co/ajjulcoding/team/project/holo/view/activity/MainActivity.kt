package kr.co.ajjulcoding.team.project.holo.view.activity

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kr.co.ajjulcoding.team.project.holo.*
import kr.co.ajjulcoding.team.project.holo.base.BaseActivity
import kr.co.ajjulcoding.team.project.holo.data.HoloUser
import kr.co.ajjulcoding.team.project.holo.data.NotificationItem
import kr.co.ajjulcoding.team.project.holo.data.SimpleChatRoom
import kr.co.ajjulcoding.team.project.holo.data.UtilityBillItem
import kr.co.ajjulcoding.team.project.holo.databinding.ActivityMainBinding
import kr.co.ajjulcoding.team.project.holo.repository.Repository
import kr.co.ajjulcoding.team.project.holo.util.ToastUtil
import kr.co.ajjulcoding.team.project.holo.view.fragment.*
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import java.io.File
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : BaseActivity<ActivityMainBinding>({
    ActivityMainBinding.inflate(it)
}) {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var homeFragment: HomeFragment
    private lateinit var profileFragment: ProfileFragment
    private lateinit var accountFragment: AccountFragment
    private lateinit var userSettingFragment: UsersettingFragment
    private val withdrawalDialogFragment = WithdrawalDialogFragment()
    private lateinit var utilityBillDialogFragment: UtilityBillDialogFragment
    private lateinit var notificationFragment: NotificationFragment
    private lateinit var scoreDialogFragment: ScoreDialogFragment
    private lateinit var chatListFragment:ChatListFragment
    private val gpsFragment = GpsFragment()
    private val userInfoBundle = Bundle()
    private lateinit var mUserInfo: HoloUser
    private var currentTag:String = AppTag.HOME_TAG
    private lateinit var frgDic:HashMap<String, Fragment>
    private lateinit var dialog: DialogFragment
    private var waitTime = 0L // 백버튼 2번 시간 간격
    private lateinit var imgLauncher: ActivityResultLauncher<Intent>
    private lateinit var utilitylist:ArrayList<UtilityBillItem>
    private lateinit var notificationlist:ArrayList<NotificationItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        mUserInfo = intent.getParcelableExtra<HoloUser>(AppTag.USER_INFO)!!
        Log.d("유저 데이터 정보", mUserInfo.toString())
        super.onCreate(savedInstanceState)

        userInfoBundle.putParcelable(AppTag.USER_INFO, mUserInfo)
        homeFragment = HomeFragment()
        profileFragment = ProfileFragment()
        userSettingFragment = UsersettingFragment()
        scoreDialogFragment = ScoreDialogFragment()
        accountFragment = AccountFragment()
        utilityBillDialogFragment = UtilityBillDialogFragment()
        notificationFragment = NotificationFragment()
        chatListFragment = ChatListFragment()

        homeFragment.arguments = userInfoBundle
        profileFragment.arguments = userInfoBundle
        userSettingFragment.arguments = userInfoBundle
        scoreDialogFragment.arguments = userInfoBundle
        accountFragment.arguments = userInfoBundle
        utilityBillDialogFragment.arguments = userInfoBundle
        notificationFragment.arguments = userInfoBundle
        chatListFragment.arguments = userInfoBundle

        imgLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val uri: Uri? = result.data?.data
            Log.d("사진 가져오기0", "${uri}")
            uri?.let { // 사진 정상적으로 가져옴
                profileFragment.setProfileImg(it)
            }
        }

        showHomeFragment()
        frgDic = hashMapOf<String, Fragment>(
            AppTag.PROFILE_TAG to profileFragment,
            AppTag.GPS_TAG to gpsFragment, AppTag.SETTING_TAG to userSettingFragment,
            AppTag.WITHDRAWALDIALOG_TAG to withdrawalDialogFragment,
            AppTag.UTILITYBILLDIALOG_TAG to utilityBillDialogFragment,
            AppTag.SCORE_TAG to scoreDialogFragment, AppTag.ACCOUNT_TAG to accountFragment,
            AppTag.CHATLIST_TAG to chatListFragment, AppTag.NOTIFICATION_TAG to notificationFragment)

        intent.getParcelableExtra<SimpleChatRoom>(SendMessageService.CHAT_TYPE)?.let {
            Log.d("알림 주소3", it.toString())
            val chatIntent: Intent = Intent(this, ChatRoomActivity::class.java)
            SettingInApp.uniqueActivity(chatIntent)
            chatIntent.putExtra(AppTag.USER_INFO, mUserInfo)
            chatIntent.putExtra(AppTag.CHATROOM_TAG, it)
            startActivity(chatIntent)
            binding.navigationBar.selectedItemId = R.id.menu_chatting
            changeFragment(AppTag.CHATLIST_TAG)
        }
        intent.getStringExtra(SendMessageService.CMT_TYPE)?.let {
            Log.d("알림 주소2", it.toString())
            if (it == SendMessageService.CHAT_LIST_TYPE)
                changeFragment(AppTag.CHATLIST_TAG)
            else if (it == SendMessageService.HOME_TYPE)
                return@let
            else
                changeFragment(WebUrl.URL_BASE +it)
        }

        if (intent.getBooleanExtra(AppTag.LOGIN_TAG, false)) {
            CoroutineScope(Dispatchers.Main).launch {
                setProfileImg("profile_" + mUserInfo.uid!!.replace(".", "") + ".jpg")
            }
            saveCache()
        }else if (intent.getBooleanExtra(AppTag.REGISTER_TAG, false)){
            saveCache()
            dialog = UtilityBillDialogFragment()
            dialog.arguments = userInfoBundle
            dialog.show(supportFragmentManager, "CustomDialog")
        }

        binding.navigationBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    changeFragment(AppTag.HOME_TAG)
                }
                R.id.menu_chatting -> {
                    changeFragment(AppTag.CHATLIST_TAG)
                }
                R.id.menu_like -> {
                    changeFragment(WebUrl.URL_BASE + WebUrl.URL_LIKE)
                }
                R.id.menu_profile -> {
                    changeFragment(AppTag.SETTING_TAG)
                }
            }
            true
        }

        KeyboardVisibilityEvent.setEventListener(this){ valid ->
            if (valid == true){
                binding.navigationBar.visibility = View.GONE
            }else
                binding.navigationBar.visibility = View.VISIBLE
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        val locale = Locale.KOREAN  // ko_KR은 웹뷰에서까지 적용 안됨
        Locale.setDefault(locale)
        val config: Configuration? = newBase?.resources?.configuration
        config?.setLocale(locale)
        config?.setLayoutDirection(locale)
        Log.d("언어 확인",config.toString()+"/"+newBase.toString())
        if (config != null)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        else
            super.attachBaseContext(newBase)
    }

    override fun onBackPressed() {
        if (currentTag == AppTag.PROFILE_TAG || currentTag == AppTag.GPS_TAG ||
            currentTag == AppTag.ACCOUNT_TAG
        ) {
            changeFragment(AppTag.SETTING_TAG)
        }else if (currentTag == AppTag.HOME_TAG && (System.currentTimeMillis() - waitTime >= 1500)) { // 1.5초 기준
            waitTime = System.currentTimeMillis()
            ToastUtil.showToast(this,"뒤로가기 버튼을 한번 더 누르면 종료됩니다.")
        }
        else if (
            currentTag == AppTag.NOTIFICATION_TAG
            || currentTag == AppTag.CHATLIST_TAG
            || currentTag == AppTag.SETTING_TAG
            || currentTag.contains(WebUrl.URL_BASE)){
            changeFragment(AppTag.HOME_TAG)
       }else
            super.onBackPressed()

    }

    fun changeFragment(frgTAG: String){
        val tran = supportFragmentManager.beginTransaction()

        if (currentTag != frgTAG){  // TODO: 채팅방 리스트 빼고 다 add로 고쳐보기
            currentTag = frgTAG
            if (AppTag.HOME_TAG == currentTag)
                tran.replace(R.id.fragmentView, homeFragment)   // (스택에 있는)이전 프래그먼트 전부 제거
            else if(currentTag == AppTag.SCORE_TAG) {
                dialog = scoreDialogFragment
                dialog.show(supportFragmentManager, "CustomDialog")
            }
            else if (currentTag.contains(WebUrl.URL_BASE)) {
                Log.d("웹뷰","들어옴:${currentTag}")
                val webviewFragment = WebViewFragment()     // 동일한 프래그먼트 객체를 연속으로 replace해선 실행되지 않는다.
                userInfoBundle.putString(WebUrl.URL_BASE, currentTag)
                webviewFragment.arguments = userInfoBundle
                tran.replace(R.id.fragmentView, webviewFragment)    // TODO: replace로 고쳐서 테스트해보기
            }else {
                frgDic[currentTag]!!.let { tran.replace(R.id.fragmentView, it) }    // add로 바꾸면 gps 적용시 강종
            }
            tran.commit()
        }
    }

    fun changetoLoginActivity() {
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        editor.clear()
        editor.commit()
        val intent = Intent(this, LoginActivity::class.java)  // 인텐트를 생성해줌,
        startActivity(intent)  // 화면 전환을 시켜줌
        finish()
    }

    fun withdrawalUser() {
        val repository = Repository()

        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        editor.clear()
        editor.commit()

        val intentLogin = Intent(this, LoginActivity::class.java)
        SettingInApp.uniqueActivity(intentLogin)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intentLogin.flags =  // 탈퇴시 기존 스택 모두 비우고 로그인화면 생성"
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP //액티비티 스택제거
        CoroutineScope(Dispatchers.Main).launch {
            val result = repository.deleteUserInfo(mUserInfo.uid)

            Log.d("탈퇴 데이터 확인", result.toString())
            if (result != false) {
                intentLogin.putExtra(AppTag.currentUserEmail(), false)
                intentLogin.putExtra(AppTag.LOGIN_TAG, false)
                startActivity(intentLogin)
            }else ToastUtil.showToast(this@MainActivity,"서버 통신 오류")
        }

    }

    fun setLocationToHome(location:String){
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        var userInfoBundle = Bundle()
        userInfoBundle.putParcelable(AppTag.USER_INFO, mUserInfo)
        userSettingFragment.arguments = userInfoBundle
        userSettingFragment.setUserLocation(location)
        editor.putString("location",location).apply()   // save location
    }

    fun setAccount(account:String){
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        userSettingFragment.setUserAccount(account)
        editor.putString("account",account).apply()   // save account
    }

    suspend fun setProfileImg(fileName:String){
        val dir = File(Environment.DIRECTORY_PICTURES + "/profile_img")
        if (!dir.isDirectory()){
            dir.mkdir()    // 가져온 이미지 저장할 디렉토리 만들기
        }
        CoroutineScope(Dispatchers.IO).async {
            downloadImg(fileName)
        }.await()
    }

    private fun downloadImg(fileName: String){
        val FBstorage = FirebaseStorage.getInstance()
        val FBstorageRef = FBstorage.reference
        FBstorageRef.child("profile_img/"+fileName).downloadUrl
            .addOnSuccessListener { imgUri ->
                Log.d("저장한 프로필 url", imgUri.toString())
                if(currentTag == AppTag.HOME_TAG) {
                    Glide.with(this).load(imgUri).apply { // TODO: 보일러 코드 고치기: 함수 만들어야될 듯
                        RequestOptions()
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                    }.into(findViewById(R.id.circleImageView))
                }else if (currentTag == AppTag.SETTING_TAG){
                    Glide.with(this).load(imgUri).apply {
                        RequestOptions()
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                    }.into(findViewById(R.id.profilePhoto))
                }
                var userInfoBundle = Bundle()
                userInfoBundle.putParcelable(AppTag.USER_INFO, mUserInfo)
                userSettingFragment.arguments = userInfoBundle
                userSettingFragment.setUserProfile(imgUri.toString())
                sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)  // 캐시 저장
                editor = sharedPref.edit()
                editor.putString("profile",imgUri.toString()).apply()
            }

    }

    fun storeUtilityCache(mUtilityBillItems: ArrayList<UtilityBillItem>) {
        mUserInfo.utilitylist = mUtilityBillItems
        utilitylist=mUtilityBillItems
        Log.d("메인액티비티 공과금 list count", utilitylist.size.toString())
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        val gson = Gson()
        val json = gson.toJson(utilitylist)
        editor.putString(AppTag.BILLCACHE_TAG, json)
        Log.d("메인액티비티 공과금 json", json)
        editor.apply()
    }

    fun getUtilityJSON(): ArrayList<UtilityBillItem> {
        val type: Type = object : TypeToken<ArrayList<UtilityBillItem?>?>() {}.getType()
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        val gson = Gson()
        val json = sharedPref.getString(AppTag.BILLCACHE_TAG, "")
        utilitylist = gson.fromJson(json, type)

        return utilitylist
    }

    fun getNotificationJSON(): ArrayList<NotificationItem> {
        val type: Type = object : TypeToken<ArrayList<NotificationItem?>?>() {}.getType()
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        val gson = Gson()
        val json = sharedPref.getString(AppTag.NOTIFICATIONCACHE_TAG, "")
        notificationlist = gson.fromJson(json, type)

        return notificationlist
    }
    
    private fun saveCache(){
        val userInfo = intent.getParcelableExtra<HoloUser>(AppTag.USER_INFO) as HoloUser
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        editor.putInt("id", userInfo.id ?: -1).apply()
        editor.putString("uid", userInfo.uid).apply()
        editor.putString("realName",userInfo.realName).apply()
        editor.putString("nickName",userInfo.nickName).apply()
        editor.putString("score", userInfo.score).apply()
        editor.putString("token", userInfo.token).apply()
        editor.putBoolean("msgValid", userInfo.msgVaild).apply()
    }

    private fun showHomeFragment(){
        val tran = supportFragmentManager.beginTransaction()
        tran.replace(R.id.fragmentView, homeFragment)
        tran.commit()
    }

    fun showAlertDialog(msg:String, vararg option:String){
        AlertDialog.Builder(this)
            .setTitle(msg)
            .setItems(option, object : DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface, idx: Int) {
                    dialog.dismiss()
                }
            })
            .create().show()
    }
    
    fun getImgCallback() = imgLauncher

    fun addAlarm(position: Int, term: Int, day: Int) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val code = (position.toString()+term.toString()+day.toString()).toInt()

        val intent = Intent(this, AlarmBroadcastReceiver::class.java)
        intent.putExtra("requestCode", code.toString())
        val pendingIntent = PendingIntent.getBroadcast(
            this, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("공과금 mainactivity", position.toString())

        val toastMessage = if (true) {
            val cal = Calendar.getInstance()
            val month = (cal.get(Calendar.MONTH))
            val year = (cal.get(Calendar.YEAR))

            for (i in year until year+10) {
                if (term==0) {
                    for (j in 0 until 12) {
                        setDateFormat(alarmManager, i, j, day, pendingIntent)
                    }
                }
                else if(term==1) {
                    if (month == 0 || month%2 == 0) {
                        for (j in 0 until 12 step(2)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                    else {
                        for (j in 1 until 12 step(2)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                }
                else {
                    if (month == 0 || month%4 == 0) {
                        for (j in 0 until 12 step(4)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                    else if (month%4 == 1) {
                        for (j in 1 until 12 step(4)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                    else if (month%4 == 2) {
                        for (j in 2 until 12 step(4)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                    else {
                        for (j in 2 until 12 step(4)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                }
            }
            "알림이 설정되었습니다."
        } else {
            alarmManager.cancel(pendingIntent)
            "알림이 설정되지 않았습니다."
        }
        Log.d(AlarmBroadcastReceiver.TAG, toastMessage)
        ToastUtil.showToast(this, toastMessage)

    }

    private fun setDateFormat(
        alarmManager: AlarmManager,
        year: Int,
        month: Int,
        day: Int,
        pendingIntent: PendingIntent
    ) {

        val calendar: Calendar = Calendar.getInstance().apply { // 1
            timeInMillis = System.currentTimeMillis()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            Log.d("설정 알람", year.toString()+"년"+month.toString()+"월"+day.toString())
        }

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun delAlarm(position: Int, term: Int, day: Int) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val code = (position.toString()+term.toString()+day.toString()).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            this, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if(pendingIntent!=null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    private val permissionLauncherForStorage = registerForActivityResult(  // 과거에 권한 차단했더라도 무조건 실행 됨
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result: MutableMap<String, Boolean> ->
        Log.d("저장소 권한 없음3", "없음")
        val deniedList: List<String> = result.filter {
            !it.value
        }.map { it.key }
        when {
            deniedList.isNotEmpty() -> {
                val map = deniedList.groupBy { permission ->
                    if (shouldShowRequestPermissionRationale(permission)) "DENIED" else "EXPLAINED"
                }
                Log.d("저장소 권한 없음2", "없음${map}")
                map["DENIED"]?.let {
                    // 뒤로 가기로 거부했을 때
                    // request denied , request again
                    Log.d("저장소 권한", "onRequestPermissionsResult() _ 권한 허용 거부")
                    ToastUtil.showToast(this, "저장소 접근 권한이 없어 해당 기능을 수행할 수 없습니다!")
                }
                map["EXPLAINED"]?.let {
                    // 거부 버튼 눌렀을 때
                    // request denied ,send to settings
                    Log.d("저장소 권한", "한() _ 권한 허용 거부")
                    ToastUtil.showToast(this, "저장소 접근 권한이 없어 해당 기능을 수행할 수 없습니다!")
                }
            }
            else -> { // All request are permitted
                Log.d("저장소 권한", "onRequestPermissionsResult() _ 권한 허용")
                changeFragment(AppTag.PROFILE_TAG)
            }
        }
    }

    fun checkPermissionForStorage(context: Context): Boolean{
        // Android 6.0 Marshmallow 이상에서는 위치 권한에 추가 런타임 권한이 필요
        Log.d("저장소 권한 없음", "들어옴")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
                Log.d("저장소 권한 있음", "있음")
                changeFragment(AppTag.PROFILE_TAG)
                true
            } else {// 권한이 없으므로 권한 요청 알림 보내기
                permissionLauncherForStorage.launch(arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE))
                Log.d("저장소 권한 없음", "없음")
                false
            }
        } else {
            true
        }
    }

    private val permissionLauncherForLocation = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result: MutableMap<String, Boolean> ->
        val deniedList: List<String> = result.filter {
            !it.value
        }.map { it.key }
        when {
            deniedList.isNotEmpty() -> {
                val map = deniedList.groupBy { permission ->
                    if (shouldShowRequestPermissionRationale(permission)) "DENIED" else "EXPLAINED"
                }
                map["DENIED"]?.let {
                    // 뒤로 가기로 거부했을 때
                    // request denied , request again
                    Log.d("위치 권한", "onRequestPermissionsResult() _ 권한 허용 거부")
                    ToastUtil.showToast(this, "위치 권한이 없어 해당 기능을 수행할 수 없습니다!")
                }
                map["EXPLAINED"]?.let {
                    // 거부 버튼 눌렀을 때
                    // request denied ,send to settings
                    Log.d("위치 권한", "한() _ 권한 허용 거부")
                    ToastUtil.showToast(this, "위치 권한이 없어 해당 기능을 수행할 수 없습니다!")
                }
            }
            else -> { // All request are permitted
                Log.d("위치 권한", "onRequestPermissionsResult() _ 권한 허용")
                changeFragment(AppTag.GPS_TAG)
            }
        }
    }

    fun checkPermissionForLocation(context: Context): Boolean {
        // Android 6.0 Marshmallow 이상에서는 위치 권한에 추가 런타임 권한이 필요
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                changeFragment(AppTag.GPS_TAG)
                true
            } else {// 권한이 없으므로 권한 요청 알림 보내기
                permissionLauncherForLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                false
            }
        } else {
            true
        }
    }
}
