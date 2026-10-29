package com.omnicast.tvsignage.tv

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.gson.JsonObject
import com.omnicast.tvsignage.data.AppPreferences
import com.omnicast.tvsignage.databinding.ActivityTvPlayerBinding
import com.omnicast.tvsignage.utils.NetworkUtils
import java.io.File

class TvPlayerActivity : AppCompatActivity(), TvHttpServer.ServerListener {

    private lateinit var binding: ActivityTvPlayerBinding
    private lateinit var preferences: AppPreferences
    private var exoPlayer: ExoPlayer? = null
    private var httpServer: TvHttpServer? = null
    private var nsdHelper: NsdServerHelper? = null

    private val handler = Handler(Looper.getMainLooper())
    private val hideOverlayRunnable = Runnable {
        binding.overlayInfo.animate().alpha(0f).setDuration(800).withEndAction {
            binding.overlayInfo.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(this)

        // Configurar pantalla completa inmersiva permanente
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        binding = ActivityTvPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupPlayer()
        startLocalServices()

        // Si ya hay un video guardado, reproducirlo de inmediato al abrir o encender
        preferences.defaultVideoUri?.let { savedUri ->
            playMedia(savedUri, preferences.isLoopEnabled)
        }
    }

    private fun setupUI() {
        val ip = NetworkUtils.getLocalIpAddress() ?: "Sin conexión"
        val mac = NetworkUtils.getMacAddress()

        binding.tvDeviceName.text = preferences.tvName
        binding.tvDeviceIp.text = "IP: $ip:8888"
        binding.tvDeviceMac.text = "MAC: $mac"

        // Toque en pantalla o botón de control para volver a mostrar info si se desea
        binding.root.setOnClickListener {
            binding.overlayInfo.alpha = 1f
            binding.overlayInfo.visibility = View.VISIBLE
            handler.removeCallbacks(hideOverlayRunnable)
            handler.postDelayed(hideOverlayRunnable, 6000)
        }
    }

    private fun setupPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            binding.playerView.player = this
            binding.playerView.useController = false // Cero controles visibles

            // Configurar bucle
            repeatMode = if (preferences.isLoopEnabled) {
                Player.REPEAT_MODE_ALL
            } else {
                Player.REPEAT_MODE_OFF
            }

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && isPlaying) {
                        // Ocultar overlay tras 4 segundos de reproducción exitosa
                        handler.removeCallbacks(hideOverlayRunnable)
                        handler.postDelayed(hideOverlayRunnable, 4000)
                    }
                }
            })
        }
    }

    private fun playMedia(uriString: String, loop: Boolean) {
        runOnUiThread {
            try {
                val uri = if (uriString.startsWith("http://") || uriString.startsWith("https://") || uriString.startsWith("content://")) {
                    Uri.parse(uriString)
                } else {
                    Uri.fromFile(File(uriString))
                }

                val mediaItem = MediaItem.fromUri(uri)
                exoPlayer?.let { player ->
                    player.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.playWhenReady = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startLocalServices() {
        try {
            httpServer = TvHttpServer(this, 8888, this).apply {
                start()
            }
            nsdHelper = NsdServerHelper(this).apply {
                registerService(preferences.tvName, 8888)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Callbacks del Servidor HTTP
    override fun onPlayUrlRequested(url: String, loop: Boolean, saveAsDefault: Boolean) {
        if (saveAsDefault) {
            preferences.defaultVideoUri = url
            preferences.isLoopEnabled = loop
        }
        playMedia(url, loop)
    }

    override fun onVideoFileUploaded(file: File, loop: Boolean, saveAsDefault: Boolean) {
        if (saveAsDefault) {
            preferences.defaultVideoUri = file.absolutePath
            preferences.isLoopEnabled = loop
        }
        playMedia(file.absolutePath, loop)
    }

    override fun onControlAction(action: String) {
        runOnUiThread {
            when (action) {
                "pause" -> exoPlayer?.pause()
                "resume" -> exoPlayer?.play()
                "stop" -> exoPlayer?.stop()
                "restart" -> {
                    exoPlayer?.seekTo(0)
                    exoPlayer?.play()
                }
                "toggle_loop" -> {
                    val newLoop = !preferences.isLoopEnabled
                    preferences.isLoopEnabled = newLoop
                    exoPlayer?.repeatMode = if (newLoop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                }
            }
        }
    }

    override fun getStatusJson(): String {
        val json = JsonObject().apply {
            addProperty("id", preferences.tvDeviceId)
            addProperty("name", preferences.tvName)
            addProperty("ip", NetworkUtils.getLocalIpAddress() ?: "")
            addProperty("mac", NetworkUtils.getMacAddress())
            addProperty("port", 8888)
            addProperty("currentVideo", preferences.defaultVideoUri)
            addProperty("isLooping", preferences.isLoopEnabled)
            addProperty("isPlaying", exoPlayer?.isPlaying ?: false)
        }
        return json.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        httpServer?.stop()
        nsdHelper?.unregisterService()
        exoPlayer?.release()
        exoPlayer = null
        handler.removeCallbacks(hideOverlayRunnable)
    }
}
