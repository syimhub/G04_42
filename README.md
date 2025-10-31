![UTM Logo](https://upload.wikimedia.org/wikipedia/commons/c/cb/UTM-LOGO-FULL.png)

# FeedMate Pet Feeder System - Installation Manual

## 1. Overview
The FeedMate Pet Feeder System is an IoT-based automated pet feeding solution. It consists of:  
- **Hardware:** ESP32 microcontroller, servo motor, sensors, and feeder device.  
- **Software:** Android mobile application (Java/XML), Firebase Realtime Database, and Firebase Storage.  

This guide explains how to install the system, set up the mobile app, configure Firebase, and connect the hardware.

---

## 2. Prerequisites
Before installation, ensure you have:

**Hardware:**
- FeedMate Pet Feeder device
- ESP32 microcontroller
- Power source for the device
- USB cable for programming ESP32

**Software:**
- Android Studio (latest stable version)
- Java JDK (compatible with Android Studio)
- Internet connection for Firebase
- Google account (for Firebase access)
- GitHub or local repository containing the project source code

---

## 3. Firebase Setup
1. **Create a Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/).
   - Click **Add project**, enter project name (e.g., FeedMate).
   - Enable and click continue for AI assistance
   - Disable Google Analytics and create project


2. **Enable Realtime Database**
   - Navigate to **Build → Realtime Database → Create Database**.
   - For Realtime Database Location, select Singapore (asia-southeast1) or closest region
   - For Securty Rules, select **Start in locked mode**
   - Import JSON file from the System Folder
   - Copy and replace the existing database path in the Android app with the current database URL

3. **Enable Firebase Authentication**
   - Navigate to **Authentication → Sign-in Method → Email/Password → Enable**.

4. **Enable Firebase Storage**
   - Navigate to **Storage → Get Started**.
   - Configure storage rules to allow authenticated users to upload profile images.

5. **Download `google-services.json`**
   - Add it to your Android Studio project under:
     ```
     app/ directory
     ```

---

## 4. Android App Installation
1. **Clone or Download the Project**
   - Clone from GitHub:
     ```bash
     git clone <repository_url>
     ```
   - Or download the ZIP and extract it.

2. **Open Project in Android Studio**
   - File → Open → Select project directory.
   - Let Gradle sync complete.

3. **Configure Firebase in Android Studio**
   - Verify `google-services.json` is present in `app/`.
   - Check `build.gradle` files for:
     ```gradle
     classpath 'com.google.gms:google-services:4.3.15'
     apply plugin: 'com.google.gms.google-services'
     ```

4. **Build and Run the App**
   - Connect an Android device or use an emulator.
   - Click **Run → Run app**.

---

## 5. Hardware Installation
1. **Program the ESP32**
   - Open Arduino IDE.
   - Install ESP32 board support:
     ```
     File → Preferences → Additional Board URLs → https://dl.espressif.com/dl/package_esp32_index.json
     Tools → Board → ESP32 → Select your board
     ```
   - Load the ESP32 code from the project.
   - Replace `firebaseURL` and `feederId` with your database values.
   - Upload to the ESP32 via USB.

2. **Assemble the Feeder Device**
   - Ensure power source is connected.
   - Confirm all hardware connections are secure.

---

## 6. Connecting App and Device
1. **Sign Up / Login**
   - Open the mobile app.
   - Sign up as an **Admin** or **User**.
   - Admins manage feeding schedules and devices.
   - Users control assigned feeders.

2. **Link Device**
   - Enter the feeder ID in the app.
   - The app syncs with Firebase, and device appears in the dashboard.

3. **Test Feeding**
   - Use **Feed Now** button in the app.
   - Verify servo motor moves and sensors update food level in the app.

---

## 7. Post-Installation Checks
- Ensure Firebase database shows `users` and `devices` nodes.
- Confirm profile image uploads and updates work.
- Verify feeding schedule triggers notifications or device actions.
- Confirm app navigation: Home, Profile, Logout functions correctly.

---

## 8. Troubleshooting
| Issue | Possible Solution |
|-------|------------------|
| App fails to sync with Firebase | Check internet connection, correct database URL, and authentication |
| ESP32 not connecting | Verify Wi-Fi credentials in code and correct feeder ID |
| Profile image upload fails | Check Firebase Storage rules and image size (<1MB recommended) |
| Feeding schedule not triggering | Ensure ESP32 is powered and sensors are connected |
