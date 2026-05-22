package com.diabeticcare.app.ui.lifestyle

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diabeticcare.app.R
import com.diabeticcare.app.databinding.FragmentLifestyleBinding
import com.diabeticcare.app.utils.PreferenceManager

class LifestyleFragment : Fragment() {

    private var _binding: FragmentLifestyleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLifestyleBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifestyleWebView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    if (uri.scheme == "http" || uri.scheme == "https") {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri.toString())))
                        return true
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    val lang = PreferenceManager(requireContext()).getLanguage()
                    view?.evaluateJavascript("setAppLanguage('$lang');", null)
                }
            }
            addJavascriptInterface(AndroidBridge(), "Android")
            loadUrl("file:///android_asset/lifestyle_screen.html")
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun goBack() {
            activity?.runOnUiThread { findNavController().popBackStack() }
        }

        @JavascriptInterface
        fun navigate(destination: String) {
            activity?.runOnUiThread {
                val target = when (destination) {
                    "dashboard" -> R.id.nav_dashboard
                    "glucose" -> R.id.nav_glucose
                    "nutrition" -> R.id.nav_nutrition
                    "lifestyle" -> R.id.nav_lifestyle
                    "about" -> R.id.nav_about_me
                    else -> R.id.nav_dashboard
                }
                if (findNavController().currentDestination?.id != target) {
                    findNavController().navigate(target)
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.lifestyleWebView.apply {
            stopLoading()
            removeJavascriptInterface("Android")
            webViewClient = WebViewClient()
            destroy()
        }
        _binding = null
        super.onDestroyView()
    }
}
