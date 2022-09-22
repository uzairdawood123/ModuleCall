package com.vdotok.streaming.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.*


/**
 * Created By: VdoTok
 * Date & Time: On 8/24/21 At 4:44 PM in 2021
 */
object FileUtils {

    fun removeFile(context: Context) {
        val root = File(context.filesDir, CRASH_DIR_NAME)
        val gpxfile = File(root, "stack.trace")
        if (gpxfile.exists()) {
            if (gpxfile.delete()) {
                println("file Deleted :" + gpxfile.path)
            } else {
                println("file not Deleted :" + gpxfile.path)
            }
        }
    }


    private val stringsOrNulls = arrayOfNulls<String>(10)
    fun stackOverflow() {
        try {
            stringsOrNulls[11] = "abc"
        } catch (ex: Exception) {
            throw Exception(ex)
        }
    }

    fun crashFileExists(context: Context): Boolean {
        val crashFile = File(context.filesDir, "$CRASH_DIR_NAME/$CRASH_FILE_NAME")
        return crashFile.exists()
    }

    fun readCrashFile(context: Context): String {
        val root = File(context.filesDir, CRASH_DIR_NAME)
        if (root.exists()) {
            val gpxfile = File(root, CRASH_FILE_NAME)
            val text = StringBuilder()
            try {
                val br = BufferedReader(FileReader(gpxfile))
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    line?.let {
                        if (!it.contains("Begin Stack trace") && !it.contains("End Stack trace")) {
                            text.append(line)
                            text.append('\n')
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("FileReadError", "Error reading file!")
                e.printStackTrace()
            } catch (e: FileNotFoundException) {
                Log.e("FileReadError", "File not found!")
                e.printStackTrace()
            }
            return text.toString()
        } else {
            return ""
        }
    }

}