package kr.co.ajjulcoding.team.project.holo.view.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kr.co.ajjulcoding.team.project.holo.AppTag
import kr.co.ajjulcoding.team.project.holo.base.BaseFragment
import kr.co.ajjulcoding.team.project.holo.view.activity.MainActivity
import kr.co.ajjulcoding.team.project.holo.databinding.FragmentGpsBinding
import kr.co.ajjulcoding.team.project.holo.util.ToastUtil
import java.io.IOException
import java.util.*


class GpsFragment : BaseFragment<FragmentGpsBinding>(), OnMapReadyCallback {
    private var gMap: GoogleMap? = null
    private var latitude:Double = 37.568291
    private var longitude:Double = 126.997780
    private var validCpl:Boolean = false
    private var lastestLocation:SpannableString? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null // 현재 위치를 가져오기 위한 변수
    private lateinit var town:String
    lateinit var mLastLocation: Location // 위치 값을 가지고 있는 객체
    internal lateinit var mLocationRequest: LocationRequest // 위치 정보 요청의 매개변수를 저장하는

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mLocationRequest =  LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGpsBinding {
        return FragmentGpsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        updateLocation()

        binding.btnBack.setOnClickListener {
            (requireActivity() as MainActivity).changeFragment(AppTag.SETTING_TAG)
        }
        binding.btnMap.setOnClickListener {
            updateLocation()
            val marker = LatLng(latitude, longitude)
            gMap!!.moveCamera(CameraUpdateFactory.zoomTo(15f))
            gMap!!.addMarker(MarkerOptions().position(marker).title("내 위치"))
            gMap!!.moveCamera(CameraUpdateFactory.newLatLng(marker))
        }
        lastestLocation?.let {
            binding.textLocation.setText(it)
        }
        binding.btnCpl.setOnClickListener {
            if (validCpl != true) {
                ToastUtil.showToast(requireContext(), "현재위치가 업데이트되지 않았습니다.")
            }else{
                Log.d("동네 이름", "$town")
                (requireActivity() as MainActivity).setLocationToHome(town)
                (requireActivity() as MainActivity).changeFragment(AppTag.SETTING_TAG)
            }
        }
    }

    private fun updateLocation(){
        //FusedLocationProviderClient의 인스턴스를 생성.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(requireActivity(),Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        // 기기의 위치에 관한 정기 업데이트를 요청하는 메서드 실행
        // 지정한 루퍼 스레드(Looper.myLooper())에서 콜백(mLocationCallback)으로 위치 업데이트를 요청
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    // 시스템으로 부터 위치 정보를 콜백으로 받음
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // 시스템에서 받은 location 정보를 onLocationChanged()에 전달
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    // 시스템으로 부터 받은 위치정보를 화면에 갱신해주는 메소드
    fun onLocationChanged(location: Location) {
        mLastLocation = location
        latitude = mLastLocation.latitude // 갱신 된 위도
        longitude = mLastLocation.longitude // 갱신 된 경도
        Log.d("변경 위치", "동작")
        convertAddress()
        val marker = LatLng(latitude, longitude)
        gMap?.uiSettings?.isMapToolbarEnabled = false
        gMap?.addMarker(MarkerOptions().position(marker).title("내 위치"))
        gMap?.moveCamera(CameraUpdateFactory.newLatLng(marker))
        gMap?.moveCamera(CameraUpdateFactory.zoomTo(15f))
    }

    // Default 위치 설정
    override fun onMapReady(googleMap: GoogleMap?) {
        val marker = LatLng(37.568291,126.997780)
        Log.d("디폴트 위치", "동작")
        gMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        )
        gMap!!.uiSettings.isMapToolbarEnabled = false
        gMap!!.addMarker(MarkerOptions().position(marker).title("여기"))
        gMap!!.moveCamera(CameraUpdateFactory.newLatLng(marker))
        gMap!!.moveCamera(CameraUpdateFactory.zoomTo(15f))
    }

    private fun convertAddress(){
        try {
            val g:Geocoder = Geocoder(context,Locale.KOREA)
            val address = g.getFromLocation(latitude, longitude, 5)
            if (address.size == 0){
                ToastUtil.showToast(requireActivity(), "해당되는 주소정보가 없습니다.")
            }
            else{
                validCpl = true
                town = "수신 실패"
                for (ad in address){
                    Log.d("주소 정보", address.toString())
                    if (ad.thoroughfare != null && ad.thoroughfare.length > 0){
                        town = ad.thoroughfare
                        break
                    }
                }
                val showTextAll: String = "\"현재 위치는 "+ town +" 입니다.\""
                val startTown = showTextAll.indexOf(town)
                val endTown = startTown+town.length
                val spannableString = SpannableString(showTextAll)

                spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#1D4999")),
                    startTown,endTown, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannableString.setSpan(StyleSpan(Typeface.BOLD)
                    , startTown, endTown, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#1D4999")),
                    0,1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#1D4999")),
                    showTextAll.length-1,showTextAll.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                lastestLocation = spannableString
                binding.textLocation.setText(spannableString)
            }
        }catch (e: IOException){
            Log.d("위치 주소 변환", "오류")
        }
    }
    // 생명 주기 맞춰주기
    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }
    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }
    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }
    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}