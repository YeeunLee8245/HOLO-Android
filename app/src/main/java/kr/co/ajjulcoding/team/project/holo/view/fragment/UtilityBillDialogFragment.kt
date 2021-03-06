package kr.co.ajjulcoding.team.project.holo.view.fragment


import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.util.GregorianCalendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import kr.co.ajjulcoding.team.project.holo.*
import kr.co.ajjulcoding.team.project.holo.data.HoloUser
import kr.co.ajjulcoding.team.project.holo.data.UtilityBillItem
import kr.co.ajjulcoding.team.project.holo.databinding.FragmentUtilityBillBinding
import kr.co.ajjulcoding.team.project.holo.util.OnItemClick
import kr.co.ajjulcoding.team.project.holo.view.activity.MainActivity
import kr.co.ajjulcoding.team.project.holo.view.adapter.UtilityBillAdapter


class UtilityBillDialogFragment() : DialogFragment(), OnItemClick {
    private lateinit var _activity: MainActivity
    private val mActivity get() = _activity
    private lateinit var _userInfo: HoloUser
    private val userInfo get() = _userInfo
    private var _binding: FragmentUtilityBillBinding? = null
    private val binding get() = _binding!!
    private var mlistView: RecyclerView? = null
    private var mUtilityBillAdapter: UtilityBillAdapter? = null
    private var mUtilityBillItems: ArrayList<UtilityBillItem>? = ArrayList()
    private var mCalender: GregorianCalendar? = null
    private var preTerms: ArrayList<Int>? = null
    private var preDates: ArrayList<Int>? = null
    private var flag: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _activity = requireActivity() as MainActivity
        _userInfo = arguments?.getParcelable<HoloUser>(AppTag.USER_INFO) as HoloUser
        mCalender = GregorianCalendar()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUtilityBillBinding.inflate(inflater, container, false)
        initList()
        return binding.root
    }

    private fun initList(){
        mUtilityBillItems = userInfo.utilitylist
        Log.d("????????? initList", mUtilityBillItems.toString())

        mUtilityBillAdapter = UtilityBillAdapter(this)
        
        if (mUtilityBillItems==null) {
            flag = 1
            mUtilityBillItems=ArrayList()
            mUtilityBillItems!!.add(UtilityBillItem("??????", 0, 1))
            mUtilityBillItems!!.add(UtilityBillItem("?????????", 0, 1))
            mUtilityBillItems!!.add(UtilityBillItem("?????????", 0, 1))
            mUtilityBillItems!!.add(UtilityBillItem("?????????", 0, 1))
            mUtilityBillAdapter!!.setNotificationList(mUtilityBillItems)
            mActivity.storeUtilityCache(mUtilityBillItems!!)
        }
        mUtilityBillItems = mActivity.getUtilityJSON()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("????????? ???????????????", "onViewCreated")

        mlistView = requireView().findViewById(R.id.listView) as RecyclerView?

//        mUtilityBillAdapter = UtilityBillAdapter(this)

        mlistView!!.adapter = mUtilityBillAdapter

        // ???????????? ????????? ???????????? ??????, ?????? ??????
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        mUtilityBillAdapter!!.setNotificationList(mUtilityBillItems)

        if (flag == 0) preValue()

        Log.d("????????? list count", mUtilityBillAdapter!!.getItemCount().toString())

        val appendbtn = binding.BtnAppend

        appendbtn.setOnClickListener() {
            mUtilityBillItems!!.add(UtilityBillItem("", 0, 1))
            mUtilityBillAdapter!!.notifyDataSetChanged()
        }

        binding.dialBtnSet.setOnClickListener {
            if (flag == 0) {
                for(i in 0 until mUtilityBillAdapter!!.getItemCount()) {
                    val term = mUtilityBillAdapter!!.getItemTerm(i)
                    val day = mUtilityBillAdapter!!.getItemDay(i)
                    if (preTerms!![i] != term || preDates!![i] != day) {
                        Log.d("????????? enroll ??????", preDates!![i].toString())
                        delete(i, preTerms!![i], preDates!![i])
                        enroll(i, term, day)
                    }
                    else if (preTerms!![i] == term && preDates!![i] == day) {
                        continue
                    }
                    else {
                        Log.d("????????? enroll ??????", i.toString())
                        enroll(i, term, day)
                    }
                }
            }
            else {
                for(i in 0 until mUtilityBillAdapter!!.getItemCount()) {
                    val term = mUtilityBillAdapter!!.getItemTerm(i)
                    val day = mUtilityBillAdapter!!.getItemDay(i)
                    enroll(i, term, day)
                }
            }
            mActivity.storeUtilityCache(mUtilityBillItems!!)
            dismiss()
        }
        binding.dialBtnExit.setOnClickListener {
            dismiss()   // ??????????????? ?????? ??????
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun enroll(position: Int?, term: Int?, day: Int?) {
        Log.d("????????? fragment", position.toString())
        mActivity.addAlarm(position!!, term!!, day!!)
    }

    fun delete(position: Int?, term: Int?, day: Int?) {
        mActivity.delAlarm(position!!, term!!, day!!)
    }

    fun preValue() {
        preTerms = ArrayList()
        preDates = ArrayList()
        for(i in 0 until mUtilityBillAdapter!!.getItemCount()) {
            val term = mUtilityBillAdapter!!.getItemTerm(i)
            val day = mUtilityBillAdapter!!.getItemDay(i)
            preTerms!!.add(term!!)
            preDates!!.add(day!!)
        }
    }

    override fun onClikDelete(position: Int?) {
        mUtilityBillItems!!.removeAt(position!!)
        mUtilityBillAdapter!!.setNotificationList(mUtilityBillItems)
    }
}