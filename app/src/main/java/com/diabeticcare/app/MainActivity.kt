package com.diabeticcare.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.data.remote.SyncRepository
import com.diabeticcare.app.databinding.ActivityMainBinding
import com.diabeticcare.app.utils.NotificationHelper
import com.diabeticcare.app.utils.PreferenceManager
import com.diabeticcare.app.utils.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("diabeticcare_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        super.attachBaseContext(DiabeticCareApp.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
        binding.bottomNav.visibility = View.GONE

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }

        // Schedule fixed lifestyle/visit reminders and clean up old periodic glucose jobs.
        val prefs = PreferenceManager(this)
        ReminderScheduler.scheduleAll(this)
        prefs.setRemindersScheduled(true)
        syncCurrentPatient()
    }

    private fun syncCurrentPatient() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@MainActivity)
                val profile = db.userProfileDao().getProfileSnapshot() ?: return@withContext
                SyncRepository.syncPatient(profile)
                db.glucoseDao().getLatestReading()?.let { reading ->
                    SyncRepository.syncGlucose(profile, reading)
                }
            }
        }
    }
}
