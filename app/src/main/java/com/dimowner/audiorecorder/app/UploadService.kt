package com.dimowner.audiorecorder.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dimowner.audiorecorder.ARApplication
import com.dimowner.audiorecorder.AppConstants
import com.dimowner.audiorecorder.R
import com.dimowner.audiorecorder.app.main.MainActivity
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class UploadService : Service() {

    companion object {
        const val ACTION_START_UPLOAD_SERVICE = "ACTION_START_UPLOAD_SERVICE"
        const val ACTION_STOP_UPLOAD_SERVICE = "ACTION_STOP_UPLOAD_SERVICE"
        const val EXTRAS_KEY_UPLOAD_PATH = "key_upload_path"

        private const val CHANNEL_ID = "com.dimowner.audiorecorder.Upload.Notification"
        private const val NOTIF_ID = 105

        @JvmStatic
        fun start(context: Context, filePath: String) {
            val intent = Intent(context, UploadService::class.java)
            intent.action = ACTION_START_UPLOAD_SERVICE
            intent.putExtra(EXTRAS_KEY_UPLOAD_PATH, filePath)
            context.startService(intent)
        }
    }

    private lateinit var notificationManager: NotificationManagerCompat
    private var lastProgressEmitMs: Long = 0
    private var lastProgressPercent: Int = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Default", NotificationManager.IMPORTANCE_DEFAULT)
            channel.setSound(null, null)
            channel.enableVibration(false)
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && ACTION_START_UPLOAD_SERVICE == intent.action) {
            val path = intent.getStringExtra(EXTRAS_KEY_UPLOAD_PATH)
            if (path.isNullOrEmpty()) {
                stopSelf()
                return START_NOT_STICKY
            }
            startForegroundCompat(buildNotification(getString(R.string.uploading_with_name, File(path).name)))
            showToast(getString(R.string.uploading_with_name, File(path).name))
            ARApplication.injector.provideLoadingTasksQueue().postRunnable {
                doUpload(path)
            }
        } else if (intent != null && ACTION_STOP_UPLOAD_SERVICE == intent.action) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startForeground(NOTIF_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
        val contentPendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, AppConstants.PENDING_INTENT_FLAGS)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_share)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    private fun updateNotificationText(text: String) {
        notificationManager.notify(NOTIF_ID, buildNotification(text))
    }

    private fun doUpload(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            notificationManager.notify(NOTIF_ID, buildNotification(getString(R.string.upload_failed)))
            stopSelf()
            return
        }
        val nameLower = file.name.lowercase()
        val isSupported = nameLower.endsWith(".mp3") || nameLower.endsWith(".wav") || nameLower.endsWith(".m4a") || nameLower.endsWith(".mp4") || nameLower.endsWith(".m4v")
        if (!isSupported) {
            val msg = getString(R.string.upload_failed) + ": Formato não suportado. Use MP3, WAV, M4A, MP4 ou M4V"
            notificationManager.notify(NOTIF_ID, buildNotification(msg))
            showToast(msg)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return
        }
        try {
            updateNotificationText(getString(R.string.uploading))

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            // Use precise content type
            val chosenMime = guessAudioMimeType(file.name)
            val mediaType = chosenMime.toMediaType()
            val progressBody = ProgressRequestBody(file, mediaType) { percent ->
                updateProgress(file.name, percent)
            }
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio", file.name, progressBody)
                .build()

            val request = Request.Builder()
                .url("https://roscas-minuta.onrender.com/api/upload?provider=assemblyai")
                .post(body)
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                val bodyStr = response.body?.string() ?: ""
                if (code in 200..299) {
                    Timber.i("Upload success: %s", bodyStr)
                    notificationManager.notify(NOTIF_ID, buildNotification(getString(R.string.upload_success)))
                    showToast(getString(R.string.upload_success))
                } else {
                    Timber.e("Upload failed: %d, body: %s", code, bodyStr)
                    notificationManager.notify(NOTIF_ID, buildNotification(getString(R.string.upload_failed)))
                    val snippet = if (bodyStr.length > 200) bodyStr.substring(0, 200) + "…" else bodyStr
                    showToast(getString(R.string.upload_failed) + ": " + snippet +
                        "\nfile=" + file.name + ", mime=" + chosenMime)
                }
            }
        } catch (t: Throwable) {
            Timber.e(t)
            notificationManager.notify(NOTIF_ID, buildNotification(getString(R.string.upload_failed)))
            showToast(getString(R.string.upload_failed) + ": " + (t.message ?: ""))
        } finally {
            // Clear foreground and stop
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
    }

    private fun updateProgress(fileName: String, percent: Int) {
        val now = System.currentTimeMillis()
        val shouldEmit = (percent == 100) ||
                (percent - lastProgressPercent >= 5) ||
                (now - lastProgressEmitMs >= 750)
        if (!shouldEmit) return
        lastProgressPercent = percent
        lastProgressEmitMs = now
        notificationManager.notify(
            NOTIF_ID,
            buildNotification("${getString(R.string.uploading_with_name, fileName)}  $percent%")
        )
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            android.widget.Toast.makeText(applicationContext, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun guessAudioMimeType(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".m4a") -> "audio/mp4"
            lower.endsWith(".mp4") -> "video/mp4"
            lower.endsWith(".m4v") -> "video/mp4"
            lower.endsWith(".aac") -> "audio/aac"
            lower.endsWith(".wav") -> "audio/wav"
            lower.endsWith(".ogg") || lower.endsWith(".oga") -> "audio/ogg"
            lower.endsWith(".3gp") || lower.endsWith(".3gpp") -> "audio/3gpp"
            lower.endsWith(".amr") -> "audio/amr"
            else -> "application/octet-stream"
        }
    }
}

private class ProgressRequestBody(
    private val file: File,
    private val contentType: MediaType,
    private val onProgress: (Int) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType = contentType

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: okio.BufferedSink) {
        val length = contentLength()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        file.inputStream().use { input ->
            var uploaded = 0L
            var read: Int
            var lastEmit = 0
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                val percent = if (length > 0L) ((uploaded * 100) / length).toInt() else 0
                if (percent != lastEmit) {
                    lastEmit = percent
                    onProgress(percent)
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8 * 1024
    }
}




