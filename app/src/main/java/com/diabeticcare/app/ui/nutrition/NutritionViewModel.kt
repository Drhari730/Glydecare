package com.diabeticcare.app.ui.nutrition

import androidx.lifecycle.*
import com.diabeticcare.app.BuildConfig
import com.diabeticcare.app.data.model.GlucoseStatus
import com.diabeticcare.app.data.model.MealLog
import com.diabeticcare.app.data.repository.GlucoseRepository
import com.diabeticcare.app.data.repository.MealRepository
import com.diabeticcare.app.network.ClaudeClient
import com.diabeticcare.app.network.ClaudeMessage
import com.diabeticcare.app.network.ClaudeRequest
import kotlinx.coroutines.launch

class NutritionViewModel(
    private val mealRepo: MealRepository,
    private val glucoseRepo: GlucoseRepository
) : ViewModel() {

    val meals = mealRepo.allMeals

    private val _aiSuggestion = MutableLiveData<String>("")
    val aiSuggestion: LiveData<String> = _aiSuggestion

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun logMeal(meal: MealLog) {
        viewModelScope.launch { mealRepo.insert(meal) }
    }

    fun deleteMeal(meal: MealLog) {
        viewModelScope.launch { mealRepo.delete(meal) }
    }

    fun getAiSuggestion() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            val latestGlucose = glucoseRepo.getLatest()
            val latestMeal = mealRepo.getLatest()

            val glucoseInfo = if (latestGlucose != null)
                "Blood glucose: ${latestGlucose.value} mg/dL (${latestGlucose.status.name.lowercase()})"
            else "No recent glucose reading"

            val mealInfo = if (latestMeal != null)
                "Last meal: ${latestMeal.mealType.lowercase()} — ${latestMeal.portionSize.lowercase()} portion of ${latestMeal.foodCategory.replace("_"," ").lowercase()}"
            else "No recent meal logged"

            val apiKey = BuildConfig.CLAUDE_API_KEY
            if (apiKey.isBlank() || apiKey == "\"\"") {
                // Fallback to rule-based suggestion
                _aiSuggestion.postValue(buildRuleSuggestion(latestGlucose?.status, latestGlucose?.value))
                _isLoading.postValue(false)
                return@launch
            }

            try {
                val prompt = """Patient: Indian T2DM adult. $glucoseInfo. $mealInfo.
Give 2-sentence South Indian diet advice for next meal. Be specific (idli/dosa/rice/roti quantities). Focus on glycemic control."""

                val response = ClaudeClient.service.sendMessage(
                    apiKey = apiKey,
                    request = ClaudeRequest(
                        model = "claude-haiku-4-5",
                        max_tokens = 120,
                        messages = listOf(ClaudeMessage("user", prompt))
                    )
                )
                val text = response.content.firstOrNull()?.text ?: buildRuleSuggestion(latestGlucose?.status, latestGlucose?.value)
                _aiSuggestion.postValue(text)
            } catch (e: Exception) {
                _aiSuggestion.postValue(buildRuleSuggestion(latestGlucose?.status, latestGlucose?.value))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun buildRuleSuggestion(status: GlucoseStatus?, value: Int?): String = when (status) {
        GlucoseStatus.LOW -> "Glucose low${if (value != null) " ($value mg/dL)" else ""}. Have 15g fast carbs — banana, juice, or 3 glucose tablets. Follow with idli/roti after 15 min."
        GlucoseStatus.NORMAL -> "Glucose normal${if (value != null) " ($value mg/dL)" else ""}. Good time for a balanced meal — 1 cup rice or 2 rotis with sambar, sabzi, and curd. Avoid sweets."
        GlucoseStatus.HIGH -> "Glucose elevated${if (value != null) " ($value mg/dL)" else ""}. Skip rice/roti for this meal. Have vegetable soup, dal, salad, or buttermilk. Take a 15-min walk."
        GlucoseStatus.VERY_HIGH -> "Glucose very high${if (value != null) " ($value mg/dL)" else ""}. Avoid all carbs now. Drink water, take medication if due. Contact doctor if persistent."
        null -> "Log your glucose reading to get personalized South Indian diet advice."
    }
}

class NutritionViewModelFactory(
    private val mealRepo: MealRepository,
    private val glucoseRepo: GlucoseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NutritionViewModel(mealRepo, glucoseRepo) as T
    }
}
