# Vision Buddy AI Glass

AI-powered vision assistance for visually impaired users. Point the phone camera
(Phase 1 stands in for the smart glass camera) and the app **sees** for you and
**speaks** what it finds — all on-device, no internet needed.

## Working Prototype (Phase 1)

| Feature | How it works |
|---|---|
| **Read Text** | Camera → ML Kit OCR → text on screen + spoken aloud |
| **Detect Object** | Camera → YOLO (TFLite) → object names + confidence spoken aloud |
| **Start Assistance** | OCR + YOLO running together, announces anything new |
| **Voice Command** | Say "Read this", "Detect object", "Help", "Start assistance" |
| **SOS** | Placeholder UI (Firebase alert + location come in Phase 2) |

## Build & Run

1. Install **Android Studio** (Ladybug or newer). It bundles JDK 21, Gradle and the Android SDK.
2. Open the folder `Vision Buddy AI Glass` (File → Open).
3. Let Gradle sync finish (first sync downloads dependencies, needs internet).
4. Connect a physical phone (recommended — emulator cameras are fake), press Run.
5. Grant **Camera** and **Microphone** permissions.

> Tip: no Android Studio on this PC yet — install it from
> https://developer.android.com/studio. Everything else is already in this folder,
> including the Gradle wrapper and the YOLO model.

## Demo Script

1. **Read Text**: open Read Text, point at a sign ("BUS STOP") → hear **"Bus stop"**.
2. **Detect Object**: open Detect Object, point at a person → hear **"Person"**.
3. **Voice Command**: open Voice Command, say **"Read this"** → app opens the reader automatically.

## Key Files

```
app/src/main/java/com/visionbuddy/glass/
├── MainActivity.kt                  # Home screen
├── assist/AssistActivity.kt         # Combined OCR + YOLO
├── read/TextReaderActivity.kt       # OCR → voice
├── detect/ObjectDetectorActivity.kt # YOLO → voice
├── voice/VoiceCommandActivity.kt    # Speech recognition → action
├── sos/SOSActivity.kt               # SOS placeholder
└── core/
    ├── CameraXManager.kt            # CameraX setup (preview + analysis)
    ├── OcrHelper.kt                 # ML Kit text recognition
    ├── YoloDetector.kt              # TFLite YOLO inference + NMS
    └── TtsManager.kt                # Text-to-speech (voice output)
```

## Tech Stack

- Kotlin 2.0, minSdk 26, targetSdk 35, AGP 8.5.2, Gradle 8.7
- CameraX 1.3.4 (live preview + frame analysis)
- Google ML Kit Text Recognition 16.0.1 (bundled model — works offline)
- TensorFlow Lite 2.16.1 + `yolov5n-fp16-320.tflite` (official ultralytics build, COCO 80 classes)
- Android `SpeechRecognizer` + `TextToSpeech`

## Roadmap (later phases)

- **BLE** — smart glass camera/sensors stream frames to the phone
- **GPS** — location + navigation guidance
- **Firebase** — SOS emergency contacts, SMS, live location sharing
- **Cloud AI (optional)** — scene descriptions via Gemini/Cloud Vision

Full plan: see `PROJECT_PLAN.md`.
