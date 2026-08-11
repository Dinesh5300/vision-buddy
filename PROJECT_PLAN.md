# Vision Buddy AI Glass — Software Prototype Plan

Smart glass that helps visually impaired users see the world through **voice**.

**Core flow:**

```
Camera / Sensors (Smart Glass)
        │  (BLE — Phase 2)
        ▼
   Mobile App  ──►  AI Processing (on-device)
        ▼
   Voice Output (TTS)
```

**Phone-first prototype:** In Phase 1 the phone's own camera stands in for the smart glass camera. Everything is processed on the phone (no cloud needed). BLE + GPS + Firebase come later.

---

## Phase 1 — First Working Prototype (this project)

| Step | Feature | Tech | Status |
|------|---------|------|--------|
| 1 | Android Studio project + Home UI (5 buttons) | Kotlin, XML Views | ✅ in repo |
| 2 | Live camera capture | CameraX (Preview + ImageAnalysis) | ✅ in repo |
| 3 | Text reading: camera → OCR → Voice | Google ML Kit Text Recognition | ✅ in repo |
| 4 | Voice output | Android Text-to-Speech (TTS) | ✅ in repo |
| 5 | Object detection: camera → YOLO → Voice | YOLOv5n (YOLOv8-compatible decoder) TFLite | ✅ in repo |
| 6 | Voice commands: "Read this" → camera → OCR → TTS | Android SpeechRecognizer | ✅ in repo |
| 7 | Start Assistance (OCR + YOLO combined) | ML Kit + TFLite | ✅ in repo |
| 8 | SOS (placeholder UI, real service in Phase 2) | — | 🔲 wired, Firebase later |

**Demo 1:** point camera at a sign ("BUS STOP") → hear "Bus stop".
**Demo 2:** point camera at a person → hear "Person ahead".
**Demo 3:** say "Read this" → app reads the text aloud automatically.

---

## Phase 2 — Hardware + Connectivity (NOT started)

| Step | Feature | Tech |
|------|---------|------|
| 9 | Smart glass camera/sensors → phone | Bluetooth BLE (send frames, gestures, buttons) |
| 10 | Location + navigation guidance | GPS / FusedLocationProvider |
| 11 | SOS: send alert + live location to emergency contacts | Firebase (Auth, Firestore, FCM, Location Sharing) |

---

## Phase 3 — Smarter AI (optional, later)

| Step | Feature | Tech |
|------|---------|------|
| 12 | Scene description ("You are in a park") | Gemini/Cloud Vision API (optional cloud) |
| 13 | Face/familiar people recognition | on-device embeddings |
| 14 | Currency/color/door detection extras | Fine-tuned YOLO or ML Kit classification |
| 15 | Navigation ("turn left in 20 m") | GPS + TTS turn-by-turn |

---

## Build & Run

1. Install **Android Studio** (Ladybug or newer) — it bundles JDK 21, Gradle, Android SDK.
2. Open the folder `Vision Buddy AI Glass`.
3. Let Gradle sync (first sync downloads dependencies — needs internet).
4. Run on a physical phone (or emulator) with a camera.
5. Accept Camera + Microphone permissions when prompted.

### Project structure

```
app/src/main/java/com/visionbuddy/glass/
├── MainActivity.kt                 # Home screen (5 actions)
├── assist/AssistActivity.kt        # Start Assistance: OCR + YOLO together
├── read/TextReaderActivity.kt      # Read Text: OCR + TTS
├── detect/ObjectDetectorActivity.kt# Detect Object: YOLO + TTS
├── voice/VoiceCommandActivity.kt   # Voice Command → action
├── sos/SOSActivity.kt              # SOS (placeholder for Phase 2)
├── core/CameraXHelper.kt           # Reusable CameraX setup
├── core/OcrHelper.kt               # ML Kit text recognition
├── core/YoloDetector.kt            # TFLite YOLO post-processing + NMS
├── core/TtsManager.kt              # Speech output queue
└── assets/
    ├── yolov5n-fp16-320.tflite     # YOLO model (~3.7 MB, official ultralytics build)
    └── coco_labels.txt             # 80 COCO class names
```

### Tech stack (Phase 1)

- Kotlin 2.0, minSdk 26 (Android 8.0+), targetSdk 35
- CameraX 1.3.4, ML Kit text-recognition 16.0.1 (bundled, works offline)
- TensorFlow Lite 2.16.1 + YOLOv5n TFLite (COCO 80 classes; the decoder also accepts YOLOv8 exports)
- Android SpeechRecognizer + TextToSpeech

### Roadmap checkpoints

- [x] Step 1–8 code written (open in Android Studio, sync, run)
- [ ] Test Demo 1 (OCR → voice)
- [ ] Test Demo 2 (YOLO → voice)
- [ ] Test Demo 3 (voice command → action)
- [ ] Phase 2: BLE → GPS → Firebase (later sessions)
