# TaskComm Admin App

A comprehensive Android admin application for managing users, tasks, and communications in the TaskComm system.

## Features

### 🔐 Authentication
- Admin login with email and password
- Secure authentication using Firebase Auth
- Session management

### 👥 User Management
- View all registered users
- Search and filter users
- Edit user profiles (name, address, business field)
- Delete user accounts (soft delete)
- View user details and statistics

### 📋 Task Management
- Create tasks under user instructions
- Assign priorities (low, medium, high)
- Update task status (pending, in progress, completed)
- Edit task details
- Delete tasks
- View task history

### 💬 Real-time Communication
- Chat with users per task
- Send text messages
- File and image sharing
- Real-time message updates
- Message read status

### 📊 Dashboard
- Overview of system statistics
- Quick access to main features
- Navigation to different sections

## Technical Stack

- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Dependency Injection**: Hilt
- **Navigation**: Navigation Compose
- **Backend**: Firebase (Auth, Firestore, Storage)
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Database**: Room (local caching)
- **Coroutines**: Kotlin Coroutines for async operations

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (API level 24)
- Kotlin 1.8+
- Firebase project

### 1. Firebase Setup

1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Enable Authentication with Email/Password
3. Create a Firestore database
4. Enable Storage
5. Download the `google-services.json` file
6. Replace the placeholder `app/google-services.json` with your actual configuration

### 2. Firebase Security Rules

Set up Firestore security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Admin can read/write all documents
    match /{document=**} {
      allow read, write: if request.auth != null && 
        get(/databases/$(database)/documents/admins/$(request.auth.uid)).data != null;
    }
    
    // Users can read their own data
    match /users/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### 3. Build Configuration

1. Open the project in Android Studio
2. Sync Gradle files
3. Build the project

### 4. Admin User Setup

1. Create an admin user in Firebase Authentication
2. Add the admin user to a special "admins" collection in Firestore:

```javascript
// In Firestore, create a document at: admins/{admin_uid}
{
  "email": "admin@example.com",
  "role": "admin",
  "createdAt": timestamp
}
```

## Project Structure

```
app/src/main/java/com/example/taskcommadmin/
├── data/
│   ├── model/           # Data models
│   └── repository/      # Data repositories
├── di/                  # Dependency injection
├── ui/
│   ├── navigation/      # Navigation components
│   ├── screen/          # UI screens
│   │   ├── auth/        # Authentication screens
│   │   ├── chat/        # Chat screens
│   │   ├── dashboard/   # Dashboard screen
│   │   ├── task/        # Task management screens
│   │   └── user/        # User management screens
│   ├── theme/           # UI theme
│   └── viewmodel/       # ViewModels
└── TaskCommAdminApplication.kt
```

## Data Models

### User
```kotlin
data class User(
    val userId: String,
    val name: String,
    val address: String,
    val businessField: String,
    val email: String,
    val createdAt: Timestamp,
    val isActive: Boolean
)
```

### Task
```kotlin
data class Task(
    val taskId: String,
    val instructionId: String,
    val adminId: String,
    val title: String,
    val description: String,
    val status: String, // pending, in_progress, completed
    val priority: String, // low, medium, high
    val dueDate: Timestamp?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
```

### ChatMessage
```kotlin
data class ChatMessage(
    val messageId: String,
    val taskId: String,
    val senderId: String,
    val senderRole: String, // user, admin
    val senderName: String,
    val text: String,
    val mediaUrl: String?,
    val fileType: String?, // image, document, text
    val fileName: String?,
    val fileSize: Long?,
    val timestamp: Timestamp,
    val isRead: Boolean
)
```

## Usage

### Login
1. Launch the app
2. Enter admin credentials
3. Tap "Sign In"

### User Management
1. Navigate to "User Management" from dashboard
2. View list of all users
3. Tap on a user to view/edit details
4. Use search to find specific users

### Task Management
1. From user details, tap "View Instructions"
2. Create new tasks for the instruction
3. Set priority and status
4. Edit or delete tasks as needed

### Communication
1. From task details, tap "Open Chat"
2. Send messages to the user
3. Attach files or images
4. View real-time updates

## Security Considerations

- All API calls use HTTPS
- Firebase security rules enforce access control
- Admin authentication required for all operations
- File uploads have size and type restrictions
- Sensitive data is encrypted

## Deployment

### Development
```bash
./gradlew assembleDebug
```

### Production
```bash
./gradlew assembleRelease
```

## Troubleshooting

### Common Issues

1. **Firebase connection failed**
   - Verify `google-services.json` is correctly configured
   - Check internet connectivity
   - Ensure Firebase project is active

2. **Authentication errors**
   - Verify admin user exists in Firebase Auth
   - Check admin document exists in Firestore
   - Ensure correct email/password

3. **Build errors**
   - Sync Gradle files
   - Clean and rebuild project
   - Check Kotlin and Compose versions compatibility

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

---

**Note**: This is the Admin App for the TaskComm system. Make sure the User App is also properly configured and connected to the same Firebase backend for full functionality.
# TaskCommAdmin
