package com.wvp.device

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.snackbar.Snackbar
import com.wvp.device.api.WVPClient
import com.wvp.device.databinding.ActivityPlayerBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var playUrl: String = ""
    private var channelName: String = ""
    private var deviceId: String = ""
    private var channelId: String = ""
    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        playUrl = intent.getStringExtra("PLAY_URL") ?: ""
        channelName = intent.getStringExtra("CHANNEL_NAME") ?: ""
        deviceId = intent.getStringExtra("DEVICE_ID") ?: ""
        channelId = intent.getStringExtra("CHANNEL_ID") ?: ""
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = channelName.ifEmpty { "视频播放" }
        binding.toolbar.setNavigationOnClickListener {
            if (isFullscreen) exitFullscreen() else { stopPlayback(); finish() }
        }
        setupPlayer()
        setupUI()
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        binding.root.setOnApplyWindowInsetsListener { view, insets ->
            binding.toolbar.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            view.onApplyWindowInsets(insets)
        }
    }

    private fun setupUI() {
        binding.stopBtn.setOnClickListener { stopPlayback(); finish() }
        binding.fullscreenBtn.setOnClickListener { toggleFullscreen() }
        binding.statusText.text = "正在连接: $playUrl"
    }

    private fun toggleFullscreen() {
        if (isFullscreen) exitFullscreen() else enterFullscreen()
    }

    private fun enterFullscreen() {
        isFullscreen = true
        binding.toolbar.visibility = View.GONE
        binding.statusLayout.visibility = View.GONE
        binding.controlLayout.visibility = View.GONE
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        binding.playerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun exitFullscreen() {
        isFullscreen = false
        binding.toolbar.visibility = View.VISIBLE
        binding.statusLayout.visibility = View.VISIBLE
        binding.controlLayout.visibility = View.VISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.show()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding.playerView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    private fun setupPlayer() {
        if (playUrl.isEmpty()) { binding.statusText.text = "播放地址为空"; return }
        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> { binding.statusText.text = "缓冲中..."; binding.progressBar.visibility = View.VISIBLE }
                        Player.STATE_READY -> { binding.statusText.text = "正在播放: $channelName"; binding.progressBar.visibility = View.GONE }
                        Player.STATE_ENDED -> { binding.statusText.text = "播放结束"; binding.progressBar.visibility = View.GONE }
                        Player.STATE_IDLE -> { binding.statusText.text = "准备中..."; binding.progressBar.visibility = View.GONE }
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    binding.statusText.text = "播放错误: ${error.message}"
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, "播放失败: ${error.message}", Snackbar.LENGTH_LONG).show()
                }
            })
            binding.playerView.player = this
            setMediaItem(MediaItem.fromUri(Uri.parse(playUrl)))
            prepare()
            playWhenReady = true
        }
    }

    private fun stopPlayback() {
        player?.let { it.stop(); it.release() }
        player = null
        if (deviceId.isNotEmpty() && channelId.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val b = intent.getStringExtra("BASE_URL") ?: return@launch
                    val u = intent.getStringExtra("USERNAME") ?: return@launch
                    val p = intent.getStringExtra("PASSWORD") ?: return@launch
                    WVPClient(b, u, p).stopPlay(deviceId, channelId)
                } catch (_: Exception) { }
            }
        }
    }

    override fun onPause() { super.onPause(); player?.pause() }
    override fun onResume() { super.onResume(); player?.play() }
    override fun onDestroy() { stopPlayback(); super.onDestroy() }
    override fun onBackPressed() { if (isFullscreen) exitFullscreen() else { stopPlayback(); super.onBackPressed() } }
}