package co.id.mmediaplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var mMediaPlayer: MediaPlayer? = null
    private var isReady: Boolean = false

    private val TAG = MainActivity::class.java.simpleName
    private var mService: Messenger? = null
    private lateinit var mBoundServiceIntent: Intent
    private var mServiceBound = false

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mServiceBound = false
        }
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = Messenger(service)
            mServiceBound = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()

        btn_play.setOnClickListener(this)
        btn_stop.setOnClickListener(this)

        mBoundServiceIntent = Intent(this@MainActivity, MediaService::class.java)
        mBoundServiceIntent.action = MediaService.ACTION_CREATE
        startService(mBoundServiceIntent)
        bindService(mBoundServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun init() {
        mMediaPlayer = MediaPlayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attribute = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            mMediaPlayer?.setAudioAttributes(attribute)
        } else {
            mMediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }
        val afd = applicationContext.resources.openRawResourceFd(R.raw.guitar_background)
        try {
            mMediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mMediaPlayer?.setOnPreparedListener({
            isReady = true
            mMediaPlayer?.start()
        })
        mMediaPlayer?.setOnErrorListener({ mp, what, extra -> false })
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_play -> {
                if (!mServiceBound) return
                try {
                    mService?.send(Message.obtain(null, MediaService.PLAY, 0, 0))
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
            R.id.btn_stop -> {
                if (!mServiceBound) return
                try {
                    mService?.send(Message.obtain(null, MediaService.STOP, 0, 0))
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
            else -> {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        unbindService(mServiceConnection)
        mBoundServiceIntent.action = MediaService.ACTION_DESTROY

        startService(mBoundServiceIntent)
    }
}