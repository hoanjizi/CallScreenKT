package com.vvhoan.callscreenkt

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.os.PowerManager
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.InCallService
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.*
import android.widget.TextView
import kotlinx.android.synthetic.main.service_activity.view.*

@Suppress("DEPRECATION")
class Service : InCallService() {
    private var cursor: Cursor? = null
    private var mView: View? = null
    private var manager: WindowManager? = null
    private var pm: PowerManager? = null
    private var number: String = ""
    private var time: CountDownTimer? = null
    private var tam: Int = 0
    private var phut: Int = 0

    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)
        OnGoingCall.call = call
        number = call!!.details.handle.schemeSpecificPart
        CreateUI(number, call.state)

    }

    private fun Light() {
        pm = baseContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        var isScreenOn: Boolean = false
        isScreenOn = pm!!.isScreenOn
        if (!isScreenOn) {
            var w1: PowerManager.WakeLock? = null
            w1 = pm!!.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "mylock")
            w1!!.acquire(10000)
            var w1_cpu: PowerManager.WakeLock? = null
            w1_cpu = pm!!.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mycpu")
            w1_cpu!!.acquire(10000)
        }
    }

    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        OnGoingCall.call = call
        if (manager != null) {
            manager!!.removeView(mView)
            mView = null
            manager = null
            cursor = null
            time!!.cancel()
            tam = 0
            phut = 0
        }
        pm = null
    }

    @SuppressLint("InflateParams")
    fun CreateUI(number: String, state: Int) {
        mView = LayoutInflater.from(this).inflate(R.layout.service_activity, null)
        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        )
        val dispalyMetris = DisplayMetrics()
        manager!!.defaultDisplay.getMetrics(dispalyMetris)
        val height: Int = dispalyMetris.heightPixels
        params.gravity = height / 2; Gravity.CENTER
        time = object : CountDownTimer(8000 * 60 * 60, 1000) {
            override fun onFinish() {
            }

            override fun onTick(p0: Long) {
                tam += 1
                mView!!.time.text = tam.toString()
                if (tam == 60) {
                    ++phut
                    mView!!.timep.text = phut.toString()
                    tam = 0
                }
            }
        }
        if (mView != null && state == Call.STATE_CONNECTING) {
            mView!!.answer.visibility = View.INVISIBLE
        }
        manager!!.addView(mView, params)
        Light()
        touch(params)
        updateUI(number)

    }

    private fun updateUI(number: String) {
        mView!!.callInfo.text = number
        mView!!.answer.setOnClickListener(View.OnClickListener {
            OnGoingCall.answer()
            mView!!.answer.visibility=View.INVISIBLE
        })
        mView!!.hangup.setOnClickListener(View.OnClickListener {
            OnGoingCall.hangup()
            time!!.cancel()
            tam = 0
            manager!!.removeView(mView)
            mView = null
            manager = null
            cursor = null
        })
        cursor = getCusor()
        if (cursor != null) {
            while (cursor!!.moveToNext()) {
                if (cursor!!.getString(cursor!!.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).trim { it <= ' ' } == number.trim { it <= ' ' }) {
                    val e = cursor!!.getString(cursor!!.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    (mView!!.findViewById(R.id.contact) as TextView).text = e
                }
            }
        }
    }

    private fun getCusor(): Cursor? {
        return contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)
    }


    fun touch(paras: WindowManager.LayoutParams) {

        mView!!.container.setOnTouchListener(object : View.OnTouchListener {
            var initialX: Int = 0
            var initialY: Int = 0
            var initialTouchX: Float = 0.0f
            var initialTouchY: Float = 0.0f
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                when (event!!.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = paras.x
                        initialY = paras.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE ->{
                        var last = initialY + (event.rawY - initialTouchY).toInt()
                        paras.x = initialX + (event.rawX-initialTouchX).toInt()
                        paras.y = last
                        manager!!.updateViewLayout(mView,paras)
                        return true
                    }
                }
                return false
            }

        })
    }

    inner class MyPhoneStateListener() : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            if (state == 2) {
                time!!.start()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val tel: TelephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val phone = MyPhoneStateListener()
            tel.listen(phone, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
