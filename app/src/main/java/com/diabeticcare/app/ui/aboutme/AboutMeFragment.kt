package com.diabeticcare.app.ui.aboutme

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.databinding.FragmentAboutMeBinding
import com.diabeticcare.app.ui.auth.LoginActivity
import com.diabeticcare.app.utils.NotificationHelper
import com.diabeticcare.app.utils.PreferenceManager
import com.diabeticcare.app.utils.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AboutMeFragment : Fragment() {

    private var _binding: FragmentAboutMeBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: PreferenceManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAboutMeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager(requireContext())
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        loadProfile()
        setupListeners()
    }

    private fun loadProfile() {
        val name = prefs.getName()
        binding.etName.setText(name)
        binding.tvProfileNameHeader.text = name.ifEmpty { "Your Name" }
        if (prefs.getLanguage() == "kn") binding.chipKn.isChecked = true
        else binding.chipEn.isChecked = true
    }

    private fun setupListeners() {
        binding.chipEn.setOnClickListener { applyLanguage("en") }
        binding.chipKn.setOnClickListener { applyLanguage("kn") }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.saveName(name)
            binding.tvProfileNameHeader.text = name

            val newLang = if (binding.chipKn.isChecked) "kn" else "en"
            val oldLang = prefs.getLanguage()
            prefs.setLanguage(newLang)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newLang))
            NotificationHelper.createChannels(requireContext())

            Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
            if (newLang != oldLang) {
                // Force locale update and recreate activity
                requireActivity().recreate()
            }
        }

        binding.switchGlucoseReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) ReminderScheduler.scheduleAll(requireContext())
        }

        binding.btnTestReminder.setOnClickListener {
            ReminderScheduler.testFireNow(requireContext())
            Toast.makeText(context, "Test notification sent!", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(requireContext()).clearAllTables()
                }
                ReminderScheduler.cancelAll(requireContext())
                prefs.clearAll()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finishAffinity()
            }
        }
    }

    private fun applyLanguage(newLang: String) {
        if (newLang == prefs.getLanguage()) return
        prefs.setLanguage(newLang)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newLang))
        NotificationHelper.createChannels(requireContext())
        requireActivity().recreate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
