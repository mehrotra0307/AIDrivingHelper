# AIDrivingHelper

An Android driving coach that uses **RAG (Retrieval-Augmented Generation) entirely on-device** to answer natural language questions about past trips and deliver personalized coaching feedback — with voice input and voice output.

Ask: *"Was I speeding more at night?"* — get back a coaching response grounded in your actual trip data. No cloud. No data leaves the phone.

> This project is an independent proof-of-concept built for learning purposes using synthetic/demo data. It is not affiliated with or based on any proprietary systems or data.

## 🎥 Watch it in action

[![Watch the demo](https://img.youtube.com/vi/DAynPP6EHvE/maxresdefault.jpg)](https://www.youtube.com/watch?v=DAynPP6EHvE)

Full project write-up: [ashishmehrotra.com/projects/ai-driving-helper](https://ashishmehrotra.com/projects/ai-driving-helper)

## Features

- **RAG-based trip retrieval** — semantic search over stored driving trips using Gecko embeddings and SqliteVectorStore
- **On-device LLM inference** — Gemma 3 1B IT (quantized int4) via MediaPipe
- **Voice input** — ask questions via microphone (Android SpeechRecognizer)
- **Voice output** — coaching responses read aloud (Android TextToSpeech)
- **Trip persistence** — all trips stored in Room database, embedded for semantic search
- **Coaching tone** — prompt-engineered to respond in second person ("you were speeding") like a real coach

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| On-device LLM | MediaPipe tasks-genai 0.10.23 (Gemma 3 1B IT int4) |
| RAG Framework | Google AI Edge local-agents 0.2.0 |
| Embeddings | GeckoEmbeddingModel (768-dim, on-device) |
| Vector Store | SqliteVectorStore (local semantic search) |
| Database | Room 2.6.1 |
| Voice I/O | Android SpeechRecognizer + TextToSpeech API |
| Async | Kotlin Coroutines + Guava bridge |
| Min SDK | 26 (Android 8.0) |

## Architecture

MVVM:
```
RAGChatUI (Compose)
    ↓
LLMViewModel
    ├── LLMInferenceChain (RAG pipeline setup)
    │   ├── GeckoEmbeddingModel (768-dim embeddings)
    │   ├── SqliteVectorStore (persistent vector DB)
    │   ├── DefaultSemanticTextMemory
    │   └── RetrievalAndInferenceChain (top-k=4)
    └── TripDao (Room)
```

## RAG Pipeline

```
User voice query
    → GeckoEmbeddingModel (embed query)
    → SqliteVectorStore (semantic search, top-k=4)
    → 4 most relevant trips retrieved
    → Gemma 3 1B IT + retrieved context
    → Coaching response (text + TTS)
```

Retrieval config: top-k=4 — retrieve the 4 most semantically similar trips to keep the LLM context window focused.

## Prompt Engineering

The system prompt instructs the model to:
- Act as a driving coach
- Use second-person language ("you were speeding", not "the driver was speeding")
- Only use information from retrieved trips (not general knowledge)
- Avoid hallucination

**LLM settings:** Temperature 1.0 / Top-P 0.95 / Top-K 64

## Setup

### Prerequisites

- Android 8.0+ (API 26+)
- Device with sufficient RAM for Gemma 3 1B (~2 GB)

### Model Setup

Push the Gemma 3 1B IT quantized model to your device:

```bash
adb push gemma3-1b-it-int4.task /data/local/tmp/llm/
```

The app expects the model at `/data/local/tmp/llm/gemma3-1b-it-int4.task`.

You also need the Gecko embedding model. Check `LLMInferenceChain.kt` for the expected asset path.

### Build

```bash
git clone https://github.com/mehrotra0307/AIDrivingHelper
cd AIDrivingHelper
# Open in Android Studio and run on device
```

## Demo Data

7 synthetic trips are pre-loaded for demonstration:
- Speeding events (highway + residential)
- Phone usage events
- Sudden braking events
- Hard acceleration events

These are inserted into the RAG vector store on first launch.

## Related Projects

- **AIDrivingHelper2** — extends this with geographic context enrichment (nearby schools, hospitals, road names)
- **CrashFNOLDemo** — uses similar telematics data for insurance FNOL report generation
- **OnDeviceFNOL** — combines RAG + voice + geo-enrichment for FNOL reports
