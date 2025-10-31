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
   - Click **Add project**, enter project name (e.g., FeedMate) and continue.
   - Enable and click continue for AI assistance.
   - Disable Google Analytics and create project.


2. **Enable Realtime Database**
   - Navigate to **Build → Realtime Database → Create Database**.
   - For Realtime Database Location, select Singapore (asia-southeast1) or closest region.
   - For Securty Rules, select **Start in locked mode**
   - Import provided JSON file from the System Folder to initialize databasee.
   - Copy the **Database URL**
   - Open Android Studio project and replace the 'DB_URL' in the app with the current database URL (Change URL in related java files that has the URL code line)
   
   > ⚠ Note: The app connects to Firebase using the database URL, so `google-services.json` is **not required** for this project.

3. **Enable Firebase Authentication**
   - Navigate to **Build → Authentication → Get Started → Sign-in Method → Email/Password → Enable**.

4. **Enable Firebase Storage**
   - Navigate to **Build → Storage → Get Started**.
   - For **Bucket Options**, select **"All locations"** and choose **"ASIA-SOUTHEAST1"** for location and **"Standard"** for **access frequency**.
   - For **Security Rules**, select **Start in production mode**
   - Adjust **storage rules** to this :
     ```
     rules_version = '2';
      service firebase.storage {
        match /b/{bucket}/o {
           match /{allPaths=**} {
              allow read, write: if request.auth != null;
           }
        }
      }
     ```

---

## 4. Android App Installation
1. **Clone or Download the Project**
   - Clone from GitHub:
     ```bash
     git clone <https://github.com/syimhub/G04_42.git>
     ```
   - Or download the ZIP and extract it.

   **Clone Using GitHub Desktop**
   - Install GitHub Desktop from https://desktop.github.com/.
   - Open GitHub Desktop and sign in with your GitHub account.
   - Click **File → Clone Repository**
   - In the dialog :
        1. Select the **URL tab**.
        2. Paste repository URL → https://github.com/syimhub/G04_42.git
        3. Choose the **local path** where the project folder should be saved.
        4. Click **Clone**

2. **Open Project in Android Studio**
   - Once cloned, open **Android Studio → File → Open → Select cloned project directory**.
   - Wait for Gradle sync to complete.

3. **Configure Firebase in Android Studio**
   - This project uses **direct Firebase database URLs**, so `google-services.json` is **not required**.
   - Verify that the Firebase URLs in Java files (e.g., `UserDashboardActivity.java`) match your Firebase Realtime Database.
   - No further configuration of `google-services.json` is needed. 

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

You're done! You should now be able to run your app and the feeder.

## Link of GitHub
[GitHub](https://github.com/syimhub/G04_42.git)

---

## Demo Accounts (for testing)
> **Security note :** Accounts shown are only for demo. Sensitive credentials are not included and should not be included.
### Admin (demo)
- **Email :** syimwannastudy@gmail.com
- **Password :** 123456

### User (demo)
- **Email :** syimiraidilr2@gmail.com
- **Password :** 123456

- **Email :** ammarsyazani3120@gmail.com
- **Password :** 123456

- **Email :** safiyakmal05@gmail.com
- **Password :** 123456

- **Email :** isyrafaiman11@gmail.com
- **Password :** 123456