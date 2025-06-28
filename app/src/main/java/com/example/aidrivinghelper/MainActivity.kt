package com.example.aidrivinghelper

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import com.example.aidrivinghelper.ui.theme.AIDrivingHelperTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var llmViewModel: LLMViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        llmViewModel= LLMViewModel(application)

        enableEdgeToEdge()
        lifecycleScope.launch {
            // Step 1: Insert demo trips (if needed)
            llmViewModel.insertDemoTrips(applicationContext)
            llmViewModel.embedTripsFromDatabase(applicationContext)
        }
        setContent {
            AIDrivingHelperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RAGChatUI(
                        viewModel = llmViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }




//        lifecycleScope.launch {
//            llmViewModel.memorizeChunk("sample_context.txt")
//        }
    }
}

//@Composable
//fun RAGChatUI(viewModel: LLMViewModel, modifier: Modifier = Modifier) {
//    val input by viewModel.userInput.collectAsState()
//    val response by viewModel.response.collectAsState()
//
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        OutlinedTextField(
//            value = input,
//            onValueChange = viewModel::onInputChanged,
//            label = { Text("Ask something about your trips") },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Button(
//            onClick = viewModel::onSubmit,
//            modifier = Modifier.align(Alignment.End)
//        ) {
//            Text("Submit")
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Text(
//            text = "LLM Response:",
//            style = MaterialTheme.typography.titleMedium
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Text(
//            text = response,
//            style = MaterialTheme.typography.bodyLarge
//        )
//    }
//}

@Composable
fun RAGChatUI(viewModel: LLMViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val input by viewModel.userInput.collectAsState()
    val response by viewModel.response.collectAsState()

    // Setup Text-to-Speech
    val tts = remember {
        var ttsInstance: TextToSpeech? = null
        TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                ttsInstance?.language = Locale.US
            }
        }.also { ttsInstance = it }
    }


    // Setup Speech-to-Text
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()

        spokenText?.let {
            // Set the input field and trigger LLM query
            viewModel.onInputChanged(it)
            viewModel.onSubmit()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Ask something about your trips",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = input,
            onValueChange = viewModel::onInputChanged,
            label = { Text("Type or speak your question") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = viewModel::onSubmit) {
                Text("Submit")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                launcher.launch(intent)
            }) {
                Icon(Icons.Filled.Mic, contentDescription = "Mic")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "LLM Response:", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        //Text(text = response, style = MaterialTheme.typography.bodyLarge)
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = response,
                style = MaterialTheme.typography.bodyLarge
            )
        }


        // Speak response out loud
        LaunchedEffect(response) {
            if (response.isNotBlank()) {
                tts.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }
}






