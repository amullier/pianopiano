package fr.antmu.pianopiano.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import fr.antmu.pianopiano.MainActivity
import fr.antmu.pianopiano.PianoPianoApp
import fr.antmu.pianopiano.R
import fr.antmu.pianopiano.ui.pause.PauseOverlayView

class PauseOverlayService : Service(), LifecycleOwner {

    private var windowManager: WindowManager? = null
    private var overlayView: PauseOverlayView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)

    companion object {
        const val ACTION_SHOW = "fr.antmu.pianopiano.action.SHOW"
        const val ACTION_HIDE = "fr.antmu.pianopiano.action.HIDE"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_IS_PERIODIC = "extra_is_periodic"

        @Volatile
        var isShowing = false
            private set
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return START_NOT_STICKY
                val isPeriodic = intent.getBooleanExtra(EXTRA_IS_PERIODIC, false)
                startForeground(PianoPianoApp.NOTIFICATION_ID, createNotification())
                showOverlay(packageName, isPeriodic)
            }
            ACTION_HIDE -> {
                hideOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(packageName: String, isPeriodic: Boolean = false) {
        if (isShowing) return

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        overlayView = PauseOverlayView(this, this) {
            hideOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager?.addView(overlayView, layoutParams)
        overlayView?.show(packageName, isPeriodic)
        isShowing = true
    }

    private fun hideOverlay() {
        overlayView?.let { view ->
            view.cleanup()
            try {
                windowManager?.removeView(view)
            } catch (e: IllegalArgumentException) {
                // View not attached
            }
        }
        overlayView = null
        isShowing = false
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, PianoPianoApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }
}
