# BulletDrone

**BulletDrone** is a Kotlin-based mobile application developed to control drones, manage video/photo recordings, and integrate with Firebase Storage for easy access and organization. The project uses **Parrot SDK** for drone control and **Firebase** for cloud storage.

---

## Features

### Main Page
- **Fly View**: Control the drone and start recording videos.
- **Gallery**: Manage videos/photos, including downloading, deleting, and uploading to Firebase Storage.

![Main Page](![Screenshot_20241217_165436_BulletDrone](https://github.com/user-attachments/assets/10a4203b-3361-49d9-bf92-d48a6c765098)
)

---

### Fly View
- Start and stop drone video recording.
- Monitor the battery status of both the drone and remote.
- Control all drone flight functionalities.

![Fly View](![Screenshot_20241217_165507_BulletDrone](https://github.com/user-attachments/assets/d7643b27-4abb-431b-9b77-0593ca4c9878)
)

---

### Gallery
- Browse through recorded videos and photos stored on the drone's SD card.
- Download videos/photos to your phone.
- Delete unnecessary files.
- **Fetch user names from Firestore**: Select the shooter's name from a dropdown/list populated by Firestore.
- **Upload videos to Firebase Storage**:
  - Automatically creates a directory for each shooter in Firebase.
  - Organizes file naming, e.g.: `ShooterName_(ActivityType)_YYYYMMDD_HHMMSS.MP4`.

![Gallery](![Screenshot_20241217_165507_BulletDrone](https://github.com/user-attachments/assets/d0cf73fc-dc23-47d3-9f16-1846d3345c30)
)

![Uploading to Firebase Storage](![Screenshot_20241217_165527_BulletDrone](https://github.com/user-attachments/assets/13f0f606-9a15-4e44-acd0-e170e228d212)
)

![Uploading to Firebase Storage](![Sieppaa](https://github.com/user-attachments/assets/323b0ac8-5686-4c39-ae0e-9b379100949d)
)

---

## Technologies Used
- **Kotlin**: Core development language  
- **Parrot SDK**: Drone control and video management  
- **Firebase Storage**: Cloud storage for video files  
- **Firestore**: User data management (fetching shooter names)  
- **Android SDK**: Mobile app development
