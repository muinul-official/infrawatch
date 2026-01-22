# 🏗️ InfraWatch

**Smart Infrastructure Maintenance App**

InfraWatch is an Android application designed to streamline infrastructure maintenance through AI-powered damage detection, real-time reporting, and comprehensive analytics.

---

## 📱 Features

### For Users
- **📸 Smart Report Submission** - Capture infrastructure issues with photos and automatic location detection
- **🤖 AI Damage Analysis** - TensorFlow Lite-powered image analysis to automatically classify damage severity (Minor/Moderate/Major)
- **🗺️ Interactive Map** - View all reported issues on a Google Maps interface with severity-based markers
- **📊 Report Tracking** - Monitor the status of your submitted reports (Pending → In Progress → Completed)
- **🔔 Push Notifications** - Receive real-time updates on report status changes

### For Administrators
- **📈 Analytics Dashboard** - Comprehensive statistics with charts powered by MPAndroidChart
- **📋 Report Management** - View, update, and resolve maintenance reports
- **👥 User Management** - Manage registered users and contractors
- **🗺️ Heat Map View** - Visualize infrastructure issues across locations
- **🔔 Notification System** - Send updates to users about their report status

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Java |
| **Framework** | Android SDK (API 24+, Target 36) |
| **Backend** | Firebase (Auth, Firestore, Storage, Cloud Messaging) |
| **AI/ML** | TensorFlow Lite (Damage Classification Model) |
| **Maps** | Google Maps SDK, Maps Utils |
| **Charts** | MPAndroidChart |
| **Database** | Room (Local), Firestore (Cloud) |
| **Image Loading** | Glide |

---

## 📂 Project Structure

```
InfraWatch_Clean/
├── app/
│   ├── src/main/
│   │   ├── java/.../maintenance_dashboard/
│   │   │   ├── ai/                 # TensorFlow Lite damage analyzer
│   │   │   ├── adapter/            # RecyclerView adapters
│   │   │   ├── data/               # Room database & models
│   │   │   ├── model/              # Data models
│   │   │   ├── push/               # Firebase messaging service
│   │   │   ├── ui/                 # UI components
│   │   │   ├── utils/              # Utility classes
│   │   │   ├── Login.java          # User authentication
│   │   │   ├── Register.java       # User registration
│   │   │   ├── UserMainActivity.java   # User dashboard
│   │   │   ├── AdminActivity.java      # Admin dashboard
│   │   │   ├── AnalyticsActivity.java  # Analytics & charts
│   │   │   └── ...
│   │   ├── res/                    # Resources (layouts, drawables, values)
│   │   └── assets/                 # ML model & labels
│   └── build.gradle.kts
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 36
- Google Maps API Key
- Firebase Project

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/muinul-official/infrawatch.git
   cd infrawatch
   ```

2. **Configure Firebase**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Enable Authentication (Email/Password)
   - Enable Firestore Database
   - Enable Cloud Storage
   - Download `google-services.json` and place it in `app/`

3. **Configure Google Maps**
   - Get an API key from [Google Cloud Console](https://console.cloud.google.com)
   - Update the API key in `AndroidManifest.xml`

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open in Android Studio and run directly.

---

## 📊 Firebase Database Structure

```
users/
  └── {userId}/
      ├── email
      ├── name
      ├── role (user/admin)
      └── phone

reports/
  └── {reportId}/
      ├── title
      ├── description
      ├── imageUrl
      ├── latitude/longitude
      ├── severity (MINOR/MODERATE/MAJOR)
      ├── status (PENDING/IN_PROGRESS/COMPLETED)
      ├── userId
      └── timestamp
```

---

## 🤖 AI Model

The app uses a custom TensorFlow Lite model for damage severity classification:

- **Input**: 224x224 RGB image
- **Output**: Severity classification (Minor, Moderate, Major)
- **Model Location**: `app/src/main/assets/model.tflite`
- **Labels**: `app/src/main/assets/labels.txt`

---

## 📸 Screenshots

| User Login | User Dashboard | Report Submission |
|:----------:|:--------------:|:-----------------:|
| Login screen with Firebase Auth | Main dashboard with report overview | Submit new issues with photo & location |

| Admin Dashboard | Analytics | Map View |
|:---------------:|:---------:|:--------:|
| Manage reports & users | Charts and statistics | Interactive issue map |

---

## 👥 Team

- Md Muinul Islam
- Sneganrao Raman
- Jayyidan Abdurrohman
- Ramadhani Ayesha Zahira
- Aliff Harith Bin Halilul Rahman
- Cita Wafa Atiah

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- TensorFlow Lite for on-device ML inference
- Firebase for backend services
- MPAndroidChart for analytics visualization
- Google Maps Platform for location services
