package com.diabeticcare.app.ui.nutrition

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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diabeticcare.app.BuildConfig
import com.diabeticcare.app.R
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.databinding.FragmentNutritionBinding
import com.diabeticcare.app.network.ClaudeClient
import com.diabeticcare.app.network.ClaudeMessage
import com.diabeticcare.app.network.ClaudeRequest
import com.diabeticcare.app.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

class NutritionFragment : Fragment() {

    private var _binding: FragmentNutritionBinding? = null
    private val binding get() = _binding!!
    private var pageReady = false
    private var latestGlucose = 118
    private var latestHba1c = 6.8f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNutritionBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = AppDatabase.getInstance(requireContext())

        binding.nutritionWebView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    if (uri.scheme == "http" || uri.scheme == "https") {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri.toString())))
                        return true
                    }
                    return false
                }

                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    pageReady = true
                    val lang = PreferenceManager(requireContext()).getLanguage()
                    view?.evaluateJavascript("setAppLanguage('$lang');", null)
                    injectPatientData()
                }
            }
            addJavascriptInterface(AndroidBridge(), "Android")
            addJavascriptInterface(NutritionBridge(), "AndroidNutrition")
            loadUrl("file:///android_asset/nutrition_screen.html")
        }

        db.glucoseDao().getLatestReadingLive().observe(viewLifecycleOwner) { reading ->
            latestGlucose = reading?.value ?: 118
            injectPatientData()
        }
        db.hba1cDao().getLatestLive().observe(viewLifecycleOwner) { record ->
            latestHba1c = record?.value ?: 6.8f
            injectPatientData()
        }
    }

    private fun injectPatientData() {
        if (!pageReady || _binding == null) return
        val hba1cText = String.format(Locale.US, "%.1f", latestHba1c)
        binding.nutritionWebView.evaluateJavascript("setPatientData($latestGlucose, $hba1cText);", null)
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

    inner class NutritionBridge {
        @JavascriptInterface
        fun askNutrition(prompt: String) {
            activity?.runOnUiThread { answerNutritionPrompt(prompt) }
        }
    }

    private fun answerNutritionPrompt(prompt: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val answer = withContext(Dispatchers.IO) { buildAiAnswer(prompt) }
            if (_binding != null) {
                binding.nutritionWebView.evaluateJavascript("showAiAnswer(${JSONObject.quote(answer)});", null)
            }
        }
    }

    private suspend fun buildAiAnswer(prompt: String): String {
        val apiKey = BuildConfig.CLAUDE_API_KEY
        if (apiKey.isBlank() || apiKey == "\"\"") {
            return ruleBasedNutrition(prompt)
        }

        return try {
            val systemPrompt = "You are Glydecare AI, a diabetes nutrition planner for Indian patients. " +
                "Use practical Indian food guidance. Include GI, portions, carbs, protein, fibre, preparation method, " +
                "glucose impact, HbA1c impact, and medication timing cautions when relevant. " +
                "Current patient glucose: $latestGlucose mg/dL. HbA1c: ${"%.1f".format(latestHba1c)}%. " +
                "Patient name: ${PreferenceManager(requireContext()).getName().ifEmpty { "Friend" }}."
            val response = ClaudeClient.service.sendMessage(
                apiKey = apiKey,
                request = ClaudeRequest(
                    model = "claude-haiku-4-5",
                    max_tokens = 900,
                    messages = listOf(ClaudeMessage("user", "$systemPrompt\n\nPatient question: $prompt"))
                )
            )
            response.content.firstOrNull()?.text ?: ruleBasedNutrition(prompt)
        } catch (_: Exception) {
            ruleBasedNutrition(prompt)
        }
    }

    private fun ruleBasedNutrition(prompt: String): String {
        val q = prompt.lowercase()
        return when {
            q.contains("7-day") || q.contains("meal plan") ->
                "7-day diabetes meal plan for an Indian T2DM patient:\n\nDay 1: Breakfast - 2 idlis + sambar + chutney without sugar. Lunch - 2 phulkas + dal + beans poriyal + curd. Dinner - vegetable dalia + salad.\nDay 2: Breakfast - ragi dosa + sambar. Lunch - small millet rice + rajma/chana + cucumber salad. Dinner - paneer/tofu bhurji + 1 phulka + vegetables.\nDay 3: Breakfast - oats vegetable upma + sprouts. Lunch - brown rice small cup + fish/egg/paneer curry + greens. Dinner - moong dal khichdi + lauki.\nDay 4: Breakfast - besan chilla + curd. Lunch - jowar roti + dal + mixed vegetables. Dinner - soup + grilled protein + salad.\nDay 5: Breakfast - pesarattu + sambar. Lunch - methi roti + chana + curd. Dinner - millet pongal + vegetables.\nDay 6: Breakfast - vegetable poha with peanuts, small portion. Lunch - 2 phulkas + chicken/fish/paneer + salad. Dinner - dal + vegetable stir-fry.\nDay 7: Breakfast - boiled egg/sprouts + 1 phulka or ragi malt without sugar. Lunch - sambar + vegetables + controlled rice. Dinner - light dal soup + vegetables.\n\nRules: half plate vegetables, one quarter protein, one quarter grains. Avoid fruit juice, sweets, and rice-only meals. Walk 10-15 minutes after lunch and dinner if safe."
            q.contains("avoid") ->
                "Foods to limit with diabetes:\n\n- Sugary tea/coffee, soft drinks, fruit juice, sweets, cakes, biscuits, and sweetened cereals.\n- Maida foods like naan, parotta, bakery items, noodles, and pizza.\n- Fried snacks such as chips, pakoda, mixture, samosa, and puri.\n- Large portions of white rice, rice eaten alone, and late-night heavy dinners.\n\nSafer swaps: sambar with vegetables, dal, curd, eggs/fish/paneer/tofu, sprouts, nuts, methi, karela, lauki, salads, and small portions of millets or phulka. Keep protein and vegetables first, grains last."
            q.contains("pre-workout") || q.contains("workout") ->
                "For light exercise, try buttermilk, roasted chana, half apple with nuts, or curd. If you use insulin or medicines that can cause low sugar, check glucose first and keep fast carbs nearby."
            q.contains("breakfast") ->
                "Low-GI Indian breakfast options:\n\n- 2 idlis with sambar and extra vegetables.\n- Ragi dosa or pesarattu with sambar.\n- Besan chilla with curd.\n- Oats vegetable upma with sprouts.\n- Boiled egg or paneer/tofu with 1 small phulka.\n\nAdd protein every time: dal, curd, egg, sprouts, paneer, tofu, fish, or chicken. Avoid sweet tea, fruit juice, white bread, jam, and large poha/upma portions without protein."
            q.contains("lunch") ->
                "Diabetes-friendly lunch ideas:\n\n- 2 phulkas + dal + one cooked vegetable + curd + salad.\n- Small millet rice + sambar + poriyal + buttermilk.\n- Brown rice small cup + fish/egg/paneer curry + greens.\n- Chana/rajma bowl with cucumber, onion, tomato, and curd.\n\nTip: finish vegetables and protein first, then grains. This can reduce the post-meal spike."
            q.contains("snack") ->
                "Healthy snack options for diabetes:\n\n- Roasted chana or peanuts, small handful.\n- Sprouts chaat without sev.\n- Unsweetened curd with cucumber.\n- Buttermilk.\n- Half apple/guava with nuts.\n- Boiled egg or paneer cubes.\n\nAvoid biscuits, fruit juice, bakery snacks, chips, sweets, and tea with sugar."
            q.contains("foot") ->
                "Diabetic foot care:\n\nCheck both feet daily, including soles and between toes. Wash with lukewarm water and dry well. Moisturise dry skin but not between toes. Never walk barefoot. Use soft, well-fitting footwear. Seek care urgently for cuts, blisters, redness, swelling, pus, black skin, numbness, burning pain, or wounds that do not heal."
            q.contains("low sugar") || q.contains("hypo") ->
                "Low sugar care:\n\nSymptoms include sweating, shaking, hunger, fast heartbeat, dizziness, confusion, weakness, or blurred vision. If glucose is below 70 mg/dL, take 15 g fast carbohydrate, recheck after 15 minutes, and repeat if still low. If unconscious, severely confused, or unable to swallow, seek emergency care."
            q.contains("hba1c") || q.contains("a1c") ->
                "HbA1c is your approximate 3-month glucose average. Many adults target around 7%, but your doctor may personalise it. Improve it with regular medicines, fasting/post-meal logs, low-GI meals, post-meal walking, sleep, and follow-up visits."
            q.contains("medicine") || q.contains("missed dose") ->
                "Medicine safety:\n\nTake medicines at the prescribed time. Do not double a missed dose unless your doctor advised it. For insulin, confirm dose, site, timing, and expiry. Carry fast sugar if your medicine can cause low glucose. Report repeated lows or severe side effects."
            q.contains("sick") || q.contains("vomit") ->
                "Sick-day rules:\n\nCheck glucose more often, drink fluids unless restricted, continue medicines unless your doctor gave a sick-day plan, and seek care for repeated vomiting, dehydration, ketones, drowsiness, breathlessness, or very high readings."
            else ->
                "Based on glucose $latestGlucose mg/dL and HbA1c ${"%.1f".format(latestHba1c)}%, choose low-GI foods such as methi, karela, lauki, moong dal, curd, ragi in controlled portions, and high-fibre vegetables. Keep rice to one small cup and pair it with dal/protein, curd, and salad."
        }
    }

    override fun onDestroyView() {
        binding.nutritionWebView.apply {
            stopLoading()
            removeJavascriptInterface("Android")
            removeJavascriptInterface("AndroidNutrition")
            webViewClient = WebViewClient()
            destroy()
        }
        _binding = null
        super.onDestroyView()
    }
}
