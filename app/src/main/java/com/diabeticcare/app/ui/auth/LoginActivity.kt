package com.diabeticcare.app.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.diabeticcare.app.DiabeticCareApp
import com.diabeticcare.app.MainActivity
import com.diabeticcare.app.databinding.ActivityLoginBinding
import com.diabeticcare.app.utils.PreferenceManager
import com.diabeticcare.app.utils.UserSessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: PreferenceManager

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("diabeticcare_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        super.attachBaseContext(DiabeticCareApp.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceManager(this)

        if (prefs.isLoggedIn()) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { handleLogin() }
        binding.btnRegister.setOnClickListener { handleRegister() }

        setupLanguageSelector()
        playIntroAnimation()
    }

    private fun setupLanguageSelector() {
        val current = prefs.getLanguage()
        binding.chipLangEn.isChecked = current != "kn"
        binding.chipLangKn.isChecked = current == "kn"

        binding.chipLangEn.setOnClickListener { selectLanguage("en") }
        binding.chipLangKn.setOnClickListener { selectLanguage("kn") }
    }

    private fun selectLanguage(lang: String) {
        val oldLang = prefs.getLanguage()
        prefs.setLanguage(lang)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        binding.chipLangEn.isChecked = lang != "kn"
        binding.chipLangKn.isChecked = lang == "kn"
        if (oldLang != lang) recreate()
    }

    private fun handleLogin() {
        val phone = binding.etPhone.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""

        if (phone.length != 10) {
            binding.tilPhone.error = "Enter a valid 10-digit number"
            return
        }
        binding.tilPhone.error = null

        if (password.length < 4) {
            binding.tilPassword.error = "Password must be at least 4 characters"
            return
        }
        binding.tilPassword.error = null

        showLoading(true)

        // Check stored credentials
        val storedPhone = prefs.getPhone()
        val storedPassword = prefs.getPassword()

        binding.root.postDelayed({
            showLoading(false)
            if (phone == DEMO_PHONE && password == DEMO_PASSWORD) {
                completeLogin(DEMO_PHONE, DEMO_PASSWORD, "Demo Patient")
            } else if (storedPhone == phone && storedPassword == password) {
                prefs.setLoggedIn(true)
                navigateToMain()
            } else if (storedPhone.isEmpty()) {
                Toast.makeText(this, "No account found. Please register first.", Toast.LENGTH_LONG).show()
            } else {
                binding.tilPassword.error = "Incorrect phone or password"
            }
        }, 800)
    }

    private fun completeLogin(phone: String, password: String, name: String) {
        showLoading(true)
        lifecycleScope.launch {
            UserSessionManager.clearPatientDataIfAccountChanged(this@LoginActivity, phone, prefs)
            prefs.savePhone(phone)
            prefs.savePassword(password)
            prefs.saveName(name)
            prefs.setProfileComplete(true)
            prefs.setLoggedIn(true)
            showLoading(false)
            navigateToMain()
        }
    }

    private fun handleRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.btnRegister.isEnabled = !show
    }

    private fun playIntroAnimation() {
        binding.ivLogo.scaleX = 0.85f
        binding.ivLogo.scaleY = 0.85f
        binding.cardLogin.translationY = 32f
        binding.cardLogin.alpha = 0f
        binding.ivLogo.animate().scaleX(1f).scaleY(1f).rotationBy(360f).setDuration(700).start()
        binding.cardLogin.animate().translationY(0f).alpha(1f).setDuration(550).setStartDelay(120).start()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val DEMO_PHONE = "9876543210"
        private const val DEMO_PASSWORD = "demo123"
    }
}
