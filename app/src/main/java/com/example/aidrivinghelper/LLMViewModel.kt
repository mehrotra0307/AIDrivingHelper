package com.example.aidrivinghelper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher

class LLMViewModel(private val application: Application) : AndroidViewModel(application) {

    private val llmInferenceChain = LLMInferenceChain(application)
    private val backgroundExecutor: Executor = Executors.newSingleThreadExecutor()

    // Observables for Compose UI
    private val _userInput = MutableStateFlow("")
    val userInput: StateFlow<String> = _userInput.asStateFlow()

    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response.asStateFlow()

    fun onInputChanged(input: String) {
        _userInput.value = input
    }

    fun onSubmit() {
        viewModelScope.launch {
            val result = llmInferenceChain.generateResponse(_userInput.value, null)
            _response.value = result
        }
    }

    suspend fun memorizeChunk(fileName: String) {
        withContext(backgroundExecutor.asCoroutineDispatcher()) {
            llmInferenceChain.memorizeChunks(application.applicationContext, fileName)
        }
    }
}
