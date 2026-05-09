package com.gamerec.pro

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color

class FloatingControlService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isPaused = false

    private var frameCount = 0
    private var lastTime = 0L
    private val choreographer = Choreographer.getInstance()
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val currentTime = System.currentTimeMillis()
            frameCount++

            if (currentTime - lastTime >= 1000) {
                val fps = frameCount
                val tvFps = floatingView?.findViewById<TextView>(R.id.tvFps)
                tvFps?.text = "$fps FPS"
                tvFps?.setTextColor(when {
                    fps >= 50 -> Color.GREEN
                    fps >= 30 -> Color.YELLOW
                    else -> Color.RED
                })
                
                frameCount = 0
                lastTime = currentTime
            }
            choreographer.postFrameCallback(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        showFloatingController()
        lastTime = System.currentTimeMillis()
        choreographer.postFrameCallback(frameCallback)
    }

    private fun showFloatingController() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_overlay, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        val imgBubble = floatingView?.findViewById<ImageView>(R.id.imgBubble)
        val controlBar = floatingView?.findViewById<LinearLayout>(R.id.layoutControlBar)
        val btnPauseResume = floatingView?.findViewById<ImageButton>(R.id.btnPauseResume)
        val btnStop = floatingView?.findViewById<ImageButton>(R.id.btnStopRecord)

        imgBubble?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var lastX = 0f
            private var lastY = 0f
            private var startTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        lastX = event.rawX
                        lastY = event.rawY
                        startTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - lastX).toInt()
                        params.y = initialY + (event.rawY - lastY).toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (System.currentTimeMillis() - startTime < 200) {
                            controlBar?.visibility = if (controlBar?.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        }
                        return true
                    }
                }
                return false
            }
        })

        btnPauseResume?.setOnClickListener {
            val intent = Intent(this, ScreenRecordService::class.java)
            if (!isPaused) {
                intent.action = ScreenRecordService.ACTION_PAUSE
                btnPauseResume.setImageResource(android.R.drawable.ic_media_play)
                isPaused = true
            } else {
                intent.action = ScreenRecordService.ACTION_RESUME
                btnPauseResume.setImageResource(android.R.drawable.ic_media_pause)
                isPaused = false
            }
            startService(intent)
        }

        btnStop?.setOnClickListener {
            val intent = Intent(this, ScreenRecordService::class.java)
            intent.action = ScreenRecordService.ACTION_STOP
            startService(intent)
            stopSelf()
        }

        windowManager?.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameCallback)
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
    }
}
