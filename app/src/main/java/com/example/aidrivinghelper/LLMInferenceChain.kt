package com.example.aidrivinghelper

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.ai.edge.localagents.rag.chains.ChainConfig
import com.google.ai.edge.localagents.rag.chains.RetrievalAndInferenceChain
import com.google.ai.edge.localagents.rag.memory.DefaultSemanticTextMemory
import com.google.ai.edge.localagents.rag.memory.SqliteVectorStore
import com.google.ai.edge.localagents.rag.models.AsyncProgressListener
import com.google.ai.edge.localagents.rag.models.Embedder
import com.google.ai.edge.localagents.rag.models.GeckoEmbeddingModel
import com.google.ai.edge.localagents.rag.models.LanguageModelResponse
import com.google.ai.edge.localagents.rag.models.MediaPipeLlmBackend
import com.google.ai.edge.localagents.rag.prompt.PromptBuilder
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig
import com.google.ai.edge.localagents.rag.retrieval.RetrievalRequest
import com.google.android.gms.tasks.Task
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Optional
import java.util.concurrent.Executors
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.tasks.await


class LLMInferenceChain(private val application: Application) {

    private val embeddingModelPath = "/data/local/tmp/Gecko_256_quant.tflite"
    private val tokenizerModelPath = "/data/local/tmp/sentencepiece.model"
    private val gemmaTaskModelPath =
        "/data/local/tmp/llm/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task"

    private val _facts: MutableStateFlow<String> = MutableStateFlow("")
    val factsString: StateFlow<String> = _facts.asStateFlow()

    private val llmInferenceOptions: LlmInferenceOptions =
        LlmInferenceOptions.builder().setModelPath(gemmaTaskModelPath).setPreferredBackend(
            LlmInference.Backend.CPU
        ).setMaxTokens(1024).build()

    private val llmInferenceSessionOptions: LlmInferenceSession.LlmInferenceSessionOptions =
        LlmInferenceSession.LlmInferenceSessionOptions.builder().setTemperature(1.0f).setTopP(0.95f)
            .setTopK(64).build()


    val mediaPipeLlmBackend: MediaPipeLlmBackend =
        MediaPipeLlmBackend(
            application.applicationContext,
            llmInferenceOptions,
            llmInferenceSessionOptions
        )


    // RAG PART STARTS HERE----------------------------------------------------------

    private val embedder: Embedder<String> =
        GeckoEmbeddingModel(embeddingModelPath, Optional.of(tokenizerModelPath), false)


    val promptTemplate = """
                You are a helpful assistant for drivers. Use the following past trip data:
                {1}
                Now answer this: {0}
            """.trimIndent()

//    val promptTemplate = """
//You are a helpful AI assistant. Below is information from the user's past trips:
//
//{1}
//
//Based strictly on this data, answer the following question:
//{0}
//
//If the data is not enough, say "I don't know based on the available data."
//""".trimIndent()


    private val config = ChainConfig.create(
        mediaPipeLlmBackend,
        PromptBuilder(promptTemplate),
        DefaultSemanticTextMemory(SqliteVectorStore(768), embedder)
    )

    private val retrievalAndInferenceChain = RetrievalAndInferenceChain(config)


    init {
        Futures.addCallback(
            mediaPipeLlmBackend.initialize(),
            object : FutureCallback<Boolean> {
                override fun onSuccess(result: Boolean?) {

                }

                override fun onFailure(t: Throwable) {

                }
            },
            Executors.newSingleThreadExecutor(),
        )
    }

    fun memorizeChunks(context: Context, fileName: String) {

        val reader = BufferedReader(InputStreamReader(context.assets.open(fileName)))

        val sb = StringBuilder()

        val texts = mutableListOf<String>()

        generateSequence { reader.readLine() }
            .forEach { line ->
                if (line.startsWith("chunk")) {
                    if (sb.isNotEmpty()) {
                        val chunk = sb.toString()
                        texts.add(chunk)
                    }
                    sb.clear()
                    sb.append(line.removePrefix("chunk").trim())
                } else {
                    sb.append(" ")
                    sb.append(line)

                }
            }
        if (sb.isNotEmpty()) {
            texts.add(sb.toString())
        }
        reader.close()
        if (texts.isNotEmpty()) {
            _facts.value = texts.toString()
            return memorize(texts)
        }
    }

    fun memorize(facts: List<String>) {
        val future =
            config.semanticMemory.getOrNull()?.recordBatchedMemoryItems(ImmutableList.copyOf(facts))
        future?.get()
    }


    suspend fun generateResponse(
        prmopt: String,
        callback: AsyncProgressListener<LanguageModelResponse>?
    ): String = coroutineScope {
        Log.d("LLMCheck", "prompt is $prmopt")
        val retrievalRequest = RetrievalRequest.create(
            prmopt,
            RetrievalConfig.create(3, 0.0f, RetrievalConfig.TaskType.QUESTION_ANSWERING)
        )
        Log.d(
            "LLMCheck",
            "retrieval response is ${
                retrievalAndInferenceChain.invoke(retrievalRequest, callback).await().text
            }"
        )
        retrievalAndInferenceChain.invoke(retrievalRequest, callback).await().text
    }
}