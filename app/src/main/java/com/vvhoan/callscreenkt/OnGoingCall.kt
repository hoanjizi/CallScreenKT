package com.vvhoan.callscreenkt

import android.telecom.Call
import android.telecom.VideoProfile

object OnGoingCall {
    var call :Call? = null
    fun answer(){
        call!!.answer(VideoProfile.STATE_AUDIO_ONLY)
    }
    fun hangup(){
        call!!.disconnect()
    }
}