package eu.ctwoon.otazer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.properties.Delegates

class Checker : CoroutineScope by MainScope() {
    var needDownload: Boolean by Delegates.notNull()
    lateinit var version: String
    lateinit var changelog: String
    lateinit var handler: CoroutineExceptionHandler

    @SuppressLint("ServiceCast")
    fun check(context: Context) {
        handler = CoroutineExceptionHandler { _, exception ->
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            val activeNetworkInfo = connectivityManager!!.activeNetworkInfo
            if (activeNetworkInfo != null) {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("error")
                    .setMessage(exception.message)
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.cancel()
                    }
                builder.show()
            }
        }
        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context)
        if (prefs.getInt("total", 0) == 1) {
            val i = 1
            checkBS(context, i) { needDownload, changelog, version ->
                if (needDownload)
                    createNotif(context, i, changelog, version)
            }
        } else if (prefs.getInt("total", 0) == 0)
            return
        else {
            for (i in 1..prefs.getInt("total", 0)) {
                checkBS(context, i) { needDownload, changelog, version ->
                    if (needDownload)
                        createNotif(context, i, changelog, version)
                }
            }
        }
    }

    fun createNotif(context: Context, i: Int, changelog: String, version: String) {
        Log.d("otazer", "notif")
        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context)
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "channel01", "name",
                NotificationManager.IMPORTANCE_HIGH
            ) // for heads up notifications

            channel.description = "description"

            val notificationManagerr: NotificationManager? =
                context.getSystemService(NotificationManager::class.java)

            notificationManagerr!!.createNotificationChannel(channel)
        }

        val intentView = Intent(Intent.ACTION_VIEW)
            .setData(Uri.parse(prefs.getString("url_$i", null) + "/releases/latest"))

        val pendingIntentView = PendingIntent.getActivity(
            context, 0,
            intentView, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(context, "channel01")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(
                context.getString(R.string.found) + "" + prefs.getString(
                    "name_$i",
                    null
                ) + " • " + version
            )
            .setContentText(changelog)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.check_out),
                pendingIntentView
            )
            .build()

        val notificationManager = NotificationManagerCompat.from(context)

        notificationManager.notify(i, notification)
    }

    fun checkBS(context: Context, i: Int, callback: (Boolean, String, String) -> Unit) {
        launch(handler) {
            try {
                val prefs: SharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(context)
                val string = prefs.getString("url_$i", null)
                var str: Array<String>? = string?.split("/")?.toTypedArray()
                withContext(Dispatchers.IO) {

                    val ed: SharedPreferences.Editor = prefs.edit()
                    val request: Request =
                        Request.Builder()
                            .url("https://api.github.com/repos/" + str?.get(3) + "/" + str?.get(4) + "/releases/latest")
                            .build()
                    val response = OkHttpClient().newCall(request).execute()
                    val parsedString = JSONObject(response.body!!.string())
                    if (parsedString.getString("name") != prefs.getString("version_$i", null)) {
                        version = parsedString.getString("name")
                        ed.putString("version_$i", version)
                        ed.apply()
                        needDownload = true
                        changelog = parsedString.getString("body")
                        callback.invoke(needDownload, changelog, version)
                    }
                    return@withContext
                }
            } catch (e: Exception) {
                throw RuntimeException(e.message)
            }
        }
    }
}