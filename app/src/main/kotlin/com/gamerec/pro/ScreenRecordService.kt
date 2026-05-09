package com.gamerec.pro

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import java.io.IOException

class ScreenRecordService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var screenDensity: Int = 0
    private var displayWidth: Int = 1080
    private var displayHeight: Int = 1920
    private var isRecording = false

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "GameRecChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenDensity = metrics.densityDpi
        displayWidth = metrics.widthPixels
        displayHeight = metrics.heightPixels
        
        if (displayWidth % 2 != 0) displayWidth--
        if (displayHeight % 2 != 0) displayHeight--
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                if (data != null) startRecording(resultCode, data)
            }
            ACTION_STOP -> stopRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(resultCode: Int, data: Intent) {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GAME RECORDING ACTIVE")
            .setContentText("Capturing gameplay with custom settings")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setColor(0xB026FF)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        val prefs = getSharedPreferences("GameRecSettings", Context.MODE_PRIVATE)
        val targetRes = prefs.getString("resolution", "1080p") ?: "1080p"
        val targetFps = prefs.getInt("fps", 60)
        val targetBitrate = prefs.getInt("bitrate", 12 * 1024 * 1024)

        val targetHeight = when(targetRes) {
            "720p" -> 720
            "1080p" -> 1080
            "1440p" -> 1440
            else -> 1080
        }

        val ratio = displayWidth.toFloat() / displayHeight.toFloat()
        var recordHeight = targetHeight
        var recordWidth = (targetHeight * ratio).toInt()
        
        if (recordWidth % 2 != 0) recordWidth--
        if (recordHeight % 2 != 0) recordHeight--

        setupMediaRecorder(recordWidth, recordHeight, targetFps, targetBitrate)

        try {
            mediaRecorder?.prepare()
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "GameRec_Display",
                recordWidth, recordHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface, null, null
            )
            mediaRecorder?.start()
            isRecording = true
            
            // Start Floating Controls
            startService(Intent(this, FloatingControlService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupMediaRecorder(width: Int, height: Int, fps: Int, bitrate: Int) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        val videoUri = createVideoUri()
        
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(bitrate)
            setVideoFrameRate(fps)
            setVideoSize(width, height)
            
            if (videoUri != null) {
                val pfd = contentResolver.openFileDescriptor(videoUri, "rw")
                setOutputFile(pfd?.fileDescriptor)
            }
        }
    }

    private fun createVideoUri(): Uri? {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "GameRec_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/GameRecPro")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording) {
            mediaRecorder?.pause()
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording) {
            mediaRecorder?.resume()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {}
        mediaRecorder?.release()
        mediaRecorder = null
        
        virtualDisplay?.release()
        mediaProjection?.stop()
        
        stopService(Intent(this, FloatingControlService::class.java))
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "GameRec Pro Recording Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }
}
