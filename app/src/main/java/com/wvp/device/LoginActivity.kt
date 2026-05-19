package com.wvp.device

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.wvp.device.api.WVPClient
import com.wvp.device.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        setupUI()
        restoreSavedLogin()
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    private fun setupUI() {
        binding.rememberPwdCheckbox.setOnCheckedChangeListener { _, isChecked ->
            saveRememberChoice(isChecked)
        }

        binding.loginBtn.setOnClickListener {
            performLogin()
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLoginButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.urlInput.addTextChangedListener(textWatcher)
        binding.userInput.addTextChangedListener(textWatcher)
        binding.pwdInput.addTextChangedListener(textWatcher)
    }

    private fun updateLoginButtonState() {
        val url = binding.urlInput.text?.toString()?.trim() ?: ""
        val user = binding.userInput.text?.toString()?.trim() ?: ""
        val pwd = binding.pwdInput.text?.toString() ?: ""
        binding.loginBtn.isEnabled = url.isNotEmpty() && user.isNotEmpty() && pwd.isNotEmpty()
    }

    private fun saveLoginInfo(url: String, username: String, password: String) {
        val sp = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        sp.edit().apply {
            putString("saved_url", url)
            putString("saved_username", username)
            putString("saved_password", password)
            putBoolean("remember_password", binding.rememberPwdCheckbox.isChecked)
            apply()
        }
    }

    private fun saveRememberChoice(isChecked: Boolean) {
        val sp = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        sp.edit().putBoolean("remember_password", isChecked).apply()
        if (!isChecked) {
            sp.edit().remove("saved_password").apply()
        }
    }

    private fun restoreSavedLogin() {
        val sp = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        val savedUrl = sp.getString("saved_url", "") ?: ""
        val savedUsername = sp.getString("saved_username", "") ?: ""
        val savedPassword = sp.getString("saved_password", "") ?: ""
        val rememberPwd = sp.getBoolean("remember_password", false)

        if (rememberPwd && savedUrl.isNotEmpty()) {
            binding.urlInput.setText(savedUrl)
            binding.userInput.setText(savedUsername)
            binding.pwdInput.setText(savedPassword)
            binding.rememberPwdCheckbox.isChecked = true
        } else {
            binding.rememberPwdCheckbox.isChecked = rememberPwd
        }
    }

    private fun performLogin() {
        val url = binding.urlInput.text?.toString()?.trim() ?: ""
        val username = binding.userInput.text?.toString()?.trim() ?: ""
        val password = binding.pwdInput.text?.toString() ?: ""

        if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Snackbar.make(binding.root, "请填写服务地址、用户名和密码", Snackbar.LENGTH_SHORT).show()
            return
        }

        binding.loginBtn.isEnabled = false
        binding.loginBtn.text = "登录中..."
        binding.statusText.text = "正在登录..."

        lifecycleScope.launch {
            try {
                val client = WVPClient(url, username, password)
                val token = withContext(Dispatchers.IO) {
                    client.login()
                }

                if (binding.rememberPwdCheckbox.isChecked) {
                    saveLoginInfo(url, username, password)
                }

                val intent = Intent(this@LoginActivity, DeviceListActivity::class.java).apply {
                    putExtra("BASE_URL", url)
                    putExtra("USERNAME", username)
                    putExtra("PASSWORD", password)
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                binding.loginBtn.isEnabled = true
                binding.loginBtn.text = "登录"
                binding.statusText.text = "登录失败: ${e.message}"
                Snackbar.make(binding.root, "登录失败: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
