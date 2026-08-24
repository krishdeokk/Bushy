package com.bushy.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BushyAIViewModel : ViewModel() {

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isUserTyping = MutableStateFlow(false)
    val isUserTyping: StateFlow<Boolean> = _isUserTyping.asStateFlow()

    private val _messages = MutableStateFlow<List<BushyAIMessage>>(emptyList())
    val messages: StateFlow<List<BushyAIMessage>> = _messages.asStateFlow()

    // Temporary API Key - optimized for app-specific answers
    private val apiKey = "AQ.Ab8RN6IbkKLQRzGzF-pbLSvaaCbii0VED7OreAQfitwfV8FQpw"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash-lite",
        apiKey = apiKey,
        systemInstruction = content {
            text("You are Bushy, an AI assistant for the Bushy fitness app. " +
                 "Your only job is to answer questions related to the Bushy app, fitness progress, and health stats. " +
                 "Do not discuss legal matters, or any topics outside of the app's scope. " +
                 "Keep your responses extremely concise: 1 to 3 sentences max. " + "There are only three tabs in app one is home tab for stats, second is for deplaying missions and third is for AI named Bushy Wushy" +
                 "If a question is outside the app's scope, politely say you can only help with Bushy app features." + "If a person ask for whats he/she should eat today ask from which country he is and suggest the healthy meal accordingly")
        }
    )

    fun onUserTyping(isTyping: Boolean) {
        _isUserTyping.value = isTyping
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMessage = BushyAIMessage(text = userText, isUser = true)
        _messages.update { it + userMessage }
        
        _isGenerating.value = true
        _isUserTyping.value = false

        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(userText)
                val aiText = response.text ?: "I am sorry, I couldn't process that."
                
                val aiMessage = BushyAIMessage(
                    text = aiText.trim(),
                    isUser = false
                )
                _messages.update { it + aiMessage }
            } catch (e: Exception) {
                _messages.update { it + BushyAIMessage(text = "Error: ${e.localizedMessage}", isUser = false) }
            } finally {
                _isGenerating.value = false
            }
        }
    }
}
