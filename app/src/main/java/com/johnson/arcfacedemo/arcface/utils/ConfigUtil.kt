package com.johnson.arcfacedemo.arcface.utils

import android.content.Context
import android.content.SharedPreferences

import com.arcsoft.face.FaceEngine

object ConfigUtil {
    private val APP_NAME = "ArcFaceDemo"
    private val TRACK_ID = "trackID"
    private val FT_ORIENT = "ftOrient"

    fun setTrackId(context: Context?, trackId: Int?) {
        if (context == null) {
            return
        }
        val sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putInt(TRACK_ID, trackId ?: -1)
            .apply()
    }

    fun getTrackId(context: Context?): Int {
        if (context == null) {
            return 0
        }
        val sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(TRACK_ID, 0)
    }

    fun setFtOrient(context: Context?, ftOrient: Int) {
        if (context == null) {
            return
        }
        val sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putInt(FT_ORIENT, ftOrient)
            .apply()
    }

    fun getFtOrient(context: Context?): Int {
        if (context == null) {
            return FaceEngine.ASF_OP_270_ONLY
        }
        val sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(FT_ORIENT, FaceEngine.ASF_OP_0_HIGHER_EXT)
    }
}
