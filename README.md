<div align="center">
  <h1>Bushy ☁️</h1>
  <p><b>Turn your real-life grind into a game. Complete fitness quests, earn XP, level up your character, and flex your progress. 🎮💪</b></p>
</div>

---

## 📖 About Bushy
**Bushy** is an innovative Android application that transforms your daily health and fitness routines into an engaging RPG-style experience. By integrating with Android's Health Connect, Bushy automatically tracks your physical activities (like steps) and converts them into Experience Points (XP). Level up your character, complete daily fitness quests, and stay motivated!

Beyond gamification, Bushy features a **Local AI Coach** running entirely on-device (via ONNX Runtime), ensuring lightning-fast advice and 100% privacy for your health questions.

## ✨ Features
* **🎮 Gamified Fitness:** Every step and logged exercise earns you XP. Watch your character level up and evolve as you get healthier.
* **🤖 Local AI Coach (Bushy AI):** Powered by ONNX Runtime, our on-device AI runs directly on your phone. Ask for workout plans, nutritional advice, and motivation—no internet connection required!
* **🔗 Health Connect Integration:** Seamlessly syncs your daily steps and health metrics securely from other fitness apps.
* **🎯 Daily Quests & Tasks:** Set custom targets for steps, pushups, and more.
* **🥗 Indian Meal Calorie Estimator:** Photo scanning & interactive food portion editor powered by an offline Indian Nutrition Database (Anuvaad INDB / ICMR standards). Supports portion choices (*Katori*, *Piece*, *Cup*, *Tbsp*, *Grams*) and preparation styles (*Homemade Low-Oil* vs *Restaurant Rich*).
* **👤 Dynamic Avatars:** Your avatar's expression changes based on your current fitness streak and activity.
* **🎨 Modern UI:** Built entirely using Jetpack Compose with beautiful Material 3 design, dynamic theming, and Dark Mode support.

## 🧠 How It Works (Under the Hood)

### 1. The Core Concept: Gamified Fitness RPG
Your fitness journey is treated like a Role-Playing Game (RPG). 
* **XP & Leveling:** Earn Experience Points (XP) by walking and completing tasks. Your level scales mathematically (`Level = sqrt(XP / 100) + 1`), meaning leveling up gets progressively harder.
* **Dynamic Avatar:** Your avatar reacts to your progress. Hit your step goals and the avatar becomes celebratory; tap it and it reacts dynamically.

### 2. The Brains: Local AI (Bushy Wushy)
Bushy features a 100% on-device, offline AI assistant utilizing **ONNX Runtime**.
* **The Models:** Offers two state-of-the-art Small Language Models (SLMs) fine-tuned for mobile:
  * **SmolLM2 360M:** Lightweight (~360 MB), ultra-fast model by Hugging Face.
  * **Qwen 2.5 0.5B:** Highly capable balanced model (~512 MB) by Alibaba Cloud.
* **Contextual Awareness:** The AI automatically reads your current stats (steps, calories, country) to give highly contextual answers (e.g., suggesting local Indian cuisine based on your remaining daily calories).

```mermaid
graph TD
    A[User Asks Question] --> B{Local Ai Manager}
    B --> C[Inject UserStats: XP, Steps, Diet, Country]
    C --> D[ONNX Runtime Session]
    D --> E[(Local SLM: SmolLM2 / Qwen)]
    E --> F[Generate Offline Response]
    F --> G[Display to User]
    
    style A fill:#4CAF50,stroke:#388E3C,stroke-width:2px,color:white
    style D fill:#2196F3,stroke:#1976D2,stroke-width:2px,color:white
    style E fill:#FF9800,stroke:#F57C00,stroke-width:2px,color:white
```

### 3. Indian Meal Calorie Estimation & Nutrition Engine
Bushy includes an **Indian Meal Calorie Estimator** designed specifically for Indian food culture:
* **Separation of Vision & Nutrition:** Food identification is decoupled from calorie math. The vision pipeline (`VisionFoodAnalyzer`) identifies candidate dishes, while the offline `IndianNutritionDatabase` calculates exact calories and macros.
* **Multilingual Dish Name Correction:** Users can tap any dish name to correct it in English or **9 Indian state languages & scripts**:
  - Devanagari (Hindi / Marathi)
  - Bengali / Assamese
  - Tamil
  - Telugu
  - Gujarati
  - Kannada
  - Malayalam
  - Gurmukhi (Punjabi)
  - English & Romanized Transliterations / Hinglish
