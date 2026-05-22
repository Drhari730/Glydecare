package com.diabeticcare.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.diabeticcare.app.databinding.ActivityOtpBinding
import com.diabeticcare.app.utils.PreferenceManager

class OtpActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PHONE = "extra_phone"
        private const val DEMO_OTP = "123456"   // replace with real OTP in production
        private const val RESEND_COOLDOWN_MS = 30_000L
    }

    private lateinit var binding: ActivityOtpBinding
    private lateinit var prefs: PreferenceManager
    private var phone: String = ""
    private var countDownTimer: CountDownTimer? = null
    private val otpFields get() = listOf(
        binding.etOtp1, binding.etOtp2, binding.etOtp3,
        binding.etOtp4, binding.etOtp5, binding.etOtp6
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        binding.tvPhoneDisplay.text = phone

        setupOtpFields()
        startResendTimer()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnVerify.setOnClickListener { verifyOtp() }
        binding.tvResend.setOnClickListener { resendOtp() }
    }

    private fun setupOtpFields() {
        otpFields.forEachIndexed { index, field ->
            field.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < otpFields.size - 1) {
                        otpFields[index + 1].requestFocus()
                    }
                    if (s?.isEmpty() == true && index > 0) {
                        otpFields[index - 1].requestFocus()
                    }
                }
            })
        }
        otpFields.first().requestFocus()
    }

    private fun getEnteredOtp(): String = otpFields.joinToString("") { it.text.toString() }

    private fun verifyOtp() {
        val otp = getEnteredOtp()
        if (otp.length != 6) {
            Toast.makeText(this, "Please enter the complete 6-digit OTP", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        binding.root.postDelayed({
            showLoading(false)
            if (otp == DEMO_OTP) {
                prefs.savePhone(phone)
                if (prefs.isProfileComplete()) {
                    prefs.setLoggedIn(true)
                    startActivity(Intent(this, com.diabeticcare.app.MainActivity::class.java))
                } else {
                    startActivity(Intent(this, ProfileSetupActivity::class.java).apply {
                        putExtra(EXTRA_PHONE, phone)
                    })
                }
                finish()
            } else {
                Toast.makeText(this, "Incorrect OTP. Try: $DEMO_OTP", Toast.LENGTH_LONG).show()
            }
        }, 1200)
    }

    private fun resendOtp() {
        Toast.makeText(this, "OTP resent to $phone", Toast.LENGTH_SHORT).show()
        startResendTimer()
    }

    private fun startResendTimer() {
        binding.tvResend.isEnabled = false
        binding.tvCountdown.visibility = View.VISIBLE
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(RESEND_COOLDOWN_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvCountdown.text = getString(
                    com.diabeticcare.app.R.string.resend_in,
                    millisUntilFinished / 1000
                )
            }
            override fun onFinish() {
                binding.tvResend.isEnabled = true
                binding.tvCountdown.visibility = View.GONE
            }
        }.start()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnVerify.isEnabled = !show
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
