package co.id.mmediaplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.IOException
import java.lang.ref.WeakReference

class MediaService : Service(), MediaPlayerCallback {

    private var isReady: Boolean = false
    private var mMediaPlayer: MediaPlayer? = null
    private val TAG = MediaService::class.java.simpleName

    companion object {
        const val ACTION_CREATE = "co.id.mmediaplayer.mediaservice.create"
        const val ACTION_DESTROY = "co.id.mmediaplayer.mediaservice.destroy"
        const val PLAY = 0
        const val STOP = 1
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (action != null) {
            when (action) {
                ACTION_CREATE -> if (mMediaPlayer == null) {
                    init()
                }
                ACTION_DESTROY -> if (mMediaPlayer?.isPlaying as Boolean) {
                    stopSelf()
                }
                else -> {
                    init()
                }
            }
        }
        Log.d(TAG, "onStartCommand: ")
        return flags
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind: ")
        return mMessenger.binder
    }
    private val mMessenger = Messenger(IncomingHandler(this))

    internal class IncomingHandler(playerCallback: MediaPlayerCallback) : Handler() {
        private val mediaPlayerCallbackWeakReference: WeakReference<MediaPlayerCallback> = WeakReference(playerCallback)
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                PLAY -> mediaPlayerCallbackWeakReference.get()?.onPlay()
                STOP -> mediaPlayerCallbackWeakReference.get()?.onStop()
                else -> super.handleMessage(msg)
            }
        }
    }

    private fun init() {
        mMediaPlayer = MediaPlayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attribute = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            mMediaPlayer?.run {
                setAudioAttributes(attribute)
            }
        } else {
            mMediaPlayer?.run { setAudioStreamType(AudioManager.STREAM_MUSIC) }
        }
        val afd = applicationContext.resources.openRawResourceFd(R.raw.guitar_background)
        try {
            mMediaPlayer?.run { setDataSource(afd.fileDescriptor, afd.startOffset, afd.length) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mMediaPlayer?.setOnPreparedListener(){
            isReady = true
            mMediaPlayer?.start()
            showNotif()
        }
        mMediaPlayer?.setOnErrorListener { mp, what, extra ->
            false
        }
    }

    override fun onPlay() {
        if (!isReady) {
            mMediaPlayer?.prepareAsync()
        } else {
            if (mMediaPlayer?.isPlaying as Boolean) {
                mMediaPlayer?.pause()
            } else {
                mMediaPlayer?.start()
                showNotif()
            }
        }
    }

    private fun showNotif() {
        val CHANNEL_DEFAULT_IMPORTANCE = "Channel_Test"
        val ONGOING_NOTIFICATION_ID = 1

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
            .setContentTitle("TES1")
            .setContentText("TES2")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setTicker("TES3")
            .build()

        createChannel(CHANNEL_DEFAULT_IMPORTANCE)

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun createChannel(CHANNEL_ID: String) {
        val mNotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Battery",
                NotificationManager.IMPORTANCE_DEFAULT)
            channel.setShowBadge(false)
            channel.setSound(null, null)
            mNotificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStop() {
        if (mMediaPlayer?.isPlaying as Boolean || isReady) {
            mMediaPlayer?.stop()
            isReady = false
            stopNotif()
        }
    }

    private fun stopNotif() {
        stopForeground(false)
    }
}
