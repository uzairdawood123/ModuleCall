package com.vdotok.callingappdemo.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

import com.google.gson.Gson
import com.vdotok.icsdks.network.models.responseModels.User


/**
 * Created By: VdoTok
 * Date & Time: On 2/16/21 At 12:14 PM in 2021
 */
class Prefs(context: Context?) {

    private val mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val REGISTERED_USER_INFO = "REGISTERED_USER_INFO"

    var userRegisterInfo: User?
        get(){
            val gson = Gson()
            val json = mPrefs.getString(REGISTERED_USER_INFO, "")
            return gson.fromJson(json, User::class.java)
        }
        set(userInfo) {
            val mEditor: SharedPreferences.Editor = mPrefs.edit()
            val gson = Gson()
            val json = gson.toJson(userInfo)
            mEditor.putString(REGISTERED_USER_INFO, json)
            mEditor.apply()
        }

    /**
     * Function to save a list of any type in prefs
     * */
    fun <T> setList(key: String?, list: List<T>?) {
        val gson = Gson()
        val json = gson.toJson(list)
        set(key, json)
    }

    /**
     * Function to save a simple key value pair in prefs
     * */
    operator fun set(key: String?, value: String?) {
        val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
        prefsEditor.putString(key, value)
        prefsEditor.apply()
    }

    /**
     * Function to clear all prefs from storage
     * */
    fun clearAll(){
        mPrefs.edit().clear().apply()
    }

}