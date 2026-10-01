# 3D Relativistic Particle Collider Virtual Environment

A high-precision, interactive 3D virtual environment and live physics laboratory simulating relativistic particle beam collisions, electromagnetic solenoidal deflection, quantum field decay channels, and detector calorimetry on Android.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The environment is optimized for landscape mobile & tablet viewing with a **60% interactive 3D Canvas** on the left and a **40% Control & Telemetry Dashboard** on the right.

- **3D Physics Engine Approach**: Custom high-performance 3D vector-projection and matrix camera engine built directly on Android Canvas with double-buffering, optimized hardware acceleration, 3D trajectory interpolation, and real-time solenoidal magnetic curvature ($r = \frac{p_\perp}{q B}$).
- **Particle Scope**: Comprehensive Standard Model catalog including Matter and Antimatter (Quarks, Anti-quarks, Leptons, Anti-leptons, Bosons, Higgs), plus composite Hadrons and Nuclei (Protons, Antiprotons, Neutrons, Alpha, Lead/Gold Ions).
- **Time Scaling**: Relativistic time-step engine configurable in fractions of lightspeed ($c$) down to sub-femtosecond slow-motion ($0.0001c$ to $1.0c$) with step-pause and frame-stepping.
- **Export Capabilities**: Rich text event logs formatted with kinematics, invariant mass calculations, and decay trees available to copy to clipboard or share via Android intent.

---

## 1. Overview & Core Concept

