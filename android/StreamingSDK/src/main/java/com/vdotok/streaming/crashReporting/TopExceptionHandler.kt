package com.vdotok.streaming.crashReporting

import android.content.Context
import com.vdotok.streaming.utils.CRASH_DIR_NAME
import com.vdotok.streaming.utils.CRASH_FILE_NAME
import java.io.File
import java.io.RandomAccessFile
import java.lang.Thread.getDefaultUncaughtExceptionHandler

/**
 * Created By: VdoTok
 * Date & Time: On 8/24/21 At 3:50 PM in 2021
 */
class TopExceptionHandler(private val app: Context) : Thread.UncaughtExceptionHandler {

    private val defaultUEH: Thread.UncaughtExceptionHandler = getDefaultUncaughtExceptionHandler()

//    override fun uncaughtException(t: Thread?, e: Throwable) {
//        var arr = e.stackTrace
//        var report = e.toString() + "\n\n"
//        report += "--------- Stack trace ---------\n\n"
//        for (i in arr.indices) {
//            report += " " + arr[i].toString() + "\n";
//        }
//        report += "-------------------------------\n\n"
//        // If the exception was thrown in a background thread inside
//        // AsyncTask, then the actual exception can be found with getCause
//        report += "--------- Cause ---------\n\n"
//        val cause = e.cause
//        if (cause != null) {
//            report += cause.toString() + "\n\n"
//            arr = cause.stackTrace
//            for (i in arr.indices) {
//                report += arr[i].toString() + "\n"
//            }
//        }
//        report += "-------------------------------\n\n"
//        val root = File(app.filesDir, CRASH_DIR_NAME)
//        if (!root.exists()) {
//            root.mkdirs()
//        }
//        val gpxfile = File(root, "stack.trace")
//        val fileLength: Long = gpxfile.length()
//        val raf = RandomAccessFile(gpxfile, "rw")
//        raf.seek(fileLength)
//        raf.write(report.toByteArray())
//        raf.close()
////        try {
////            val trace: FileOutputStream = app.openFileOutput(
////                "stack.trace",
////                Context.MODE_PRIVATE
////            )
////            val writer = OutputStreamWriter(trace)
////            writer.append(report)
////            writer.close()
//////            trace.write(report.toByteArray())
////            trace.close()
////        } catch (ioe: IOException) {
////            ioe.toString()
////        }
//        defaultUEH.uncaughtException(t, e)
//    }

    override fun uncaughtException(t: Thread?, e: Throwable) {
        var arr = e.stackTrace
        var report = "Begin Stack trace\n"
        report += e.toString() + "\n"
        for (i in arr.indices) {
            report += " " + arr[i].toString() + "\n";
        }
        report += "End Stack trace\n\n"
        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
//        report += "--------- Cause ---------\n\n"
//        val cause = e.cause
//        if (cause != null) {
//            report += cause.toString() + "\n\n"
//            arr = cause.stackTrace
//            for (i in arr.indices) {
//                report += arr[i].toString() + "\n"
//            }
//        }
//        report += "-------------------------------\n\n"
        val root = File(app.filesDir, CRASH_DIR_NAME)
        if (!root.exists()) {
            root.mkdirs()
        }
        val gpxfile = File(root, CRASH_FILE_NAME)
        val fileLength: Long = gpxfile.length()
        val raf = RandomAccessFile(gpxfile, "rw")
        raf.seek(fileLength)
        raf.write(report.toByteArray())
        raf.close()
        defaultUEH.uncaughtException(t, e)
    }
}
