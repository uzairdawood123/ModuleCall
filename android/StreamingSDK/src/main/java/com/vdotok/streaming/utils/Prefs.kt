package com.vdotok.streaming.utils


import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Created By: VdoTok
 * Date & Time: On 1/20/21 At 3:31 PM in 2021
 *
 * This class is mainly used to locally store and use data in the application
 * @param context the context of the application or the activity from where it is called
 */
class Prefs(context: Context?) {
    private val mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val PROJECT_ID = "projectID"
    private val MC_TOKEN = "mcToken"
    private val TIME_INTERVAL = "time_interval"
    private val PING_INTERVAL = "ping_interval"
    private val USER_REF_ID = "user_ref_id"

    var projectID: String?
        get() {
            return mPrefs.getString(PROJECT_ID, "")
        }
        set(projectID) {
            val mEditor: SharedPreferences.Editor = mPrefs.edit()
            mEditor.putString(PROJECT_ID, projectID)
            mEditor.apply()
        }

    var mcToken: String?
        get() {
            return mPrefs.getString(MC_TOKEN, "")
        }
        set(mcToken) {
            val mEditor: SharedPreferences.Editor = mPrefs.edit()
            mEditor.putString(MC_TOKEN, mcToken)
            mEditor.apply()
        }

    var ownRefId: String?
        get() {
            return mPrefs.getString(USER_REF_ID, "")
        }
        set(ownRefId) {
            val mEditor: SharedPreferences.Editor = mPrefs.edit()
            mEditor.putString(USER_REF_ID, ownRefId)
            mEditor.apply()
        }

    var timeInterval: Int
        get() {
            return mPrefs.getInt(TIME_INTERVAL, 0)
        }
        set(timeInterval) {
            val mEditor: SharedPreferences.Editor = mPrefs.edit()
            mEditor.putInt(TIME_INTERVAL, timeInterval)
            mEditor.apply()
        }

    var pingInterval: Int
        get() {
            return mPrefs.getInt(PING_INTERVAL, 0)
        }
        set(pingInterval) {
            val mEditor: SharedPreferences.Editor = mPrefs.edit()
            mEditor.putInt(PING_INTERVAL, pingInterval)
            mEditor.apply()
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
    fun clearAll() {
        mPrefs.edit().clear().apply()
    }
}
