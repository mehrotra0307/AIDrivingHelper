package com.example.aidrivinghelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import com.example.aidrivinghelper.ui.theme.AIDrivingHelperTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var llmViewModel: LLMViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        llmViewModel= LLMViewModel(application)

        enableEdgeToEdge()
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

        lifecycleScope.launch {
            llmViewModel.memorizeChunk("sample_context.txt")
        }
    }
}

@Composable
fun RAGChatUI(viewModel: LLMViewModel, modifier: Modifier = Modifier) {
    val input by viewModel.userInput.collectAsState()
    val response by viewModel.response.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = viewModel::onInputChanged,
            label = { Text("Ask something about your trips") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = viewModel::onSubmit,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Submit")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "LLM Response:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = response,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
