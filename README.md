# ☁️ Bushy iOS — Gamified Health & Procedural Companion

> An intelligent, privacy-first iOS companion that turns personal health, daily movement, and Indian nutrition tracking into an interactive, gamified journey powered by procedural physics and offline AI.

---

## 🌟 Key Highlights

- **Procedural Bloub Character Engine**: 64-radial Catmull-Rom spline deformation, dynamic 3D gaze tracking, harmonic breathing, and quintic-eased state machines rendered live via SwiftUI `Canvas` and `TimelineView`.
- **Liquid Glass & High-Contrast Design System**: Multi-layered `.ultraThinMaterial` glass cards, specular highlights, dynamic ambient fluid background orbs, and full support for both Vibrant Material 3 and Ultra-Minimalist Monochrome styles.
- **Offline Indian Nutrition & NLP Engine**: Multilingual food parsing (supporting Hindi/regional culinary names, dish preparation styles like *tadka*, *shallow fried*, *steamed*, and traditional serving units like *katori*, *plate*, *piece*).
- **Offline Bushy AI Mentor**: Purely local rule-based and heuristic mentor answering nutrition questions, analyzing calorie budgets, and auto-synthesizing in-app health quests on demand.
- **Apple HealthKit Sync**: Real-time background query synchronization for daily step counts and active calorie burn.
- **RPG Progression Loop**: Dynamic XP scaling curve, daily streak mechanics, level rewards, and interactive story card generator for social sharing.

---

## 🔬 Core Algorithms & Mathematical Foundations

### 1. Bloub Procedural Spline Deformation

Rather than static raster graphics or pre-rendered sprite animations, the hero avatar **Bloub** is procedurally computed in real time.

```
       [ 64 Radial Anchors ]
                 │
                 ▼
     [ Shape Profile Sample ] ─────────► [ Breath Harmonic Oscillator ]
                 │                                    │
                 ▼                                    ▼
       [ 2D Cartesian Conversion: x = cx + r·cosθ, y = cy + r·sinθ ]
                 │
                 ▼
   [ Closed Catmull-Rom Centripetal Spline Interpolation ]
                 │
                 ▼
         [ Live SwiftUI Canvas Body Stroke & Fill ]
```

#### A. 64-Radial Coordinate Sampling
The avatar outline is modeled across $N = 64$ radial slices uniformly distributed over the interval $[0, 2\pi]$:

$$\theta_i = i \cdot \frac{2\pi}{64}, \quad i \in [0, 63]$$

To eliminate trigonometry overhead during the 60/120 FPS render loop, precomputed lookup tables are retained for $\cos(\theta_i)$ and $\sin(\theta_i)$:

$$x_i = c_x + r_i \cdot R \cdot \cos(\theta_i)$$
$$y_i = c_y + r_i \cdot R \cdot \sin(\theta_i) \cdot \alpha_{\text{aspect}}$$

Where:
- $c_x, c_y$ represent the computed barycentric center of the body.
- $R$ is the baseline radius scaled by the view frame ($R = \min(w, h) \times 0.38$).
- $r_i \in [0.75, 1.30]$ is the normalized profile radius vector defining morphology (e.g., Cloud, Egg, Hexagon, Star).
- $\alpha_{\text{aspect}}$ is an aspect ratio scalar providing subtle vertical squish.

#### B. Closed Catmull-Rom Spline Interpolation
To transform discrete radial points into a continuous fluid curve without sharp facets, a closed Catmull-Rom spline is computed for each consecutive quartet of points $(P_{i-1}, P_i, P_{i+1}, P_{i+2})$ modulo $N$:

The cubic Bézier control points $C_1$ and $C_2$ for the segment connecting $P_i$ to $P_{i+1}$ are derived as:

$$C_1 = P_i + \frac{P_{i+1} - P_{i-1}}{6}$$
$$C_2 = P_{i+1} - \frac{P_{i+2} - P_i}{6}$$

This formulation guarantees $C^1$ continuity (continuous tangent vectors) across all 64 node boundaries, creating an organic fluid droplet silhouette.