* **Fuzzy Typo & Ambiguity Matcher (`MultilingualDishInterpreter`):** Handles common typos (e.g., *paner*, *chappati*, *biryanie*) using Levenshtein distance, suggests non-destructive corrections, and disambiguates generic queries (e.g. *dal* $\rightarrow$ Dal Tadka, Dal Makhani, Dal Khichdi).
* **Portion Units:** Users can adjust serving sizes using intuitive Indian culinary units: `Katori (Bowl)`, `Piece`, `Cup`, `Tablespoon`, or `Grams`.
* **Preparation Uncertainty:** Recipes vary widely in oil and ghee usage. Bushy allows users to toggle preparation styles (`Homemade Low-Oil`, `Standard`, `Restaurant Rich`) and displays realistic calorie uncertainty ranges (e.g., `310 - 380 kcal`).
* **Offline Nutrition Ground Truth:** Based on Anuvaad INDB and ICMR nutrition data.

#### Local Qwen Vision Model (VLM) Runtime Considerations
* **Feasibility & Trade-offs:** Running multi-modal Vision-Language Models on-device (like Qwen2-VL-2B or Qwen3-VL) requires 1.8 GB – 2.2 GB 4-bit quantized files and 6 GB+ RAM. Standard `onnxruntime-android` handles single-graph execution; multi-modal VLM execution requires native C++ JNI bindings (such as `llama.cpp` or `Executorch`).
* **Transparent Pipeline:** Bushy uses a modular `VisionFoodAnalyzer` abstraction layer and provides an **AI Status Info Dialog**, clearly distinguishing between heuristic feature recognition and local VLM inference.

### 4. Data Syncing: Health Connect
Instead of draining battery by counting steps in the background, Bushy integrates with the **Android Health Connect API**.
* Securely reads aggregate daily steps from midnight.
* Automatically updates your XP and step missions when you open the app.

```mermaid
sequenceDiagram
    participant User
    participant BushyApp
    participant HealthConnect
    participant DeviceSensors

    DeviceSensors->>HealthConnect: Record Steps (Background)
    User->>BushyApp: Opens App
    BushyApp->>HealthConnect: Request Daily Aggregate Steps
    HealthConnect-->>BushyApp: Return Step Count
    BushyApp->>BushyApp: Calculate XP & Update Level
    BushyApp->>BushyApp: Check Mission Completion
    BushyApp-->>User: Display Updated RPG Stats & Avatar
```

## 🛠 Tech Stack
* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Architecture:** MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
* **Health Data:** [Android Health Connect API](https://developer.android.com/health-and-fitness/guides/health-connect)
* **On-Device Machine Learning:** [ONNX Runtime for Android](https://onnxruntime.ai/docs/tutorials/mobile/)
* **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
* **Camera Features:** [CameraX](https://developer.android.com/training/camerax) (Support for 16 KB page size architecture)

## 📱 Requirements
* Android Device running **Android 8.0 (API 26)** or higher.
* To use the Health Connect features, the [Health Connect](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata) app must be installed on older devices (built-in on Android 14+).

## 🚀 How to Build and Run
1. Clone the repository:
   ```bash
   git clone https://github.com/krishdeokk/Bushy.git
   ```
2. Open the project in **Android Studio** (Koala Feature Drop or newer recommended).
3. Let Gradle sync the dependencies.
4. If you plan to test the on-device AI, be aware that models will be downloaded locally upon first use.
5. Connect your device or emulator and click **Run**.

## 🛡 Privacy First
Because health data is deeply personal, **Bushy** respects your privacy. 
* Steps and activity data are read locally via **Health Connect**.
* AI interactions are processed locally using **ONNX Runtime**—your prompts and fitness questions never leave your device.

---
<div align="center">
  <i>Developed with ❤️ by <a href="mailto:krishdeokate206@gmail.com">krishdeokk</a></i>
</div>
