
# VISION: Development of a Visual Impairment Solution with Integrated Object Navigation
An Android application designed to assist visually impaired users in navigating their environment using real-time object detection, depth estimation, spatial analysis, and text-to-speech feedback.
---
## Overview
VISION is a thesis project developed as an assistive technology solution for the blind and visually impaired. The app combines multiple AI models and Android hardware capabilities to provide real-time navigation guidance through audio feedback.
This repository contains the **ARCore version** of the application, developed by the primary author. A separate MiDaS-based version was developed by another thesis group member.
---
## Features
- 🔍 **Real-time Object Detection** — YOLOv5 detects objects in the camera feed and announces them via TTS
- 📏 **Depth Estimation** — ARCore provides spatial depth data to estimate object distances
- 🧠 **AI Spatial Analysis** — On-demand scene analysis using Llama v4 Scout (via Groq API), providing navigation instructions limited to 25 words for clarity
- 🔊 **Text-to-Speech Feedback** — All detections and navigation guidance are announced aloud
- 🛑 **Obstacle Warning** — Automatically warns the user to stop and turn around if a large object is within 1 meter
---
## Tech Stack
- **Language** — Java
- **Platform** — Android
- **AR** — ARCore (Google)
- **Object Detection** — YOLOv5 (TensorFlow Lite)
- **AI Analysis** — Llama v4 Scout via Groq API
- **Networking** — OkHttp3
- **TTS** — Android TextToSpeech API
---
## How It Works
1. The camera feed is processed in real time by YOLOv5 to detect objects
2. ARCore provides depth data for distance estimation
3. Detected objects and distances are announced via TTS
4. When the user presses the on-screen button, the current frame and depth data are sent to the Groq API
5. Llama v4 Scout analyzes the scene and returns a navigation instruction (e.g. move forward, turn left, STOP)
6. The instruction is read aloud to the user

## Offline Capability
VISION is designed to work in environments with limited or no internet connection.
| Feature | Offline | Online |
|---|---|---|
| Object Detection (YOLOv5) | ✅ | ✅ |
| Depth Estimation (ARCore) | ✅ | ✅ |
| Text-to-Speech | ✅ | ✅ |
| AI Spatial Analysis (Llama v4 Scout) | ❌ | ✅ |
Core navigation functionality remains available at all times. AI-powered scene analysis requires an active internet connection to reach the Groq API.

---
## Setup
### Prerequisites
- Android Studio
- Android device with ARCore support (tested on Infinix Hot 50 Pro+)
- Groq API key
### Installation
1. Clone the repository
```bash
   git clone https://github.com/yourusername/vision-app.git
```
2. Create a `local.properties` file in the root directory and add:
   GROQ_API_KEY=your_groq_api_key_here
3. Open the project in Android Studio and sync Gradle
4. Build and run on a supported ARCore device
---
## Team
This app was developed as a thesis project by a group of 3 members.
- **[Your Name]** — Primary Developer (ARCore version)
- [Member 2] — MiDaS version
- [Member 3] — [their role]
---
## Disclaimer
This project was developed as an academic thesis. The Groq API key is not included in this repository. You must provide your own key via `local.properties`.