#### C. Harmonic Breathing Oscillator
The avatar undergoes natural ambient respiration simulated using a continuous sine wave generator:

$$S_{\text{breath}}(t) = 1.0 + 0.015 \cdot \sin\left(\frac{t}{3.4} \cdot 2\pi\right)$$
$$R_{\text{current}} = R_{\text{base}} \cdot S_{\text{breath}}(t)$$

#### D. Quintic State Transition Easing
When morphing between expressions (e.g. `neutral` $\rightarrow$ `excited` $\rightarrow$ `thinking`), radial vectors and eye configurations are interpolated with a quintic ease-out function:

$$f(t) = 1 - (1 - t)^5, \quad t = \text{clamp}\left(\frac{\Delta t}{\tau_{\text{duration}}}, 0, 1\right)$$

This yields rapid initial deformation that gently cushions into the target profile, mimicking liquid surface tension.

#### E. 3D Projective Gaze & Parallax Tracking
Bloub tracks touch gestures across the screen via simulated spherical eye projection:

$$\Delta x_{\text{eye}} = R_{\text{scale}} \cdot 0.50 \cdot \sin(\text{yaw}_{\text{rad}})$$
$$\Delta y_{\text{eye}} = -R_{\text{scale}} \cdot 0.55 \cdot \sin(\text{pitch}_{\text{rad}})$$

When gazing downwards, eyelid squish is applied dynamically:

$$h_{\text{lid}} = h_{\text{base}} \cdot \left(1.0 + \frac{\text{pitch}}{100.0}\right)$$

---

### 2. Health & Progression Curve

Bushy uses a non-linear convex power function to scale XP requirements, balancing fast early-stage gratification with long-term retention:

$$\text{XP}_{\text{required}}(L) = \left\lfloor 100 \cdot L^{1.4} \right\rfloor$$

```
Level 1:   100 XP
Level 2:   263 XP
Level 3:   465 XP
Level 5:   951 XP
Level 10: 2,511 XP
```

- **HealthKit Sync**: 1 Step = 0.05 XP; 1 Active Calorie = 0.2 XP.
- **Quest Completion**: Standard missions yield a fixed +500 XP burst.
- **Level Progress Ratio**:

$$\rho = \frac{\text{XP}_{\text{current}} - \text{XP}_{\text{floor}}(L)}{\text{XP}_{\text{required}}(L)}$$

---

### 3. Offline Multilingual Indian Nutrition Interpreter

Indian meals present unique challenges for traditional fitness trackers due to non-standard serving sizes (*katori*, *roti*, *ladle*), regional synonyms (*phulka* vs *chapati*), and preparation variations (*tadka*, *fried*, *steamed*).

#### Preparation Style Caloric Multipliers
$$\text{Calories}_{\text{adjusted}} = \text{Calories}_{\text{base}} \cdot M_{\text{prep}}$$

| Preparation Style | Multiplier ($M_{\text{prep}}$) | Typical Added Fat |
| :--- | :--- | :--- |
| **Boiled / Steamed** (e.g. Idli, Plain Rice) | $\times 0.90$ | Minimal to zero oil |
| **Standard Homemade** (e.g. Regular Dal, Sabzi) | $\times 1.00$ | Moderate oil/ghee (1 tsp) |
| **Tadka / Extra Ghee** (e.g. Dal Tadka, Ghee Roti) | $\times 1.25$ | Tempered with 1-2 tbsp ghee |
| **Shallow Fried** (e.g. Paratha, Cutlet, Paneer Tikka) | $\times 1.35$ | Surface oil absorptive pan frying |
| **Deep Fried** (e.g. Poori, Bhatura, Samosa, Pakora) | $\times 1.70$ | High oil absorption matrix |

---

## 🎨 Design Philosophy: Liquid Glass & Monochromatic Noir

Bushy iOS features an ultra-sleek, minimalist aesthetic inspired by Apple's highest design standards:

