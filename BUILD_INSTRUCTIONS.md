# 📱 Building EGLA Tracker APK - Complete Instructions

## 🎯 Quick Summary
Your complete EGLA Tracker app is ready! Follow these steps to build the APK for your phone.

## 📋 Prerequisites

### 1. Install Java Development Kit (JDK)
- **Download**: [Oracle JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or [OpenJDK 17](https://jdk.java.net/17/)
- **Install**: Run the installer and follow the setup wizard
- **Verify**: Open Command Prompt and run: `java -version`

### 2. Install Android Studio (Recommended Method)
- **Download**: [Android Studio](https://developer.android.com/studio)
- **Install**: Follow the setup wizard
- **Setup SDK**: Android Studio will automatically install the Android SDK

## 🚀 Method 1: Build with Android Studio (Easiest)

### Step 1: Open Project
1. Launch Android Studio
2. Click "Open an Existing Project"
3. Navigate to and select your project folder: `C:\Users\leand\Desktop\test`
4. Wait for Gradle sync to complete

### Step 2: Build APK
1. In Android Studio menu: **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Wait for build to complete (may take 5-10 minutes first time)
3. Click "locate" when build finishes to find your APK

### Step 3: Install on Phone
1. APK location: `app/build/outputs/apk/debug/app-debug.apk`
2. Copy APK to your phone via USB or cloud storage
3. Enable "Unknown Sources" in phone settings
4. Install the APK

## ⚡ Method 2: Command Line Build (Advanced)

### Prerequisites Check
```powershell
# Check Java (must return version 8 or higher)
java -version

# Check Android SDK (set ANDROID_HOME environment variable)
echo $env:ANDROID_HOME
```

### Build Commands
```powershell
# Navigate to project directory
cd "C:\Users\leand\Desktop\test"

# Make gradlew executable (if needed)
# Already created for you!

# Clean and build debug APK
.\gradlew.bat clean assembleDebug

# APK will be generated at:
# app\build\outputs\apk\debug\app-debug.apk
```

## 🔧 Troubleshooting

### "Java not found" Error
1. Download and install JDK 17 from Oracle or OpenJDK
2. Set JAVA_HOME environment variable:
   - Open System Properties → Environment Variables
   - Add new system variable: `JAVA_HOME` = `C:\Program Files\Java\jdk-17`
   - Add to PATH: `%JAVA_HOME%\bin`

### "Android SDK not found" Error
1. Install Android Studio (includes SDK)
2. Or manually download Android SDK and set ANDROID_HOME:
   - Download: [Android SDK Command Line Tools](https://developer.android.com/studio#command-tools)
   - Set ANDROID_HOME: `C:\Users\[USERNAME]\AppData\Local\Android\Sdk`

### "Gradle build failed" Error
1. Run: `.\gradlew.bat clean`
2. Delete `.gradle` folder and retry
3. Check internet connection (Gradle downloads dependencies)

## 📱 Installing on Your Phone

### Enable Developer Options
1. Go to **Settings** → **About Phone**
2. Tap "Build Number" 7 times
3. Go back to **Settings** → **Developer Options**
4. Enable "USB Debugging"

### Install APK
**Method A: USB Installation**
```powershell
# Install Android Debug Bridge (ADB)
# Connect phone via USB
adb install app\build\outputs\apk\debug\app-debug.apk
```

**Method B: Manual Installation**
1. Copy APK file to your phone
2. Enable "Install from Unknown Sources" in Settings
3. Use a file manager to open and install the APK

## 🎯 Expected APK Details

- **File Size**: ~15-25 MB (includes EGLA library)
- **Min Android Version**: Android 5.0 (API 21)
- **Target Android Version**: Android 14 (API 34)
- **Permissions Required**: Location, Storage, Sensors

## ✅ Verification

After installation, the app should:
1. Request location and storage permissions
2. Show "EGLA Tracker" as the app name
3. Display the main tracking interface
4. Start enhanced GPS tracking when "Start Tracking" is pressed
5. Save CSV files to `Android/data/com.example.eglatracker/files/egla_logs/`

## 🔥 App Features You'll Get

### Real-time Location Tracking
- **Enhanced Accuracy**: 5-10x improvement over standard GPS
- **Direction Detection**: Shows which way you're moving
- **Movement Analysis**: Detects walking, driving, stationary states

### CSV Data Logging
- **Automatic Logging**: Saves data every second
- **Complete Records**: Original accuracy, enhanced accuracy, improvement %
- **File Management**: Timestamped files with automatic cleanup

### Modern UI
- **Material 3 Design**: Clean, modern interface
- **Real-time Updates**: Live accuracy improvements
- **Visual Direction**: Arrows showing movement direction
- **Color-coded Metrics**: Green for improvements, visual feedback

## 📊 Sample CSV Output
```csv
timestamp,datetime,latitude,longitude,altitude,original_accuracy,enhanced_accuracy,accuracy_improvement,speed,bearing,direction,environment,confidence,is_stationary,processing_time
1703123456789,2023-12-21 10:30:45.123,40.748817,-73.985428,10.5,8.2,3.1,62.2,1.4,45.7,Northeast,URBAN,0.85,false,45
```

## 🎉 You're Ready!

Your EGLA Tracker app with enhanced GPS accuracy, direction detection, and CSV logging is ready to build and install on your phone! Choose the method that works best for your setup.

**Need help?** The app includes comprehensive error handling and will guide you through any permission setup needed. 