package com.diabeticcare.app.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.diabeticcare.app.DiabeticCareApp
import com.diabeticcare.app.MainActivity
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.data.model.DEFAULT_DOCTOR_ID
import com.diabeticcare.app.data.model.UserProfile
import com.diabeticcare.app.data.remote.SyncRepository
import com.diabeticcare.app.databinding.ActivityProfileSetupBinding
import com.diabeticcare.app.utils.PreferenceManager
import kotlinx.coroutines.launch

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBinding
    private lateinit var prefs: PreferenceManager

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("diabeticcare_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        super.attachBaseContext(DiabeticCareApp.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PreferenceManager(this)

        binding.btnSaveProfile.setOnClickListener { saveProfile() }
    }

    private fun saveProfile() {
        val name = binding.etName.text?.toString()?.trim() ?: ""
        val ageStr = binding.etAge.text?.toString()?.trim() ?: ""
        val diagnosisYearStr = binding.etDiagnosisYear.text?.toString()?.trim() ?: ""

        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            return
        }
        binding.tilName.error = null

        if (ageStr.isEmpty() || ageStr.toIntOrNull() == null) {
            binding.tilAge.error = "Enter a valid age"
            return
        }
        binding.tilAge.error = null

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

        val diagnosisYear = diagnosisYearStr.toIntOrNull() ?: 2020
        val doctor = binding.etDoctor.text?.toString()?.trim() ?: ""
        val phone = intent.getStringExtra(OtpActivity.EXTRA_PHONE) ?: prefs.getPhone()

        val profile = UserProfile(
            name = name,
            phone = phone,
            age = ageStr.toInt(),
            gender = gender,
            diabetesType = diabetesType,
            diagnosisYear = diagnosisYear,
            doctorName = doctor,
            doctorId = DEFAULT_DOCTOR_ID
        )

        lifecycleScope.launch {
            AppDatabase.getInstance(this@ProfileSetupActivity)
                .userProfileDao()
                .insert(profile)
            SyncRepository.syncPatient(profile)

            prefs.setProfileComplete(true)
            prefs.setLoggedIn(true)
            prefs.saveName(name)
            prefs.saveDoctorId(DEFAULT_DOCTOR_ID)

            runOnUiThread {
                Toast.makeText(
                    this@ProfileSetupActivity,
                    "Welcome, $name!",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@ProfileSetupActivity, MainActivity::class.java))
                finishAffinity()
            }
        }
    }
}