```
┌────────────────────────────────────────────────────────┐
│  Glass Surface (.ultraThinMaterial, opacity: 0.85)     │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Specular Gradient Stroke (Linear: White 0.35->0) │  │
│  │ Inner Ambient Shadow                             │  │
│  │ Responsive Haptic Surface Engine                 │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

- **Dynamic Contrast Inversion**: Uses `Color.bushyAppBackground` combined with `Color.primary` to ensure that buttons and active states automatically invert correctly between Light and Dark modes.

---

## 📂 Codebase Structure

```
BushyIOS/
├── BushyIOS/
│   ├── MyApp.swift                        # App Entry Point & Dependency Injection
│   ├── ContentView.swift                  # Root Tab Controller & Style Coordinators
│   ├── DesignSystem.swift                 # Liquid Glass modifiers & Fluid Canvas background
│   │
│   ├── Avatar Engine/
│   │   ├── BloubAvatar.swift              # Live SwiftUI Canvas avatar renderer
│   │   ├── BloubEngine.swift              # Animation cycle, blinking, gaze, and sampling
│   │   ├── BloubMath.swift                # Catmull-Rom splines, 64-radial math, & easings
│   │   ├── BloubProfiles.swift            # Geometric morph targets (Cloud, Egg, Hexagon)
│   │   └── BloubStates.swift              # Expression states (Neutral, Happy, Tired, etc.)
│   │
│   ├── Screens/
│   │   ├── HealthScreen.swift             # Main tab navigator and container
│   │   ├── HomeScreen.swift               # Dashboard: Avatar hero, stats, active missions
│   │   ├── TasksScreen.swift              # Quests, macro tracking, logged meal history
│   │   ├── BushyAIScreen.swift            # Offline conversational mentor & suggestions
│   │   ├── MealAnalysisScreen.swift       # Food logger & Indian dish interpreter
│   │   ├── StoryShare.swift               # Social card export & render sheet
│   │   ├── SetupScreen.swift              # Onboarding flow & preference selection
│   │   └── SwitchHeroSheet.swift          # Avatar personality & morphology selector
│   │
│   ├── Core Architecture & Data/
│   │   ├── HealthViewModel.swift          # Main state manager (XP, quests, meals, theme)
│   │   ├── BushyAIViewModel.swift         # Offline NLP heuristic conversation engine
│   │   ├── HealthManager.swift            # HealthKit HKHealthStore background queries
│   │   ├── IndianNutritionDatabase.swift  # Pre-compiled Indian macro & food dictionary
│   │   ├── MultilingualDishInterpreter.swift # Dish resolver & synonym normalization
│   │   ├── FoodAnalysisModels.swift       # Macro data models & parsing rules
│   │   ├── Models.swift                   # Quest, avatar, and UI state models
│   │   └── NavigationPill.swift           # Floating liquid glass navigation controller
│   │
│   └── Assets & Configuration/
│       ├── BushyIOS.entitlements          # HealthKit capabilities
│       └── Info.plist                     # HealthKit & Camera privacy descriptions
└── README.md                              # Technical documentation
```

---

## 🛠️ Build & Installation

### Prerequisites
- **macOS Sonoma (14.0+)** or newer.
- **Xcode 15.0+** with iOS 17.0+ SDK.
- Physical iOS Device (recommended for HealthKit step querying) or iOS Simulator.

### Setup Instructions
1. Clone the repository and switch to the `ios` branch:
   ```bash
   git clone https://github.com/krishdeokk/Bushy.git
   cd Bushy
   git checkout ios
   ```
2. Open the Xcode Project:
   ```bash
   open BushyIOS.xcodeproj
   ```
3. Set your **Development Team** under `BushyIOS Target` $\rightarrow$ `Signing & Capabilities`.
4. Build and run on your selected device (`Cmd + R`).

---

## 🔒 Privacy & Security

Bushy is built on a **zero-cloud telemetry** principle:
- **Health Data**: All HealthKit queries execute directly on-device using local `HKHealthStore` instances.
- **Nutrition Analysis**: Food search and macro lookups occur through local static data structures.
- **AI Mentorship**: Responses are generated on-device with zero server roundtrips.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
