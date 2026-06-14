package com.v2ray.ang.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.R
import com.v2ray.ang.api.ApiClient
import com.v2ray.ang.api.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // اگر قبلاً لاگین کرده، مستقیم برو به صفحه اصلی
        val prefs = getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
        val savedToken = prefs.getString("access_token", null)
        if (!savedToken.isNullOrEmpty()) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                showError("لطفاً نام کاربری و رمز عبور را وارد کنید")
                return@setOnClickListener
            }

            performLogin(username, password)
        }
    }

    private fun performLogin(username: String, password: String) {
        showLoading(true)
        hideError()

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.login(LoginRequest(username, password))
                }

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // ذخیره اطلاعات
                    getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("access_token", data.access_token)
                        .putString("username", data.username)
                        .putString("subscription_url", data.subscription_url)
                        .putString("expire_date", data.expire_date ?: "")
                        .apply()

                    Toast.makeText(
                        this@LoginActivity,
                        "خوش آمدید ${data.username} 👋",
                        Toast.LENGTH_SHORT
                    ).show()

                    goToMain()

                } else {
                    val msg = when (response.code()) {
                        401 -> "نام کاربری یا رمز عبور اشتباه است"
                        403 -> "اشتراک شما منقضی یا غیرفعال است"
                        else -> "خطا در ارتباط با سرور (${response.code()})"
                    }
                    showError(msg)
                }

            } catch (e: Exception) {
                showError("خطا در اتصال به سرور.\nاینترنت خود را بررسی کنید.")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        etUsername.isEnabled = !show
        etPassword.isEnabled = !show
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}
