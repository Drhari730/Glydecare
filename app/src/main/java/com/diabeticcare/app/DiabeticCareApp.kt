package com.diabeticcare.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.diabeticcare.app.utils.NotificationHelper
import java.util.Locale

class DiabeticCareApp : Application() {

    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("diabeticcare_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        super.attachBaseContext(applyLocale(base, lang))
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }

    companion object {
        fun applyLocale(context: Context, lang: String): Context {
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            config.setLayoutDirection(locale)
            return context.createConfigurationContext(config)
        }
    }
}