The **3D Relativistic Particle Collider Virtual Environment** turns the phone or tablet into a virtual Hadron & Lepton collider (like CERN's LHC or SLAC). Users select beam particles (e.g., Proton-Proton, Electron-Positron, Antiproton-Proton, Lead Ion collision), tune beam energies ($\sqrt{s}$ up to 14 TeV) and solenoidal magnetic fields ($B$ up to 8 Tesla), and trigger head-on relativistic collisions.

### Key Capabilities
- **60/40 Split Landscape Layout**: 60% viewport dedicated to a touch-interactive 3D particle detector & beam pipe, 40% viewport for live telemetry and multi-tab control panel.
- **Interactive 3D View**: 2-finger pinch zoom, 3D rotational pan, wireframe overlay toggle, field-line grid visualization, and particle orbit tracing.
- **Relativistic Collision Physics**: Conserved charge, momentum, lepton/baryon numbers, relativistic invariant mass ($E^2 = p^2c^2 + m^2c^4$), and stochastic decay channels generating daughter jets, muon tracks, and photon clusters.
- **Advanced Control & Tuning**: Independent sliders for magnetic field, collision energy, luminosity, impact parameter, trajectory glow, and camera parameters.
- **Data Logging & Export**: Copyable raw text logs and event summaries.

---

## 2. User Experience & Visual Design

### Landscape Spatial Layout (60% / 40% Split)

```
┌───────────────────────────────────────────────┬──────────────────────────────────────────────┐
│  3D COLLIDER ENVIRONMENT (60% Width)          │  TELEMETRY & CONTROL DASHBOARD (40% Width)   │
│ ┌───────────────────────────────────────────┐ │ ┌──────────────────────────────────────────┐ │
│ │  Top Bar: [Light/Dark] [Wireframe] [Exit] │ │ │ Tabs: [Telemetry] [Particles] [Controls] │ │
│ │                                           │ │ ├──────────────────────────────────────────┤ │
│ │  3D Beam Pipe & Detector Chamber Wireframe│ │ │ Live Beam Energy: 13.6 TeV               │ │
│ │  Orbit Trajectories & B-field curvature   │ │ │ Magnetic Field B: 3.8 Tesla             │ │
│ │  Collision Point Spark & Daughter Shower  │ │ │ Multiplicity: 42 particles generated     │ │
│ │                                           │ │ │ Invariant Mass Peak: 125.1 GeV (Higgs)   │ │
│ │  Gestures: 2-finger zoom / 1-2 finger pan │ │ ├──────────────────────────────────────────┤ │
│ │                                           │ │ │ [Fire Collision] [Time Control (0.001c)] │ │
│ │  Floating Camera Reset & Play/Pause       │ │ │ [Export Data Logs] [Copy Event Text]    │ │
│ └───────────────────────────────────────────┘ │ └──────────────────────────────────────────┘ │
└───────────────────────────────────────────────┴──────────────────────────────────────────────┘
```

### Visual Identity & Theme
- **Theme Modes**: Both High-Contrast Quantum Dark Theme (default: deep space blue `#080D1A`, particle neon cyan `#00F2FE`, higgs magenta `#FF0844`, hadron amber `#FFB100`) and Crisp Light Laboratory Theme (`#F4F7FC` background with deep slate navy controls).
- **Typography**: Monospace typography (`FontFamily.Monospace`) for energy measurements, invariant masses, and vector coordinates ($p_x, p_y, p_z$), paired with clean Material 3 system typography.
- **Micro-Interactions**: Pulsing beam collision flashes, particle track color transitions based on energy/charge, interactive trajectory hover inspection, spring sliders, and tactile button feedback.

---

## 3. Key Product Decisions & Trade-Offs

1. **Custom 3D Matrix Projection Engine vs External Heavy Native Library**:
   - *Chosen Approach*: Lightweight, ultra-responsive custom 3D matrix-projection renderer in Kotlin with hardware-accelerated Canvas drawing.
   - *Why*: Ensures instant loading, zero native binary bloat, 60fps smooth touch interaction, precise control over wireframe mesh rendering, track glow effects, and sub-pixel particle drawing.

2. **Standard Model Particle Physics & Decay Dynamics**:
   - *Chosen Approach*: Physics database containing exact masses, electric charges, spin, baryon/lepton numbers, and mean lifetime ($\tau$) for Quarks ($u, d, c, s, t, b$), Leptons ($e^-, e^+, \mu^-, \mu^+, \tau^-, \tau^+, \nu_e, \nu_\mu, \nu_\tau$), Bosons ($\gamma, g, W^+, W^-, Z^0, H^0$), and Hadrons ($p, \bar{p}, n, \alpha, \text{Au/Pb}^{82+}$).
   - *Why*: Delivers authentic physical behavior without sacrificing frame rates during complex multi-particle cascade showers.

3. **Touch Gestures (Pan & Zoom)**:
   - *Chosen Approach*: Dual-pointer gesture detector processing pinch distance (zoom $0.2\times$ to $5.0\times$) and drag translation/rotation angles ($\theta, \phi$) with smooth momentum damping.

---

## 4. Technical Architecture & System Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                    MAIN ACTIVITY                                       │
│                         (Landscape Forced & Edge-to-Edge Enabled)                      │
└──────────────────────────────────────────┬─────────────────────────────────────────────┘
                                           │
                    ┌──────────────────────┴──────────────────────┐
                    ▼                                             ▼
┌─────────────────────────────────────────┐   ┌──────────────────────────────────────────┐
│        3D COLLIDER CANVAS VIEW          │   │      TELEMETRY & DASHBOARD PANEL         │
│               (60% Width)               │   │               (40% Width)                │
├─────────────────────────────────────────┤   ├──────────────────────────────────────────┤
│ - Render Loop (60 FPS coroutine tick)   │   │ - Tab Switcher (Telemetry/Beam/Logs)     │
│ - Matrix Camera (Yaw, Pitch, Zoom, Pan) │   │ - Particle Choice A & B Pickers          │
│ - Detector Wireframe Meshes             │   │ - Relativistic Energy & B-Field Sliders  │
│ - 3D Trajectory Math (B-field Bending)  │   │ - Time Interval ($c$) Controls           │
│ - Touch Gesture Controller (Zoom/Pan)   │   │ - Log Exporter & Clipboard Copy Actions  │
└────────────────────┬────────────────────┘   └────────────────────┬─────────────────────┘
                     │                                             │
                     └──────────────────────┬──────────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 COLLIDER VIEWMODEL                                     │
│  - StateFlow<SimulationState>: Beam particles, Collision Energy, B-Field, Time Scale   │
│  - StateFlow<TelemetryData>: Event ID, Multiplicity, $p_T$ distribution, Decay Tree   │
│  - StateFlow<List<Particle3D>>: Particle coordinates $(x,y,z)$, velocity $(v_x,v_y,v_z)$ │
│  - Physics Engine: Relativistic momentum calculations & particle generator              │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### Key Data Models & State Architecture

1. **`ParticleSpecies`**:
   - `id`: String identifier (e.g., `"proton"`, `"electron"`, `"positron"`, `"higgs"`, `"muon_minus"`)
   - `name`: Human readable name
   - `symbol`: Latex-style symbol ($p, e^-, e^+, H^0, \mu^-$)
   - `restMassGeV`: Rest mass $m_0$ in $\text{GeV}/c^2$
   - `charge`: Electric charge $q$ in elementary charge units ($+1, -1, 0, +2/3, -1/3$)
   - `isAntimatter`: Boolean flag
   - `category`: Enum (`QUARK`, `LEPTON`, `BOSON`, `HADRON`, `NUCLEUS`)
   - `color`: Primary visual color hex code

2. **`Particle3D` (Live Kinematic State)**:
   - `id`: Unique particle instance ID
   - `species`: `ParticleSpecies` reference
   - `position`: Vector3D $(x, y, z)$ in meters/femtometers
   - `momentum`: Vector3D $(p_x, p_y, p_z)$ in $\text{GeV}/c$
   - `charge`: Float
   - `lifetimeNs`: Remaining lifetime before decay
   - `trajectoryHistory`: Queue of historical 3D points for real-time track visualization
   - `generation`: Integer (0 for initial beam, 1+ for shower daughter products)

3. **`SimulationState`**:
   - `particleA`, `particleB`: Selected colliding species
   - `centerOfMassEnergyGeV`: Beam energy $\sqrt{s}$
   - `magneticFieldTesla`: Solenoidal field $B_z$
   - `timeScale`: Slow-motion factor in $c$ ($0.0001$ to $1.0$)
   - `isPaused`: Boolean
   - `wireframeEnabled`: Boolean
   - `gridOverlayEnabled`: Boolean
   - `detectorVisibility`: Map of detector layer toggles (Tracker, ECAL, HCAL, Muon)
   - `isDarkTheme`: Boolean

4. **`EventLogEntry`**:
   - `eventId`: Sequential event number
   - `timestampMs`: System time
   - `initialState`: Collision description
   - `multiplicity`: Total count of generated particles
   - `totalEnergyGeV`: Total energy balance check
   - `invariantMassGeV`: Calculated invariant mass peak
   - `decayTreeText`: Hierarchical text representation of daughter particles

---

## Verification Plan

### Automated Build Verification
1. Execute `compile_applet` to confirm zero compilation errors across ViewModel, Compose UI, physics math engine, and graphics renderer.

### Manual Feature Verification Checklist
1. **60/40 Landscape Layout**: Verify screen split and proper resizing behavior in horizontal orientation.
2. **Interactive 3D View & Gestures**:
   - Test 1-finger drag to rotate camera yaw/pitch.
   - Test 2-finger pinch to zoom in/out smoothly.
   - Test 2-finger pan to translate camera offset.
3. **Collision Dynamics & Physics**:
   - Select Proton-Proton collision at 13.6 TeV.
   - Fire collision and verify shower particle generation.
   - Observe helical track bending in 3.8 Tesla magnetic field.
4. **Wireframe Mode & Controls**:
   - Toggle Wireframe mode on and off.
   - Adjust magnetic field slider and observe trajectory curvature changes in real-time.
   - Adjust time scale slider ($0.0005c$) and verify smooth slow-mo animation.
5. **Data Export & Telemetry**:
   - Check live telemetry counters ($p_T$, event count, multiplicity).
   - Open Log Export dialog, tap "Copy Event Logs", and confirm formatted log text is copied to clipboard.
6. **Theme & Exit**:
   - Toggle Light/Dark theme and verify all colors, text readability, and canvas grid reflect theme posture.
   - Tap Exit button to verify app minimization/close handling.
