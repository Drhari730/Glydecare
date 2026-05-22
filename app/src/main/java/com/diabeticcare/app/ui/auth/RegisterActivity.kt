package com.diabeticcare.app.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.diabeticcare.app.DiabeticCareApp
import com.diabeticcare.app.MainActivity
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.data.model.DEFAULT_DOCTOR_ID
import com.diabeticcare.app.data.model.UserProfile
import com.diabeticcare.app.data.remote.SyncRepository
import com.diabeticcare.app.databinding.ActivityRegisterBinding
import com.diabeticcare.app.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var prefs: PreferenceManager

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("diabeticcare_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        super.attachBaseContext(DiabeticCareApp.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PreferenceManager(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCreateAccount.setOnClickListener { createAccount() }
        playIntroAnimation()
    }

    private fun playIntroAnimation() {
        binding.cardRegister.translationY = 36f
        binding.cardRegister.alpha = 0f
        binding.cardRegister.animate().translationY(0f).alpha(1f).setDuration(520).start()
    }

    private fun createAccount() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()
        val age = binding.etAge.text?.toString()?.trim()?.toIntOrNull()
        val diagnosisYear = binding.etDiagnosisYear.text?.toString()?.trim()?.toIntOrNull()
        val emergency = binding.etEmergency.text?.toString()?.trim().orEmpty()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        clearErrors()

        when {
            name.isBlank() -> {
                binding.tilName.error = "Full name is required"
                return
            }
            phone.length != 10 -> {
                binding.tilPhone.error = "Enter a valid 10-digit mobile number"
                return
            }
            password.length < 6 -> {
                binding.tilPassword.error = "Create a password with at least 6 characters"
                return
            }
            password != confirmPassword -> {
                binding.tilConfirmPassword.error = "Passwords do not match"
                return
            }
            age == null || age !in 1..120 -> {
                binding.tilAge.error = "Enter a valid age"
                return
            }
            diagnosisYear != null && diagnosisYear !in 1900..currentYear -> {
                binding.tilDiagnosisYear.error = "Enter a year between 1900 and $currentYear"
                return
            }
            emergency.isNotBlank() && emergency.length != 10 -> {
                binding.tilEmergency.error = "Enter a valid 10-digit emergency contact"
                return
            }
        }

        val gender = when (binding.chipGender.checkedChipId) {
            binding.chipMale.id -> "MALE"
            binding.chipFemale.id -> "FEMALE"
            else -> "OTHER"
        }

        val diabetesType = when (binding.chipDiabetesType.checkedChipId) {
            binding.chipType1.id -> "TYPE1"
            binding.chipType2.id -> "TYPE2"
            binding.chipGestational.id -> "GESTATIONAL"
            binding.chipPrediabetes.id -> "PREDIABETES"
            else -> "TYPE2"
        }

        val profile = UserProfile(
            name = name,
            phone = phone,
            age = age,
            gender = gender,
            diabetesType = diabetesType,
            diagnosisYear = diagnosisYear ?: currentYear,
            doctorName = binding.etDoctor.text?.toString()?.trim().orEmpty(),
            doctorId = DEFAULT_DOCTOR_ID
        )

        showLoading(true)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@RegisterActivity)
                db.clearAllTables()
                db.userProfileDao().insert(profile)
                SyncRepository.syncPatient(profile)
            }
            prefs.saveName(name)
            prefs.savePhone(phone)
            prefs.savePassword(password)
            prefs.saveDoctorId(DEFAULT_DOCTOR_ID)
            prefs.setProfileComplete(true)
            prefs.setLoggedIn(true)

            runOnUiThread {
                Toast.makeText(this@RegisterActivity, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                finishAffinity()
            }
        }
    }

    private fun clearErrors() {
        listOf(
            binding.tilName,
            binding.tilPhone,
            binding.tilPassword,
            binding.tilConfirmPassword,
            binding.tilAge,
            binding.tilDiagnosisYear,
            binding.tilEmergency
        ).forEach { it.error = null }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnCreateAccount.isEnabled = !show
        binding.btnBack.isEnabled = !show
    }
}
