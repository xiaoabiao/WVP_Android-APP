package com.wvp.device

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.PopupMenu
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wvp.device.api.WVPClient
import com.wvp.device.databinding.ActivityDeviceListBinding
import com.wvp.device.databinding.ItemDeviceBinding
import com.wvp.device.model.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设备列表页面
 * 对应原始 Python 版本 device_list.py
 */
class DeviceListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceListBinding
    private lateinit var client: WVPClient
    private lateinit var adapter: DeviceAdapter

    private var allDevices: List<DeviceInfo> = emptyList()
    private var filteredDevices: List<DeviceInfo> = emptyList()

    companion object {
        private const val TAG = "DeviceListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        val baseUrl = intent.getStringExtra("BASE_URL") ?: ""
        val username = intent.getStringExtra("USERNAME") ?: ""
        val password = intent.getStringExtra("PASSWORD") ?: ""

        client = WVPClient(baseUrl, username, password)

        setupRecyclerView()
        setupSearch()
        setupRefresh()

        loadDevices()
    }

    private fun enableEdgeToEdge() {
        // 内容延伸到状态栏和导航栏下方（真沉浸）
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 状态栏和导航栏透明
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // 给 Toolbar 加上状态栏高度 padding，避免被状态栏遮挡
        binding.root.setOnApplyWindowInsetsListener { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            binding.toolbar.updatePadding(top = statusBarHeight)
            view.onApplyWindowInsets(insets)
        }
    }

    private fun setupRecyclerView() {
        adapter = DeviceAdapter { device ->
            // 双击打开通道列表
            openChannelList(device)
        }
        binding.deviceRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.deviceRecyclerView.adapter = adapter

        // 长按显示右键菜单
        adapter.setOnLongClickListener { device, view ->
            showDeviceContextMenu(device, view)
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDevices(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRefresh() {
        binding.refreshBtn.setOnClickListener {
            loadDevices()
        }
    }

    private fun loadDevices() {
        binding.statusLabel.text = "正在登录..."
        binding.refreshBtn.isEnabled = false

        lifecycleScope.launch {
            try {
                // 先登录获取 token
                withContext(Dispatchers.IO) {
                    client.login()
                }

                binding.statusLabel.text = "正在获取设备列表..."

                val devices = withContext(Dispatchers.IO) {
                    client.getAllDevices()
                }
                allDevices = devices.map { DeviceInfo.fromMap(it) }
                filterDevices(binding.searchInput.text?.toString() ?: "")
                binding.statusLabel.text = "共 ${filteredDevices.size}/${allDevices.size} 个设备"
            } catch (e: Exception) {
                binding.statusLabel.text = "加载失败"
                Snackbar.make(binding.root, "获取设备列表失败: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.refreshBtn.isEnabled = true
            }
        }
    }

    private fun filterDevices(keyword: String) {
        filteredDevices = if (keyword.isBlank()) {
            allDevices.toList()
        } else {
            allDevices.filter { device ->
                keyword.lowercase() in device.deviceId.lowercase() ||
                keyword.lowercase() in device.name.lowercase() ||
                keyword.lowercase() in device.manufacturer.lowercase() ||
                keyword.lowercase() in device.model.lowercase() ||
                keyword.lowercase() in device.hostAddress.lowercase()
            }
        }
        adapter.submitList(filteredDevices)
        binding.statusLabel.text = "共 ${filteredDevices.size}/${allDevices.size} 个设备"
    }

    private fun openChannelList(device: DeviceInfo) {
        binding.statusLabel.text = "正在打开通道: ${device.name}..."
        
        lifecycleScope.launch {
            try {
                val channels = withContext(Dispatchers.IO) {
                    client.getChannels(device.deviceId)
                }
                binding.statusLabel.text = "通道: ${device.name} - ${channels.size} 条"

                val intent = Intent(this@DeviceListActivity, ChannelListActivity::class.java).apply {
                    putExtra("DEVICE_ID", device.deviceId)
                    putExtra("DEVICE_NAME", device.name)
                    putExtra("BASE_URL", intent.getStringExtra("BASE_URL"))
                    putExtra("USERNAME", intent.getStringExtra("USERNAME"))
                    putExtra("PASSWORD", intent.getStringExtra("PASSWORD"))
                }
                startActivity(intent)
            } catch (e: Exception) {
                binding.statusLabel.text = "加载通道失败"
                Snackbar.make(binding.root, "获取通道列表失败: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeviceContextMenu(device: DeviceInfo, view: View) {
        val popup = PopupMenu(this, view)
        // click on card already opens channel list, no need duplicate here
        popup.menu.add(0, 2, 0, "修改设备名称")
        popup.menu.add(0, 3, 0, "修改收流IP")
        popup.menu.add(0, 4, 0, "同步设备目录")
        
        val subMenu = popup.menu.addSubMenu(0, 5, 0, "收流模式")
        subMenu.add(0, 6, 0, "TCP 模式")
        subMenu.add(0, 7, 0, "UDP 模式")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                // 1 -> openChannelList(device)  // click on card already opens channel list
                2 -> showEditNameDialog(device)
                3 -> showEditSdpIpDialog(device)
                4 -> syncDevice(device)
                6 -> setTransportMode(device, "TCP")
                7 -> setTransportMode(device, "UDP")
            }
            true
        }
        popup.show()
    }

    private fun showEditNameDialog(device: DeviceInfo) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val textInput = dialogView.findViewById<TextInputEditText>(R.id.editText)
        textInput.setText(device.name)
        textInput.hint = "新名称"

        MaterialAlertDialogBuilder(this)
            .setTitle("修改设备名称")
            .setMessage("设备ID: ${device.deviceId}")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val newName = textInput.text?.toString()?.trim() ?: ""
                if (newName.isEmpty()) {
                    Snackbar.make(binding.root, "请输入设备名称", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                updateDeviceName(device, newName)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateDeviceName(device: DeviceInfo, newName: String) {
        binding.statusLabel.text = "正在修改名称..."

        lifecycleScope.launch {
            try {
                // 先获取设备详情
                val detail = withContext(Dispatchers.IO) {
                    client.getDeviceDetail(device.deviceId)
                }
                // 更新名称
                val updatedData = detail.toMutableMap()
                updatedData["name"] = newName
                
                val result = withContext(Dispatchers.IO) {
                    client.updateDevice(updatedData)
                }
                
                val code = (result["code"] as? Number)?.toInt() ?: -1
                if (code == 0) {
                    Snackbar.make(binding.root, "设备名称已修改为: $newName", Snackbar.LENGTH_SHORT).show()
                    loadDevices() // 刷新列表
                } else {
                    val msg = result["msg"] as? String ?: "未知错误"
                    Snackbar.make(binding.root, "修改失败: $msg", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.statusLabel.text = "修改失败"
                Snackbar.make(binding.root, "修改失败: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showEditSdpIpDialog(device: DeviceInfo) {
        binding.statusLabel.text = "正在获取 ${device.name} 的详细信息..."

        lifecycleScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    client.getDeviceDetail(device.deviceId)
                }
                val currentIp = detail["sdpIp"] as? String ?: ""

                val dialogView = layoutInflater.inflate(R.layout.dialog_edit_text, null)
                val textInput = dialogView.findViewById<TextInputEditText>(R.id.editText)
                textInput.setText(currentIp)
                textInput.hint = "新收流IP"

                MaterialAlertDialogBuilder(this@DeviceListActivity)
                    .setTitle("修改收流IP")
                    .setMessage("设备: ${device.name}\n当前收流IP: ${currentIp.ifEmpty { "(空)" }}")
                    .setView(dialogView)
                    .setPositiveButton("确定") { _, _ ->
                        val newIp = textInput.text?.toString()?.trim() ?: ""
                        if (newIp.isEmpty()) {
                            Snackbar.make(binding.root, "请输入收流IP地址", Snackbar.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        updateSdpIp(device, newIp)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } catch (e: Exception) {
                binding.statusLabel.text = "获取设备详情失败"
                Snackbar.make(binding.root, "获取设备详情失败: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun updateSdpIp(device: DeviceInfo, newIp: String) {
        binding.statusLabel.text = "正在修改收流IP..."

        lifecycleScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    client.getDeviceDetail(device.deviceId)
                }
                val updatedData = detail.toMutableMap()
                updatedData["sdpIp"] = newIp

                val result = withContext(Dispatchers.IO) {
                    client.updateDevice(updatedData)
                }

                val code = (result["code"] as? Number)?.toInt() ?: -1
                if (code == 0) {
                    Snackbar.make(binding.root, "设备 ${device.name} 收流IP已修改为: $newIp", Snackbar.LENGTH_SHORT).show()
                    loadDevices()
                } else {
                    val msg = result["msg"] as? String ?: "未知错误"
                    Snackbar.make(binding.root, "修改失败: $msg", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.statusLabel.text = "修改失败"
                Snackbar.make(binding.root, "修改失败: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun syncDevice(device: DeviceInfo) {
        binding.statusLabel.text = "正在同步 ${device.name}..."

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    client.syncDevice(device.deviceId)
                }
                val msg = result["msg"] as? String ?: "同步完成"
                binding.statusLabel.text = "同步完成: $msg"
                Snackbar.make(binding.root, "同步结果: $msg", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.statusLabel.text = "同步失败"
                Snackbar.make(binding.root, "同步失败: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setTransportMode(device: DeviceInfo, mode: String) {
        binding.statusLabel.text = "设置收流模式: $mode..."

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    client.setTransportMode(device.deviceId, mode)
                }
                val msg = result["msg"] as? String ?: "设置成功"
                binding.statusLabel.text = "收流模式设置: $msg"
                Snackbar.make(binding.root, "设置结果: $msg", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.statusLabel.text = "设置失败"
                Snackbar.make(binding.root, "设置失败: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}

/**
 * 设备列表适配器
 */
class DeviceAdapter(
    private val onClick: (DeviceInfo) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    private var devices: List<DeviceInfo> = emptyList()
    private var onLongClickListener: ((DeviceInfo, View) -> Unit)? = null

    fun setOnLongClickListener(listener: (DeviceInfo, View) -> Unit) {
        onLongClickListener = listener
    }

    fun submitList(list: List<DeviceInfo>) {
        devices = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount() = devices.size

    inner class ViewHolder(private val binding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(device: DeviceInfo) {
            binding.deviceName.text = device.name.ifEmpty { device.deviceId }
            binding.deviceId.text = device.deviceId
            binding.manufacturer.text = device.manufacturer.ifEmpty { "-" }
            binding.model.text = device.model.ifEmpty { "-" }
            binding.ipAddress.text = device.hostAddress.ifEmpty { "-" }
            // Port display (commented out - uncomment to show :port below IP)
            // binding.port.text = if (device.port > 0) ":${device.port}" else ""

            // 在线状态
            binding.onlineText.text = if (device.online) "在线" else "离线"
            val statusColor = if (device.online) {
                binding.root.context.getColor(R.color.status_online)
            } else {
                binding.root.context.getColor(R.color.status_offline)
            }
            binding.statusIndicator.setBackgroundColor(statusColor)

            // 点击事件
            binding.root.setOnClickListener { onClick(device) }
            binding.root.setOnLongClickListener {
                onLongClickListener?.invoke(device, binding.root)
                true
            }
        }
    }
}
