package com.computer.skyvault.ui.login

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.computer.skyvault.MainActivity
import com.computer.skyvault.common.dto.LoginRequest
import com.computer.skyvault.databinding.ModuleActivityLoginBinding
import com.computer.skyvault.manager.LoginManager
import com.computer.skyvault.service.AccountService
import com.computer.skyvault.utils.showToast


private const val TAG = "LoginActivity"

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ModuleActivityLoginBinding
    private lateinit var loginManager: LoginManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 LoginManager
        loginManager = LoginManager.getInstance(application)

        // 检查是否已登录
        if (loginManager.isLoggedIn()){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        window.statusBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding = ModuleActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.tilEmail.editText?.text.toString().trim()
            val password = binding.tilPassword.editText?.text.toString().trim()
            val code = binding.tilVerificationCode.editText?.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || code.isEmpty()) {
                "请输入所有字段".showToast(this)
                return@setOnClickListener
            }

            val loginRequest = LoginRequest(
                email = email,
                password = password,
                verificationCode = code
            )
            AccountService.login(
                loginRequest,
                onSuccess = { result ->
                    if (result.code == 200) {
                        result.data?.let { loginInfo ->
                            // 保存登录信息到 SharePreferences
                            Log.d(TAG, email)
                            loginInfo.user.email = email
                            loginManager.saveLoginInfo(loginInfo)
                            Log.d(TAG, "Login successful")
                            // 登录成功
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        result.message.showToast(this)
                    }
                },
                onFailure = {
                    "Network error. Please try again.".showToast(this)
                    Log.e(TAG, "Login exception: $it")
                }
            )
        }
    }
}