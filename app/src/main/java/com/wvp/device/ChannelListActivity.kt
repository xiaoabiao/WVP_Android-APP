package com.wvp.device

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.wvp.device.api.WVPClient
import com.wvp.device.databinding.ActivityChannelListBinding
import com.wvp.device.databinding.ItemChannelBinding
import com.wvp.device.model.ChannelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChannelListBinding
    private lateinit var client: WVPClient
    private lateinit var adapter: ChannelAdapter

    private var allChannels: List<ChannelInfo> = emptyList()
    private var filteredChannels: List<ChannelInfo> = emptyList()
    private var deviceId: String = ""
    private var deviceName: String = ""
    private var serverIp: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        deviceId = intent.getStringExtra("DEVICE_ID") ?: ""
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: deviceId
        val baseUrl = intent.getStringExtra("BASE_URL") ?: ""
        val username = intent.getStringExtra("USERNAME") ?: ""
        val password = intent.getStringExtra("PASSWORD") ?: ""

        // extract server ip from login url
        try {
            val url = java.net.URL(baseUrl)
            serverIp = url.host
        } catch (e: Exception) {
            serverIp = ""
        }

        client = WVPClient(baseUrl, username, password)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "通道列表 - $deviceName"
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupSearch()
        loadChannels()
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        binding.root.setOnApplyWindowInsetsListener { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            binding.toolbar.updatePadding(top = statusBarHeight)
            view.onApplyWindowInsets(insets)
        }
    }

    private fun setupRecyclerView() {
        adapter = ChannelAdapter { channel ->
            startPlayChannel(channel)
        }
        binding.channelRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.channelRecyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterChannels(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadChannels() {
        binding.statusLabel.text = "正在获取通道列表..."
        binding.searchInput.isEnabled = false

        lifecycleScope.launch {
            try {
                val channels = withContext(Dispatchers.IO) {
                    client.getChannels(deviceId)
                }
                allChannels = channels.map { ChannelInfo.fromMap(it) }
                filterChannels(binding.searchInput.text?.toString() ?: "")
                binding.statusLabel.text = "共 ${filteredChannels.size}/${allChannels.size} 条通道"
            } catch (e: Exception) {
                binding.statusLabel.text = "加载失败"
                Snackbar.make(binding.root, "获取通道列表失败: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.searchInput.isEnabled = true
            }
        }
    }

    private fun filterChannels(keyword: String) {
        filteredChannels = if (keyword.isBlank()) {
            allChannels.toList()
        } else {
            allChannels.filter { channel ->
                keyword.lowercase() in channel.deviceId.lowercase() ||
                keyword.lowercase() in channel.name.lowercase() ||
                keyword.lowercase() in channel.manufacturer.lowercase()
            }
        }
        adapter.submitList(filteredChannels)
        binding.statusLabel.text = "共 ${filteredChannels.size}/${allChannels.size} 条通道"
    }

    private fun startPlayChannel(channel: ChannelInfo) {
        binding.statusLabel.text = "正在点播..."

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    client.startPlay(deviceId, channel.deviceId)
                }

                val code = (result["code"] as? Number)?.toInt() ?: -1
                if (code == 0) {
                    val data = result["data"] as? Map<*, *>
                    var playUrl = ""
                    if (data != null) {
                        playUrl = (data["rtsp"] as? String)
                            ?: (data["httpFLV"] as? String)
                            ?: (data["flv"] as? String)
                            ?: (data["wsFlv"] as? String)
                            ?: (data["wssFlv"] as? String)
                            ?: ""
                    }

                    if (playUrl.isNotEmpty()) {
                        val fixedUrl = replaceStreamIp(playUrl, serverIp)
                        binding.statusLabel.text = "点播: ${channel.name}"

                        val intent = Intent(this@ChannelListActivity, PlayerActivity::class.java).apply {
                            putExtra("PLAY_URL", fixedUrl)
                            putExtra("CHANNEL_NAME", channel.name)
                            putExtra("DEVICE_ID", deviceId)
                            putExtra("CHANNEL_ID", channel.deviceId)
                            putExtra("BASE_URL", intent.getStringExtra("BASE_URL"))
                            putExtra("USERNAME", intent.getStringExtra("USERNAME"))
                            putExtra("PASSWORD", intent.getStringExtra("PASSWORD"))
                        }
                        startActivity(intent)
                    } else {
                        Snackbar.make(binding.root, "点播成功但未获取到播放地址", Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    val msg = result["msg"] as? String ?: "未知错误"
                    Snackbar.make(binding.root, "点播失败: $msg", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.statusLabel.text = "点播失败"
                Snackbar.make(binding.root, "点播失败: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

        private fun replaceStreamIp(url: String, serverIp: String): String {
        if (serverIp.isBlank()) return url
        return try {
            val originalUri = java.net.URI(url)
            java.net.URI(
                originalUri.scheme,
                originalUri.userInfo,
                serverIp,
                originalUri.port,
                originalUri.rawPath,
                originalUri.rawQuery,
                originalUri.rawFragment
            ).toString()
        } catch (e: Exception) {
            url
        }
    }
}

class ChannelAdapter(
    private val onClick: (ChannelInfo) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    private var channels: List<ChannelInfo> = emptyList()

    fun submitList(list: List<ChannelInfo>) {
        channels = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(channels[position])
    }

    override fun getItemCount() = channels.size

    inner class ViewHolder(private val binding: ItemChannelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: ChannelInfo) {
            binding.channelName.text = channel.name.ifEmpty { channel.deviceId }
            binding.channelId.text = channel.deviceId
            binding.manufacturer.text = channel.manufacturer.ifEmpty { "-" }
            binding.model.text = channel.model.ifEmpty { "-" }

            binding.onlineText.text = if (channel.online) "在线" else "离线"
            val statusColor = if (channel.online) {
                binding.root.context.getColor(R.color.status_online)
            } else {
                binding.root.context.getColor(R.color.status_offline)
            }
            binding.statusIndicator.setBackgroundColor(statusColor)

            if (channel.streamId.isNotEmpty()) {
                binding.streamId.text = "流: ${channel.streamId}"
                binding.streamId.visibility = View.VISIBLE
            } else {
                binding.streamId.visibility = View.GONE
            }

            binding.root.setOnClickListener { onClick(channel) }
        }
    }
}
